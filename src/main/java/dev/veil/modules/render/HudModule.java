package dev.veil.modules.render;

import dev.veil.core.Category;
import dev.veil.core.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public abstract class HudModule extends Module {
    protected static final MinecraftClient MC = MinecraftClient.getInstance();

    protected HudModule(String name, String description) {
        super(name, description, Category.RENDER);
    }

    public abstract void render(DrawContext ctx, float delta);
}
