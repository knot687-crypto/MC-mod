package dev.vanta.modules.combat;

import dev.vanta.Vanta;
import dev.vanta.core.Category;
import dev.vanta.core.Module;
import dev.vanta.core.setting.BoolSetting;
import dev.vanta.core.setting.NumberSetting;
import dev.vanta.util.MathUtil;

/** Scales knockback on the way in; the caller applies the returned velocity. */
public final class Velocity extends Module {

    private final NumberSetting horizontal = num("Horizontal %", 0, 0, 100, 1);
    private final NumberSetting vertical = num("Vertical %", 0, 0, 100, 1);
    private final NumberSetting chance = num("Chance %", 100, 1, 100, 1);
    private final BoolSetting onlyGround = bool("Ground Only", false);

    public Velocity() {
        super("Velocity", "Reduces knockback taken from hits", Category.COMBAT);
    }

    @Override
    public double[] onVelocity(double x, double y, double z) {
        var player = Vanta.mc().player;
        if (player == null) {
            return null;
        }
        if (onlyGround.get() && !player.isOnGround()) {
            return null;
        }
        if (!MathUtil.roll(chance.get())) {
            return null;
        }
        return new double[]{x * (horizontal.get() / 100.0), y * (vertical.get() / 100.0), z * (horizontal.get() / 100.0)};
    }
}
