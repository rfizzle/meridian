// Tier: 3 (Fabric Gametest)
package com.rfizzle.meridian.gametest;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.MeridianRegistry;
import com.rfizzle.meridian.event.WardenLootHandler;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

public class SpecialtyMaterialsGameTest implements FabricGameTest {

    // --- TEST-5.4-T3a: InfusedBreathItem registered ---

    @GameTest(template = "meridian:empty_3x3")
    public void infusedBreathRegistered(GameTestHelper helper) {
        ResourceLocation loc = Meridian.id("infused_breath");
        if (!BuiltInRegistries.ITEM.containsKey(loc)) {
            helper.fail("InfusedBreathItem not registered: " + loc);
            return;
        }
        if (BuiltInRegistries.ITEM.get(loc) != MeridianRegistry.INFUSED_BREATH) {
            helper.fail("Registry entry doesn't match singleton");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void infusedBreathIsEpicRarity(GameTestHelper helper) {
        ItemStack stack = new ItemStack(MeridianRegistry.INFUSED_BREATH);
        if (stack.getRarity() != Rarity.EPIC) {
            helper.fail("Infused Breath should be EPIC rarity, got " + stack.getRarity());
            return;
        }
        helper.succeed();
    }

    // --- TEST-5.4-T3b: WardenTendrilItem registered ---

    @GameTest(template = "meridian:empty_3x3")
    public void wardenTendrilRegistered(GameTestHelper helper) {
        ResourceLocation loc = Meridian.id("warden_tendril");
        if (!BuiltInRegistries.ITEM.containsKey(loc)) {
            helper.fail("WardenTendrilItem not registered: " + loc);
            return;
        }
        if (BuiltInRegistries.ITEM.get(loc) != MeridianRegistry.WARDEN_TENDRIL) {
            helper.fail("Registry entry doesn't match singleton");
            return;
        }
        helper.succeed();
    }

    // --- TEST-5.4-T3c: WardenLootHandler modifies warden loot table ---

    @GameTest(template = "meridian:empty_3x3")
    public void wardenPoolConditionTypeRegistered(GameTestHelper helper) {
        ResourceLocation loc = Meridian.id("warden_pool");
        if (!BuiltInRegistries.LOOT_CONDITION_TYPE.containsKey(loc)) {
            helper.fail("WardenPoolCondition type not registered: " + loc);
            return;
        }
        if (BuiltInRegistries.LOOT_CONDITION_TYPE.get(loc) != MeridianRegistry.WARDEN_POOL_CONDITION) {
            helper.fail("LootItemConditionType doesn't match singleton");
            return;
        }
        helper.succeed();
    }

    // --- TEST-5.4-T3d: Warden kill drops tendril via loot table modifier ---

    @GameTest(template = "meridian:empty_3x3", timeoutTicks = 100)
    public void wardenDropsTendrilOnKill(GameTestHelper helper) {
        Warden warden = helper.spawnWithNoFreeWill(EntityType.WARDEN, new BlockPos(1, 1, 1));
        warden.hurt(helper.getLevel().damageSources().genericKill(), Float.MAX_VALUE);

        helper.runAfterDelay(10, () -> {
            long tendrilCount = helper.getEntities(EntityType.ITEM).stream()
                    .map(e -> ((ItemEntity) e).getItem())
                    .filter(s -> s.is(MeridianRegistry.WARDEN_TENDRIL))
                    .count();

            if (tendrilCount >= 1) {
                helper.succeed();
            } else {
                helper.fail("Expected at least 1 warden_tendril drop (config.warden.tendrilDropChance defaults to 1.0), got "
                        + tendrilCount);
            }
        });
    }
}
