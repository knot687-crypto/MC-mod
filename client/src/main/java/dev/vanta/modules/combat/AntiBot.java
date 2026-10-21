package dev.vanta.modules.combat;

import dev.vanta.Vanta;
import dev.vanta.core.Category;
import dev.vanta.core.Module;
import dev.vanta.core.setting.BoolSetting;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerEntity;

/** Drops NPCs and fake players out of every target list. */
public final class AntiBot extends Module {

    private static AntiBot instance;

    private final BoolSetting tabList = bool("Tab List", true);
    private final BoolSetting invisible = bool("Invisible", true);

    public AntiBot() {
        super("AntiBot", "Ignores NPCs and tab-less fake players", Category.COMBAT);
        instance = this;
    }

    /** @return true when the entity should never be targeted */
    public static boolean filtered(PlayerEntity player) {
        AntiBot module = instance;
        if (module == null || !module.enabled()) {
            return false;
        }
        if (module.invisible.get() && player.isInvisible()) {
            return true;
        }
        if (module.tabList.get()) {
            var handler = Vanta.mc().getNetworkHandler();
            if (handler == null) {
                return false;
            }
            PlayerListEntry entry = handler.getPlayerListEntry(player.getUuid());
            return entry == null;
        }
        return false;
    }
}
