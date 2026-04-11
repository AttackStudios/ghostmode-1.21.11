package net.attackstudioyt.ghostmode.mixin;

import net.attackstudioyt.ghostmode.GhostManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Prevents mobs from targeting or keeping a ghost player as their target. */
@Mixin(MobEntity.class)
public class MobEntityMixin {

    /** Block ghosts from being set as a mob's target. */
    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void cancelGhostTarget(LivingEntity target, CallbackInfo ci) {
        if (target instanceof ServerPlayerEntity player && GhostManager.isGhost(player.getUuid())) {
            ci.cancel();
        }
    }

    /** If a mob already has a ghost as its target, return null so goal AI sees no target. */
    @Inject(method = "getTarget", at = @At("RETURN"), cancellable = true)
    private void nullifyGhostTarget(CallbackInfoReturnable<LivingEntity> cir) {
        LivingEntity target = cir.getReturnValue();
        if (target instanceof ServerPlayerEntity player && GhostManager.isGhost(player.getUuid())) {
            cir.setReturnValue(null);
        }
    }
}
