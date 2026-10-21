package dev.vanta.core;

import dev.vanta.modules.client.Interface;
import dev.vanta.modules.combat.AimAssist;
import dev.vanta.modules.combat.AntiBot;
import dev.vanta.modules.combat.AutoClicker;
import dev.vanta.modules.combat.AutoShield;
import dev.vanta.modules.combat.AutoTotem;
import dev.vanta.modules.combat.Criticals;
import dev.vanta.modules.combat.HitBox;
import dev.vanta.modules.combat.JumpReset;
import dev.vanta.modules.combat.KillAura;
import dev.vanta.modules.combat.Reach;
import dev.vanta.modules.combat.TriggerBot;
import dev.vanta.modules.combat.Velocity;
import dev.vanta.modules.combat.WTap;
import dev.vanta.modules.visual.HitMarker;
import dev.vanta.modules.visual.TargetHud;
import dev.vanta.modules.visual.Watermark;
import dev.vanta.util.Input;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;

import java.util.ArrayList;
import java.util.List;

public final class ModuleManager {

    private static final List<Module> MODULES = new ArrayList<>();

    private ModuleManager() {
    }

    public static void init() {
        register(new KillAura());
        register(new AutoClicker());
        register(new AimAssist());
        register(new TriggerBot());
        register(new Reach());
        register(new HitBox());
        register(new Velocity());
        register(new Criticals());
        register(new WTap());
        register(new JumpReset());
        register(new AutoTotem());
        register(new AutoShield());
        register(new AntiBot());

        register(new Watermark());
        register(new TargetHud());
        register(new HitMarker());

        register(new Interface());
    }

    private static void register(Module module) {
        MODULES.add(module);
        if (module.key() <= 0) {
            module.setKey(-1);
        }
    }

    public static void tick() {
        for (Module module : MODULES) {
            if (module.key() > 0 && Input.justPressed(module.key())
                    && dev.vanta.Vanta.mc().currentScreen == null) {
                module.onKeyPressed();
            }
            if (module.enabled()) {
                module.onTick();
            }
        }
        Config.save();
    }

    public static void renderHud(DrawContext context) {
        for (Module module : MODULES) {
            if (module.enabled()) {
                module.onHud(context);
            }
        }
    }

    public static void onAttack(Entity target) {
        for (Module module : MODULES) {
            if (module.enabled()) {
                module.onAttack(target);
            }
        }
    }

    public static double[] onVelocity(double x, double y, double z) {
        double[] result = null;
        for (Module module : MODULES) {
            if (!module.enabled()) {
                continue;
            }
            module.onKnockback(x, y, z);
            double[] modified = module.onVelocity(x, y, z);
            if (modified != null) {
                result = modified;
            }
        }
        return result;
    }

    public static List<Module> all() {
        return MODULES;
    }

    public static List<Module> of(Category category) {
        List<Module> result = new ArrayList<>();
        for (Module module : MODULES) {
            if (module.category() == category) {
                result.add(module);
            }
        }
        return result;
    }

    public static Module byName(String name) {
        for (Module module : MODULES) {
            if (module.name().equalsIgnoreCase(name)) {
                return module;
            }
        }
        return null;
    }

    public static int enabledCount() {
        int count = 0;
        for (Module module : MODULES) {
            if (module.enabled()) {
                count++;
            }
        }
        return count;
    }
}
