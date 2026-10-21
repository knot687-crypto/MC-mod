package dev.vanta.core.setting;

/** A named, typed value owned by a module. */
public abstract class Setting<T> {

    private final String name;
    protected T value;

    protected Setting(String name, T def) {
        this.name = name;
        this.value = def;
    }

    public String name() {
        return name;
    }

    public T value() {
        return value;
    }

    public void set(T value) {
        this.value = value;
    }

    /** Short right-aligned readout used by the menu. */
    public abstract String display();
}
