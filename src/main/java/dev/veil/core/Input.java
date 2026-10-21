package dev.veil.core;

import dev.veil.mixin.MinecraftClientAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.Hand;

public final class Input {
    private static final MinecraftClient MC = MinecraftClient.getInstance();

    private Input() {
    }

    /** Runs vanilla's own attack path so cooldowns, blocking and swing packets stay intact. */
    public static void leftClick() {
        // Entity hits are counted by the interaction mixin; only count the rest here.
        if (!(MC.crosshairTarget instanceof net.minecraft.util.hit.EntityHitResult)) {
            CombatStats.onClick();
        }
        ((MinecraftClientAccessor) MC).veil$doAttack();
    }

    public static void attack(Entity target) {
        if (MC.player == null || MC.interactionManager == null) {
            return;
        }
        MC.interactionManager.attackEntity(MC.player, target);
        MC.player.swingHand(Hand.MAIN_HAND);
    }

    public static void swing() {
        if (MC.player != null) {
            MC.player.swingHand(Hand.MAIN_HAND);
        }
    }
}
