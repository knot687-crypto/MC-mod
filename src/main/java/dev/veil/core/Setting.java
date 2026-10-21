package dev.veil.core;

import java.util.function.BooleanSupplier;

/**
 * Base for every tunable value a module exposes. The click gui only ever talks to this
 * surface, so new setting types drop in without touching the renderer.
 */
public abstract class Setting<T> {
    private final String name;
    private final String description;
    private final T defaultValue;
    protected T value;
    private BooleanSupplier visibility = () -> true;

    protected Setting(String name, String description, T defaultValue) {
        this.name = name;
        this.description = description;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public T value() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public T defaultValue() {
        return defaultValue;
    }

    public void reset() {
        this.value = defaultValue;
    }

    /** Settings that depend on another setting hide themselves instead of greying out. */
    public Setting<T> visibleWhen(BooleanSupplier condition) {
        this.visibility = condition;
        return this;
    }

    public boolean isVisible() {
        return visibility.getAsBoolean();
    }

    public abstract String display();

    public boolean isBoolean() {
        return false;
    }

    public boolean isMode() {
        return false;
    }

    public boolean isNumber() {
        return false;
    }

    public void toggle() {
    }

    public void cycle() {
    }

    public double fraction() {
        return 0.0;
    }

    public void drag(double fraction) {
    }
}
