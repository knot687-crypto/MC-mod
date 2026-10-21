package dev.veil.modules.combat;

import dev.veil.core.Category;
import dev.veil.core.Module;
import dev.veil.core.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.util.Identifier;

public final class Reach extends Module {
    private static final MinecraftClient MC = MinecraftClient.getInstance();
    private static final Identifier ENTITY_ID = Identifier.of("veil", "reach_entity");
    private static final Identifier BLOCK_ID = Identifier.of("veil", "reach_block");
    private static final double VANILLA_RANGE = 3.0;

    private final NumberSetting entityRange = add(new NumberSetting("Entity", "Entity interaction range.", 3.4, 3.0, 6.0, 0.1, ""));
    private final NumberSetting blockRange = add(new NumberSetting("Block", "Block interaction range.", 4.8, 4.5, 7.0, 0.1, ""));

    private double appliedEntity = -1.0;
    private double appliedBlock = -1.0;

    public Reach() {
        super("Reach", "Extends the client interaction range through vanilla attributes.", Category.COMBAT);
    }

    @Override
    public void onEnable() {
        appliedEntity = -1.0;
        appliedBlock = -1.0;
    }

    @Override
    public void onDisable() {
        ClientPlayerEntity player = MC.player;
        if (player == null) {
            return;
        }
        clear(player.getAttributeInstance(EntityAttributes.ENTITY_INTERACTION_RANGE), ENTITY_ID);
        clear(player.getAttributeInstance(EntityAttributes.BLOCK_INTERACTION_RANGE), BLOCK_ID);
        appliedEntity = -1.0;
        appliedBlock = -1.0;
    }

    @Override
    public void onTick() {
        ClientPlayerEntity player = MC.player;
        if (player == null) {
            return;
        }
        if (entityRange.value() != appliedEntity) {
            apply(player.getAttributeInstance(EntityAttributes.ENTITY_INTERACTION_RANGE), ENTITY_ID, entityRange.value());
            appliedEntity = entityRange.value();
        }
        if (blockRange.value() != appliedBlock) {
            apply(player.getAttributeInstance(EntityAttributes.BLOCK_INTERACTION_RANGE), BLOCK_ID, blockRange.value());
            appliedBlock = blockRange.value();
        }
    }

    private static void apply(EntityAttributeInstance instance, Identifier id, double target) {
        if (instance == null) {
            return;
        }
        instance.removeModifier(id);
        instance.addPersistentModifier(new EntityAttributeModifier(id, target - VANILLA_RANGE, EntityAttributeModifier.Operation.ADD_VALUE));
    }

    private static void clear(EntityAttributeInstance instance, Identifier id) {
        if (instance != null) {
            instance.removeModifier(id);
        }
    }

    public double entityRange() {
        return entityRange.value();
    }
}
