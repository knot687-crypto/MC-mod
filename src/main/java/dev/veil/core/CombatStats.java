package dev.veil.core;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Rolling window of click timestamps. Shared so the hud never has to guess a click rate.
 */
public final class CombatStats {
    private static final Deque<Long> CLICKS = new ArrayDeque<>();

    private CombatStats() {
    }

    public static void onClick() {
        long now = System.currentTimeMillis();
        CLICKS.addLast(now);
        while (!CLICKS.isEmpty() && now - CLICKS.peekFirst() > 1000L) {
            CLICKS.pollFirst();
        }
    }

    public static int cps() {
        long now = System.currentTimeMillis();
        while (!CLICKS.isEmpty() && now - CLICKS.peekFirst() > 1000L) {
            CLICKS.pollFirst();
        }
        return CLICKS.size();
    }
}
