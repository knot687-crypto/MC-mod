package dev.vanta.render;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/**
 * All two dimensional drawing goes through here.
 * If a Minecraft GUI method ever changes name, this is the only file to touch.
 */
public final class Render2D {

    private Render2D() {
    }

    // ---- colour -----------------------------------------------------------

    public static int alpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
    }

    public static int fade(int color, float factor) {
        int a = (int) (((color >>> 24) & 0xFF) * Math.clamp(factor, 0.0f, 1.0f));
        return (color & 0x00FFFFFF) | (a << 24);
    }

    public static int lerp(int from, int to, float t) {
        t = Math.clamp(t, 0.0f, 1.0f);
        int a = (int) (((from >>> 24) & 0xFF) + (((to >>> 24) & 0xFF) - ((from >>> 24) & 0xFF)) * t);
        int r = (int) (((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * t);
        int g = (int) (((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * t);
        int b = (int) ((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int gradientAt(int from, int to, float t) {
        return lerp(from, to, t);
    }

    // ---- primitives -------------------------------------------------------

    public static void rect(DrawContext ctx, float x, float y, float width, float height, int color) {
        ctx.fill(Math.round(x), Math.round(y), Math.round(x + width), Math.round(y + height), color);
    }

    /** Vertical gradient made of one pixel bands; cheap enough for a handful of panels. */
    public static void vGradient(DrawContext ctx, float x, float y, float width, float height, int top, int bottom) {
        int rows = Math.max(1, Math.round(height));
        for (int row = 0; row < rows; row++) {
            float t = rows == 1 ? 0.0f : (float) row / (rows - 1);
            ctx.fill(Math.round(x), Math.round(y) + row, Math.round(x + width), Math.round(y) + row + 1,
                    lerp(top, bottom, t));
        }
    }

    public static void hGradient(DrawContext ctx, float x, float y, float width, float height, int left, int right) {
        int columns = Math.max(1, Math.round(width));
        for (int column = 0; column < columns; column++) {
            float t = columns == 1 ? 0.0f : (float) column / (columns - 1);
            ctx.fill(Math.round(x) + column, Math.round(y), Math.round(x) + column + 1, Math.round(y + height),
                    lerp(left, right, t));
        }
    }

    public static void rounded(DrawContext ctx, float x, float y, float width, float height, float radius, int color) {
        roundedV(ctx, x, y, width, height, radius, color, color);
    }

    /** Rounded rect with a vertical gradient; rows are single spans so corners stay soft. */
    public static void roundedV(DrawContext ctx, float x, float y, float width, float height, float radius, int top, int bottom) {
        float r = Math.min(radius, Math.min(width, height) / 2.0f);
        int firstRow = Math.round(y);
        int rows = Math.max(1, Math.round(height));
        int left = Math.round(x);
        int right = Math.round(x + width);
        float halfWithoutRadius = height / 2.0f - r;

        for (int row = 0; row < rows; row++) {
            float centre = row + 0.5f - height / 2.0f;
            float inset = 0.0f;
            float beyond = Math.abs(centre) - halfWithoutRadius;
            if (beyond > 0.0f && r > 0.0f) {
                inset = r - (float) Math.sqrt(Math.max(0.0f, r * r - beyond * beyond));
            }
            float t = rows == 1 ? 0.0f : (float) row / (rows - 1);
            ctx.fill(Math.round(left + inset), firstRow + row, Math.round(right - inset), firstRow + row + 1,
                    lerp(top, bottom, t));
        }
    }

    public static void roundedOutline(DrawContext ctx, float x, float y, float width, float height, float radius, int color) {
        float r = Math.min(radius, Math.min(width, height) / 2.0f);
        int rows = Math.max(1, Math.round(height));
        int firstRow = Math.round(y);
        int left = Math.round(x);
        int right = Math.round(x + width);
        float halfWithoutRadius = height / 2.0f - r;

        for (int row = 0; row < rows; row++) {
            float centre = row + 0.5f - height / 2.0f;
            float inset = 0.0f;
            float beyond = Math.abs(centre) - halfWithoutRadius;
            if (beyond > 0.0f && r > 0.0f) {
                inset = r - (float) Math.sqrt(Math.max(0.0f, r * r - beyond * beyond));
            }
            ctx.fill(Math.round(left + inset), firstRow + row, Math.round(left + inset) + 1, firstRow + row + 1, color);
            ctx.fill(Math.round(right - inset) - 1, firstRow + row, Math.round(right - inset), firstRow + row + 1, color);
        }
        ctx.fill(left + Math.round(r), firstRow, right - Math.round(r), firstRow + 1, color);
        ctx.fill(left + Math.round(r), firstRow + rows - 1, right - Math.round(r), firstRow + rows, color);
    }

    public static void shadow(DrawContext ctx, float x, float y, float width, float height, float radius, int color) {
        for (int layer = 5; layer >= 1; layer--) {
            float grow = layer * 2.0f;
            rounded(ctx, x - grow, y - grow + 3.0f, width + grow * 2.0f, height + grow * 2.0f,
                    radius + grow, alpha(color, Math.max(2, 10 - layer)));
        }
    }

    public static void scissor(DrawContext ctx, float x, float y, float width, float height) {
        ctx.enableScissor(Math.round(x), Math.round(y), Math.round(x + width), Math.round(y + height));
    }

    public static void unScissor(DrawContext ctx) {
        ctx.disableScissor();
    }

    // ---- text -------------------------------------------------------------

    public static int width(TextRenderer font, String text) {
        return font.getWidth(text);
    }

    public static void text(DrawContext ctx, TextRenderer font, String text, float x, float y, int color, boolean shadow) {
        text(ctx, font, text, x, y, 1.0f, color, shadow);
    }

    public static void text(DrawContext ctx, TextRenderer font, String text, float x, float y, float scale, int color, boolean shadow) {
        if (text == null || text.isEmpty()) {
            return;
        }
        var matrices = ctx.getMatrices();
        matrices.pushMatrix();
        matrices.translate(x, y);
        matrices.scale(scale, scale);
        ctx.drawText(font, text, 0, 0, color, shadow);
        matrices.popMatrix();
    }

    public static void textRight(DrawContext ctx, TextRenderer font, String text, float right, float y, int color, boolean shadow) {
        text(ctx, font, text, right - width(font, text), y, 1.0f, color, shadow);
    }

    public static void textCentred(DrawContext ctx, TextRenderer font, String text, float centreX, float y, float scale, int color, boolean shadow) {
        text(ctx, font, text, centreX - (width(font, text) * scale) / 2.0f, y, scale, color, shadow);
    }

    /** Letter-spaced uppercase label, the type-kit voice for section headers. */
    public static void caps(DrawContext ctx, TextRenderer font, String text, float x, float y, float scale, float spacing, int color) {
        float cursor = x;
        for (int i = 0; i < text.length(); i++) {
            String glyph = String.valueOf(text.charAt(i));
            text(ctx, font, glyph, cursor, y, scale, color, false);
            cursor += font.getWidth(glyph) * scale + spacing;
        }
    }

    public static float capsWidth(TextRenderer font, String text, float scale, float spacing) {
        float total = 0.0f;
        for (int i = 0; i < text.length(); i++) {
            total += font.getWidth(String.valueOf(text.charAt(i))) * scale + spacing;
        }
        return total;
    }

    // ---- icons ------------------------------------------------------------

    public static void icon(DrawContext ctx, String type, float x, float y, float size, int color) {
        float unit = size / 8.0f;
        switch (type) {
            case "crosshair" -> {
                // Four ticks around an open centre, so the glyph reads at 8px.
                ctx.fill(Math.round(x + 3 * unit), Math.round(y + 1 * unit), Math.round(x + 5 * unit), Math.round(y + 2.5f * unit), color);
                ctx.fill(Math.round(x + 3 * unit), Math.round(y + 5.5f * unit), Math.round(x + 5 * unit), Math.round(y + 7 * unit), color);
                ctx.fill(Math.round(x + 1 * unit), Math.round(y + 3 * unit), Math.round(x + 2.5f * unit), Math.round(y + 5 * unit), color);
                ctx.fill(Math.round(x + 5.5f * unit), Math.round(y + 3 * unit), Math.round(x + 7 * unit), Math.round(y + 5 * unit), color);
            }
            case "eye" -> {
                ctx.fill(Math.round(x), Math.round(y + 3 * unit), Math.round(x + 8 * unit), Math.round(y + 5 * unit), color);
                ctx.fill(Math.round(x + 1 * unit), Math.round(y + 2 * unit), Math.round(x + 7 * unit), Math.round(y + 6 * unit), color);
                ctx.fill(Math.round(x + 3 * unit), Math.round(y + 3 * unit), Math.round(x + 5 * unit), Math.round(y + 5 * unit), 0xFF0A0C11);
            }
            default -> {
                ctx.fill(Math.round(x + 1 * unit), Math.round(y + 1 * unit), Math.round(x + 7 * unit), Math.round(y + 3 * unit), color);
                ctx.fill(Math.round(x + 1 * unit), Math.round(y + 5 * unit), Math.round(x + 7 * unit), Math.round(y + 7 * unit), color);
                ctx.fill(Math.round(x + 3 * unit), Math.round(y + 3 * unit), Math.round(x + 5 * unit), Math.round(y + 5 * unit), color);
            }
        }
    }
}
