package dev.vanta.modules.client;

import dev.vanta.Vanta;
import dev.vanta.core.Category;
import dev.vanta.core.Module;
import dev.vanta.core.setting.ModeSetting;
import dev.vanta.render.Theme;
import dev.vanta.util.Input;
import org.lwjgl.glfw.GLFW;

/** Owns the menu bind and the accent palette. */
public final class Interface extends Module {

    private final ModeSetting accent = mode("Accent", "Violet", "Violet", "Ember", "Mint", "Rose", "Ice");

    public Interface() {
        super("Interface", "Menu bind and accent palette", Category.CLIENT);
        bind(GLFW.GLFW_KEY_RIGHT_SHIFT);
        setEnabled(true);
    }

    @Override
    public boolean onKeyPressed() {
        Vanta.openGui();
        return true;
    }

    @Override
    public void onTick() {
        if (!Theme.accentName().equals(accent.get())) {
            Theme.accent(accent.get());
        }
    }
}
