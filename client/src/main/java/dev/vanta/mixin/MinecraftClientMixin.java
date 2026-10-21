package dev.vanta.mixin;

import dev.vanta.Vanta;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void vanta$tick(CallbackInfo info) {
        Vanta.tick();
    }
}
