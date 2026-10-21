package dev.vanta.core;

import dev.vanta.Vanta;
import dev.vanta.core.setting.BoolSetting;
import dev.vanta.core.setting.KeySetting;
import dev.vanta.core.setting.ModeSetting;
import dev.vanta.core.setting.NumberSetting;
import dev.vanta.core.setting.Setting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;

import java.util.ArrayList;
import java.util.List;

/** Base for every togglable feature. Holds its own settings and bind. */
public abstract class Module {

    private final String name;
    private final String description;
    private final Category category;
    private final List<Setting<?>> settings = new ArrayList<>();

    private boolean enabled;
    private int key = -1;

    protected Module(String name, String description, Category category) {
        this.name = name;
        this.description = description;
        this.category = category;
    }

    // ---- lifecycle --------------------------------------------------------

    public void onEnable() {
    }

    public void onDisable() {
    }

    public void onTick() {
    }

    public void onHud(DrawContext context) {
    }

    public void onAttack(Entity target) {
    }

    /** Notified on every incoming knockback, whether or not it is modified. */
    public void onKnockback(double x, double y, double z) {
    }

    /** @return the replacement velocity, or null to leave it alone */
    public double[] onVelocity(double x, double y, double z) {
        return null;
    }

    /** Called when the module's own bind fires; default toggles the module. */
    public boolean onKeyPressed() {
        toggle();
        return true;
    }

    public final void toggle() {
        setEnabled(!enabled);
    }

    public final void setEnabled(boolean value) {
        if (enabled == value) {
            return;
        }
        enabled = value;
        if (value) {
            onEnable();
        } else {
            onDisable();
        }
        if (Vanta.mc().player != null) {
            Config.markDirty();
        }
    }

    // ---- settings ---------------------------------------------------------

    protected BoolSetting bool(String name, boolean def) {
        BoolSetting setting = new BoolSetting(name, def);
        settings.add(setting);
        return setting;
    }

    protected NumberSetting num(String name, double def, double min, double max, double step) {
        NumberSetting setting = new NumberSetting(name, def, min, max, step);
        settings.add(setting);
        return setting;
    }

    protected ModeSetting mode(String name, String def, String... modes) {
        ModeSetting setting = new ModeSetting(name, def, modes);
        settings.add(setting);
        return setting;
    }

    public KeySetting bind(int def) {
        KeySetting setting = new KeySetting("Bind", def);
        key = def;
        return setting;
    }

    public List<Setting<?>> settings() {
        return settings;
    }

    // ---- accessors --------------------------------------------------------

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public Category category() {
        return category;
    }

    public boolean enabled() {
        return enabled;
    }

    public int key() {
        return key;
    }

    public void setKey(int key) {
        this.key = key;
    }

    public String label() {
        return name;
    }
}
