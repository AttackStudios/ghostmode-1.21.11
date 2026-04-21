package net.attackstudioyt.ghostmode;

import net.attackstudioyt.ghostmode.item.RevivalBeaconItem;
import net.attackstudioyt.ghostmode.network.GhostStatePayload;
import net.attackstudioyt.ghostmode.network.GhostVisibilityPayload;
import net.attackstudioyt.ghostmode.network.RespawnPayload;
import net.attackstudioyt.ghostmode.network.RevivePlayerPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.rule.GameRules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GhostMod implements ModInitializer {
    public static final String MOD_ID = "ghostmode";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final RegistryKey<Item> REVIVAL_BEACON_KEY =
            RegistryKey.of(RegistryKeys.ITEM, Identifier.of(MOD_ID, "revival_beacon"));

    public static final RevivalBeaconItem REVIVAL_BEACON = Registry.register(
            Registries.ITEM, REVIVAL_BEACON_KEY,
            new RevivalBeaconItem(new Item.Settings().maxCount(1).registryKey(REVIVAL_BEACON_KEY)));

    /** Radius (blocks) for the wither-spawn kill sound played to nearby players on a player death. */
    private static final double KILL_SOUND_RADIUS = 240.0;
    private static final RegistryEntry<SoundEvent> KILL_SOUND_ENTRY =
            Registries.SOUND_EVENT.getEntry(SoundEvents.ENTITY_WITHER_SPAWN);

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playS2C().register(GhostStatePayload.ID, GhostStatePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(GhostVisibilityPayload.ID, GhostVisibilityPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(RespawnPayload.ID, RespawnPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(RevivePlayerPayload.ID, RevivePlayerPayload.CODEC);

        // Revive a ghost player via Revival Beacon
        ServerPlayNetworking.registerGlobalReceiver(RevivePlayerPayload.ID, (payload, context) -> {
            ServerPlayerEntity target = context.server().getPlayerManager().getPlayer(payload.targetUuid());
            if (target != null && GhostManager.isGhost(target.getUuid())) {
                exitGhostState(target);
            }
        });

        // Per-tick scrub: arrows, bee stingers and on-fire state can be applied AFTER
        // enterGhostState (e.g. the arrow that triggered the death stickifies post-damage,
        // and the LivingEntityDamageMixin cancel doesn't stop the projectile from sticking).
        // Cheap to do — only iterates if any ghosts exist.
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (GhostManager.getAllGhosts().isEmpty()) return;
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                if (!GhostManager.isGhost(p.getUuid())) continue;
                if (p.getStuckArrowCount() != 0) p.setStuckArrowCount(0);
                if (p.getStingerCount() != 0) p.setStingerCount(0);
                if (p.isOnFire()) p.extinguish();
            }
        });

        // On death → become a ghost instead. Broadcast a red death message and play the
        // kill sound to nearby players. Vanilla suppresses its own death message because
        // we cancel the death (return false), so we send our own.
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayerEntity player && !GhostManager.isGhost(player.getUuid())) {
                Text msg = Text.literal(player.getName().getString() + " has been killed!")
                        .styled(s -> s.withColor(Formatting.RED));
                ((ServerWorld) player.getEntityWorld()).getServer().getPlayerManager().broadcast(msg, false);
                playKillSoundNearby(player);
                enterGhostState(player, source);
                return false;
            }
            return true;
        });

        // Client pressed R → respawn
        ServerPlayNetworking.registerGlobalReceiver(RespawnPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            if (GhostManager.isGhost(player.getUuid())) {
                exitGhostState(player);
            }
        });

        // Send existing ghost/visibility states to newly joined players; restore if they relogged as ghost
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity newPlayer = handler.getPlayer();
            java.util.UUID uuid = newPlayer.getUuid();

            if (GhostPersistence.contains(uuid) && !GhostManager.isGhost(uuid)) {
                // Restore ghost state if player was a ghost before relog/restart
                restoreGhostState(newPlayer, server);
            } else if (!GhostPersistence.contains(uuid)) {
                // Defensive: strip any leftover ghost-style INVISIBILITY (very-long duration)
                // that vanilla restored from NBT but our persistence no longer recognises.
                // Without this, exiting ghost state right before disconnecting could leave a
                // stale infinite-INVIS in the player's save file.
                StatusEffectInstance existing = newPlayer.getStatusEffect(StatusEffects.INVISIBILITY);
                if (existing != null && (existing.isInfinite() || existing.getDuration() >= 1_000_000)) {
                    newPlayer.removeStatusEffect(StatusEffects.INVISIBILITY);
                }
            }

            // Sync all current ghost states to this joining player
            for (java.util.UUID ghostUuid : GhostManager.getAllGhosts()) {
                ServerPlayNetworking.send(newPlayer, new GhostStatePayload(ghostUuid, true));
                ServerPlayNetworking.send(newPlayer, new GhostVisibilityPayload(ghostUuid, GhostManager.isVisibleToOthers(ghostUuid)));
            }
        });

        // Clean up on disconnect
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            java.util.UUID uuid = handler.getPlayer().getUuid();
            if (GhostManager.isGhost(uuid)) {
                GhostManager.removeLocal(uuid);
                GhostStatePayload payload = new GhostStatePayload(uuid, false);
                for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                    ServerPlayNetworking.send(p, payload);
                }
            }
        });

        // Block breaking
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) ->
                !GhostManager.isGhost(player.getUuid()));

        // Right-click block
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (GhostManager.isGhost(player.getUuid())) return ActionResult.FAIL;
            return ActionResult.PASS;
        });

        // Use item in hand — ghosts can't use items, EXCEPT the Revival Beacon
        // (beacon opens a client-side screen; without this exemption the client's
        // UseItemCallback in GhostClient never runs because FAIL short-circuits the event).
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (GhostManager.isGhost(player.getUuid())) {
                if (player.getStackInHand(hand).isOf(REVIVAL_BEACON)) return ActionResult.PASS;
                return ActionResult.FAIL;
            }
            return ActionResult.PASS;
        });

        // Punch entity
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (GhostManager.isGhost(player.getUuid())) return ActionResult.FAIL;
            return ActionResult.PASS;
        });

        // Punch / start mining block
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (GhostManager.isGhost(player.getUuid())) return ActionResult.FAIL;
            return ActionResult.PASS;
        });

        // /ghostmode — OP commands. The .requires(ADMINS_CHECK) on the root literal gates
        // every nested subcommand below, so /ghostmode mode and /ghostmode default are OP-only too.
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("ghostmode")
                .requires(CommandManager.requirePermissionLevel(CommandManager.ADMINS_CHECK))
                // /ghostmode — toggle self
                .executes(ctx -> {
                    ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                    return toggleGhost(player);
                })
                // /ghostmode <player> — toggle another player
                .then(CommandManager.argument("player", EntityArgumentType.player())
                    .executes(ctx -> {
                        ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");
                        int result = toggleGhost(target);
                        ctx.getSource().sendFeedback(() -> Text.literal(
                            (GhostManager.isGhost(target.getUuid()) ? "§7" : "§a") +
                            target.getName().getString() +
                            (GhostManager.isGhost(target.getUuid()) ? " is now a ghost." : " is no longer a ghost.")), true);
                        return result;
                    })
                )
                // /ghostmode visible — toggle own visibility
                .then(CommandManager.literal("visible")
                    .executes(ctx -> {
                        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                        return toggleVisibility(player, ctx);
                    })
                    .then(CommandManager.argument("player", EntityArgumentType.player())
                        .executes(ctx -> {
                            ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");
                            return toggleVisibility(target, ctx);
                        })
                    )
                )
                // /ghostmode mode <transparent|invisible> [player] — set per-player death-form preference
                .then(CommandManager.literal("mode")
                    .then(CommandManager.argument("form", com.mojang.brigadier.arguments.StringArgumentType.word())
                        .suggests((c, b) -> { b.suggest("transparent"); b.suggest("invisible"); return b.buildFuture(); })
                        .executes(ctx -> {
                            ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                            return setMode(player, com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "form"), ctx);
                        })
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                            .executes(ctx -> {
                                ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");
                                return setMode(target, com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "form"), ctx);
                            })
                        )
                    )
                )
                // /ghostmode default <transparent|invisible> — set server-wide default death form
                .then(CommandManager.literal("default")
                    .then(CommandManager.argument("form", com.mojang.brigadier.arguments.StringArgumentType.word())
                        .suggests((c, b) -> { b.suggest("transparent"); b.suggest("invisible"); return b.buildFuture(); })
                        .executes(ctx -> setDefault(com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "form"), ctx))
                    )
                )
            );
        });

        LOGGER.info("Ghost Mode initialised.");
    }

    /** Play the wither-spawn kill sound to every player within KILL_SOUND_RADIUS of the dying player. */
    private static void playKillSoundNearby(ServerPlayerEntity dying) {
        ServerWorld world = (ServerWorld) dying.getEntityWorld();
        double r2 = KILL_SOUND_RADIUS * KILL_SOUND_RADIUS;
        double x = dying.getX(), y = dying.getY(), z = dying.getZ();
        long seed = world.getRandom().nextLong();
        for (ServerPlayerEntity p : world.getPlayers()) {
            if (p.squaredDistanceTo(x, y, z) <= r2) {
                // Send the packet directly so the audible range is exactly KILL_SOUND_RADIUS,
                // not the volume-attenuated range of World#playSound (≈16 * volume blocks).
                p.networkHandler.sendPacket(new PlaySoundS2CPacket(
                        KILL_SOUND_ENTRY, SoundCategory.HOSTILE,
                        x, y, z, 1.0f, 1.0f, seed));
            }
        }
    }

    /** Restore ghost state for a player who relogged while a ghost (no item/XP drop). */
    private static void restoreGhostState(ServerPlayerEntity player, net.minecraft.server.MinecraftServer server) {
        player.setHealth(20.0f);
        player.getHungerManager().setFoodLevel(20);
        player.getHungerManager().setSaturationLevel(5.0f);
        player.setStuckArrowCount(0);
        player.setStingerCount(0);
        player.extinguish();
        // Strip vanilla-restored INVISIBILITY first so the re-applied instance has guaranteed
        // showParticles=false / showIcon=false flags — NBT round-trip can drop them otherwise,
        // leaving the ∞-duration icon visible in the HUD after relog.
        player.removeStatusEffect(StatusEffects.INVISIBILITY);
        player.addStatusEffect(new StatusEffectInstance(
                StatusEffects.INVISIBILITY, Integer.MAX_VALUE, 0, false, false, false));
        GhostManager.addGhost(server, player.getUuid(), GhostConfig.get().getFormFor(player.getUuid()));
        LOGGER.info("[GhostMode] Restored ghost state for {}", player.getName().getString());
    }

    private static int toggleGhost(ServerPlayerEntity player) {
        if (GhostManager.isGhost(player.getUuid())) {
            exitGhostState(player);
        } else {
            enterGhostState(player, null);
        }
        return 1;
    }

    private static int setMode(ServerPlayerEntity target, String formArg,
                               com.mojang.brigadier.context.CommandContext<net.minecraft.server.command.ServerCommandSource> ctx) {
        DeathForm form = DeathForm.parse(formArg);
        if (form == null) {
            ctx.getSource().sendFeedback(() -> Text.literal("§cUnknown form '" + formArg + "'. Use transparent or invisible."), false);
            return 0;
        }
        GhostConfig.get().setFormFor(target.getUuid(), form);
        // Q2: if target is currently a ghost, re-apply visibility instantly so the change is live.
        if (GhostManager.isGhost(target.getUuid())) {
            ServerWorld world = (ServerWorld) target.getEntityWorld();
            GhostManager.setVisibility(world.getServer(), target.getUuid(), form == DeathForm.TRANSPARENT);
        }
        ctx.getSource().sendFeedback(() -> Text.literal(
            target.getName().getString() + "'s death form is now §b" + form.name().toLowerCase() + "§r."), true);
        return 1;
    }

    private static int setDefault(String formArg,
                                  com.mojang.brigadier.context.CommandContext<net.minecraft.server.command.ServerCommandSource> ctx) {
        DeathForm form = DeathForm.parse(formArg);
        if (form == null) {
            ctx.getSource().sendFeedback(() -> Text.literal("§cUnknown form '" + formArg + "'. Use transparent or invisible."), false);
            return 0;
        }
        GhostConfig.get().setDefaultDeathForm(form);
        ctx.getSource().sendFeedback(() -> Text.literal(
            "§7Server-wide default death form set to §b" + form.name().toLowerCase() + "§r."), true);
        return 1;
    }

    private static int toggleVisibility(ServerPlayerEntity target, com.mojang.brigadier.context.CommandContext<net.minecraft.server.command.ServerCommandSource> ctx) {
        if (!GhostManager.isGhost(target.getUuid())) {
            ctx.getSource().sendFeedback(() -> Text.literal("§c" + target.getName().getString() + " is not a ghost."), false);
            return 0;
        }
        ServerWorld world = (ServerWorld) target.getEntityWorld();
        boolean nowVisible = GhostManager.toggleVisibility(world.getServer(), target.getUuid());
        ctx.getSource().sendFeedback(() -> Text.literal(
            target.getName().getString() + " is now " + (nowVisible ? "§avisible§r (translucent)" : "§7invisible§r to others.")), true);
        return 1;
    }

    private static void enterGhostState(ServerPlayerEntity player, net.minecraft.entity.damage.DamageSource source) {
        ServerWorld serverWorld = (ServerWorld) player.getEntityWorld();

        // Drop inventory items with random trajectories (like vanilla death)
        var inv = player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            if (!stack.isEmpty()) {
                player.dropItem(stack, true, false);
                inv.setStack(i, ItemStack.EMPTY);
            }
        }

        // Drop XP (unless keepInventory — which covers XP too in vanilla)
        if (!serverWorld.getGameRules().getValue(GameRules.KEEP_INVENTORY)) {
            int xp = player.totalExperience;
            if (xp > 0) {
                ExperienceOrbEntity.spawn(serverWorld, new Vec3d(player.getX(), player.getY(), player.getZ()), xp);
            }
            player.totalExperience = 0;
            player.experienceLevel = 0;
            player.experienceProgress = 0.0f;
        }

        // Restore health and hunger so they function while a ghost
        player.setHealth(20.0f);
        player.getHungerManager().setFoodLevel(20);
        player.getHungerManager().setSaturationLevel(5.0f);

        // Clear arrows/stingers stuck in the body — ghost died, no more projectiles showing
        player.setStuckArrowCount(0);
        player.setStingerCount(0);
        player.extinguish();

        // INVISIBILITY → renders at ~15% opacity for all players (ghost look)
        player.addStatusEffect(new StatusEffectInstance(
                StatusEffects.INVISIBILITY, Integer.MAX_VALUE, 0, false, false, false));

        // Clear mob targets within 64 blocks so they immediately stop pursuing
        Box searchBox = player.getBoundingBox().expand(64);
        serverWorld.getEntitiesByClass(MobEntity.class, searchBox,
                mob -> mob.getTarget() != null && mob.getTarget().getUuid().equals(player.getUuid()))
                .forEach(mob -> mob.setTarget(null));

        // Persist ghost state so it survives relogs/restarts
        GhostPersistence.add(player.getUuid());

        // Register ghost — broadcasts to all clients. Form respects per-player preference
        // (set via /ghostmode mode), falling back to server-wide default in GhostConfig.
        GhostManager.addGhost(serverWorld.getServer(), player.getUuid(),
                GhostConfig.get().getFormFor(player.getUuid()));
    }

    public static void exitGhostState(ServerPlayerEntity player) {
        ServerWorld serverWorld = (ServerWorld) player.getEntityWorld();
        GhostPersistence.remove(player.getUuid()); // remove before broadcast so clients get clean state
        GhostManager.removeGhost(serverWorld.getServer(), player.getUuid());
        player.removeStatusEffect(StatusEffects.INVISIBILITY);
        player.setHealth(player.getMaxHealth());
        player.getHungerManager().setFoodLevel(20);
        player.getHungerManager().setSaturationLevel(5.0f);
    }
}
