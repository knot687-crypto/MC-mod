package dev.vanta.core.setting;

import java.util.List;

/** Ordered string choices. */
public final class ModeSetting extends Setting<String> {

    private final List<String> modes;

    public ModeSetting(String name, String def, String... modes) {
        super(name, def);
        this.modes = List.of(modes);
        if (!this.modes.contains(def)) {
            this.value = this.modes.get(0);
        }
    }

    public List<String> modes() {
        return modes;
    }

    public String get() {
        return value;
    }

    public boolean is(String other) {
        return value.equalsIgnoreCase(other);
    }

    public void cycle(int direction) {
        int index = modes.indexOf(value);
        if (index < 0) {
            index = 0;
        }
        int size = modes.size();
        value = modes.get(((index + direction) % size + size) % size);
    }

    @Override
    public String display() {
        return value;
    }
}
