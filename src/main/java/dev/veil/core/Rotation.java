package dev.veil.core;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Rotation math shared by every aim feature. Deltas are snapped to the mouse sensitivity
 * grid the same way real mouse input is, which keeps synthetic movement inside the
 * distribution a human hand produces.
 */
public final class Rotation {
    private static final MinecraftClient MC = MinecraftClient.getInstance();

    private Rotation() {
    }

    public static Angle look(Vec3d from, Vec3d to, boolean vertical) {
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, horizontal));
        return new Angle(yaw, vertical ? pitch : MC.player == null ? pitch : MC.player.getPitch());
    }

    /** Applies one smoothing step toward the target on the player camera. */
    public static void apply(Angle target, double speed, double randomization, boolean vertical) {
        ClientPlayerEntity player = MC.player;
        if (player == null) {
            return;
        }
        double clamp = Math.max(0.01, Math.min(1.0, speed / 10.0));
        float curve = (float) (clamp * 0.72 + 0.04);
        float yawDelta = MathHelper.wrapDegrees(target.yaw() - player.getYaw());
        float pitchDelta = MathHelper.wrapDegrees(target.pitch() - player.getPitch());

        if (randomization > 0.0) {
            double noise = ThreadLocalRandom.current().nextDouble(-1.0, 1.0) * randomization * 0.12;
            curve = (float) Math.max(0.01, Math.min(1.0, curve + noise));
        }

        float yawStep = quantize(yawDelta * curve);
        player.setYaw(player.getYaw() + yawStep);

        if (vertical) {
            float pitchStep = quantize(pitchDelta * curve);
            player.setPitch(MathHelper.clamp(player.getPitch() + pitchStep, -90.0f, 90.0f));
        }
    }

    public static float yawTo(Vec3d from, Vec3d to) {
        double dx = to.x - from.x;
        double dz = to.z - from.z;
        return (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
    }

    public static float pitchTo(Vec3d from, Vec3d to) {
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        return (float) -Math.toDegrees(Math.atan2(dy, horizontal));
    }

    /** Angular distance in degrees between where the player looks and a point in the world. */
    public static double angleTo(Vec3d from, Vec3d to, float yaw, float pitch) {
        float targetYaw = yawTo(from, to);
        float targetPitch = pitchTo(from, to);
        double yawDelta = MathHelper.wrapDegrees(targetYaw - yaw);
        double pitchDelta = targetPitch - pitch;
        return Math.sqrt(yawDelta * yawDelta + pitchDelta * pitchDelta);
    }

    /**
     * Minecraft's mouse pipeline multiplies raw input by a sensitivity derived step. Snapping
     * to that step removes the floating point tell of direct camera writes.
     */
    private static float quantize(float delta) {
        double sensitivity = MC.options.getMouseSensitivity().getValue();
        double f = sensitivity * 0.6 + 0.2;
        double gcd = f * f * f * 8.0 * 0.15;
        if (gcd <= 1.0E-7) {
            return delta;
        }
        return (float) (Math.round(delta / gcd) * gcd);
    }

    public record Angle(float yaw, float pitch) {
    }
}
