package dev.vanta.core.setting;

import dev.vanta.util.MathUtil;

/** Bounded double, snapped to a step so the UI and the config always agree. */
public final class NumberSetting extends Setting<Double> {

    private final double min;
    private final double max;
    private final double step;

    public NumberSetting(String name, double def, double min, double max, double step) {
        super(name, MathUtil.clamp(def, min, max));
        this.min = min;
        this.max = max;
        this.step = step <= 0 ? 0.01 : step;
    }

    public double get() {
        return value;
    }

    public float asFloat() {
        return value.floatValue();
    }

    public int asInt() {
        return (int) Math.round(value);
    }

    public void set(double raw) {
        double snapped = Math.round(MathUtil.clamp(raw, min, max) / step) * step;
        value = MathUtil.clamp(snapped, min, max);
    }

    public double min() {
        return min;
    }

    public double max() {
        return max;
    }

    public double step() {
        return step;
    }

    public double ratio() {
        return (value - min) / (max - min);
    }

    public boolean integer() {
        return step >= 1.0;
    }

    @Override
    public String display() {
        if (integer()) {
            return Long.toString(Math.round(value));
        }
        return String.format("%.2f", value);
    }
}
