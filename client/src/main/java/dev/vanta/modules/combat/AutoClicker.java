package dev.vanta.modules.combat;

import dev.vanta.Vanta;
import dev.vanta.core.Category;
import dev.vanta.core.Module;
import dev.vanta.core.Stats;
import dev.vanta.core.setting.BoolSetting;
import dev.vanta.core.setting.NumberSetting;
import dev.vanta.util.Clicker;
import dev.vanta.util.CombatUtil;
import net.minecraft.entity.Entity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;

/** Clicks at a randomised rate, optionally only on an entity or while holding attack. */
public final class AutoClicker extends Module {

    private final NumberSetting minCps = num("Min CPS", 8, 1, 20, 1);
    private final NumberSetting maxCps = num("Max CPS", 14, 1, 20, 1);
    private final BoolSetting hold = bool("Hold Click", true);
    private final BoolSetting onlyEntity = bool("Entity Only", true);
    private final BoolSetting cooldown = bool("Cooldown", true);
    private final BoolSetting weapons = bool("Weapons Only", false);

    private final Clicker clicker = new Clicker();

    public AutoClicker() {
        super("AutoClicker", "Clicks for you with humanised spacing", Category.COMBAT);
    }

    @Override
    public void onDisable() {
        clicker.reset();
    }

    @Override
    public void onTick() {
        var client = Vanta.mc();
        var player = client.player;
        if (player == null || client.interactionManager == null) {
            return;
        }
        if (hold.get() && !client.options.attackKey.isPressed()) {
            return;
        }
        if (weapons.get() && !CombatUtil.holdingWeapon()) {
            return;
        }
        if (cooldown.get() && player.getAttackCooldownProgress(0.5f) < 0.95f) {
            return;
        }

        Entity target = null;
        if (client.crosshairTarget instanceof EntityHitResult hit) {
            target = hit.getEntity();
        }
        if (onlyEntity.get() && target == null) {
            return;
        }
        if (!clicker.ready(minCps.get(), maxCps.get())) {
            return;
        }
        if (target != null) {
            client.interactionManager.attackEntity(player, target);
        }
        player.swingHand(Hand.MAIN_HAND);
        Stats.onClick();
    }
}
