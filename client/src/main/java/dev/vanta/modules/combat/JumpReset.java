package dev.vanta.modules.combat;

import dev.vanta.Vanta;
import dev.vanta.core.Category;
import dev.vanta.core.Module;
import dev.vanta.core.Ticker;
import dev.vanta.core.setting.BoolSetting;
import dev.vanta.core.setting.NumberSetting;
import dev.vanta.util.MathUtil;

/** Jumps the tick after a hit lands so the knockback is reduced. */
public final class JumpReset extends Module {

    private final NumberSetting chance = num("Chance %", 70, 1, 100, 1);
    private final NumberSetting delay = num("Delay", 1, 0, 4, 1);
    private final BoolSetting groundOnly = bool("Ground Only", true);

    public JumpReset() {
        super("JumpReset", "Auto-jumps the knockback on incoming hits", Category.COMBAT);
    }

    @Override
    public void onKnockback(double x, double y, double z) {
        var player = Vanta.mc().player;
        if (player == null || !MathUtil.roll(chance.get())) {
            return;
        }
        if (groundOnly.get() && !player.isOnGround()) {
            return;
        }
        Ticker.after(delay.asInt(), () -> {
            var current = Vanta.mc().player;
            if (current == null || !current.isOnGround() || current.isUsingItem()) {
                return;
            }
            Vanta.mc().options.jumpKey.setPressed(true);
            Ticker.after(1, () -> Vanta.mc().options.jumpKey.setPressed(false));
        });
    }
}
