package net.attackstudioyt.afterlight.mixin;

import net.attackstudioyt.afterlight.GhostManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Prevents ghost players from attracting or picking up experience orbs.
 */
@Mixin(ExperienceOrbEntity.class)
public class ExperienceOrbMixin {

    @Shadow private PlayerEntity target;

    /**
     * If the orb's current target is a ghost, clear it before moveTowardsPlayer runs
     * so the orb doesn't continue moving toward them.
     */
    @Inject(method = "moveTowardsPlayer", at = @At("HEAD"))
    private void clearGhostTarget(CallbackInfo ci) {
        if (target instanceof ServerPlayerEntity player && GhostManager.isGhost(player.getUuid())) {
            target = null;
        }
    }

    /**
     * Redirect the getClosestPlayer call inside moveTowardsPlayer so that ghost
     * players are never chosen as a new target.
     */
    @Redirect(method = "moveTowardsPlayer",
              at = @At(value = "INVOKE",
                       target = "Lnet/minecraft/world/World;getClosestPlayer(Lnet/minecraft/entity/Entity;D)Lnet/minecraft/entity/player/PlayerEntity;"))
    private PlayerEntity filterGhostFromClosestPlayer(World world, Entity entity, double range) {
        PlayerEntity player = world.getClosestPlayer(entity, range);
        if (player instanceof ServerPlayerEntity sp && GhostManager.isGhost(sp.getUuid())) {
            return null;
        }
        return player;
    }

    /** Block the actual XP pickup at collision. */
    @Inject(method = "onPlayerCollision", at = @At("HEAD"), cancellable = true)
    private void preventGhostXpPickup(PlayerEntity player, CallbackInfo ci) {
        if (GhostManager.isGhost(player.getUuid())) {
            ci.cancel();
        }
    }
}
