package dev.veil.modules.combat;

import dev.veil.core.BoolSetting;
import dev.veil.core.Category;
import dev.veil.core.Input;
import dev.veil.core.Module;
import dev.veil.core.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.EntityHitResult;

import java.util.concurrent.ThreadLocalRandom;

public final class TriggerBot extends Module {
    private static final MinecraftClient MC = MinecraftClient.getInstance();

    private final NumberSetting range = add(new NumberSetting("Range", "Maximum trigger distance.", 3.2, 1.0, 6.0, 0.1, ""));
    private final NumberSetting delay = add(new NumberSetting("Delay", "Reaction time before the hit fires.", 55.0, 0.0, 300.0, 5.0, "ms"));
    private final BoolSetting weapon = add(new BoolSetting("Weapon Only", "Only trigger while holding a sword or axe.", false));
    private final BoolSetting players = add(new BoolSetting("Players Only", "Ignore mobs.", true));

    private long lockedAt;
    private Entity locked;

    public TriggerBot() {
        super("Trigger Bot", "Attacks the moment the crosshair settles on a valid target.", Category.COMBAT);
    }

    @Override
    public void onTick() {
        ClientPlayerEntity player = MC.player;
        if (player == null || MC.currentScreen != null || MC.interactionManager == null) {
            locked = null;
            return;
        }

        Entity hovered = null;
        if (MC.crosshairTarget instanceof EntityHitResult hit && hit.getEntity() instanceof LivingEntity living
                && living != player && living.isAlive()) {
            if (!players.value() || living.getType() == net.minecraft.entity.EntityType.PLAYER) {
                if (player.squaredDistanceTo(living) <= range.value() * range.value()) {
                    hovered = living;
                }
            }
        }

        if (hovered == null || !validWeapon()) {
            locked = null;
            lockedAt = 0L;
            return;
        }

        long now = System.currentTimeMillis();
        if (hovered != locked) {
            locked = hovered;
            double spread = ThreadLocalRandom.current().nextDouble(-16.0, 16.0);
            lockedAt = now + Math.max(0L, Math.round(delay.value() + spread));
            return;
        }
        if (now < lockedAt || player.getAttackCooldownProgress(0.0f) < 0.92f) {
            return;
        }

        Input.attack(hovered);
        locked = null;
        lockedAt = 0L;
    }

    private boolean validWeapon() {
        if (!weapon.value() || MC.player == null) {
            return true;
        }
        String id = MC.player.getMainHandStack().getItem().getTranslationKey();
        return id.contains("sword") || id.contains("axe");
    }
}
