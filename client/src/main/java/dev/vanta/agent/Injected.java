package dev.vanta.agent;

import dev.vanta.Vanta;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

/**
 * Bootstrap for the injected path.
 *
 * <p>Packaged inside the client jar and loaded by a URLClassLoader parented to the game
 * loader, so Minecraft classes resolve normally while the client's own classes stay in
 * a child loader. Everything the bytecode hooks can reach is {@link Bridge}.
 */
public final class Injected {

    private static boolean wired;
    private static long lastError;

    private Injected() {
    }

    public static void start(ClassLoader loader) {
        if (wired) {
            return;
        }
        wired = true;
        Vanta.start();

        Bridge.tick = ignored -> guard(Vanta::tick);
        Bridge.hud = context -> guard(() -> Vanta.renderHud((DrawContext) context));
        Bridge.attack = (player, target) -> guard(() -> {
            if (player == Vanta.mc().player && target instanceof Entity entity) {
                Vanta.onAttack(entity);
            }
        });
        Bridge.velocityFilter = payload -> {
            Object[] args = (Object[]) payload;
            if (!(args[0] instanceof Entity entity) || !(args[1] instanceof Vec3d velocity)) {
                return null;
            }
            if (entity != Vanta.mc().player) {
                return null;
            }
            double[] scaled = Vanta.onVelocity(velocity.x, velocity.y, velocity.z);
            return scaled == null ? null : new Vec3d(scaled[0], scaled[1], scaled[2]);
        };
    }

    /** Never let a module exception kill the render or tick thread. */
    private static void guard(Runnable action) {
        try {
            action.run();
        } catch (Throwable throwable) {
            long now = System.currentTimeMillis();
            if (now - lastError > 5000L) {
                lastError = now;
                System.err.println("[vanta] hook error: " + throwable);
            }
        }
    }
}
