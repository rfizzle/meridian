// Tier: 3 (Fabric Gametest)
package com.rfizzle.meridian.event;

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
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
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
}
