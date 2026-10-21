package dev.vanta.util;

import java.util.concurrent.ThreadLocalRandom;

public final class MathUtil {

    private MathUtil() {
    }

    public static double clamp(double value, double min, double max) {
        return value < min ? min : Math.min(value, max);
    }

    public static float clamp(float value, float min, float max) {
        return value < min ? min : Math.min(value, max);
    }

    public static int clamp(int value, int min, int max) {
        return value < min ? min : Math.min(value, max);
    }

    public static double random(double min, double max) {
        if (max <= min) {
            return min;
        }
        return ThreadLocalRandom.current().nextDouble(min, max);
    }

    public static float random(float min, float max) {
        if (max <= min) {
            return min;
        }
        return (float) ThreadLocalRandom.current().nextDouble(min, max);
    }

    public static int random(int min, int max) {
        if (max <= min) {
            return min;
        }
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    /** Human-ish gaussian jitter, clamped to +/- 1. */
    public static double gaussian() {
        return clamp(ThreadLocalRandom.current().nextGaussian() * 0.5, -1.0, 1.0);
    }

    public static double chance(double percent) {
        return ThreadLocalRandom.current().nextDouble(100.0) < percent ? 1 : 0;
    }

    public static boolean roll(double percent) {
        return ThreadLocalRandom.current().nextDouble(100.0) < percent;
    }

    /** Frame-rate independent easing factor. */
    public static float ease(float current, float target, float speed, float deltaSeconds) {
        float factor = 1.0f - (float) Math.exp(-speed * deltaSeconds);
        return current + (target - current) * factor;
    }
}
