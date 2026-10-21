package dev.vanta.modules.visual;

import dev.vanta.Vanta;
import dev.vanta.core.Category;
import dev.vanta.core.Module;
import dev.vanta.render.Render2D;
import dev.vanta.render.Theme;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;

/** Four ticks that flash off the crosshair on every landed hit. */
public final class HitMarker extends Module {

    private long hitAt;

    public HitMarker() {
        super("HitMarker", "Crosshair flash on landed hits", Category.VISUAL);
    }

    @Override
    public void onAttack(Entity target) {
        if (target != null) {
            hitAt = System.currentTimeMillis();
        }
    }

    @Override
    public void onHud(DrawContext context) {
        long elapsed = System.currentTimeMillis() - hitAt;
        if (elapsed > 240L) {
            return;
        }
        float progress = elapsed / 240.0f;
        var client = Vanta.mc();
        float cx = client.getWindow().getScaledWidth() / 2.0f;
        float cy = client.getWindow().getScaledHeight() / 2.0f;
        float spread = 4.0f + progress * 5.0f;
        int color = Render2D.alpha(Render2D.lerp(Theme.TEXT, Theme.accent(), progress),
                (int) (235 * (1.0f - progress)));

        for (int quadrant = 0; quadrant < 4; quadrant++) {
            float dx = (quadrant % 2 == 0 ? -1 : 1);
            float dy = (quadrant < 2 ? -1 : 1);
            float x = cx + dx * spread;
            float y = cy + dy * spread;
            Render2D.rect(context, x - 1.0f, y - 1.0f, 2.0f, 2.0f, color);
        }
    }
}
