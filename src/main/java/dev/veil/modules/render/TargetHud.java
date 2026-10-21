package dev.veil.modules.render;

import dev.veil.core.Rotation;
import dev.veil.core.TargetManager;
import dev.veil.gui.Render2D;
import dev.veil.gui.Theme;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.LivingEntity;

public final class TargetHud extends HudModule {
    private LivingEntity last;
    private float health;
    private float healthMax = 20.0f;
    private float alpha;

    public TargetHud() {
        super("Target HUD", "Floating readout for whoever is currently targeted.");
    }

    @Override
    public void render(DrawContext ctx, float delta) {
        LivingEntity target = TargetManager.current();
        if (target != null) {
            last = target;
            healthMax = Math.max(1.0f, target.getMaxHealth());
            health = approach(health, target.getHealth(), delta * 9.0f);
            alpha = approach(alpha, 1.0f, delta * 10.0f);
        } else if (last != null) {
            alpha = approach(alpha, 0.0f, delta * 7.0f);
            health = approach(health, 0.0f, delta * 5.0f);
            if (alpha < 0.02f) {
                last = null;
                return;
            }
        } else {
            return;
        }

        LivingEntity display = target != null ? target : last;
        String name = display.getName().getString();
        int width = 178;
        int height = 46;
        int x = (MC.getWindow().getScaledWidth() - width) / 2;
        int y = MC.getWindow().getScaledHeight() / 2 + 52;

        int backdrop = Theme.alpha(0xFF0A0A10, 0.85f * alpha);
        Render2D.rounded(ctx, x, y, width, height, 7, backdrop);
        Render2D.rounded(ctx, x, y, width, height, 7, Theme.alpha(0xFFFFFF, 0.03f * alpha));
        Render2D.rounded(ctx, x, y, 2, height, 1, Theme.alpha(Theme.accent(), alpha));

        Render2D.text(ctx, MC.textRenderer, name, x + 12, y + 9, Theme.alpha(Theme.TEXT, alpha));

        String distance = String.format("%.1fm", MC.player == null ? 0.0 : MC.player.distanceTo(display));
        Render2D.text(ctx, MC.textRenderer, distance, x + width - 12 - Render2D.width(MC.textRenderer, distance), y + 9,
                Theme.alpha(Theme.TEXT_FAINT, alpha));

        String hpText = String.format("%.1f", Math.max(0.0f, health));
        Render2D.text(ctx, MC.textRenderer, hpText, x + 12, y + 21, Theme.alpha(Theme.accent(), alpha));

        int barX = x + 12 + Render2D.width(MC.textRenderer, hpText) + 6;
        int barW = width - (barX - x) - 12;
        Render2D.rounded(ctx, barX, y + 26, barW, 3, 1, Theme.alpha(0xFFFFFF, 0.10f * alpha));
        float fraction = Math.max(0.0f, Math.min(1.0f, health / healthMax));
        Render2D.rounded(ctx, barX, y + 26, Math.max(1, Math.round(barW * fraction)), 3, 1, Theme.alpha(Theme.accent(), 0.95f * alpha));

        if (display instanceof net.minecraft.entity.player.PlayerEntity player) {
            String ping = pingOf(player) + "ms";
            Render2D.text(ctx, MC.textRenderer, ping, x + 12, y + 33, Theme.alpha(Theme.TEXT_FAINT, alpha));
        }
        String angle = Math.round(Rotation.angleTo(MC.player == null ? display.getEntityPos() : MC.player.getEyePos(),
                display.getBoundingBox().getCenter(), MC.player == null ? 0 : MC.player.getYaw(),
                MC.player == null ? 0 : MC.player.getPitch())) + "\u00B0";
        Render2D.text(ctx, MC.textRenderer, angle, x + width - 12 - Render2D.width(MC.textRenderer, angle), y + 33,
                Theme.alpha(Theme.TEXT_FAINT, alpha));
    }

    private int pingOf(net.minecraft.entity.player.PlayerEntity player) {
        if (MC.getNetworkHandler() == null) {
            return 0;
        }
        PlayerListEntry entry = MC.getNetworkHandler().getPlayerListEntry(player.getUuid());
        return entry == null ? 0 : entry.getLatency();
    }

    private static float approach(float value, float target, float delta) {
        return value + (target - value) * Math.min(1.0f, delta);
    }
}
