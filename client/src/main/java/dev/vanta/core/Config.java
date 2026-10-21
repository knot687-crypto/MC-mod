package dev.vanta.core;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.vanta.Vanta;
import dev.vanta.core.setting.BoolSetting;
import dev.vanta.core.setting.KeySetting;
import dev.vanta.core.setting.ModeSetting;
import dev.vanta.core.setting.NumberSetting;
import dev.vanta.core.setting.Setting;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Flat json config, one section per module, saved on toggle and on shutdown. */
public final class Config {

    private static final Path DIR = FabricLoader.getInstance().getConfigDir().resolve("vanta");
    private static final Path FILE = DIR.resolve("config.json");
    private static volatile boolean dirty;

    private Config() {
    }

    public static void markDirty() {
        dirty = true;
    }

    public static void save() {
        if (!dirty) {
            return;
        }
        dirty = false;

        JsonObject root = new JsonObject();
        JsonObject modules = new JsonObject();
        for (Module module : ModuleManager.all()) {
            JsonObject node = new JsonObject();
            node.addProperty("enabled", module.enabled());
            node.addProperty("key", module.key());

            JsonObject values = new JsonObject();
            for (Setting<?> setting : module.settings()) {
                if (setting instanceof BoolSetting bool) {
                    values.addProperty(setting.name(), bool.get());
                } else if (setting instanceof NumberSetting number) {
                    values.addProperty(setting.name(), number.get());
                } else if (setting instanceof KeySetting key) {
                    values.addProperty(setting.name(), key.get());
                } else if (setting instanceof ModeSetting value) {
                    values.addProperty(setting.name(), value.get());
                }
            }
            node.add("settings", values);
            modules.add(module.name(), node);
        }
        root.add("modules", modules);

        try {
            Files.createDirectories(DIR);
            Path temp = DIR.resolve("config.json.tmp");
            Files.writeString(temp, new GsonBuilder().setPrettyPrinting().create().toJson(root));
            Files.move(temp, FILE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ignored) {
            // A failed write leaves the previous file intact; nothing else to do at shutdown.
        }
    }

    public static void load() {
        if (!Files.exists(FILE)) {
            return;
        }
        try {
            JsonObject root = JsonParser.parseString(Files.readString(FILE)).getAsJsonObject();
            JsonObject modules = root.getAsJsonObject("modules");
            if (modules == null) {
                return;
            }
            for (Module module : ModuleManager.all()) {
                JsonObject node = modules.getAsJsonObject(module.name());
                if (node == null) {
                    continue;
                }
                JsonObject values = node.getAsJsonObject("settings");
                if (values != null) {
                    for (Setting<?> setting : module.settings()) {
                        JsonElement element = values.get(setting.name());
                        if (element == null) {
                            continue;
                        }
                        apply(setting, element);
                    }
                }
                if (node.has("key")) {
                    module.setKey(node.get("key").getAsInt());
                    for (Setting<?> setting : module.settings()) {
                        if (setting instanceof KeySetting key && key.name().equals("Bind")) {
                            key.setKey(module.key());
                        }
                    }
                }
                if (node.has("enabled") && node.get("enabled").getAsBoolean()) {
                    module.setEnabled(true);
                }
            }
        } catch (Exception ignored) {
            // Corrupt config: start clean rather than refusing to boot.
        }
    }

    private static void apply(Setting<?> setting, JsonElement element) {
        if (setting instanceof BoolSetting bool) {
            bool.set(element.getAsBoolean());
        } else if (setting instanceof NumberSetting number) {
            number.set(element.getAsDouble());
        } else if (setting instanceof KeySetting key) {
            key.setKey(element.getAsInt());
        } else if (setting instanceof ModeSetting mode) {
            String value = element.getAsString();
            if (mode.modes().contains(value)) {
                mode.set(value);
            }
        }
    }
}
