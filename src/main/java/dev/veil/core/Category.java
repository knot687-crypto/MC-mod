package dev.veil.core;

/**
 * Top level grouping for modules. Order defines the order of the tabs in the click gui.
 */
public enum Category {
    COMBAT("Combat"),
    RENDER("Render"),
    CLIENT("Client");

    private final String label;

    Category(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
