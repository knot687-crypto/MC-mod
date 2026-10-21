package dev.vanta.util;

import dev.vanta.Vanta;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

public final class PacketUtil {

    private PacketUtil() {
    }

    /**
     * Sends a single position tick with a tiny upward offset and onGround=false.
     * Vanilla flags the next attack as a critical hit; the regular position
     * packet on the following tick restores the truth.
     */
    public static void critSpoof(ClientPlayerEntity player) {
        if (player.networkHandler == null) {
            return;
        }
        double y = player.getY() + 0.0625;
        player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                player.getX(), y, player.getZ(), false, player.horizontalCollision));
    }

    public static void sprint(ClientPlayerEntity player, boolean sprinting) {
        if (player.networkHandler == null) {
            return;
        }
        ClientCommandC2SPacket.Mode mode = sprinting
                ? ClientCommandC2SPacket.Mode.START_SPRINTING
                : ClientCommandC2SPacket.Mode.STOP_SPRINTING;
        player.networkHandler.sendPacket(new ClientCommandC2SPacket(player, mode));
    }

    public static void swing() {
        if (Vanta.mc().player != null) {
            Vanta.mc().player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
        }
    }
}
