// Tier: 3 (Fabric Gametest)
package com.rfizzle.meridian.event;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.enchanting.EnduranceHealMath;
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
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;

public class MountedEnchantmentHandlerGameTest implements FabricGameTest {

    // Trample: a moving horse wearing Trample armor damages an adjacent creature, and the
    // per-horse cooldown prevents it from re-triggering on the same tick.
    @GameTest(template = "meridian:empty_3x3")
    public void tramplingHorseDamagesNearbyEntityOnce(GameTestHelper helper) {
        Registry<Enchantment> reg = helper.getLevel().registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> trample = reg.getHolder(Meridian.id("trample")).orElse(null);
        if (trample == null) { helper.fail("trample not in registry"); return; }

        Horse horse = helper.spawnWithNoFreeWill(EntityType.HORSE, new BlockPos(1, 1, 1));
        ItemStack armor = new ItemStack(Items.IRON_HORSE_ARMOR);
        armor.enchant(trample, 3);
        horse.setItemSlot(EquipmentSlot.BODY, armor);
        horse.setDeltaMovement(new Vec3(0.3, 0.0, 0.0));

        Mob target = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 1, 1));
        float before = target.getHealth();

        MountedEnchantmentHandler.handleTrample(horse);
        float afterFirst = target.getHealth();
        if (afterFirst >= before) {
            helper.fail("Trample III should damage the adjacent zombie (before=" + before
                    + ", after=" + afterFirst + ")");
            return;
        }

        // Same tick: the cooldown should suppress a second hit.
        MountedEnchantmentHandler.handleTrample(horse);
        if (target.getHealth() < afterFirst) {
            helper.fail("Trample cooldown should prevent a second hit on the same tick");
            return;
        }

        helper.succeed();
    }

    // Endurance: one heal pulse restores healPerPulse(level) to a wounded mount, never heals past
    // max health, and does nothing without the enchant on the body slot.
    @GameTest(template = "meridian:empty_3x3")
    public void enduringHorseRegeneratesHealth(GameTestHelper helper) {
        Registry<Enchantment> reg = helper.getLevel().registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> endurance = reg.getHolder(Meridian.id("endurance")).orElse(null);
        if (endurance == null) { helper.fail("endurance not in registry"); return; }

        Horse horse = helper.spawnWithNoFreeWill(EntityType.HORSE, new BlockPos(1, 1, 1));
        ItemStack armor = new ItemStack(Items.IRON_HORSE_ARMOR);
        armor.enchant(endurance, 3);
        horse.setItemSlot(EquipmentSlot.BODY, armor);

        float wounded = horse.getMaxHealth() - 6.0f;
        horse.setHealth(wounded);
        MountedEnchantmentHandler.handleEndurance(horse);

        float expected = wounded + EnduranceHealMath.healPerPulse(3);
        if (Math.abs(horse.getHealth() - expected) > 0.001f) {
            helper.fail("Endurance III should heal " + EnduranceHealMath.healPerPulse(3)
                    + " (before=" + wounded + ", after=" + horse.getHealth() + ")");
            return;
        }

        // At full health the pulse is a no-op — no over-heal.
        horse.setHealth(horse.getMaxHealth());
        MountedEnchantmentHandler.handleEndurance(horse);
        if (horse.getHealth() > horse.getMaxHealth()) {
            helper.fail("Endurance must not heal past max health");
            return;
        }

        // A mount without Endurance never heals from the pulse.
        Horse plain = helper.spawnWithNoFreeWill(EntityType.HORSE, new BlockPos(1, 1, 1));
        plain.setItemSlot(EquipmentSlot.BODY, new ItemStack(Items.IRON_HORSE_ARMOR));
        float plainWounded = plain.getMaxHealth() - 6.0f;
        plain.setHealth(plainWounded);
        MountedEnchantmentHandler.handleEndurance(plain);
        if (plain.getHealth() != plainWounded) {
            helper.fail("A mount without Endurance must not heal");
            return;
        }

        helper.succeed();
    }

    // Endurance end to end: a wounded, enchanted mount left to tick in a real level heals through
    // the EnduranceMixin AbstractHorse#tick inject within one pulse interval — exercises the mixin
    // target, the tickCount gate, and the isClientSide guard, not just the handler seam. The
    // timeout gives one full PULSE_INTERVAL_TICKS (plus margin) for the first pulse to land.
    @GameTest(template = "meridian:empty_3x3", timeoutTicks = EnduranceHealMath.PULSE_INTERVAL_TICKS + 40)
    public void enduranceRegenFiresThroughTheHorseTick(GameTestHelper helper) {
        Registry<Enchantment> reg = helper.getLevel().registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> endurance = reg.getHolder(Meridian.id("endurance")).orElse(null);
        if (endurance == null) { helper.fail("endurance not in registry"); return; }

        Horse horse = helper.spawnWithNoFreeWill(EntityType.HORSE, new BlockPos(1, 1, 1));
        ItemStack armor = new ItemStack(Items.IRON_HORSE_ARMOR);
        armor.enchant(endurance, 3);
        horse.setItemSlot(EquipmentSlot.BODY, armor);
        float wounded = horse.getMaxHealth() - 8.0f;
        horse.setHealth(wounded);

        helper.succeedWhen(() -> helper.assertTrue(horse.getHealth() > wounded,
                "Endurance mount should regenerate through its own tick"));
    }

    // Wavestride: a moving mount stands on the water surface, a stationary one sinks, and a mount
    // without the enchant never stands on water. Drives the canStandOnFluid gate directly.
    @GameTest(template = "meridian:empty_3x3")
    public void wavestrideHorseStandsOnMovingWater(GameTestHelper helper) {
        Registry<Enchantment> reg = helper.getLevel().registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> wavestride = reg.getHolder(Meridian.id("wavestride")).orElse(null);
        if (wavestride == null) { helper.fail("wavestride not in registry"); return; }

        FluidState water = Fluids.WATER.defaultFluidState();

        Horse horse = helper.spawnWithNoFreeWill(EntityType.HORSE, new BlockPos(1, 1, 1));
        ItemStack armor = new ItemStack(Items.IRON_HORSE_ARMOR);
        armor.enchant(wavestride, 1);
        horse.setItemSlot(EquipmentSlot.BODY, armor);

        horse.setDeltaMovement(new Vec3(0.3, 0.0, 0.0));
        if (!horse.canStandOnFluid(water)) {
            helper.fail("A moving Wavestride mount should stand on the water surface");
            return;
        }

        horse.setDeltaMovement(Vec3.ZERO);
        if (horse.canStandOnFluid(water)) {
            helper.fail("A stationary Wavestride mount should sink, not stand on water");
            return;
        }

        Horse plain = helper.spawnWithNoFreeWill(EntityType.HORSE, new BlockPos(1, 1, 1));
        plain.setItemSlot(EquipmentSlot.BODY, new ItemStack(Items.IRON_HORSE_ARMOR));
        plain.setDeltaMovement(new Vec3(0.3, 0.0, 0.0));
        if (plain.canStandOnFluid(water)) {
            helper.fail("A mount without Wavestride should not stand on water");
            return;
        }

        helper.succeed();
    }
}
