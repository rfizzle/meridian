// Tier: 3 (Fabric Gametest)
package com.rfizzle.meridian.enchanting;

import com.rfizzle.meridian.MeridianRegistry;
import com.rfizzle.meridian.shelf.FilteringShelfBlockEntity;
import com.rfizzle.meridian.shelf.TreasureShelfBlockEntity;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.List;
import java.util.Set;

public class FilteringTreasureGameTest implements FabricGameTest {

    private static final BlockPos TEST_POS = new BlockPos(1, 1, 1);

    // --- TEST-3.5-T3a: FilteringShelfBlockEntity ---

    @GameTest(template = "meridian:empty_3x3")
    public void filteringShelfCreatesCorrectBlockEntity(GameTestHelper helper) {
        helper.setBlock(TEST_POS, MeridianRegistry.FILTERING_SHELF.defaultBlockState());
        var be = helper.getLevel().getBlockEntity(helper.absolutePos(TEST_POS));
        if (!(be instanceof FilteringShelfBlockEntity)) {
            helper.fail("Expected FilteringShelfBlockEntity, got "
                    + (be == null ? "null" : be.getClass().getSimpleName()));
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void filteringShelfInsertGrowsBlacklist(GameTestHelper helper) {
        FilteringShelfBlockEntity be = placeFilteringShelf(helper);
        if (be == null) { helper.fail("BE not created"); return; }

        if (!be.getEnchantmentBlacklist().isEmpty()) {
            helper.fail("Fresh shelf should have empty blacklist");
            return;
        }

        be.setItem(0, enchantedBook(helper, Enchantments.SHARPNESS, 1));
        Set<ResourceKey<Enchantment>> after1 = be.getEnchantmentBlacklist();
        if (after1.size() != 1 || !after1.contains(Enchantments.SHARPNESS)) {
            helper.fail("After Sharpness insert: expected {SHARPNESS}, got " + after1);
            return;
        }

        be.setItem(2, enchantedBook(helper, Enchantments.UNBREAKING, 3));
        Set<ResourceKey<Enchantment>> after2 = be.getEnchantmentBlacklist();
        if (after2.size() != 2 || !after2.contains(Enchantments.UNBREAKING)) {
            helper.fail("After second insert: expected 2 entries including UNBREAKING, got " + after2);
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void filteringShelfExtractShrinksBlacklist(GameTestHelper helper) {
        FilteringShelfBlockEntity be = placeFilteringShelf(helper);
        if (be == null) { helper.fail("BE not created"); return; }

        be.setItem(0, enchantedBook(helper, Enchantments.SHARPNESS, 1));
        be.setItem(1, enchantedBook(helper, Enchantments.UNBREAKING, 1));
        if (be.getEnchantmentBlacklist().size() != 2) {
            helper.fail("Expected 2-entry blacklist before extract");
            return;
        }

        be.setItem(0, ItemStack.EMPTY);
        Set<ResourceKey<Enchantment>> after = be.getEnchantmentBlacklist();
        if (after.size() != 1 || !after.contains(Enchantments.UNBREAKING)
                || after.contains(Enchantments.SHARPNESS)) {
            helper.fail("After extracting slot 0, only UNBREAKING should remain, got " + after);
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void filteringShelfFullRejectsAdditionalInserts(GameTestHelper helper) {
        FilteringShelfBlockEntity be = placeFilteringShelf(helper);
        if (be == null) { helper.fail("BE not created"); return; }

        for (int i = 0; i < 6; i++) {
            be.setItem(i, enchantedBook(helper, Enchantments.SHARPNESS, 1));
        }
        if (be.count() != 6) {
            helper.fail("Expected 6 items in full shelf, got " + be.count());
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void filteringShelfBlacklistPropagatesInGather(GameTestHelper helper) {
        FilteringShelfBlockEntity be = placeFilteringShelf(helper);
        if (be == null) { helper.fail("BE not created"); return; }

        be.setItem(0, enchantedBook(helper, Enchantments.SHARPNESS, 1));
        be.setItem(1, enchantedBook(helper, Enchantments.MENDING, 1));

        EnchantingStatRegistry reg = new EnchantingStatRegistry();
        StatCollection result = reg.gatherStatsFromOffsets(
                List.of(new BlockPos(0, 0, 0)),
                pos -> EnchantingStats.ZERO,
                pos -> true,
                pos -> be);

        Set<ResourceKey<Enchantment>> bl = result.blacklist();
        if (bl.size() != 2 || !bl.contains(Enchantments.SHARPNESS) || !bl.contains(Enchantments.MENDING)) {
            helper.fail("Blacklist should contain {SHARPNESS, MENDING}, got " + bl);
            return;
        }
        helper.succeed();
    }

    // --- TEST-3.5-T3b: TreasureShelfBlockEntity ---

    @GameTest(template = "meridian:empty_3x3")
    public void treasureShelfSetsTreasureAllowed(GameTestHelper helper) {
        helper.setBlock(TEST_POS, MeridianRegistry.TREASURE_SHELF.defaultBlockState());
        var be = helper.getLevel().getBlockEntity(helper.absolutePos(TEST_POS));
        if (!(be instanceof TreasureShelfBlockEntity)) {
            helper.fail("Expected TreasureShelfBlockEntity, got "
                    + (be == null ? "null" : be.getClass().getSimpleName()));
            return;
        }

        EnchantingStatRegistry reg = new EnchantingStatRegistry();
        StatCollection result = reg.gatherStatsFromOffsets(
                List.of(new BlockPos(0, 0, 0)),
                pos -> EnchantingStats.ZERO,
                pos -> true,
                pos -> be);

        if (!result.treasureAllowed()) {
            helper.fail("Treasure shelf in range should set treasureAllowed=true");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void treasureShelfAbsenceKeepsTreasureFalse(GameTestHelper helper) {
        EnchantingStatRegistry reg = new EnchantingStatRegistry();
        StatCollection result = reg.gatherStatsFromOffsets(
                List.of(new BlockPos(0, 0, 0)),
                pos -> EnchantingStats.ZERO,
                pos -> true,
                pos -> null);

        if (result.treasureAllowed()) {
            helper.fail("Without treasure shelf, treasureAllowed should be false");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void treasureShelfZeroEternaContribution(GameTestHelper helper) {
        helper.setBlock(TEST_POS, MeridianRegistry.TREASURE_SHELF.defaultBlockState());
        var be = helper.getLevel().getBlockEntity(helper.absolutePos(TEST_POS));

        EnchantingStatRegistry reg = new EnchantingStatRegistry();
        StatCollection result = reg.gatherStatsFromOffsets(
                List.of(new BlockPos(0, 0, 0)),
                pos -> EnchantingStats.ZERO,
                pos -> true,
                pos -> be);

        if (Math.abs(result.eterna()) > 1e-6 || Math.abs(result.maxEterna()) > 1e-6
                || Math.abs(result.quanta()) > 1e-6 || Math.abs(result.arcana()) > 1e-6) {
            helper.fail("Treasure shelf should have zero stat contribution, got eterna="
                    + result.eterna() + " maxEterna=" + result.maxEterna());
            return;
        }
        if (!result.treasureAllowed()) {
            helper.fail("Treasure flag should still be set despite zero stats");
            return;
        }
        helper.succeed();
    }

    // --- Helpers ---

    private static FilteringShelfBlockEntity placeFilteringShelf(GameTestHelper helper) {
        helper.setBlock(TEST_POS, MeridianRegistry.FILTERING_SHELF.defaultBlockState());
        return (FilteringShelfBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(TEST_POS));
    }

    private static ItemStack enchantedBook(GameTestHelper helper, ResourceKey<Enchantment> key, int level) {
        Registry<Enchantment> reg = helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> holder = reg.getHolderOrThrow(key);
        ItemStack stack = new ItemStack(Items.ENCHANTED_BOOK);
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        mutable.set(holder, level);
        stack.set(DataComponents.STORED_ENCHANTMENTS, mutable.toImmutable());
        return stack;
    }
}
