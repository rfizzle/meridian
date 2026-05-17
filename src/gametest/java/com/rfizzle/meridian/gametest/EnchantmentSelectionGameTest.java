// Tier: 3 (Fabric Gametest)
package com.rfizzle.meridian.gametest;

import com.rfizzle.meridian.enchanting.RealEnchantmentHelper;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.List;
import java.util.Set;

public class EnchantmentSelectionGameTest implements FabricGameTest {

    // --- S-4.2a: only IN_ENCHANTING_TABLE enchantments when treasure=false ---

    @GameTest(template = "meridian:empty_3x3")
    public void nonTreasurePoolContainsOnlyTableEnchantments(GameTestHelper helper) {
        Registry<Enchantment> reg = helper.getLevel().registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT);
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        List<EnchantmentInstance> results = RealEnchantmentHelper.getAvailableEnchantmentResults(
                30, sword, reg, false, Set.of());

        if (results.isEmpty()) {
            helper.fail("Expected non-empty pool for diamond sword at power=30");
            return;
        }
        for (EnchantmentInstance inst : results) {
            if (!inst.enchantment.is(EnchantmentTags.IN_ENCHANTING_TABLE)) {
                helper.fail("Non-table enchantment in pool with treasure=false: "
                        + inst.enchantment.getRegisteredName());
                return;
            }
        }
        helper.succeed();
    }

    // --- S-4.2b: treasure pool includes treasure-only enchantments ---

    @GameTest(template = "meridian:empty_3x3")
    public void treasurePoolIncludesTreasureEnchantments(GameTestHelper helper) {
        Registry<Enchantment> reg = helper.getLevel().registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT);
        ItemStack book = new ItemStack(Items.BOOK);
        List<EnchantmentInstance> results = RealEnchantmentHelper.getAvailableEnchantmentResults(
                30, book, reg, true, Set.of());

        boolean hasTreasureOnly = false;
        for (EnchantmentInstance inst : results) {
            if (inst.enchantment.is(EnchantmentTags.TREASURE)
                    && !inst.enchantment.is(EnchantmentTags.IN_ENCHANTING_TABLE)) {
                hasTreasureOnly = true;
                break;
            }
        }
        if (!hasTreasureOnly) {
            helper.fail("Expected at least one treasure-only enchantment (e.g. Mending) in pool");
            return;
        }
        helper.succeed();
    }

    // --- S-4.2c: blacklisted enchantments never appear (100 rolls) ---

    @GameTest(template = "meridian:empty_3x3")
    public void blacklistedEnchantmentNeverAppears(GameTestHelper helper) {
        Set<ResourceKey<Enchantment>> blacklist = Set.of(Enchantments.SHARPNESS);
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);

        for (int seed = 0; seed < 100; seed++) {
            RandomSource rand = RandomSource.create(seed);
            List<EnchantmentInstance> results = RealEnchantmentHelper.selectEnchantment(
                    rand, sword, 30, 15F, 50F, 0F, false, blacklist,
                    helper.getLevel().registryAccess());
            for (EnchantmentInstance inst : results) {
                if (inst.enchantment.is(Enchantments.SHARPNESS)) {
                    helper.fail("Blacklisted SHARPNESS appeared in roll with seed=" + seed);
                    return;
                }
            }
        }
        helper.succeed();
    }

    // --- S-4.2d: enchantments already on item are excluded ---

    @GameTest(template = "meridian:empty_3x3")
    public void existingEnchantmentsExcludedFromSelection(GameTestHelper helper) {
        Registry<Enchantment> reg = helper.getLevel().registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT);
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        Holder<Enchantment> sharpness = reg.getHolderOrThrow(Enchantments.SHARPNESS);
        sword.enchant(sharpness, 5);

        for (int seed = 0; seed < 100; seed++) {
            RandomSource rand = RandomSource.create(seed);
            List<EnchantmentInstance> results = RealEnchantmentHelper.selectEnchantment(
                    rand, sword, 30, 15F, 50F, 0F, false, Set.of(),
                    helper.getLevel().registryAccess());
            for (EnchantmentInstance inst : results) {
                if (inst.enchantment.is(Enchantments.SHARPNESS)) {
                    helper.fail("Sharpness (already on sword) appeared in roll with seed=" + seed);
                    return;
                }
            }
        }
        helper.succeed();
    }

    // --- S-4.2e: only enchantments valid for item type appear ---

    @GameTest(template = "meridian:empty_3x3")
    public void swordPoolExcludesBowEnchantments(GameTestHelper helper) {
        Registry<Enchantment> reg = helper.getLevel().registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT);
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        List<EnchantmentInstance> results = RealEnchantmentHelper.getAvailableEnchantmentResults(
                30, sword, reg, false, Set.of());

        if (results.isEmpty()) {
            helper.fail("Expected non-empty pool for diamond sword");
            return;
        }
        for (EnchantmentInstance inst : results) {
            if (inst.enchantment.is(Enchantments.POWER)
                    || inst.enchantment.is(Enchantments.PUNCH)
                    || inst.enchantment.is(Enchantments.FLAME)
                    || inst.enchantment.is(Enchantments.INFINITY)) {
                helper.fail("Bow enchantment " + inst.enchantment.getRegisteredName()
                        + " should not appear in sword pool");
                return;
            }
        }
        helper.succeed();
    }
}
