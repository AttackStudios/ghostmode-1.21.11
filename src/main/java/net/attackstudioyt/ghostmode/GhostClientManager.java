package net.attackstudioyt.ghostmode;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class GhostClientManager {
    private static final Set<UUID> ghosts = Collections.synchronizedSet(new HashSet<>());
    /** Ghosts in this set are visible to other players (translucent). */
    private static final Set<UUID> visibleGhosts = Collections.synchronizedSet(new HashSet<>());
    private static boolean localPlayerIsGhost = false;

    public static void setGhost(UUID uuid, boolean isGhost) {
        if (isGhost) {
            ghosts.add(uuid);
            visibleGhosts.add(uuid); // default visible until told otherwise
        } else {
            ghosts.remove(uuid);
            visibleGhosts.remove(uuid);
        }
    }

    public static void setVisibility(UUID uuid, boolean visibleToOthers) {
        if (visibleToOthers) visibleGhosts.add(uuid);
        else visibleGhosts.remove(uuid);
    }

    public static boolean isGhost(UUID uuid) {
        return ghosts.contains(uuid);
    }

    /** True if this ghost is visible (translucent) to other players. */
    public static boolean isGhostVisible(UUID uuid) {
        return visibleGhosts.contains(uuid);
    }

    public static void setLocalGhost(boolean ghost) {
        localPlayerIsGhost = ghost;
    }

    public static boolean isLocalPlayerGhost() {
        return localPlayerIsGhost;
    }

    public static void clear() {
        ghosts.clear();
        visibleGhosts.clear();
        localPlayerIsGhost = false;
    }
}
