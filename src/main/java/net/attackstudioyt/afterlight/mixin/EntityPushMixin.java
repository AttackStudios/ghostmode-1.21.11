package net.attackstudioyt.afterlight.mixin;

import net.attackstudioyt.afterlight.GhostClientManager;
import net.attackstudioyt.afterlight.GhostManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/**
 * Ghosts pass through everything: cancel Entity#pushAwayFrom when either side is a ghost.
 * Common (both sides) — server is authoritative but the client-side cancel keeps local
 * collision prediction from briefly shoving entities before the server snaps them back.
 */
@Mixin(Entity.class)
public class EntityPushMixin {
    @Inject(method = "pushAwayFrom", at = @At("HEAD"), cancellable = true)
    private void afterlight$noPushForGhosts(Entity entity, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (isGhostPlayer(self) || isGhostPlayer(entity)) ci.cancel();
    }

    private static boolean isGhostPlayer(Entity e) {
        if (!(e instanceof PlayerEntity p)) return false;
        UUID uuid = p.getUuid();
        // Check both sides — GhostManager is populated on the server,
        // GhostClientManager mirrors via packets on the client.
        return GhostManager.isGhost(uuid) || GhostClientManager.isGhost(uuid);
    }
}
