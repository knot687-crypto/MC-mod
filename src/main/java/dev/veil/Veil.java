package dev.veil;

import dev.veil.core.CombatStats;
import dev.veil.core.ConfigManager;
import dev.veil.core.Module;
import dev.veil.core.ModuleManager;
import dev.veil.core.TargetManager;
import dev.veil.gui.VeilScreen;
import dev.veil.modules.client.Appearance;
import dev.veil.modules.combat.*;
import dev.veil.modules.render.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public final class Veil implements ClientModInitializer {
    public static final String NAME = "Veil";
    public static final String VERSION = "1.0.0";
    public static final Logger LOGGER = LoggerFactory.getLogger("veil");

    private static ModuleManager modules;
    private static ConfigManager config;
    private static VeilScreen screen;
    private static boolean menuPressed;
    private static final Map<Integer, Boolean> keyStates = new HashMap<>();

    @Override
    public void onInitializeClient() {
        modules = new ModuleManager();
        registerModules();

        // Defaults first, then the saved file wins.
        modules.get(Appearance.class).setEnabled(true);
        modules.get(Watermark.class).setEnabled(true);
        modules.get(ArrayListHud.class).setEnabled(true);

        config = new ConfigManager(FabricLoader.getInstance().getConfigDir());
        config.load(modules);

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> config.save(modules));
        LOGGER.info("{} {} loaded", NAME, VERSION);
    }

    private void registerModules() {
        modules.register(new AimAssist());
        modules.register(new AutoClicker());
        modules.register(new TriggerBot());
        modules.register(new Reach());
        modules.register(new HitBox());
        modules.register(new Velocity());
        modules.register(new HitSelect());
        modules.register(new AntiBot());

        modules.register(new Watermark());
        modules.register(new ArrayListHud());
        modules.register(new TargetHud());
        modules.register(new InfoHud());

        modules.register(new Appearance());
    }

    private void onTick(MinecraftClient mc) {
        if (mc.player == null || mc.world == null) {
            return;
        }

        if (mc.currentScreen == null) {
            handleKeys(mc);
        } else if (mc.currentScreen instanceof VeilScreen) {
            screen = (VeilScreen) mc.currentScreen;
        }

        if (mc.currentScreen == null) {
            TargetManager.update();
            modules.tick();
        }
    }

    private void handleKeys(MinecraftClient mc) {
        long handle = mc.getWindow().getHandle();
        boolean menuDown = InputUtil.isKeyPressed(mc.getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT);
        if (menuDown && !menuPressed) {
            screen = new VeilScreen();
            mc.setScreen(screen);
        }
        menuPressed = menuDown;

        for (Module module : modules.all()) {
            int key = module.getKey();
            if (key <= 0) {
                continue;
            }
            boolean down = InputUtil.isKeyPressed(mc.getWindow(), key);
            boolean was = keyStates.getOrDefault(key, false);
            if (down && !was) {
                module.toggle();
                config.save(modules);
            }
            keyStates.put(key, down);
        }
    }

    /** Called from the attack mixin so every click path shares sprint and telemetry logic. */
    public static void onAttack(Entity target) {
        CombatStats.onClick();
        HitSelect hitSelect = modules.get(HitSelect.class);
        if (hitSelect != null) {
            hitSelect.onAttack(MinecraftClient.getInstance().player != null && MinecraftClient.getInstance().player.isSprinting());
        }
    }

    public static ModuleManager modules() {
        return modules;
    }

    public static ConfigManager config() {
        return config;
    }

    public static VeilScreen screen() {
        return screen;
    }
}
