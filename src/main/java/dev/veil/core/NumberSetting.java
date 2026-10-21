package dev.veil.core;

public final class NumberSetting extends Setting<Double> {
    private final double min;
    private final double max;
    private final double step;
    private final String unit;

    public NumberSetting(String name, String description, double defaultValue, double min, double max, double step, String unit) {
        super(name, description, defaultValue);
        this.min = min;
        this.max = max;
        this.step = step;
        this.unit = unit;
    }

    @Override
    public void setValue(Double value) {
        double clamped = Math.max(min, Math.min(max, value));
        if (step > 0.0) {
            clamped = Math.round(clamped / step) * step;
        }
        super.setValue(clamped);
    }

    @Override
    public String display() {
        double rounded = Math.round(value * 100.0) / 100.0;
        String text = (rounded == Math.floor(rounded) ? String.valueOf((int) rounded) : String.valueOf(rounded));
        return unit.isEmpty() ? text : text + unit;
    }

    @Override
    public boolean isNumber() {
        return true;
    }

    @Override
    public double fraction() {
        return (value - min) / (max - min);
    }

    @Override
    public void drag(double fraction) {
        setValue(min + (max - min) * Math.max(0.0, Math.min(1.0, fraction)));
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
}
