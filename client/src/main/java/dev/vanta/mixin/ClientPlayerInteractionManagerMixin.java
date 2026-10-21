package dev.vanta.mixin;

import dev.vanta.Vanta;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {

    @Inject(method = "attackEntity", at = @At("HEAD"))
    private void vanta$attack(PlayerEntity player, Entity target, CallbackInfo info) {
        if (player == Vanta.mc().player) {
            Vanta.onAttack(target);
        }
    }
}
