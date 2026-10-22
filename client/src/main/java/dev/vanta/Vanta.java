package dev.vanta;

import dev.vanta.core.Config;
import dev.vanta.core.ModuleManager;
import dev.vanta.core.Stats;
import dev.vanta.core.Ticker;
import dev.vanta.gui.ClickGui;
import dev.vanta.modules.combat.HitBox;
import dev.vanta.util.Input;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;

/**
 * Client root. Everything the game hooks and the agent touch funnels through here.
 */
public final class Vanta {

    public static final String NAME = "Vanta";
    public static final String VERSION = "1.0.0";

    private static boolean started;
    private static ClickGui gui;
    private static Entity lastAttackTarget;

    private Vanta() {
    }

    public static MinecraftClient mc() {
        return MinecraftClient.getInstance();
    }

    /** Idempotent boot; the agent calls this once its remapping loader is up. */
    public static synchronized void start() {
        if (started) {
            return;
        }
        started = true;
        ModuleManager.init();
        Config.load();
        Runtime.getRuntime().addShutdownHook(new Thread(Config::save, "vanta-config-save"));
    }

    public static boolean started() {
        return started;
    }

    public static ClickGui gui() {
        if (gui == null) {
            gui = new ClickGui();
        }
        return gui;
    }

    public static void openGui() {
        MinecraftClient client = mc();
        if (client != null && client.currentScreen == null) {
            client.setScreen(gui());
        }
    }

    // ---- frame hooks ------------------------------------------------------

    public static void tick() {
        if (!started) {
            start();
        }
        Input.poll();
        Ticker.run();
        ModuleManager.tick();
        Stats.tick();
    }

    public static void renderHud(DrawContext context) {
        if (!started) {
            return;
        }
        ModuleManager.renderHud(context);
    }

    /** Called from the interaction manager hook before vanilla swings at the entity. */
    public static void onAttack(Entity target) {
        lastAttackTarget = target;
        Stats.onAttack(target);
        ModuleManager.onAttack(target);
    }

    public static Entity lastAttackTarget() {
        return lastAttackTarget;
    }

    /**
     * Called before knockback is applied to the local player.
     *
     * @return the scaled velocity, or null when nothing modified it
     */
    public static double[] onVelocity(double x, double y, double z) {
        return ModuleManager.onVelocity(x, y, z);
    }

    /** Called from the entity hook for every bounding box the game reads. */
    public static Box onBoundingBox(Entity entity, Box original) {
        return HitBox.apply(entity, original);
    }
}
