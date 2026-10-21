package dev.veil.modules.combat;

import dev.veil.core.BoolSetting;
import dev.veil.core.Category;
import dev.veil.core.ModeSetting;
import dev.veil.core.Module;
import dev.veil.core.NumberSetting;
import dev.veil.core.Rotation;
import dev.veil.core.TargetManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;

public final class AimAssist extends Module {
    private static final MinecraftClient MC = MinecraftClient.getInstance();

    private final NumberSetting range = add(new NumberSetting("Range", "Maximum target distance.", 4.6, 2.5, 6.0, 0.1, ""));
    private final NumberSetting fov = add(new NumberSetting("FOV", "Field of view the aim is allowed to work inside.", 110.0, 20.0, 360.0, 5.0, "\u00B0"));
    private final NumberSetting speed = add(new NumberSetting("Speed", "How much of the remaining angle is covered each tick.", 5.4, 1.0, 10.0, 0.1, ""));
    private final NumberSetting randomization = add(new NumberSetting("Randomization", "Organic variance on the smoothing curve.", 1.4, 0.0, 5.0, 0.1, ""));
    private final BoolSetting vertical = add(new BoolSetting("Vertical", "Also correct pitch.", true));
    private final BoolSetting visible = add(new BoolSetting("Visible", "Only aim at targets with line of sight.", true));
    private final BoolSetting clickOnly = add(new BoolSetting("Click Only", "Only assist while the attack key is held.", false));
    private final BoolSetting mobs = add(new BoolSetting("Mobs", "Include hostile and passive mobs.", false));
    private final BoolSetting head = add(new BoolSetting("Head Aim", "Bias the aim point toward the head.", true));
    private final ModeSetting sort = add(new ModeSetting("Priority", "Target ordering.", "Angle", "Angle", "Distance", "Health"));

    public AimAssist() {
        super("Aim Assist", "Silent-grade smoothing that keeps camera movement inside human bounds.", Category.COMBAT);
    }

    @Override
    public void onTick() {
        ClientPlayerEntity player = MC.player;
        if (player == null || MC.currentScreen != null) {
            return;
        }
        if (clickOnly.value() && !MC.options.attackKey.isPressed()) {
            return;
        }

        TargetManager.Sort sorting = switch (sort.value()) {
            case "Distance" -> TargetManager.Sort.DISTANCE;
            case "Health" -> TargetManager.Sort.HEALTH;
            default -> TargetManager.Sort.ANGLE;
        };

        LivingEntity target = TargetManager.find(range.value(), fov.value(), mobs.value(), visible.value(), sorting);
        if (target == null) {
            return;
        }

        Rotation.Angle rotation = Rotation.look(player.getEyePos(), TargetManager.aimPoint(target, head.value()), vertical.value());
        Rotation.apply(rotation, speed.value(), randomization.value(), vertical.value());
    }

    public double range() {
        return range.value();
    }

    public double fov() {
        return fov.value();
    }

    public boolean aimMobs() {
        return mobs.value();
    }

    public boolean requireVisible() {
        return visible.value();
    }
}
