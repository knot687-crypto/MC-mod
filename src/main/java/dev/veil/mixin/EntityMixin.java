package dev.veil.mixin;

import dev.veil.modules.combat.HitBox;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method = "getBoundingBox", at = @At("RETURN"), cancellable = true)
    private void veil$expandHitbox(CallbackInfoReturnable<Box> info) {
        Entity self = (Entity) (Object) this;
        if (!HitBox.shouldExpand(self)) {
            return;
        }
        info.setReturnValue(info.getReturnValue().expand(HitBox.expansion()));
    }
}
