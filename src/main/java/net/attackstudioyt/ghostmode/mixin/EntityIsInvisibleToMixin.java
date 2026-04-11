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
 * Controls ghost player visibility to other players.
 * - Visible mode (default): returns false ("not invisible") so the entity renders at ~15% alpha
 *   via vanilla's translucent invisible-entity path.
 * - Hidden mode: lets INVISIBILITY take full effect → completely unseen by others.
 */
@Environment(EnvType.CLIENT)
@Mixin(Entity.class)
public class EntityIsInvisibleToMixin {
    @Inject(method = "isInvisibleTo", at = @At("HEAD"), cancellable = true)
    private void ghostVisibility(PlayerEntity observer, CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (self instanceof PlayerEntity ghost && GhostClientManager.isGhost(ghost.getUuid())) {
            if (GhostClientManager.isGhostVisible(ghost.getUuid())) {
                // Visible to others: override invisibility → renders translucent (~15% alpha)
                cir.setReturnValue(false);
            }
            // Hidden: don't override → INVISIBILITY makes them completely invisible
        }
    }
}
