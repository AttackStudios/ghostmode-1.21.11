package net.attackstudioyt.ghostmode;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Server-side GhostMode configuration: world-wide default death form plus per-player overrides.
 * File: <game_dir>/ghostmode_config.json.
 */
public final class GhostConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Object LOCK = new Object();
    private static GhostConfig instance;

    private DeathForm defaultDeathForm = DeathForm.TRANSPARENT;
    private final Map<String, DeathForm> playerForms = new HashMap<>();

    private GhostConfig() {}

    public static GhostConfig get() {
        synchronized (LOCK) {
            if (instance == null) instance = load();
            return instance;
        }
    }

    public DeathForm getDefaultDeathForm() {
        return defaultDeathForm;
    }

    public void setDefaultDeathForm(DeathForm form) {
        synchronized (LOCK) {
            this.defaultDeathForm = form;
            save();
        }
    }

    /** Returns the player's chosen death form, or the server default if none set. */
    public DeathForm getFormFor(UUID uuid) {
        DeathForm f = playerForms.get(uuid.toString());
        return f != null ? f : defaultDeathForm;
    }

    public boolean hasExplicitForm(UUID uuid) {
        return playerForms.containsKey(uuid.toString());
    }

    public void setFormFor(UUID uuid, DeathForm form) {
        synchronized (LOCK) {
            playerForms.put(uuid.toString(), form);
            save();
        }
    }

    private static Path file() {
        return FabricLoader.getInstance().getGameDir().resolve("ghostmode_config.json");
    }

    private static GhostConfig load() {
        Path f = file();
        if (!Files.exists(f)) return new GhostConfig();
        try {
            String json = Files.readString(f);
            GhostConfig cfg = GSON.fromJson(json, GhostConfig.class);
            if (cfg == null) return new GhostConfig();
            if (cfg.defaultDeathForm == null) cfg.defaultDeathForm = DeathForm.TRANSPARENT;
            return cfg;
        } catch (IOException | JsonSyntaxException e) {
            GhostMod.LOGGER.error("[GhostMode] Failed to load config: {}", e.getMessage());
            return new GhostConfig();
        }
    }

    private void save() {
        try {
            Files.writeString(file(), GSON.toJson(this));
        } catch (IOException e) {
            GhostMod.LOGGER.error("[GhostMode] Failed to save config: {}", e.getMessage());
        }
    }
}
