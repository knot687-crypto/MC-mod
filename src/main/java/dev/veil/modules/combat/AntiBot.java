package dev.veil.modules.combat;

import dev.veil.Veil;
import dev.veil.core.BoolSetting;
import dev.veil.core.Category;
import dev.veil.core.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Drops synthetic entities that servers spawn to bait aim and trigger features.
 */
public final class AntiBot extends Module {
    private final BoolSetting uuid = add(new BoolSetting("UUID", "Reject entities with non v4 UUIDs.", true));
    private final BoolSetting tab = add(new BoolSetting("Tab", "Reject players missing from the tab list.", true));
    private final BoolSetting ping = add(new BoolSetting("Ping", "Reject entries with a zero read latency.", false));

    public AntiBot() {
        super("Anti Bot", "Filters server side decoys out of every target list.", Category.COMBAT);
    }

    public static boolean accepts(LivingEntity entity) {
        AntiBot module = Veil.modules().get(AntiBot.class);
        if (module == null || !module.isEnabled()) {
            return true;
        }
        if (!(entity instanceof PlayerEntity player)) {
            return true;
        }
        if (module.uuid.value() && player.getUuid().version() != 4) {
            return false;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getNetworkHandler() == null) {
            return true;
        }
        PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(player.getUuid());
        if (module.tab.value() && entry == null) {
            return false;
        }
        if (module.ping.value() && entry != null && entry.getLatency() <= 0) {
            return false;
        }
        return true;
    }
}
