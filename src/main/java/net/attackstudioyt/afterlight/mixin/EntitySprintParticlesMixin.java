package net.attackstudioyt.afterlight.mixin;

import net.attackstudioyt.afterlight.GhostClientManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Suppress sprint dust particles under ghost players. Sprint particles are spawned per-client
 * in Entity#tick — every observing client computes them locally — so this mixin must be CLIENT
 * to silence what other players see.
 */
@Environment(EnvType.CLIENT)
@Mixin(Entity.class)
public class EntitySprintParticlesMixin {
    @Inject(method = "spawnSprintingParticles", at = @At("HEAD"), cancellable = true)
    private void afterlight$skipSprintParticles(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self instanceof PlayerEntity p && GhostClientManager.isGhost(p.getUuid())) {
            ci.cancel();
        }
    }
}
