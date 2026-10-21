package dev.veil.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.veil.Veil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Flat JSON store: module -> enabled, key, settings. Deliberately schema-less so adding a
 * module never invalidates an existing config file.
 */
public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path path;

    public ConfigManager(Path directory) {
        this.path = directory.resolve("veil.json");
    }

    public void load(ModuleManager modules) {
        if (!Files.exists(path)) {
            return;
        }
        try {
            JsonObject root = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            JsonObject moduleRoot = root.getAsJsonObject("modules");
            if (moduleRoot == null) {
                return;
            }
            for (Module module : modules.all()) {
                JsonObject entry = moduleRoot.getAsJsonObject(module.name());
                if (entry == null) {
                    continue;
                }
                if (entry.has("enabled")) {
                    module.setEnabled(entry.get("enabled").getAsBoolean());
                }
                if (entry.has("key")) {
                    module.setKey(entry.get("key").getAsInt());
                }
                JsonObject settings = entry.getAsJsonObject("settings");
                if (settings == null) {
                    continue;
                }
                for (Setting<?> setting : module.settings()) {
                    JsonElement value = settings.get(setting.name());
                    if (value != null) {
                        readSetting(setting, value);
                    }
                }
            }
        } catch (IOException | RuntimeException exception) {
            Veil.LOGGER.warn("Could not read config", exception);
        }
    }

    @SuppressWarnings("unchecked")
    private void readSetting(Setting<?> setting, JsonElement value) {
        if (setting instanceof BoolSetting bool) {
            bool.setValue(value.getAsBoolean());
        } else if (setting instanceof NumberSetting number) {
            number.setValue(value.getAsDouble());
        } else if (setting instanceof ModeSetting mode) {
            mode.setValue(value.getAsString());
        } else {
            ((Setting<Object>) setting).setValue(value.getAsString());
        }
    }

    public void save(ModuleManager modules) {
        JsonObject root = new JsonObject();
        JsonObject moduleRoot = new JsonObject();

        for (Module module : modules.all()) {
            JsonObject entry = new JsonObject();
            entry.addProperty("enabled", module.isEnabled());
            entry.addProperty("key", module.getKey());
            JsonObject settings = new JsonObject();
            for (Setting<?> setting : module.settings()) {
                if (setting.value() instanceof Number number) {
                    settings.addProperty(setting.name(), number);
                } else if (setting.value() instanceof Boolean bool) {
                    settings.addProperty(setting.name(), bool);
                } else {
                    settings.addProperty(setting.name(), String.valueOf(setting.value()));
                }
            }
            entry.add("settings", settings);
            moduleRoot.add(module.name(), entry);
        }

        root.add("modules", moduleRoot);
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(root));
        } catch (IOException exception) {
            Veil.LOGGER.warn("Could not write config", exception);
        }
    }
}
