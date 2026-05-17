// Tier: 3 (Fabric Gametest) — TEST-2.1-T3
package com.rfizzle.meridian.gametest;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.enchanting.EnchantingStatRegistry;
import com.rfizzle.meridian.enchanting.EnchantingStats;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class StatRegistryGameTest implements FabricGameTest {

    private static final String[] SHELF_IDS_WITH_STATS = {
            "beeshelf", "melonshelf", "stoneshelf",
            "hellshelf", "blazing_hellshelf", "glowing_hellshelf", "infused_hellshelf",
            "seashelf", "heart_seashelf", "crystal_seashelf", "infused_seashelf",
            "endshelf", "pearl_endshelf", "draconic_endshelf",
            "deepshelf", "dormant_deepshelf", "echoing_deepshelf", "soul_touched_deepshelf",
            "echoing_sculkshelf", "soul_touched_sculkshelf",
            "sightshelf", "sightshelf_t2",
            "rectifier", "rectifier_t2", "rectifier_t3",
            "filtering_shelf"
    };

    @GameTest(template = "meridian:empty_3x3")
    public void vanillaBookshelfLookupReturnsExpected(GameTestHelper helper) {
        EnchantingStats stats = EnchantingStatRegistry.lookup(
                helper.getLevel(), Blocks.BOOKSHELF.defaultBlockState());

        if (Math.abs(stats.maxEterna() - 15F) > 1e-6) {
            helper.fail("Expected maxEterna=15 for vanilla bookshelf, got " + stats.maxEterna());
            return;
        }
        if (Math.abs(stats.eterna() - 1F) > 1e-6) {
            helper.fail("Expected eterna=1 for vanilla bookshelf, got " + stats.eterna());
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void everyShelfBlockHasNonZeroStats(GameTestHelper helper) {
        for (String id : SHELF_IDS_WITH_STATS) {
            ResourceLocation loc = Meridian.id(id);
            Block block = BuiltInRegistries.BLOCK.get(loc);
            if (block == Blocks.AIR) {
                helper.fail("Shelf block not registered: " + loc);
                return;
            }
            EnchantingStats stats = EnchantingStatRegistry.lookup(
                    helper.getLevel(), block.defaultBlockState());
            if (stats.equals(EnchantingStats.ZERO)) {
                helper.fail("Shelf " + loc + " returned ZERO stats after datapack reload");
                return;
            }
        }
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void blockEntriesMapHasExpectedSize(GameTestHelper helper) {
        int entryCount = EnchantingStatRegistry.getInstance().blockEntries().size();
        if (entryCount < SHELF_IDS_WITH_STATS.length) {
            helper.fail("Expected at least " + SHELF_IDS_WITH_STATS.length
                    + " block entries in stat registry, got " + entryCount);
            return;
        }
        helper.succeed();
    }
}
