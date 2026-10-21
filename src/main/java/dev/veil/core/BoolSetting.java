package dev.veil.core;

public final class BoolSetting extends Setting<Boolean> {
    public BoolSetting(String name, String description, boolean defaultValue) {
        super(name, description, defaultValue);
    }

    @Override
    public String display() {
        return value ? "On" : "Off";
    }

    @Override
    public boolean isBoolean() {
        return true;
    }

    @Override
    public void toggle() {
        value = !value;
    }

    @Override
    public void setValue(Boolean value) {
        super.setValue(value);
    }
}
