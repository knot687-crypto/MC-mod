package dev.vanta.modules.combat;

import dev.vanta.Vanta;
import dev.vanta.core.Category;
import dev.vanta.core.Module;
import dev.vanta.core.Ticker;
import dev.vanta.core.setting.BoolSetting;
import dev.vanta.core.setting.ModeSetting;
import dev.vanta.core.setting.NumberSetting;
import dev.vanta.util.MathUtil;
import dev.vanta.util.PacketUtil;
import net.minecraft.entity.Entity;

/** Fakes the falling state for one tick so the hit registers as a critical. */
public final class Criticals extends Module {

    private final ModeSetting mode = mode("Mode", "Packet", "Packet", "Jump");
    private final NumberSetting chance = num("Chance %", 100, 1, 100, 1);
    private final BoolSetting releaseSprint = bool("Drop Sprint", true);
    private final NumberSetting holdTicks = num("Hold", 2, 1, 5, 1);

    public Criticals() {
        super("Criticals", "Turns grounded hits into critical hits", Category.COMBAT);
    }

    @Override
    public void onAttack(Entity target) {
        var player = Vanta.mc().player;
        if (player == null || target == null || !MathUtil.roll(chance.get())) {
            return;
        }
        if (!player.isOnGround() || player.isClimbing() || player.isTouchingWater()) {
            return;
        }
        if (mode.is("Jump")) {
            Vanta.mc().options.jumpKey.setPressed(true);
            Ticker.after(1, () -> Vanta.mc().options.jumpKey.setPressed(false));
            return;
        }
        if (releaseSprint.get() && player.isSprinting()) {
            player.setSprinting(false);
            PacketUtil.sprint(player, false);
            Ticker.after(holdTicks.asInt(), () -> {
                var current = Vanta.mc().player;
                if (current != null && Vanta.mc().options.forwardKey.isPressed()) {
                    current.setSprinting(true);
                }
            });
        }
        PacketUtil.critSpoof(player);
    }
}
