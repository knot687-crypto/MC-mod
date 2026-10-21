package dev.veil.modules.combat;

import dev.veil.core.Category;
import dev.veil.core.Input;
import dev.veil.core.Module;
import dev.veil.core.NumberSetting;
import dev.veil.core.BoolSetting;
import dev.veil.core.TargetManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

import java.util.concurrent.ThreadLocalRandom;

public final class AutoClicker extends Module {
    private static final MinecraftClient MC = MinecraftClient.getInstance();

    private final NumberSetting minCps = add(new NumberSetting("Min CPS", "Lower bound of the click rate.", 9.0, 1.0, 20.0, 1.0, ""));
    private final NumberSetting maxCps = add(new NumberSetting("Max CPS", "Upper bound of the click rate.", 13.0, 1.0, 24.0, 1.0, ""));
    private final BoolSetting hold = add(new BoolSetting("Hold", "Only click while the attack key is held.", true));
    private final BoolSetting onTarget = add(new BoolSetting("On Target", "Only click when a target is under the aim.", false));
    private final BoolSetting cooldown = add(new BoolSetting("Cooldown", "Wait for the attack cooldown to be ready.", true));
    private final NumberSetting jitter = add(new NumberSetting("Jitter", "Random delay spread in milliseconds.", 24.0, 0.0, 90.0, 1.0, "ms"));

    private long nextClick;

    public AutoClicker() {
        super("Auto Clicker", "Clicks at a human rate with bounded jitter.", Category.COMBAT);
    }

    @Override
    public void onTick() {
        ClientPlayerEntity player = MC.player;
        if (player == null || MC.currentScreen != null || MC.interactionManager == null) {
            return;
        }
        long now = System.currentTimeMillis();

        if (hold.value() && !MC.options.attackKey.isPressed()) {
            nextClick = now;
            return;
        }
        if (onTarget.value() && TargetManager.current() == null) {
            nextClick = now;
            return;
        }
        if (now < nextClick) {
            return;
        }
        if (cooldown.value() && player.getAttackCooldownProgress(0.0f) < 0.92f) {
            return;
        }

        Input.leftClick();
        nextClick = now + interval();
    }

    private long interval() {
        double low = Math.max(1.0, minCps.value());
        double high = Math.max(low, maxCps.value());
        double cps = ThreadLocalRandom.current().nextDouble(low, high);
        double base = 1000.0 / cps;
        double spread = ThreadLocalRandom.current().nextDouble(-jitter.value(), jitter.value());
        return Math.max(15L, Math.round(base + spread));
    }
}
