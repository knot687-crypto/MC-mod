package dev.vanta.modules.combat;

import dev.vanta.Vanta;
import dev.vanta.core.Category;
import dev.vanta.core.Module;
import dev.vanta.core.Stats;
import dev.vanta.core.setting.BoolSetting;
import dev.vanta.core.setting.NumberSetting;
import dev.vanta.util.InventoryUtil;
import net.minecraft.item.Items;

/** Keeps a totem in the offhand once health drops below the threshold. */
public final class AutoTotem extends Module {

    private final NumberSetting threshold = num("Health", 16, 2, 20, 1);
    private final NumberSetting delay = num("Delay ms", 120, 0, 800, 10);
    private final BoolSetting inCombatOnly = bool("Combat Only", false);

    private long next;

    public AutoTotem() {
        super("AutoTotem", "Swaps a totem into the offhand when low", Category.COMBAT);
    }

    @Override
    public void onTick() {
        var player = Vanta.mc().player;
        if (player == null) {
            return;
        }
        if (player.getHealth() > threshold.get()) {
            return;
        }
        if (inCombatOnly.get() && System.currentTimeMillis() - Stats.lastCombat() > 4000L) {
            return;
        }
        if (InventoryUtil.offhand(Items.TOTEM_OF_UNDYING)) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now < next) {
            return;
        }
        int slot = InventoryUtil.find(Items.TOTEM_OF_UNDYING);
        if (slot < 0) {
            return;
        }
        InventoryUtil.moveToOffhand(slot);
        next = now + (long) delay.get();
    }
}
