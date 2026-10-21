package dev.vanta.modules.combat;

import dev.vanta.Vanta;
import dev.vanta.core.Category;
import dev.vanta.core.Module;
import dev.vanta.core.Rotations;
import dev.vanta.core.Targeting;
import dev.vanta.core.setting.BoolSetting;
import dev.vanta.core.setting.ModeSetting;
import dev.vanta.core.setting.NumberSetting;
import net.minecraft.entity.LivingEntity;

/** Small, slow camera pull — never a snap, never through walls. */
public final class AimAssist extends Module {

    private final NumberSetting range = num("Range", 4.5, 1.0, 8.0, 0.1);
    private final NumberSetting fov = num("FOV", 40, 5, 180, 1);
    private final NumberSetting strength = num("Strength", 0.35, 0.05, 1.0, 0.01);
    private final NumberSetting smoothness = num("Smooth", 0.30, 0.05, 1.0, 0.01);
    private final NumberSetting noise = num("Noise", 0.35, 0.0, 2.0, 0.05);
    private final BoolSetting visible = bool("Visible", true);
    private final BoolSetting vertical = bool("Vertical", true);
    private final ModeSetting sort = mode("Sort", "Angle", "Angle", "Distance");

    public AimAssist() {
        super("AimAssist", "Soft camera pull toward the closest angle", Category.COMBAT);
    }

    @Override
    public void onTick() {
        var player = Vanta.mc().player;
        if (player == null) {
            return;
        }
        LivingEntity target = Targeting.best(range.get(), fov.get(), visible.get(), false,
                true, Targeting.Sort.valueOf(sort.get()));
        if (target == null) {
            return;
        }
        float pitch = player.getPitch();
        Rotations.look(target, strength.get() * smoothness.get() * 2.0, noise.get());
        if (!vertical.get()) {
            player.setPitch(pitch);
        }
    }
}
