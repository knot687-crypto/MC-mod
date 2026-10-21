package dev.vanta.core;

import dev.vanta.Vanta;
import dev.vanta.util.Angles;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;

/** Steps the camera toward a target in mouse-sized increments. */
public final class Rotations {

    private Rotations() {
    }

    /**
     * @param speed  fraction of the remaining delta applied this tick (0-1)
     * @param noise  extra gaussian degrees, use 0 for a clean lock
     * @return true when the crosshair is within one step of the target
     */
    public static boolean look(LivingEntity target, double speed, double noise) {
        var player = Vanta.mc().player;
        if (player == null || target == null) {
            return false;
        }
        var eyes = player.getEyePos();
        var point = target.getBoundingBox().getCenter();
        float[] wanted = Angles.rotationTo(eyes.x, eyes.y, eyes.z, point.x, point.y, point.z);

        float yawDelta = Angles.deltaYaw(player.getYaw(), wanted[0]);
        float pitchDelta = wanted[1] - player.getPitch();

        float stepYaw = (float) (yawDelta * speed) + (float) Angles.gaussianNoise((float) noise);
        float stepPitch = (float) (pitchDelta * speed) + (float) Angles.gaussianNoise((float) noise);

        stepYaw = Angles.snap(MathHelper.clamp(stepYaw, -55.0f, 55.0f));
        stepPitch = Angles.snap(MathHelper.clamp(stepPitch, -35.0f, 35.0f));

        player.setYaw(player.getYaw() + stepYaw);
        player.setPitch(MathHelper.clamp(player.getPitch() + stepPitch, -90.0f, 90.0f));

        return Math.abs(yawDelta) < 2.5f && Math.abs(pitchDelta) < 2.5f;
    }

    /** Is the crosshair already inside the tolerance window for this target? */
    public static boolean aligned(LivingEntity target, double tolerance) {
        var player = Vanta.mc().player;
        if (player == null || target == null) {
            return false;
        }
        var eyes = player.getEyePos();
        var point = target.getBoundingBox().getCenter();
        float[] wanted = Angles.rotationTo(eyes.x, eyes.y, eyes.z, point.x, point.y, point.z);
        return Math.abs(Angles.deltaYaw(player.getYaw(), wanted[0])) <= tolerance
                && Math.abs(wanted[1] - player.getPitch()) <= tolerance;
    }

    /** Raw angle from the current camera to a target, for gating. */
    public static double angleTo(LivingEntity target) {
        var player = Vanta.mc().player;
        if (player == null) {
            return 180.0;
        }
        return Angles.angleTo(player, target);
    }
}
