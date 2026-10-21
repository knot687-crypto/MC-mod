package dev.vanta.core;

import dev.vanta.Vanta;
import dev.vanta.util.MathUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;

import java.util.ArrayDeque;
import java.util.Deque;

/** Click / reach / fps / ping counters shown by the HUD. */
public final class Stats {

    private static final Deque<Long> CLICKS = new ArrayDeque<>();
    private static double reach;
    private static long reachAt;
    private static long lastCombat;

    private Stats() {
    }

    public static void tick() {
        long cutoff = System.currentTimeMillis() - 1000L;
        while (!CLICKS.isEmpty() && CLICKS.peekFirst() < cutoff) {
            CLICKS.pollFirst();
        }
    }

    public static void onClick() {
        CLICKS.addLast(System.currentTimeMillis());
    }

    public static void onAttack(Entity target) {
        if (target == null || Vanta.mc().player == null) {
            return;
        }
        reach = Math.sqrt(Vanta.mc().player.squaredDistanceTo(target));
        reachAt = System.currentTimeMillis();
        lastCombat = reachAt;
    }

    public static long lastCombat() {
        return lastCombat;
    }

    public static int cps() {
        tick();
        return CLICKS.size();
    }

    public static double reach() {
        return System.currentTimeMillis() - reachAt > 3000L ? 0.0 : reach;
    }

    public static int fps() {
        return MinecraftClient.getInstance().getCurrentFps();
    }

    public static int ping() {
        MinecraftClient client = Vanta.mc();
        if (client.player == null || client.getNetworkHandler() == null) {
            return 0;
        }
        var entry = client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
        return entry == null ? 0 : entry.getLatency();
    }

    public static int activeModules() {
        return ModuleManager.enabledCount();
    }

    public static String fmt(double value) {
        return MathUtil.clamp(value, 0, 9999) == 0 ? "0.0" : String.format("%.1f", value);
    }
}
