package net.attackstudioyt.ghostmode.mixin;

import net.attackstudioyt.ghostmode.GhostClientManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes ghost players always visible to all observers (rendered at ~15% alpha
 * by vanilla's invisible-entity path, giving a ghostly appearance).
 */
@Environment(EnvType.CLIENT)
@Mixin(Entity.class)
public class EntityIsInvisibleToMixin {
    @Inject(method = "isInvisibleTo", at = @At("HEAD"), cancellable = true)
    private void ghostAlwaysVisible(PlayerEntity observer, CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (self instanceof PlayerEntity ghost && GhostClientManager.isGhost(ghost.getUuid())) {
            // Return false = "not invisible to this observer"
            // Entity still has INVISIBILITY so renderer uses translucent path (~15% alpha)
            cir.setReturnValue(false);
        }
    }
}
