package dev.vanta.mixin;

import dev.vanta.Vanta;
import dev.vanta.modules.combat.HitBox;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityMixin {

    @Inject(method = "getBoundingBox", at = @At("RETURN"), cancellable = true)
    private void vanta$boundingBox(CallbackInfoReturnable<Box> info) {
        Entity self = (Entity) (Object) this;
        Box original = info.getReturnValue();
        Box expanded = HitBox.apply(self, original);
        if (expanded != original) {
            info.setReturnValue(expanded);
        }
    }

    @Inject(method = "setVelocityClient", at = @At("HEAD"), cancellable = true)
    private void vanta$velocity(Vec3d velocity, CallbackInfo info) {
        Entity self = (Entity) (Object) this;
        if (!(self instanceof ClientPlayerEntity) || self != Vanta.mc().player) {
            return;
        }
        double[] scaled = Vanta.onVelocity(velocity.x, velocity.y, velocity.z);
        if (scaled != null) {
            self.setVelocity(scaled[0], scaled[1], scaled[2]);
            self.velocityDirty = true;
            info.cancel();
        }
    }
}
