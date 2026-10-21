package dev.veil.core;

import dev.veil.modules.combat.AntiBot;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Single place that answers "who am I looking at". Keeping selection here means aim assist,
 * trigger bot and the target hud can never disagree about the current target.
 */
public final class TargetManager {
    public enum Sort {
        ANGLE, DISTANCE, HEALTH
    }

    private static final MinecraftClient MC = MinecraftClient.getInstance();
    private static LivingEntity current;

    private TargetManager() {
    }

    public static LivingEntity current() {
        return current;
    }

    public static void update() {
        current = find(6.0, 180.0, false, false, Sort.ANGLE);
    }

    public static LivingEntity find(double range, double fov, boolean includeMobs, boolean requireVisible, Sort sort) {
        ClientPlayerEntity player = MC.player;
        if (player == null || MC.world == null) {
            return null;
        }

        double rangeSq = range * range;
        Box search = player.getBoundingBox().expand(range);
        List<LivingEntity> candidates = new ArrayList<>();

        for (Entity entity : MC.world.getOtherEntities(player, search, e -> e instanceof LivingEntity && e.isAlive())) {
            if (!(entity instanceof LivingEntity living) || living == player) {
                continue;
            }
            if (!(living instanceof PlayerEntity) && !includeMobs) {
                continue;
            }
            if (living.getHealth() <= 0.0f || living.isRemoved()) {
                continue;
            }
            if (player.squaredDistanceTo(living) > rangeSq) {
                continue;
            }
            if (!AntiBot.accepts(living)) {
                continue;
            }
            if (requireVisible && !player.canSee(living)) {
                continue;
            }
            double angle = Rotation.angleTo(player.getEyePos(), living.getBoundingBox().getCenter(), player.getYaw(), player.getPitch());
            if (angle > fov * 0.5) {
                continue;
            }
            candidates.add(living);
        }

        if (candidates.isEmpty()) {
            return null;
        }

        Vec3d eye = player.getEyePos();
        Comparator<LivingEntity> comparator = switch (sort) {
            case DISTANCE -> Comparator.comparingDouble(player::squaredDistanceTo);
            case HEALTH -> Comparator.comparingDouble(LivingEntity::getHealth);
            case ANGLE -> Comparator.comparingDouble(e ->
                    Rotation.angleTo(eye, e.getBoundingBox().getCenter(), player.getYaw(), player.getPitch()));
        };

        candidates.sort(comparator);
        return candidates.get(0);
    }

    /** Best point on the hitbox to aim at, nudged to the closest visible corner for a natural path. */
    public static Vec3d aimPoint(LivingEntity target, boolean head) {
        Box box = target.getBoundingBox();
        if (head && target instanceof PlayerEntity) {
            return new Vec3d(box.getCenter().x, box.maxY - 0.12, box.getCenter().z);
        }
        return new Vec3d(box.getCenter().x, box.minY + box.getLengthY() * 0.62, box.getCenter().z);
    }
}
