package net.attackstudioyt.afterlight.mixin;

import net.attackstudioyt.afterlight.GhostManager;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Prevents ghost players from right-clicking entities. */
@Mixin(ServerPlayNetworkHandler.class)
public class ServerPlayNetworkHandlerMixin {
    @Shadow public ServerPlayerEntity player;

    @Inject(method = "onPlayerInteractEntity", at = @At("HEAD"), cancellable = true)
    private void preventGhostEntityInteract(PlayerInteractEntityC2SPacket packet, CallbackInfo ci) {
        if (GhostManager.isGhost(player.getUuid())) {
            ci.cancel();
        }
    }
}
