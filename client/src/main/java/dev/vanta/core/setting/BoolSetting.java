package dev.vanta.core.setting;

public final class BoolSetting extends Setting<Boolean> {

    public BoolSetting(String name, boolean def) {
        super(name, def);
    }

    public boolean get() {
        return value;
    }

    public void toggle() {
        value = !value;
    }

    @Override
    public String display() {
        return value ? "on" : "off";
    }
}
