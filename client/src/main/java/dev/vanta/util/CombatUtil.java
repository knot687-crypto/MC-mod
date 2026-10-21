package dev.vanta.util;

import dev.vanta.Vanta;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;

public final class CombatUtil {

    private CombatUtil() {
    }

    /** 1.21.5+ has no SwordItem class; weapons are flagged by the WEAPON component. */
    public static boolean holdingWeapon() {
        var player = Vanta.mc().player;
        if (player == null) {
            return false;
        }
        ItemStack stack = player.getMainHandStack();
        return !stack.isEmpty() && stack.getComponents().contains(DataComponentTypes.WEAPON);
    }

    public static boolean cooldownReady(float threshold) {
        var player = Vanta.mc().player;
        return player != null && player.getAttackCooldownProgress(0.5f) >= threshold;
    }
}
