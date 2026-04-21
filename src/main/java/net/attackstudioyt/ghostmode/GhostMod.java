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
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
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

        // On death → become a ghost instead
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayerEntity player && !GhostManager.isGhost(player.getUuid())) {
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

            // Restore ghost state if player was a ghost before relog/restart
            if (GhostPersistence.contains(uuid) && !GhostManager.isGhost(uuid)) {
                restoreGhostState(newPlayer, server);
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

        // /ghostmode — OP commands
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
            );
        });

        LOGGER.info("Ghost Mode initialised.");
    }

    /** Restore ghost state for a player who relogged while a ghost (no item/XP drop). */
    private static void restoreGhostState(ServerPlayerEntity player, net.minecraft.server.MinecraftServer server) {
        player.setHealth(20.0f);
        player.getHungerManager().setFoodLevel(20);
        player.getHungerManager().setSaturationLevel(5.0f);
        player.setStuckArrowCount(0);
        player.setStingerCount(0);
        player.extinguish();
        player.addStatusEffect(new StatusEffectInstance(
                StatusEffects.INVISIBILITY, Integer.MAX_VALUE, 0, false, false, false));
        GhostManager.addGhost(server, player.getUuid());
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

        // Register ghost — broadcasts to all clients
        GhostManager.addGhost(serverWorld.getServer(), player.getUuid());
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
