package dev.vanta.modules.combat;

import dev.vanta.Vanta;
import dev.vanta.core.Category;
import dev.vanta.core.Module;
import dev.vanta.core.setting.BoolSetting;
import dev.vanta.core.setting.NumberSetting;
import dev.vanta.util.MathUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;

/** Grows the client-side box so clicks land where the model looks. */
public final class HitBox extends Module {

    private static HitBox instance;

    private final NumberSetting expand = num("Expand", 0.12, 0.0, 0.5, 0.005);
    private final BoolSetting players = bool("Players", true);
    private final NumberSetting chance = num("Chance", 100, 1, 100, 1);

    public HitBox() {
        super("HitBox", "Expands entity hitboxes client-side", Category.COMBAT);
        instance = this;
    }

    /** Used by the entity hook; returns the original box untouched when off. */
    public static Box apply(Entity entity, Box original) {
        HitBox module = instance;
        if (module == null || !module.enabled() || entity == Vanta.mc().player) {
            return original;
        }
        if (module.players.get() && !(entity instanceof PlayerEntity)) {
            return original;
        }
        if (entity instanceof net.minecraft.entity.LivingEntity living && living.isDead()) {
            return original;
        }
        double value = module.expand.get();
        if (value <= 0.0) {
            return original;
        }
        return original.expand(value * MathUtil.random(0.85, 1.0));
    }
}
