package dev.vanta.modules.combat;

import dev.vanta.Vanta;
import dev.vanta.core.Category;
import dev.vanta.core.Module;
import dev.vanta.core.Ticker;
import dev.vanta.core.setting.NumberSetting;
import dev.vanta.util.MathUtil;
import dev.vanta.util.PacketUtil;
import net.minecraft.entity.Entity;

/** Drops sprint for a couple of ticks after every hit, then restores it. */
public final class WTap extends Module {

    private final NumberSetting chance = num("Chance %", 100, 1, 100, 1);
    private final NumberSetting holdTicks = num("Hold Ticks", 2, 1, 6, 1);

    public WTap() {
        super("WTap", "Sprint resets on hit to hold the knockback", Category.COMBAT);
    }

    @Override
    public void onAttack(Entity target) {
        var player = Vanta.mc().player;
        if (player == null || target == null || !player.isSprinting() || !MathUtil.roll(chance.get())) {
            return;
        }
        player.setSprinting(false);
        PacketUtil.sprint(player, false);
        Ticker.after(holdTicks.asInt(), () -> {
            var current = Vanta.mc().player;
            if (current != null && !current.isSneaking() && current.forwardSpeed > 0.0f) {
                current.setSprinting(true);
            }
        });
    }
}
