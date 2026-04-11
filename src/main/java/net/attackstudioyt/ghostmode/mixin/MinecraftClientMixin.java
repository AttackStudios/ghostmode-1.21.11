package net.attackstudioyt.ghostmode.mixin;

import net.attackstudioyt.ghostmode.GhostClientManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Suppresses the death screen when the local player is in ghost mode.
 */
@Environment(EnvType.CLIENT)
@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void suppressDeathScreen(Screen screen, CallbackInfo ci) {
        if (screen instanceof DeathScreen && GhostClientManager.isLocalPlayerGhost()) {
            ci.cancel();
        }
    }
}
