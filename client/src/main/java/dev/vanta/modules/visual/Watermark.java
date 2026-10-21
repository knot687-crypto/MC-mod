package dev.vanta.modules.visual;

import dev.vanta.Vanta;
import dev.vanta.core.Category;
import dev.vanta.core.Module;
import dev.vanta.core.ModuleManager;
import dev.vanta.core.Stats;
import dev.vanta.core.setting.BoolSetting;
import dev.vanta.render.Render2D;
import dev.vanta.render.Theme;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Top-left badge and the right-hand module list. */
public final class Watermark extends Module {

    private final BoolSetting info = bool("Info", true);
    private final BoolSetting arrayList = bool("Array List", true);

    public Watermark() {
        super("Watermark", "Client badge and enabled module list", Category.VISUAL);
    }

    @Override
    public void onHud(DrawContext context) {
        Render2D.rounded(context, 6, 6, 74, 18, 6, Render2D.alpha(Theme.HEADER, 200));
        Render2D.roundedOutline(context, 6, 6, 74, 18, 6, Theme.STROKE);
        Render2D.roundedV(context, 10, 12, 2.5f, 7, 1.0f, Theme.accent(), Theme.accentAlt());
        Render2D.caps(context, Vanta.mc().textRenderer, "VANTA", 16, 11, 0.85f, 1.2f, Theme.TEXT);

        if (info.get()) {
            String line = Stats.fps() + " fps  ·  " + Stats.ping() + " ms  ·  " + Stats.cps() + " cps";
            Render2D.caps(context, Vanta.mc().textRenderer, line, 7, 28, 0.66f, 0.7f, Theme.TEXT_FAINT);
        }

        if (!arrayList.get()) {
            return;
        }
        List<Module> shown = new ArrayList<>();
        for (Module module : ModuleManager.all()) {
            if (module.enabled() && module != this) {
                shown.add(module);
            }
        }
        shown.sort(Comparator.comparingDouble((Module module) -> -Vanta.mc().textRenderer.getWidth(module.name())));

        int screenWidth = Vanta.mc().getWindow().getScaledWidth();
        float y = 6.0f;
        for (Module module : shown) {
            String name = module.name();
            float width = Render2D.width(Vanta.mc().textRenderer, name) + 14.0f;
            Render2D.rounded(context, screenWidth - width - 6.0f, y, width, 14.0f, 4.0f,
                    Render2D.alpha(Theme.HEADER, 190));
            Render2D.rounded(context, screenWidth - 6.0f - width, y, 2.0f, 14.0f, 1.0f, Theme.accent());
            Render2D.text(context, Vanta.mc().textRenderer, name, screenWidth - width - 1.0f, y + 3.5f, 0.85f,
                    Theme.TEXT_DIM, false);
            y += 16.0f;
        }
    }
}
