package dev.vanta;

import dev.vanta.agent.Bridge;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * Entry point for the injected path.
 *
 * <p>Loaded through the agent's remapping class loader, so Minecraft classes here are
 * already the running game's classes. It does nothing but connect {@link Bridge} to the
 * client core, and it never throws into a game thread.
 */
public final class Bootstrap {

    private static long lastError;

    private Bootstrap() {
    }

    public static void start() {
        Bridge.tick = ignored -> guard(Vanta::tick);
        Bridge.hud = context -> guard(() -> {
            if (context instanceof DrawContext drawContext) {
                Vanta.renderHud(drawContext);
            }
        });
        Bridge.attack = (player, target) -> guard(() -> {
            MinecraftClient client = Vanta.mc();
            if (target instanceof Entity entity && client != null && player == client.player) {
                Vanta.onAttack(entity);
            }
        });
        Bridge.velocity = (entity, velocity) -> {
            if (!(entity instanceof Entity self) || !(velocity instanceof Vec3d vec)) {
                return null;
            }
            MinecraftClient client = Vanta.mc();
            if (client == null || self != client.player) {
                return null;
            }
            double[] scaled = Vanta.onVelocity(vec.x, vec.y, vec.z);
            return scaled == null ? null : new Vec3d(scaled[0], scaled[1], scaled[2]);
        };
        Bridge.boundingBox = (entity, box) -> {
            if (!(entity instanceof Entity self) || !(box instanceof Box original) || !Vanta.started()) {
                return null;
            }
            Box expanded = Vanta.onBoundingBox(self, original);
            return expanded == original ? null : expanded;
        };

        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            // Boot on the render thread so module construction never races the game loop.
            client.execute(() -> guard(Vanta::start));
        } else {
            Vanta.start();
        }
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
