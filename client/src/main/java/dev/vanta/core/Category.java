package dev.vanta.core;

/** Module buckets, in the order the menu shows them. */
public enum Category {
    COMBAT("Combat", Icon.CROSSHAIR),
    VISUAL("Visuals", Icon.EYE),
    CLIENT("Client", Icon.GEAR);

    private final String label;
    private final Icon icon;

    Category(String label, Icon icon) {
        this.label = label;
        this.icon = icon;
    }

    public String label() {
        return label;
    }

    public Icon icon() {
        return icon;
    }

    public enum Icon {
        CROSSHAIR,
        EYE,
        GEAR
    }
}
