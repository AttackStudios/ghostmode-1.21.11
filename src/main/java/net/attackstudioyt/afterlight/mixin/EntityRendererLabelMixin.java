package net.attackstudioyt.afterlight.mixin;

import net.attackstudioyt.afterlight.GhostClientManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

/**
 * Prefix nametags of transparent ghosts with "[Ghost]" so observers can see who's a ghost.
 * Only applies to TRANSPARENT ghosts — invisible ones aren't rendered at all to others
 * (vanilla suppresses nametags of INVISIBILITY-affected entities outside the same team).
 */
@Environment(EnvType.CLIENT)
@Mixin(EntityRenderer.class)
public abstract class EntityRendererLabelMixin {
    @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true)
    private void afterlight$prefixGhostName(Entity entity, CallbackInfoReturnable<Text> cir) {
        if (!(entity instanceof PlayerEntity p)) return;
        UUID uuid = p.getUuid();
        if (!GhostClientManager.isGhost(uuid)) return;
        if (!GhostClientManager.isGhostVisible(uuid)) return;
        Text orig = cir.getReturnValue();
        if (orig == null) return;
        cir.setReturnValue(Text.literal("§7[Ghost] §r").append(orig));
    }
}
