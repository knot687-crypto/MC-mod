package dev.veil.modules.combat;

import dev.veil.core.Category;
import dev.veil.core.Module;
import dev.veil.core.ModeSetting;
import dev.veil.core.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

public final class HitSelect extends Module {
    private static final MinecraftClient MC = MinecraftClient.getInstance();

    private final ModeSetting mode = add(new ModeSetting("Mode", "Sprint behaviour after a hit lands.", "W Tap", "W Tap", "Sprint Reset", "Keep Sprint"));
    private final NumberSetting delay = add(new NumberSetting("Delay", "Time the sprint stays released.", 60.0, 10.0, 250.0, 5.0, "ms"));

    private long releaseUntil;

    public HitSelect() {
        super("Hit Select", "Resets sprint on hit to shed knockback and keep combos tight.", Category.COMBAT);
    }

    @Override
    public void onTick() {
        ClientPlayerEntity player = MC.player;
        if (player == null) {
            return;
        }
        if (releaseUntil != 0L && System.currentTimeMillis() >= releaseUntil) {
            releaseUntil = 0L;
            MC.options.sprintKey.setPressed(true);
        }
    }

    /** Fired from the attack mixin so manual and automated clicks behave identically. */
    public void onAttack(boolean sprinting) {
        if (!isEnabled() || MC.player == null) {
            return;
        }
        if (mode.value().equals("Keep Sprint")) {
            MC.options.sprintKey.setPressed(true);
            MC.player.setSprinting(true);
            return;
        }
        if (mode.value().equals("Sprint Reset") && !MC.options.sprintKey.isPressed()) {
            return;
        }
        MC.options.sprintKey.setPressed(false);
        MC.player.setSprinting(false);
        releaseUntil = System.currentTimeMillis() + Math.round(delay.value());
    }

    @Override
    protected void onDisable() {
        releaseUntil = 0L;
    }
}
