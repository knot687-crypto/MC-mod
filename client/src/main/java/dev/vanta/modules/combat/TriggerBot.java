package dev.vanta.modules.combat;

import dev.vanta.Vanta;
import dev.vanta.core.Category;
import dev.vanta.core.Module;
import dev.vanta.core.Stats;
import dev.vanta.core.setting.BoolSetting;
import dev.vanta.core.setting.NumberSetting;
import dev.vanta.util.Clicker;
import dev.vanta.util.CombatUtil;
import dev.vanta.util.MathUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;

/** Attacks exactly what the crosshair already touches. */
public final class TriggerBot extends Module {

    private final NumberSetting range = num("Range", 3.0, 2.0, 5.0, 0.05);
    private final NumberSetting minCps = num("Min CPS", 8, 1, 20, 1);
    private final NumberSetting maxCps = num("Max CPS", 12, 1, 20, 1);
    private final NumberSetting missChance = num("Miss %", 0, 0, 30, 1);
    private final BoolSetting weapons = bool("Weapons Only", true);
    private final BoolSetting playersOnly = bool("Players Only", true);
    private final BoolSetting cooldown = bool("Cooldown", true);

    private final Clicker clicker = new Clicker();

    public TriggerBot() {
        super("TriggerBot", "Swings when the crosshair lands on a target", Category.COMBAT);
    }

    @Override
    public void onTick() {
        var client = Vanta.mc();
        var player = client.player;
        if (player == null || client.interactionManager == null || client.currentScreen != null) {
            return;
        }
        if (!(client.crosshairTarget instanceof EntityHitResult hit)) {
            return;
        }
        Entity entity = hit.getEntity();
        if (!(entity instanceof LivingEntity living) || !living.isAlive()) {
            return;
        }
        if (playersOnly.get() && !(entity instanceof net.minecraft.entity.player.PlayerEntity)) {
            return;
        }
        if (player.distanceTo(entity) > range.get()) {
            return;
        }
        if (weapons.get() && !CombatUtil.holdingWeapon()) {
            return;
        }
        if (cooldown.get() && player.getAttackCooldownProgress(0.5f) < 0.95f) {
            return;
        }
        if (!clicker.ready(minCps.get(), maxCps.get())) {
            return;
        }
        if (MathUtil.roll(missChance.get())) {
            return;
        }
        client.interactionManager.attackEntity(player, entity);
        player.swingHand(Hand.MAIN_HAND);
        Stats.onClick();
    }
}
