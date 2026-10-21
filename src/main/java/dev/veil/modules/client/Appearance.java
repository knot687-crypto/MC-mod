package dev.veil.modules.client;

import dev.veil.core.Category;
import dev.veil.core.ModeSetting;
import dev.veil.core.Module;
import dev.veil.gui.Theme;

public final class Appearance extends Module {
    private final ModeSetting accent = add(new ModeSetting("Accent", "Interface accent palette.", "Violet",
            "Violet", "Ice", "Mint", "Rose", "Amber", "Blood"));

    public Appearance() {
        super("Appearance", "Palette control for the gui and the hud.", Category.CLIENT);
    }

    @Override
    public void onEnable() {
        Theme.apply(accent.value());
    }

    @Override
    public void onTick() {
        Theme.apply(accent.value());
    }
}
