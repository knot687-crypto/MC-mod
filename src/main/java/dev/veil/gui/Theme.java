package dev.veil.gui;

/**
 * One accent, two stops. Everything else is neutral glass so the accent stays loud
 * without the interface turning into a rainbow.
 */
public final class Theme {
    private static final int[] PRESET_A = {0xFFA78BFA, 0xFF7DD3FC, 0xFF6EE7B7, 0xFFF9A8D4, 0xFFFCD34D, 0xFFFB7185};
    private static final int[] PRESET_B = {0xFF6366F1, 0xFF3B82F6, 0xFF10B981, 0xFFF472B6, 0xFFF59E0B, 0xFFE11D48};

    public static final int TEXT = 0xFFF2F2F7;
    public static final int TEXT_DIM = 0xFF9A9AB0;
    public static final int TEXT_FAINT = 0xFF5E5E75;
    public static final int PANEL = 0xF70A0A10;
    public static final int PANEL_INNER = 0xFF0E0E17;
    public static final int CARD = 0xFF13131D;
    public static final int CARD_HOVER = 0xFF1A1A27;
    public static final int STROKE = 0x1FFFFFFF;
    public static final int ENABLED_PLATE = 0x2AFFFFFF;

    private static int accentA = PRESET_A[0];
    private static int accentB = PRESET_B[0];
    private static String mode = "Violet";
    private static float blend = 1.0f;

    private Theme() {
    }

    public static void apply(String preset) {
        if (preset.equals(mode)) {
            return;
        }
        mode = preset;
        int index = switch (preset) {
            case "Ice" -> 1;
            case "Mint" -> 2;
            case "Rose" -> 3;
            case "Amber" -> 4;
            case "Blood" -> 5;
            default -> 0;
        };
        accentA = PRESET_A[index];
        accentB = PRESET_B[index];
        blend = 0.0f;
    }

    /** Smooths palette swaps, called once per frame from the gui and hud. */
    public static void tick(float delta) {
        if (blend < 1.0f) {
            blend = Math.min(1.0f, blend + delta * 4.0f);
        }
    }

    public static int accent() {
        return accentA;
    }

    public static int accentDeep() {
        return accentB;
    }

    public static int accent(int alpha) {
        return (accentA & 0x00FFFFFF) | (Math.max(0, Math.min(255, alpha)) << 24);
    }

    public static int mix(int a, int b, float t) {
        float clamped = Math.max(0.0f, Math.min(1.0f, t));
        int aa = (a >>> 24) & 0xFF;
        int ar = (a >> 16) & 0xFF;
        int ag = (a >> 8) & 0xFF;
        int ab = a & 0xFF;
        int ba = (b >>> 24) & 0xFF;
        int br = (b >> 16) & 0xFF;
        int bg = (b >> 8) & 0xFF;
        int bb = b & 0xFF;
        int ra = Math.round(aa + (ba - aa) * clamped);
        int rr = Math.round(ar + (br - ar) * clamped);
        int rg = Math.round(ag + (bg - ag) * clamped);
        int rb = Math.round(ab + (bb - ab) * clamped);
        return (ra << 24) | (rr << 16) | (rg << 8) | rb;
    }

    public static int alpha(int color, float alpha) {
        return (color & 0x00FFFFFF) | (Math.max(0, Math.min(255, Math.round(alpha * 255.0f))) << 24);
    }

    public static String presetName() {
        return mode;
    }
}
