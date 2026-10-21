package dev.vanta.core;

import dev.vanta.Vanta;
import dev.vanta.modules.combat.AntiBot;
import dev.vanta.util.Angles;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Picks what the aura swings at. Filters mirror what a hand would do. */
public final class Targeting {

    public enum Sort {
        Distance,
        Angle,
        Health
    }

    private Targeting() {
    }

    /**
     * @param range     max centre-to-centre distance
     * @param fov       max angle in degrees off the crosshair
     * @param visible   require an unobstructed sightline
     * @param mobs      include non-player living entities
     * @param botFilter drop entities that are not in the tab list
     */
    public static LivingEntity best(double range, double fov, boolean visible, boolean mobs, boolean botFilter, Sort sort) {
        MinecraftClient client = Vanta.mc();
        if (client.player == null || client.world == null) {
            return null;
        }
        ClientWorld world = client.world;
        PlayerEntity self = client.player;

        Vec3d eyes = self.getEyePos();
        Box search = self.getBoundingBox().expand(range + 2.0);
        List<LivingEntity> candidates = new ArrayList<>();

        for (AbstractClientPlayerEntity player : world.getPlayers()) {
            if (isValid(self, player, range, fov, visible, botFilter)) {
                candidates.add(player);
            }
        }
        if (mobs) {
            for (LivingEntity entity : world.getEntitiesByClass(LivingEntity.class, search, e -> !(e instanceof PlayerEntity))) {
                if (isValid(self, entity, range, fov, visible, false)) {
                    candidates.add(entity);
                }
            }
        }
        if (candidates.isEmpty()) {
            return null;
        }

        Comparator<LivingEntity> comparator = switch (sort) {
            case Angle -> Comparator.comparingDouble(target -> Angles.angleTo(self, target));
            case Health -> Comparator.comparingDouble(LivingEntity::getHealth);
            default -> Comparator.comparingDouble(self::squaredDistanceTo);
        };
        candidates.sort(comparator);
        return candidates.get(0);
    }

    private static boolean isValid(PlayerEntity self, LivingEntity target, double range, double fov, boolean visible, boolean botFilter) {
        if (target == self || !target.isAlive() || target.isRemoved() || target.getHealth() <= 0.0f) {
            return false;
        }
        double distance = Math.sqrt(self.squaredDistanceTo(target));
        if (distance > range) {
            return false;
        }
        if (Angles.angleTo(self, target) > fov) {
            return false;
        }
        if (self.isTeammate(target) && !botFilter) {
            return false;
        }
        if (botFilter && target instanceof PlayerEntity player && AntiBot.filtered(player)) {
            return false;
        }
        if (visible && !self.canSee(target)) {
            return false;
        }
        return true;
    }
}
