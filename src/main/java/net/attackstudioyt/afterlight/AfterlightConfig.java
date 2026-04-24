package net.attackstudioyt.afterlight;

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
 * Server-side Afterlight configuration: world-wide default death form plus per-player overrides.
 * File: <game_dir>/afterlight_config.json.
 */
public final class AfterlightConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Object LOCK = new Object();
    private static AfterlightConfig instance;

    private DeathForm defaultDeathForm = DeathForm.TRANSPARENT;
    private final Map<String, DeathForm> playerForms = new HashMap<>();

    private AfterlightConfig() {}

    public static AfterlightConfig get() {
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
        return FabricLoader.getInstance().getGameDir().resolve("afterlight_config.json");
    }

    private static AfterlightConfig load() {
        Path f = file();
        if (!Files.exists(f)) return new AfterlightConfig();
        try {
            String json = Files.readString(f);
            AfterlightConfig cfg = GSON.fromJson(json, AfterlightConfig.class);
            if (cfg == null) return new AfterlightConfig();
            if (cfg.defaultDeathForm == null) cfg.defaultDeathForm = DeathForm.TRANSPARENT;
            return cfg;
        } catch (IOException | JsonSyntaxException e) {
            AfterlightMod.LOGGER.error("[Afterlight] Failed to load config: {}", e.getMessage());
            return new AfterlightConfig();
        }
    }

    private void save() {
        try {
            Files.writeString(file(), GSON.toJson(this));
        } catch (IOException e) {
            AfterlightMod.LOGGER.error("[Afterlight] Failed to save config: {}", e.getMessage());
        }
    }
}
