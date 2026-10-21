package dev.vanta.render;

/** Every colour the menu and HUD draw, in one place. */
public final class Theme {

    public static final int WINDOW_TOP = 0xF40C1017;
    public static final int WINDOW_BOTTOM = 0xF4090C11;
    public static final int HEADER = 0xFF10151E;
    public static final int PANEL = 0xFF0D1219;
    public static final int CARD = 0xFF141A25;
    public static final int CARD_HOVER = 0xFF1A2231;
    public static final int CARD_ACTIVE = 0xFF1C2537;
    public static final int ELEVATED = 0xFF1A2231;
    public static final int ELEVATED_HOVER = 0xFF222C3E;
    public static final int TRACK = 0xFF1E2634;

    public static final int STROKE = 0x14FFFFFF;
    public static final int STROKE_STRONG = 0x2BFFFFFF;

    public static final int TEXT = 0xFFE9EDF7;
    public static final int TEXT_DIM = 0xFF8A93AA;
    public static final int TEXT_FAINT = 0xFF59627A;

    public static final int GOOD = 0xFF4ADE80;
    public static final int BAD = 0xFFFF5D73;
    public static final int WARN = 0xFFFFC661;

    private static String accentName = "Violet";
    private static int accentA = 0xFF7C5CFF;
    private static int accentB = 0xFF3FD9FF;

    private Theme() {
    }

    public static int accent() {
        return accentA;
    }

    public static int accentAlt() {
        return accentB;
    }

    public static String accentName() {
        return accentName;
    }

    public static void accent(String name) {
        switch (name) {
            case "Ember" -> set("Ember", 0xFFFF7A45, 0xFFFFC661);
            case "Mint" -> set("Mint", 0xFF4ADE80, 0xFF2DD4BF);
            case "Rose" -> set("Rose", 0xFFFF6B9D, 0xFFFF5D73);
            case "Ice" -> set("Ice", 0xFF4FA8FF, 0xFFB3E5FF);
            default -> set("Violet", 0xFF7C5CFF, 0xFF3FD9FF);
        }
    }

    private static void set(String name, int a, int b) {
        accentName = name;
        accentA = a;
        accentB = b;
    }
}
