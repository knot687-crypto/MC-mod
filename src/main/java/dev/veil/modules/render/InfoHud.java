package dev.veil.modules.render;

import dev.veil.core.BoolSetting;
import dev.veil.core.CombatStats;
import dev.veil.gui.Render2D;
import dev.veil.gui.Theme;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;

public final class InfoHud extends HudModule {
    private final BoolSetting showCps = add(new BoolSetting("CPS", "Clicks per second.", true));
    private final BoolSetting showFps = add(new BoolSetting("FPS", "Frames per second.", true));
    private final BoolSetting showPing = add(new BoolSetting("Ping", "Current server latency.", true));

    public InfoHud() {
        super("Info", "Slim telemetry chips in the lower corner.");
    }

    @Override
    public void render(DrawContext ctx, float delta) {
        int y = MC.getWindow().getScaledHeight() - 20;
        int x = 8;
        if (showCps.value()) {
            x += chip(ctx, x, y, "cps", String.valueOf(CombatStats.cps()));
        }
        if (showFps.value()) {
            x += chip(ctx, x, y, "fps", String.valueOf(MC.getCurrentFps()));
        }
        if (showPing.value()) {
            chip(ctx, x, y, "ping", String.valueOf(ping()));
        }
    }

    private int chip(DrawContext ctx, int x, int y, String label, String value) {
        int width = Render2D.width(MC.textRenderer, label) + Render2D.width(MC.textRenderer, value) + 20;
        Render2D.rounded(ctx, x, y, width, 16, 4, 0xC70A0A10);
        Render2D.rounded(ctx, x, y, width, 16, 4, Theme.alpha(0xFFFFFF, 0.02f));
        Render2D.text(ctx, MC.textRenderer, label, x + 7, y + 4, Theme.accent());
        Render2D.text(ctx, MC.textRenderer, value, x + width - 7 - Render2D.width(MC.textRenderer, value), y + 4, Theme.TEXT);
        return width + 5;
    }

    private int ping() {
        if (MC.player == null || MC.getNetworkHandler() == null) {
            return 0;
        }
        PlayerListEntry entry = MC.getNetworkHandler().getPlayerListEntry(MC.player.getUuid());
        return entry == null ? 0 : entry.getLatency();
    }
}
