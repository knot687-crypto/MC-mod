package dev.veil.modules.render;

import dev.veil.Veil;
import dev.veil.core.BoolSetting;
import dev.veil.core.Module;
import dev.veil.gui.Render2D;
import dev.veil.gui.Theme;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ArrayListHud extends HudModule {
    private final BoolSetting bar = add(new BoolSetting("Accent Bar", "Thin accent strip on the active edge.", true));
    private final BoolSetting lowerCase = add(new BoolSetting("Lowercase", "Softer type treatment.", false));

    private final Map<String, Float> appear = new HashMap<>();

    public ArrayListHud() {
        super("Array List", "Right rail list of everything currently running.");
    }

    @Override
    public void render(DrawContext ctx, float delta) {
        List<Module> active = new ArrayList<>();
        for (Module module : Veil.modules().all()) {
            if (module.isEnabled() && !(module instanceof HudModule)) {
                active.add(module);
            }
        }
        for (Map.Entry<String, Float> entry : appear.entrySet()) {
            if (active.stream().noneMatch(m -> m.name().equals(entry.getKey()))) {
                entry.setValue(Math.max(0.0f, entry.getValue() - delta * 6.0f));
            }
        }
        for (Module module : active) {
            appear.merge(module.name(), delta * 8.0f, (oldValue, add) -> Math.min(1.0f, oldValue + add));
        }
        active.sort(Comparator.comparingInt((Module m) -> Render2D.width(MC.textRenderer, label(m))).reversed());

        int right = MC.getWindow().getScaledWidth() - 8;
        int y = 8;
        for (Module module : active) {
            String text = label(module);
            float progress = appear.getOrDefault(module.name(), 0.0f);
            int width = Render2D.width(MC.textRenderer, text);
            int plateWidth = width + 16;
            int offset = (int) ((1.0f - progress) * (plateWidth + 8));
            int x = right - plateWidth + offset;
            int textY = y + 4;

            Render2D.rounded(ctx, x, y, plateWidth, 16, 4, 0xD90A0A10);
            Render2D.rounded(ctx, x, y, plateWidth, 16, 4, Theme.alpha(0xFFFFFF, 0.025f));
            if (bar.value()) {
                Render2D.rounded(ctx, x + plateWidth - 2, y + 4, 2, 8, 1, Theme.accent());
            }
            Render2D.text(ctx, MC.textRenderer, text, x + 8, textY, Theme.TEXT);
            y += 18;
        }
    }

    private String label(Module module) {
        return lowerCase.value() ? module.name().toLowerCase() : module.name();
    }
}
