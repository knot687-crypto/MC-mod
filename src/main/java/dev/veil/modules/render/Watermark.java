package dev.veil.modules.render;

import dev.veil.Veil;
import dev.veil.gui.Render2D;
import dev.veil.gui.Theme;
import net.minecraft.client.gui.DrawContext;

public final class Watermark extends HudModule {
    public Watermark() {
        super("Watermark", "Wordmark, build tag and live client telemetry.");
    }

    @Override
    public void render(DrawContext ctx, float delta) {
        int x = 8;
        int y = 8;
        String mark = "VEIL";
        int markWidth = Render2D.spacedWidth(textRenderer(), mark, 2);

        Render2D.rounded(ctx, x, y, markWidth + 34, 20, 6, 0xE60A0A10);
        Render2D.rounded(ctx, x, y, markWidth + 34, 20, 6, Theme.alpha(0xFFFFFF, 0.03f));
        Render2D.rounded(ctx, x + 6, y + 5, 3, 10, 1, Theme.accent());
        Render2D.spacedText(ctx, textRenderer(), mark, x + 14, y + 6, Theme.TEXT, 2);
        Render2D.text(ctx, textRenderer(), "1.0", x + markWidth + 18, y + 7, Theme.TEXT_FAINT);

        int fps = MC.getCurrentFps();
        String stats = fps + " fps";
        int statsY = y + 24;
        Render2D.rounded(ctx, x, statsY, Render2D.width(textRenderer(), stats) + 14, 15, 5, 0xB30A0A10);
        Render2D.text(ctx, textRenderer(), stats, x + 7, statsY + 4, Theme.TEXT_DIM);
    }

    private net.minecraft.client.font.TextRenderer textRenderer() {
        return MC.textRenderer;
    }
}
