package dev.veil.core;

import java.util.Arrays;
import java.util.List;

public final class ModeSetting extends Setting<String> {
    private final List<String> modes;

    public ModeSetting(String name, String description, String defaultValue, String... modes) {
        super(name, description, defaultValue);
        this.modes = Arrays.asList(modes);
    }

    @Override
    public void setValue(String value) {
        if (modes.contains(value)) {
            super.setValue(value);
        }
    }

    @Override
    public String display() {
        return value;
    }

    @Override
    public boolean isMode() {
        return true;
    }

    @Override
    public void cycle() {
        int index = modes.indexOf(value);
        value = modes.get((index + 1) % modes.size());
    }

    public List<String> modes() {
        return modes;
    }
}
