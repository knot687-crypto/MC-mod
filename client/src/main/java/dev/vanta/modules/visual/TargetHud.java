package dev.vanta.modules.visual;

import dev.vanta.Vanta;
import dev.vanta.core.Category;
import dev.vanta.core.Module;
import dev.vanta.core.Targeting;
import dev.vanta.core.setting.NumberSetting;
import dev.vanta.render.Render2D;
import dev.vanta.render.Theme;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.LivingEntity;

/** Compact target card: name, health, distance. */
public final class TargetHud extends Module {

    private final NumberSetting yOffset = num("Y Offset", 30, 10, 200, 1);
    private final NumberSetting range = num("Range", 8, 2, 20, 1);

    private float shownHealth = 1.0f;
    private LivingEntity cached;

    public TargetHud() {
        super("TargetHud", "Shows the current target under the crosshair", Category.VISUAL);
    }

    @Override
    public void onHud(DrawContext context) {
        LivingEntity target = pick();
        if (target == null) {
            cached = null;
            return;
        }
        if (cached != target) {
            cached = target;
            shownHealth = target.getHealth();
        }
        shownHealth += (target.getHealth() - shownHealth) * 0.18f;

        var client = Vanta.mc();
        float width = 132.0f;
        float height = 36.0f;
        float x = client.getWindow().getScaledWidth() / 2.0f - width / 2.0f;
        float y = client.getWindow().getScaledHeight() / 2.0f + yOffset.asFloat();

        Render2D.roundedV(context, x, y, width, height, 8.0f, Render2D.alpha(Theme.WINDOW_TOP, 225),
                Render2D.alpha(Theme.WINDOW_BOTTOM, 225));
        Render2D.roundedOutline(context, x, y, width, height, 8.0f, Theme.STROKE);

        String name = target.getName().getString();
        if (name.length() > 16) {
            name = name.substring(0, 16) + "..";
        }
        Render2D.text(context, client.textRenderer, name, x + 9.0f, y + 8.0f, 0.9f, Theme.TEXT, false);

        String distance = String.format("%.1f", Math.sqrt(client.player.squaredDistanceTo(target)));
        Render2D.text(context, client.textRenderer, distance, x + width - 9.0f - Render2D.width(client.textRenderer, distance),
                y + 8.0f, 0.85f, Theme.TEXT_FAINT, false);

        float ratio = Math.clamp(shownHealth / Math.max(1.0f, target.getMaxHealth()), 0.0f, 1.0f);
        Render2D.rounded(context, x + 9.0f, y + 23.0f, width - 18.0f, 4.0f, 2.0f, Theme.TRACK);
        Render2D.roundedV(context, x + 9.0f, y + 23.0f, Math.max(1.0f, (width - 18.0f) * ratio), 4.0f, 2.0f,
                Render2D.lerp(Theme.BAD, Theme.GOOD, ratio), Theme.accentAlt());
    }

    private LivingEntity pick() {
        var attack = Vanta.lastAttackTarget();
        if (attack instanceof LivingEntity living && living.isAlive()
                && Vanta.mc().player != null && Vanta.mc().player.distanceTo(living) <= range.get()) {
            return living;
        }
        return Targeting.best(range.get(), 180, false, false, true, Targeting.Sort.Distance);
    }
}
