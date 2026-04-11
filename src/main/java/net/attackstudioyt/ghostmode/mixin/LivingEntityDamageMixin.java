package net.attackstudioyt.ghostmode.mixin;

import net.attackstudioyt.ghostmode.GhostManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Ghosts take no damage — they cannot die again. */
@Mixin(LivingEntity.class)
public class LivingEntityDamageMixin {
    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void preventGhostDamage(ServerWorld world, DamageSource source, float amount,
                                    CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof ServerPlayerEntity player && GhostManager.isGhost(player.getUuid())) {
            cir.setReturnValue(false);
        }
    }
}
