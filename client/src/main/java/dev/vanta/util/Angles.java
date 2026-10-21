package dev.vanta.util;

import dev.vanta.Vanta;
import net.minecraft.util.math.MathHelper;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Rotation maths for the aura / aim assist.
 * Every step is snapped to the game's mouse pixel grid so the movement looks like a hand.
 */
public final class Angles {

    private static float gcd = 0.15f;
    private static int gcdTick;

    private Angles() {
    }

    /** Degrees per mouse count for the current sensitivity. Recomputed twice a second. */
    public static float gcd() {
        if (++gcdTick % 10 == 0 && Vanta.mc().options != null) {
            float sensitivity = Vanta.mc().options.getMouseSensitivity().getValue().floatValue() * 0.6f + 0.2f;
            float value = sensitivity * sensitivity * sensitivity * 8.0f * 0.15f;
            gcd = value < 0.001f ? 0.001f : value;
        }
        return gcd;
    }

    public static float snap(float delta) {
        float step = gcd();
        return Math.round(delta / step) * step;
    }

    public static float yawTo(double fromX, double fromY, double fromZ, double toX, double toY, double toZ) {
        double dx = toX - fromX;
        double dy = toY - fromY;
        double dz = toZ - fromZ;
        double dist = Math.sqrt(dx * dx + dz * dz);
        double yaw = Math.toDegrees(Math.atan2(dz, dx)) - 90.0;
        double pitch = -Math.toDegrees(Math.atan2(dy, dist));
        return (float) yaw + 0.0f * (float) pitch;
    }

    public static float pitchTo(double fromX, double fromY, double fromZ, double toX, double toY, double toZ) {
        double dx = toX - fromX;
        double dy = toY - fromY;
        double dz = toZ - fromZ;
        double dist = Math.sqrt(dx * dx + dz * dz);
        return (float) (-Math.toDegrees(Math.atan2(dy, dist)));
    }

    public static float[] rotationTo(double fromX, double fromY, double fromZ, double toX, double toY, double toZ) {
        float yaw = yawTo(fromX, fromY, fromZ, toX, toY, toZ);
        float pitch = pitchTo(fromX, fromY, fromZ, toX, toY, toZ);
        return new float[]{MathHelper.wrapDegrees(yaw), MathHelper.clamp(pitch, -90.0f, 90.0f)};
    }

    /** Signed yaw difference, wrapped to +/-180. */
    public static float deltaYaw(float from, float to) {
        return MathHelper.wrapDegrees(to - from);
    }

    public static float deltaPitch(float from, float to) {
        return to - from;
    }

    /** Angle in degrees between the player's look vector and the target, in 3D. */
    public static double angleTo(net.minecraft.entity.player.PlayerEntity player, net.minecraft.entity.Entity target) {
        net.minecraft.util.math.Vec3d eyes = player.getEyePos();
        net.minecraft.util.math.Vec3d point = target.getBoundingBox().getCenter();
        float[] rot = rotationTo(eyes.x, eyes.y, eyes.z, point.x, point.y, point.z);
        double yawDelta = Math.abs(deltaYaw(player.getYaw(), rot[0]));
        double pitchDelta = Math.abs(deltaPitch(player.getPitch(), rot[1]));
        return Math.sqrt(yawDelta * yawDelta + pitchDelta * pitchDelta);
    }

    public static double gaussianNoise(float amplitude) {
        return ThreadLocalRandom.current().nextGaussian() * amplitude * 0.35;
    }
}
