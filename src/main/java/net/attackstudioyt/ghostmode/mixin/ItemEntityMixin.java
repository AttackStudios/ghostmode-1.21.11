package net.attackstudioyt.ghostmode.mixin;

import net.attackstudioyt.ghostmode.GhostManager;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public class ItemEntityMixin {
    @Inject(method = "onPlayerCollision", at = @At("HEAD"), cancellable = true)
    private void preventGhostPickup(PlayerEntity player, CallbackInfo ci) {
        if (GhostManager.isGhost(player.getUuid())) {
            ci.cancel();
        }
    }
}
