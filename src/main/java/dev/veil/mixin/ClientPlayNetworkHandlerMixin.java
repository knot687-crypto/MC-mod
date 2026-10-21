package dev.veil.mixin;

import dev.veil.modules.combat.Velocity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {
    @Inject(method = "onEntityVelocityUpdate", at = @At("TAIL"))
    private void veil$onVelocity(EntityVelocityUpdateS2CPacket packet, CallbackInfo info) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null && packet.getEntityId() == mc.player.getId()) {
            Velocity.onKnockback();
        }
    }
}
