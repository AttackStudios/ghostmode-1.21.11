package net.attackstudioyt.ghostmode.mixin;

import net.attackstudioyt.ghostmode.GhostManager;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Prevents ghost players from picking up experience orbs. */
@Mixin(ExperienceOrbEntity.class)
public class ExperienceOrbMixin {
    @Inject(method = "onPlayerCollision", at = @At("HEAD"), cancellable = true)
    private void preventGhostXpPickup(PlayerEntity player, CallbackInfo ci) {
        if (GhostManager.isGhost(player.getUuid())) {
            ci.cancel();
        }
    }
}
