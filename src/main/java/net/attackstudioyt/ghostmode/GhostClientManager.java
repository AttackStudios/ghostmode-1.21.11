package net.attackstudioyt.ghostmode;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class GhostClientManager {
    private static final Set<UUID> ghosts = Collections.synchronizedSet(new HashSet<>());
    private static boolean localPlayerIsGhost = false;

    public static void setGhost(UUID uuid, boolean isGhost) {
        if (isGhost) ghosts.add(uuid);
        else ghosts.remove(uuid);
    }

    public static boolean isGhost(UUID uuid) {
        return ghosts.contains(uuid);
    }

    public static void setLocalGhost(boolean ghost) {
        localPlayerIsGhost = ghost;
    }

    public static boolean isLocalPlayerGhost() {
        return localPlayerIsGhost;
    }

    public static void clear() {
        ghosts.clear();
        localPlayerIsGhost = false;
    }
}
