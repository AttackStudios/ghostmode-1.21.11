package net.attackstudioyt.ghostmode;

import net.attackstudioyt.ghostmode.network.GhostStatePayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class GhostManager {
    private static final Set<UUID> ghosts = Collections.synchronizedSet(new HashSet<>());

    public static boolean isGhost(UUID uuid) {
        return ghosts.contains(uuid);
    }

    public static Set<UUID> getAllGhosts() {
        return Collections.unmodifiableSet(ghosts);
    }

    public static void addGhost(MinecraftServer server, UUID uuid) {
        ghosts.add(uuid);
        broadcast(server, uuid, true);
    }

    public static void removeGhost(MinecraftServer server, UUID uuid) {
        ghosts.remove(uuid);
        broadcast(server, uuid, false);
    }

    /** Remove without broadcasting (e.g. on disconnect). */
    public static void removeLocal(UUID uuid) {
        ghosts.remove(uuid);
    }

    private static void broadcast(MinecraftServer server, UUID uuid, boolean isGhost) {
        GhostStatePayload payload = new GhostStatePayload(uuid, isGhost);
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(player, payload);
        }
    }
}
