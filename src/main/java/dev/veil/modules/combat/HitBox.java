package dev.veil.modules.combat;

import dev.veil.Veil;
import dev.veil.core.BoolSetting;
import dev.veil.core.Category;
import dev.veil.core.Module;
import dev.veil.core.NumberSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

public final class HitBox extends Module {
    private final NumberSetting scale = add(new NumberSetting("Scale", "Hitbox multiplier for other entities.", 1.25, 1.0, 2.0, 0.05, "x"));
    private final BoolSetting players = add(new BoolSetting("Players Only", "Leave mobs untouched.", true));

    public HitBox() {
        super("Hit Box", "Expands target boxes client side for easier tracking.", Category.COMBAT);
    }

    public static boolean shouldExpand(Entity entity) {
        HitBox module = Veil.modules().get(HitBox.class);
        if (module == null || !module.isEnabled()) {
            return false;
        }
        if (!(entity instanceof LivingEntity)) {
            return false;
        }
        if (module.players.value() && !(entity instanceof PlayerEntity)) {
            return false;
        }
        return entity != net.minecraft.client.MinecraftClient.getInstance().player;
    }

    public static double expansion() {
        HitBox module = Veil.modules().get(HitBox.class);
        if (module == null) {
            return 0.0;
        }
        return (module.scale.value() - 1.0) * 0.5;
    }
}
