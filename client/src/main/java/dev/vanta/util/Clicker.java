package dev.vanta.util;

/** Schedules clicks from a cps range with gaussian spacing, never a fixed beat. */
public final class Clicker {

    private long next;

    public boolean ready(double minCps, double maxCps) {
        long now = System.nanoTime();
        if (now < next) {
            return false;
        }
        double low = Math.max(1.0, Math.min(minCps, maxCps));
        double high = Math.max(low, Math.max(minCps, maxCps));
        double cps = MathUtil.random(low, high) + MathUtil.gaussian() * 0.45;
        cps = MathUtil.clamp(cps, 1.0, 22.0);
        double intervalMs = 1000.0 / cps;
        next = now + (long) (intervalMs * 1_000_000.0);
        return true;
    }

    public void reset() {
        next = 0L;
    }
}
