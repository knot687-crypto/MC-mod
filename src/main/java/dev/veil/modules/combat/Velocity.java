package dev.veil.modules.combat;

import dev.veil.Veil;
import dev.veil.core.Category;
import dev.veil.core.Module;
import dev.veil.core.NumberSetting;
import net.minecraft.util.math.Vec3d;

import java.util.concurrent.ThreadLocalRandom;

public final class Velocity extends Module {
    private final NumberSetting horizontal = add(new NumberSetting("Horizontal", "Horizontal knockback kept.", 0.0, 0.0, 100.0, 1.0, "%"));
    private final NumberSetting vertical = add(new NumberSetting("Vertical", "Vertical knockback kept.", 0.0, 0.0, 100.0, 1.0, "%"));
    private final NumberSetting chance = add(new NumberSetting("Chance", "Chance for the reduction to apply.", 100.0, 0.0, 100.0, 1.0, "%"));

    public Velocity() {
        super("Velocity", "Trims incoming knockback before it reaches the camera.", Category.COMBAT);
    }

    /** Called from the network mixin right after vanilla applies the packet. */
    public static void onKnockback() {
        Velocity module = Veil.modules().get(Velocity.class);
        if (module == null || !module.isEnabled() || net.minecraft.client.MinecraftClient.getInstance().player == null) {
            return;
        }
        if (ThreadLocalRandom.current().nextDouble(100.0) > module.chance.value()) {
            return;
        }
        Vec3d velocity = net.minecraft.client.MinecraftClient.getInstance().player.getVelocity();
        net.minecraft.client.MinecraftClient.getInstance().player.setVelocity(
                velocity.x * (module.horizontal.value() / 100.0),
                velocity.y * (module.vertical.value() / 100.0),
                velocity.z * (module.horizontal.value() / 100.0));
    }
}
