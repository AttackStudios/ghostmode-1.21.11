package net.attackstudioyt.afterlight.mixin;

import net.attackstudioyt.afterlight.GhostClientManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Force the rendered health to 0 when the local player is a ghost, so the bar shows 10 empty
 * heart outlines. Override both lastHealth and health so the heart-blink diff stays at 0
 * (otherwise the renderer animates a constant drop from real health to 0 each tick).
 */
@Environment(EnvType.CLIENT)
@Mixin(InGameHud.class)
public class InGameHudHealthMixin {
    private static boolean afterlight$isLocalGhost() {
        var client = MinecraftClient.getInstance();
        return client.player != null && GhostClientManager.isGhost(client.player.getUuid());
    }

    @ModifyVariable(method = "renderHealthBar", at = @At("HEAD"), argsOnly = true, index = 8)
    private int afterlight$zeroLastHealth(int lastHealth) {
        return afterlight$isLocalGhost() ? 0 : lastHealth;
    }

    @ModifyVariable(method = "renderHealthBar", at = @At("HEAD"), argsOnly = true, index = 9)
    private int afterlight$zeroHealth(int health) {
        return afterlight$isLocalGhost() ? 0 : health;
    }
}
