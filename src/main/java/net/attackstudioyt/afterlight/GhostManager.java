package net.attackstudioyt.afterlight;

import net.attackstudioyt.afterlight.network.GhostStatePayload;
import net.attackstudioyt.afterlight.network.GhostVisibilityPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class GhostManager {
    private static final Set<UUID> ghosts = Collections.synchronizedSet(new HashSet<>());
    /** Ghosts in this set are visible to other players (translucent). Default: all ghosts visible. */
    private static final Set<UUID> visibleGhosts = Collections.synchronizedSet(new HashSet<>());

    public static boolean isGhost(UUID uuid) {
        return ghosts.contains(uuid);
    }

    /** True if this ghost should render as translucent to other players; false = fully hidden. */
    public static boolean isVisibleToOthers(UUID uuid) {
        return visibleGhosts.contains(uuid);
    }

    public static Set<UUID> getAllGhosts() {
        return Collections.unmodifiableSet(ghosts);
    }

    public static void addGhost(MinecraftServer server, UUID uuid) {
        addGhost(server, uuid, DeathForm.TRANSPARENT);
    }

    public static void addGhost(MinecraftServer server, UUID uuid, DeathForm form) {
        ghosts.add(uuid);
        boolean visible = form == DeathForm.TRANSPARENT;
        if (visible) visibleGhosts.add(uuid); else visibleGhosts.remove(uuid);
        broadcastState(server, uuid, true);
        broadcastVisibility(server, uuid, visible);
    }

    public static void removeGhost(MinecraftServer server, UUID uuid) {
        ghosts.remove(uuid);
        visibleGhosts.remove(uuid);
        broadcastState(server, uuid, false);
    }

    /** Toggle visibility for a ghost. Returns the new visibility state. */
    public static boolean toggleVisibility(MinecraftServer server, UUID uuid) {
        boolean nowVisible = !visibleGhosts.contains(uuid);
        setVisibility(server, uuid, nowVisible);
        return nowVisible;
    }

    /** Set visibility for a ghost explicitly. Broadcasts to all players. */
    public static void setVisibility(MinecraftServer server, UUID uuid, boolean visible) {
        if (visible) visibleGhosts.add(uuid); else visibleGhosts.remove(uuid);
        broadcastVisibility(server, uuid, visible);
    }

    /** Remove without broadcasting (e.g. on disconnect). */
    public static void removeLocal(UUID uuid) {
        ghosts.remove(uuid);
        visibleGhosts.remove(uuid);
    }

    private static void broadcastState(MinecraftServer server, UUID uuid, boolean isGhost) {
        GhostStatePayload payload = new GhostStatePayload(uuid, isGhost);
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(player, payload);
        }
    }

    private static void broadcastVisibility(MinecraftServer server, UUID uuid, boolean visibleToOthers) {
        GhostVisibilityPayload payload = new GhostVisibilityPayload(uuid, visibleToOthers);
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(player, payload);
        }
    }
}
