package com.rfizzle.meridian.gametest.enchantments;

import com.rfizzle.meridian.Meridian;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;

public class AttributeEnchantmentEffectTest implements FabricGameTest {

    private Holder<Enchantment> lookup(GameTestHelper helper, String id) {
        Registry<Enchantment> reg = helper.getLevel().registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT);
        return reg.getHolder(Meridian.id(id)).orElse(null);
    }

    @GameTest(template = "meridian:empty_3x3")
    public void bulwarkAddsKnockbackResistance(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "bulwark");
        if (ench == null) { helper.fail("bulwark not in registry"); return; }

        Mob mob = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 1, 1));
        double base = mob.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);

        ItemStack chest = new ItemStack(Items.DIAMOND_CHESTPLATE);
        chest.enchant(ench, 3);
        mob.setItemSlot(EquipmentSlot.CHEST, chest);

        helper.runAfterDelay(1, () -> {
            double modified = mob.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
            if (modified <= base) {
                helper.fail("Bulwark III should increase knockback resistance. Base: " + base + ", got: " + modified);
                return;
            }
            helper.succeed();
        });
    }

    @GameTest(template = "meridian:empty_3x3")
    public void bulwarkScalesWithLevel(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "bulwark");
        if (ench == null) { helper.fail("bulwark not in registry"); return; }

        Mob mob1 = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 1, 1));
        ItemStack chest1 = new ItemStack(Items.DIAMOND_CHESTPLATE);
        chest1.enchant(ench, 1);
        mob1.setItemSlot(EquipmentSlot.CHEST, chest1);

        Mob mob3 = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 1, 2));
        ItemStack chest3 = new ItemStack(Items.DIAMOND_CHESTPLATE);
        chest3.enchant(ench, 3);
        mob3.setItemSlot(EquipmentSlot.CHEST, chest3);

        helper.runAfterDelay(1, () -> {
            double kr1 = mob1.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
            double kr3 = mob3.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
            if (kr3 <= kr1) {
                helper.fail("Bulwark III should give more knockback resistance than I. L1: " + kr1 + ", L3: " + kr3);
                return;
            }
            helper.succeed();
        });
    }

    @GameTest(template = "meridian:empty_3x3")
    public void vitalityIncreasesMaxHealth(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "vitality");
        if (ench == null) { helper.fail("vitality not in registry"); return; }

        Mob mob = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 1, 1));
        double baseHealth = mob.getAttributeValue(Attributes.MAX_HEALTH);

        ItemStack chest = new ItemStack(Items.DIAMOND_CHESTPLATE);
        chest.enchant(ench, 5);
        mob.setItemSlot(EquipmentSlot.CHEST, chest);

        helper.runAfterDelay(1, () -> {
            double modified = mob.getAttributeValue(Attributes.MAX_HEALTH);
            if (modified <= baseHealth) {
                helper.fail("Vitality V should increase max health. Base: " + baseHealth + ", got: " + modified);
                return;
            }
            helper.succeed();
        });
    }

    @GameTest(template = "meridian:empty_3x3")
    public void alacrityIncreasesMovementSpeed(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "alacrity");
        if (ench == null) { helper.fail("alacrity not in registry"); return; }

        Mob mob = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 1, 1));
        double baseSpeed = mob.getAttributeValue(Attributes.MOVEMENT_SPEED);

        ItemStack boots = new ItemStack(Items.DIAMOND_BOOTS);
        boots.enchant(ench, 3);
        mob.setItemSlot(EquipmentSlot.FEET, boots);

        helper.runAfterDelay(1, () -> {
            double modified = mob.getAttributeValue(Attributes.MOVEMENT_SPEED);
            if (modified <= baseSpeed) {
                helper.fail("Alacrity III should increase movement speed. Base: " + baseSpeed + ", got: " + modified);
                return;
            }
            helper.succeed();
        });
    }

    @GameTest(template = "meridian:empty_3x3")
    public void alacrityScalesWithLevel(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "alacrity");
        if (ench == null) { helper.fail("alacrity not in registry"); return; }

        Mob mob3 = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 1, 1));
        ItemStack boots3 = new ItemStack(Items.DIAMOND_BOOTS);
        boots3.enchant(ench, 3);
        mob3.setItemSlot(EquipmentSlot.FEET, boots3);

        Mob mob5 = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 1, 2));
        ItemStack boots5 = new ItemStack(Items.DIAMOND_BOOTS);
        boots5.enchant(ench, 5);
        mob5.setItemSlot(EquipmentSlot.FEET, boots5);

        helper.runAfterDelay(1, () -> {
            double speed3 = mob3.getAttributeValue(Attributes.MOVEMENT_SPEED);
            double speed5 = mob5.getAttributeValue(Attributes.MOVEMENT_SPEED);
            if (speed5 <= speed3) {
                helper.fail("Alacrity V should give more speed than III. L3: " + speed3 + ", L5: " + speed5);
                return;
            }
            helper.succeed();
        });
    }

    @GameTest(template = "meridian:empty_3x3")
    public void tempoIncreasesAttackSpeed(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "tempo");
        if (ench == null) { helper.fail("tempo not in registry"); return; }

        Mob mob = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 1, 1));
        double baseSpeed = mob.getAttributeValue(Attributes.ATTACK_SPEED);

        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.enchant(ench, 2);
        mob.setItemSlot(EquipmentSlot.MAINHAND, sword);

        helper.runAfterDelay(1, () -> {
            double modified = mob.getAttributeValue(Attributes.ATTACK_SPEED);
            if (modified <= baseSpeed) {
                helper.fail("Tempo II should increase attack speed. Base: " + baseSpeed + ", got: " + modified);
                return;
            }
            helper.succeed();
        });
    }

    @GameTest(template = "meridian:empty_3x3")
    public void clamberIncreasesStepHeight(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "clamber");
        if (ench == null) { helper.fail("clamber not in registry"); return; }

        Mob mob = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 1, 1));
        double baseStep = mob.getAttributeValue(Attributes.STEP_HEIGHT);

        ItemStack boots = new ItemStack(Items.DIAMOND_BOOTS);
        boots.enchant(ench, 2);
        mob.setItemSlot(EquipmentSlot.FEET, boots);

        helper.runAfterDelay(1, () -> {
            double modified = mob.getAttributeValue(Attributes.STEP_HEIGHT);
            if (modified <= baseStep) {
                helper.fail("Clamber II should increase step height. Base: " + baseStep + ", got: " + modified);
                return;
            }
            helper.succeed();
        });
    }

    @GameTest(template = "meridian:empty_3x3")
    public void recklessReducesArmorIncreasesAttack(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "reckless");
        if (ench == null) { helper.fail("reckless not in registry"); return; }

        Mob mob = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 1, 1));

        ItemStack chest = new ItemStack(Items.DIAMOND_CHESTPLATE);
        chest.enchant(ench, 2);
        mob.setItemSlot(EquipmentSlot.CHEST, chest);

        Mob ref = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 1, 2));
        ref.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));

        helper.runAfterDelay(1, () -> {
            double recklessArmor = mob.getAttributeValue(Attributes.ARMOR);
            double plainArmor = ref.getAttributeValue(Attributes.ARMOR);
            double recklessAttack = mob.getAttributeValue(Attributes.ATTACK_DAMAGE);
            double plainAttack = ref.getAttributeValue(Attributes.ATTACK_DAMAGE);

            if (recklessArmor >= plainArmor) {
                helper.fail("Reckless II should decrease armor. Got: " + recklessArmor + ", plain: " + plainArmor);
                return;
            }
            if (recklessAttack <= plainAttack) {
                helper.fail("Reckless II should increase attack damage. Got: " + recklessAttack + ", plain: " + plainAttack);
                return;
            }
            helper.succeed();
        });
    }
}
