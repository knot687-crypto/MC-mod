package dev.vanta.modules.combat;

import dev.vanta.Vanta;
import dev.vanta.core.Category;
import dev.vanta.core.Module;
import dev.vanta.core.Rotations;
import dev.vanta.core.Stats;
import dev.vanta.core.Targeting;
import dev.vanta.core.setting.BoolSetting;
import dev.vanta.core.setting.ModeSetting;
import dev.vanta.core.setting.NumberSetting;
import dev.vanta.util.Clicker;
import dev.vanta.util.CombatUtil;
import dev.vanta.util.Input;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Hand;
import org.lwjgl.glfw.GLFW;

/** Rotates onto the closest legal target and clicks it at a human rate. */
public final class KillAura extends Module {

    private final NumberSetting range = num("Range", 3.1, 2.0, 6.0, 0.05);
    private final NumberSetting minCps = num("Min CPS", 9, 1, 20, 1);
    private final NumberSetting maxCps = num("Max CPS", 13, 1, 20, 1);
    private final NumberSetting fov = num("FOV", 120, 10, 360, 5);
    private final NumberSetting rotationSpeed = num("Rot Speed", 0.55, 0.1, 1.0, 0.01);
    private final NumberSetting tolerance = num("Aim Tol", 6.0, 0.0, 30.0, 0.5);
    private final ModeSetting sort = mode("Sort", "Distance", "Distance", "Angle", "Health");
    private final BoolSetting rotations = bool("Rotations", true);
    private final BoolSetting visibleOnly = bool("Visible", true);
    private final BoolSetting mobs = bool("Mobs", false);
    private final BoolSetting weaponOnly = bool("Weapon Only", true);
    private final BoolSetting holdToAim = bool("Hold Click", false);

    private final Clicker clicker = new Clicker();

    public KillAura() {
        super("KillAura", "Rotates and swings at the closest legal target", Category.COMBAT);
        bind(GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onDisable() {
        clicker.reset();
    }

    @Override
    public void onTick() {
        var client = Vanta.mc();
        var player = client.player;
        if (player == null || client.world == null || client.interactionManager == null) {
            return;
        }
        if (player.isDead() || player.getHealth() <= 0.0f) {
            return;
        }
        if (weaponOnly.get() && !CombatUtil.holdingWeapon()) {
            return;
        }
        if (holdToAim.get() && !client.options.attackKey.isPressed()) {
            return;
        }

        LivingEntity target = Targeting.best(range.get(), fov.get(), visibleOnly.get(), mobs.get(),
                true, Targeting.Sort.valueOf(sort.get()));
        if (target == null) {
            return;
        }

        if (rotations.get()) {
            Rotations.look(target, rotationSpeed.get(), 0.25);
            if (!Rotations.aligned(target, tolerance.get())) {
                return;
            }
        }

        if (player.getAttackCooldownProgress(0.5f) < 0.92f) {
            return;
        }
        if (!clicker.ready(minCps.get(), maxCps.get())) {
            return;
        }
        client.interactionManager.attackEntity(player, target);
        player.swingHand(Hand.MAIN_HAND);
        Stats.onClick();
    }
}
