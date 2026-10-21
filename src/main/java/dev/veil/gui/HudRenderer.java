package dev.veil.gui;

import dev.veil.Veil;
import dev.veil.core.Module;
import dev.veil.modules.render.HudModule;
import net.minecraft.client.gui.DrawContext;

/**
 * Called once per frame from the in-game hud mixin. Modules stay independent; this only
 * sequences them and advances the shared palette animation.
 */
public final class HudRenderer {
    private static long lastFrame = System.nanoTime();

    private HudRenderer() {
    }

    public static void render(DrawContext ctx) {
        float delta = Math.min(0.1f, (System.nanoTime() - lastFrame) / 1_000_000_000.0f);
        lastFrame = System.nanoTime();
        Theme.tick(delta);

        for (Module module : Veil.modules().all()) {
            if (module instanceof HudModule hud && hud.isEnabled()) {
                hud.render(ctx, delta);
            }
        }
    }
}
