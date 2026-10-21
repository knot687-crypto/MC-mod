package dev.vanta.modules.combat;

import dev.vanta.Vanta;
import dev.vanta.core.Category;
import dev.vanta.core.Module;
import dev.vanta.core.Targeting;
import dev.vanta.core.setting.BoolSetting;
import dev.vanta.core.setting.NumberSetting;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

/** Raises the offhand shield while a target is inside the react range. */
public final class AutoShield extends Module {

    private final NumberSetting range = num("Range", 4.0, 1.5, 6.0, 0.1);
    private final NumberSetting delay = num("Delay ms", 220, 50, 900, 10);
    private final BoolSetting requireTarget = bool("Needs Target", true);

    private long next;

    public AutoShield() {
        super("AutoShield", "Blocks with the offhand shield near targets", Category.COMBAT);
    }

    @Override
    public void onTick() {
        var client = Vanta.mc();
        var player = client.player;
        if (player == null || client.interactionManager == null) {
            return;
        }
        if (!player.getOffHandStack().isOf(Items.SHIELD) || player.isUsingItem()) {
            return;
        }
        if (requireTarget.get()) {
            var target = Targeting.best(range.get(), 120, false, false, true, Targeting.Sort.Distance);
            if (target == null) {
                return;
            }
        }
        long now = System.currentTimeMillis();
        if (now < next) {
            return;
        }
        next = now + (long) delay.get();
        client.interactionManager.interactItem(player, Hand.OFF_HAND);
    }

    @Override
    public void onDisable() {
        var client = Vanta.mc();
        if (client.options != null && client.currentScreen == null) {
            client.options.useKey.setPressed(false);
        }
    }
}
