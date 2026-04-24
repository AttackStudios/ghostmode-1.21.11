package net.attackstudioyt.afterlight;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Persists ghost UUIDs to disk so ghost state survives server restarts and player relogs.
 * File: <game_dir>/afterlight_ghosts.dat (one UUID per line)
 */
public final class AfterlightPersistence {
    private AfterlightPersistence() {}

    private static Path file() {
        return FabricLoader.getInstance().getGameDir().resolve("afterlight_ghosts.dat");
    }

    public static Set<UUID> load() {
        Set<UUID> set = new HashSet<>();
        Path f = file();
        if (!Files.exists(f)) return set;
        try {
            for (String line : Files.readAllLines(f)) {
                line = line.trim();
                if (!line.isEmpty()) {
                    try { set.add(UUID.fromString(line)); } catch (IllegalArgumentException ignored) {}
                }
            }
        } catch (IOException e) {
            AfterlightMod.LOGGER.error("[Afterlight] Failed to load ghost persistence: {}", e.getMessage());
        }
        return set;
    }

    public static void add(UUID uuid) {
        Set<UUID> set = load();
        set.add(uuid);
        save(set);
    }

    public static void remove(UUID uuid) {
        Set<UUID> set = load();
        if (set.remove(uuid)) save(set);
    }

    public static boolean contains(UUID uuid) {
        return load().contains(uuid);
    }

    private static void save(Set<UUID> set) {
        try {
            Files.writeString(file(), set.stream()
                    .map(UUID::toString)
                    .reduce("", (a, b) -> a.isEmpty() ? b : a + "\n" + b));
        } catch (IOException e) {
            AfterlightMod.LOGGER.error("[Afterlight] Failed to save ghost persistence: {}", e.getMessage());
        }
    }
}
