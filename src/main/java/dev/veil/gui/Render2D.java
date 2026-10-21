package dev.veil.gui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

public final class Render2D {
    private Render2D() {
    }

    public static void rect(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + h, color);
    }

    public static void rounded(DrawContext ctx, int x, int y, int w, int h, int radius, int color) {
        if (w <= 0 || h <= 0) {
            return;
        }
        int r = Math.max(0, Math.min(radius, Math.min(w, h) / 2));
        if (r == 0) {
            rect(ctx, x, y, w, h, color);
            return;
        }
        rect(ctx, x + r, y, w - r * 2, h, color);
        rect(ctx, x, y + r, r, h - r * 2, color);
        rect(ctx, x + w - r, y + r, r, h - r * 2, color);

        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int inset = (int) Math.round(r - Math.sqrt(Math.max(0.0, r * (double) r - dy * dy)));
            rect(ctx, x + inset, y + i, r - inset, 1, color);
            rect(ctx, x + w - r, y + i, r - inset, 1, color);
            rect(ctx, x + inset, y + h - i - 1, r - inset, 1, color);
            rect(ctx, x + w - r, y + h - i - 1, r - inset, 1, color);
        }
    }

    public static void roundedGradient(DrawContext ctx, int x, int y, int w, int h, int radius, int top, int bottom) {
        int r = Math.max(0, Math.min(radius, Math.min(w, h) / 2));
        rounded(ctx, x, y, w, h, r, bottom);
        int inner = h - r * 2;
        if (inner > 0) {
            int steps = Math.min(inner, 24);
            int stepHeight = Math.max(1, inner / steps);
            for (int i = 0; i < inner; i += stepHeight) {
                int color = Theme.mix(top, bottom, (float) i / (float) inner);
                ctx.fill(x, y + r + i, x + w, y + r + Math.min(inner, i + stepHeight), color);
            }
        }
        roundedTop(ctx, x, y, w, h, r, top);
    }

    private static void roundedTop(DrawContext ctx, int x, int y, int w, int h, int r, int color) {
        if (r <= 0) {
            return;
        }
        rect(ctx, x + r, y, w - r * 2, r, color);
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int inset = (int) Math.round(r - Math.sqrt(Math.max(0.0, r * (double) r - dy * dy)));
            rect(ctx, x + inset, y + i, r - inset, 1, color);
            rect(ctx, x + w - r, y + i, r - inset, 1, color);
        }
    }

    public static void glow(DrawContext ctx, int x, int y, int w, int h, int color, int layers) {
        for (int i = layers; i > 0; i--) {
            int spread = i * 2;
            int alpha = Math.max(4, 22 - i * 3);
            rounded(ctx, x - spread, y - spread, w + spread * 2, h + spread * 2, 6 + spread, Theme.alpha(color, alpha / 255.0f));
        }
    }

    public static void shadow(DrawContext ctx, int x, int y, int w, int h, int radius) {
        rounded(ctx, x + 2, y + 4, w, h, radius, 0x45000000);
        rounded(ctx, x + 1, y + 2, w, h, radius, 0x30000000);
    }

    public static void text(DrawContext ctx, TextRenderer font, String value, int x, int y, int color) {
        ctx.drawText(font, value, x, y, color, false);
    }

    public static void textShadow(DrawContext ctx, TextRenderer font, String value, int x, int y, int color) {
        ctx.drawText(font, value, x, y, color, true);
    }

    public static int width(TextRenderer font, String value) {
        return font.getWidth(value);
    }

    public static void spacedText(DrawContext ctx, TextRenderer font, String value, int x, int y, int color, int spacing) {
        int cursor = x;
        for (int i = 0; i < value.length(); i++) {
            String glyph = String.valueOf(value.charAt(i));
            ctx.drawText(font, glyph, cursor, y, color, false);
            cursor += font.getWidth(glyph) + spacing;
        }
    }

    public static int spacedWidth(TextRenderer font, String value, int spacing) {
        int width = 0;
        for (int i = 0; i < value.length(); i++) {
            width += font.getWidth(String.valueOf(value.charAt(i))) + spacing;
        }
        return width - spacing;
    }

    public static void circle(DrawContext ctx, int cx, int cy, int radius, int color) {
        for (int dy = -radius; dy <= radius; dy++) {
            int dx = (int) Math.sqrt(Math.max(0, radius * radius - dy * dy));
            ctx.fill(cx - dx, cy + dy, cx + dx + 1, cy + dy + 1, color);
        }
    }
}
