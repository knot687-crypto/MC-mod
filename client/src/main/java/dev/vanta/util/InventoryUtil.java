package dev.vanta.util;

import dev.vanta.Vanta;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.item.ItemStack;

public final class InventoryUtil {

    private static final int OFFHAND_SCREEN_SLOT = 45;

    private InventoryUtil() {
    }

    /** @return inventory index (0-35) of the item, or -1 */
    public static int find(Item item) {
        ClientPlayerEntity player = Vanta.mc().player;
        if (player == null) {
            return -1;
        }
        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = player.getInventory().getStack(slot);
            if (!stack.isEmpty() && stack.isOf(item)) {
                return slot;
            }
        }
        return -1;
    }

    public static boolean offhand(Item item) {
        ClientPlayerEntity player = Vanta.mc().player;
        return player != null && player.getOffHandStack().isOf(item);
    }

    /** Hotbar order matters: indexes shift between the inventory and the screen handler. */
    public static void moveToOffhand(int inventorySlot) {
        ClientPlayerEntity player = Vanta.mc().player;
        if (player == null || Vanta.mc().interactionManager == null) {
            return;
        }
        int screenSlot = inventorySlot < 9 ? inventorySlot + 36 : inventorySlot;
        Vanta.mc().interactionManager.clickSlot(player.playerScreenHandler.syncId,
                screenSlot, OFFHAND_SCREEN_SLOT, SlotActionType.SWAP, player);
    }
}
