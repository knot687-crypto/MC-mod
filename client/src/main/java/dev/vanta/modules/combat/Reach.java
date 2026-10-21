package dev.vanta.modules.combat;

import dev.vanta.Vanta;
import dev.vanta.core.Category;
import dev.vanta.core.Module;
import dev.vanta.core.setting.NumberSetting;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;

/**
 * Pushes the client interaction range attribute. The server still enforces its
 * own limit, so this is a placement aid rather than a teleport.
 */
public final class Reach extends Module {

    private final NumberSetting entity = num("Entity", 3.15, 3.0, 6.0, 0.01);
    private final NumberSetting block = num("Block", 4.6, 4.5, 8.0, 0.01);

    private double savedEntity = -1.0;
    private double savedBlock = -1.0;

    public Reach() {
        super("Reach", "Extends interaction range within server limits", Category.COMBAT);
    }

    @Override
    public void onTick() {
        var player = Vanta.mc().player;
        if (player == null) {
            return;
        }
        EntityAttributeInstance entityAttr = player.getAttributeInstance(EntityAttributes.ENTITY_INTERACTION_RANGE);
        EntityAttributeInstance blockAttr = player.getAttributeInstance(EntityAttributes.BLOCK_INTERACTION_RANGE);
        if (entityAttr != null) {
            if (savedEntity < 0.0) {
                savedEntity = entityAttr.getBaseValue();
            }
            entityAttr.setBaseValue(entity.get());
        }
        if (blockAttr != null) {
            if (savedBlock < 0.0) {
                savedBlock = blockAttr.getBaseValue();
            }
            blockAttr.setBaseValue(block.get());
        }
    }

    @Override
    public void onDisable() {
        var player = Vanta.mc().player;
        if (player == null) {
            return;
        }
        if (savedEntity > 0.0) {
            EntityAttributeInstance instance = player.getAttributeInstance(EntityAttributes.ENTITY_INTERACTION_RANGE);
            if (instance != null) {
                instance.setBaseValue(savedEntity);
            }
        }
        if (savedBlock > 0.0) {
            EntityAttributeInstance instance = player.getAttributeInstance(EntityAttributes.BLOCK_INTERACTION_RANGE);
            if (instance != null) {
                instance.setBaseValue(savedBlock);
            }
        }
        savedEntity = -1.0;
        savedBlock = -1.0;
    }
}
