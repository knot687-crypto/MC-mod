package dev.vanta.util;

import dev.vanta.Vanta;
import net.minecraft.client.MinecraftClient;

/** Tracks which keys are held so binds fire once per press. */
public final class Input {

    private static final int RANGE = 512;
    private static final boolean[] DOWN = new boolean[RANGE];
    private static final boolean[] PREV = new boolean[RANGE];

    private Input() {
    }

    public static void poll() {
        MinecraftClient client = Vanta.mc();
        var window = client.getWindow();
        for (int key = 32; key < RANGE; key++) {
            PREV[key] = DOWN[key];
            DOWN[key] = window != null && net.minecraft.client.util.InputUtil.isKeyPressed(window, key);
        }
    }

    public static boolean down(int key) {
        return key > 0 && key < RANGE && DOWN[key];
    }

    public static boolean justPressed(int key) {
        return key > 0 && key < RANGE && DOWN[key] && !PREV[key];
    }

    public static boolean typing() {
        MinecraftClient client = Vanta.mc();
        return client.currentScreen != null && !(client.currentScreen instanceof dev.vanta.gui.ClickGui);
    }
}
