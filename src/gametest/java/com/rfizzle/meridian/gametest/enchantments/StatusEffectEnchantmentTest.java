package com.rfizzle.meridian.gametest.enchantments;

import com.rfizzle.meridian.Meridian;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;

public class StatusEffectEnchantmentTest implements FabricGameTest {

    private Holder<Enchantment> lookup(GameTestHelper helper, String id) {
        Registry<Enchantment> reg = helper.getLevel().registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT);
        return reg.getHolder(Meridian.id(id)).orElse(null);
    }

    @GameTest(template = "meridian:empty_3x3")
    public void shackleAppliesSlowness(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "shackle");
        if (ench == null) { helper.fail("shackle not in registry"); return; }

        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.enchant(ench, 3);

        Mob attacker = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 1, 1));
        attacker.setItemSlot(EquipmentSlot.MAINHAND, sword);

        Mob victim = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 1, 2));
        attacker.doHurtTarget(victim);

        helper.runAfterDelay(2, () -> {
            if (victim.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) {
                helper.succeed();
            } else {
                helper.fail("Victim should have Slowness from Shackle III");
            }
        });
    }

    @GameTest(template = "meridian:empty_3x3")
    public void shackleScalesDurationWithLevel(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "shackle");
        if (ench == null) { helper.fail("shackle not in registry"); return; }

        ItemStack sword1 = new ItemStack(Items.DIAMOND_SWORD);
        sword1.enchant(ench, 1);
        Mob attacker1 = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 1, 1));
        attacker1.setItemSlot(EquipmentSlot.MAINHAND, sword1);
        Mob victim1 = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(2, 1, 1));
        attacker1.doHurtTarget(victim1);

        ItemStack sword3 = new ItemStack(Items.DIAMOND_SWORD);
        sword3.enchant(ench, 3);
        Mob attacker3 = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 1, 2));
        attacker3.setItemSlot(EquipmentSlot.MAINHAND, sword3);
        Mob victim3 = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(2, 1, 2));
        attacker3.doHurtTarget(victim3);

        helper.runAfterDelay(2, () -> {
            var effect1 = victim1.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
            var effect3 = victim3.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
            if (effect1 == null || effect3 == null) {
                helper.fail("Both victims should have Slowness. L1: " + (effect1 != null) + ", L3: " + (effect3 != null));
                return;
            }
            if (effect3.getDuration() <= effect1.getDuration()) {
                helper.fail("Shackle III should have longer duration than I. L1: "
                        + effect1.getDuration() + ", L3: " + effect3.getDuration());
                return;
            }
            helper.succeed();
        });
    }

    @GameTest(template = "meridian:empty_3x3")
    public void blightAppliesPoison(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "blight");
        if (ench == null) { helper.fail("blight not in registry"); return; }

        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.enchant(ench, 2);

        Mob attacker = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 1, 1));
        attacker.setItemSlot(EquipmentSlot.MAINHAND, sword);

        Mob victim = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 1, 2));
        attacker.doHurtTarget(victim);

        helper.runAfterDelay(2, () -> {
            if (victim.hasEffect(MobEffects.POISON)) {
                helper.succeed();
            } else {
                helper.fail("Victim should have Poison from Blight II");
            }
        });
    }

    @GameTest(template = "meridian:empty_3x3")
    public void decayAppliesWither(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "decay");
        if (ench == null) { helper.fail("decay not in registry"); return; }

        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.enchant(ench, 2);

        Mob attacker = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 1, 1));
        attacker.setItemSlot(EquipmentSlot.MAINHAND, sword);

        Mob victim = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 1, 2));
        attacker.doHurtTarget(victim);

        helper.runAfterDelay(2, () -> {
            if (victim.hasEffect(MobEffects.WITHER)) {
                helper.succeed();
            } else {
                helper.fail("Victim should have Wither from Decay II");
            }
        });
    }

    @GameTest(template = "meridian:empty_3x3")
    public void nightfallAppliesDarkness(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "nightfall");
        if (ench == null) { helper.fail("nightfall not in registry"); return; }

        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.enchant(ench, 1);

        Mob attacker = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 1, 1));
        attacker.setItemSlot(EquipmentSlot.MAINHAND, sword);

        Mob victim = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 1, 2));
        attacker.doHurtTarget(victim);

        helper.runAfterDelay(2, () -> {
            if (victim.hasEffect(MobEffects.DARKNESS)) {
                helper.succeed();
            } else {
                helper.fail("Victim should have Darkness from Nightfall");
            }
        });
    }
}
