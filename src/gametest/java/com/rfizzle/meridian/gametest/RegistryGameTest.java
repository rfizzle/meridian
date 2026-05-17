// Tier: 3 (Fabric Gametest)
package com.rfizzle.meridian.gametest;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.MeridianRegistry;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;

public class RegistryGameTest implements FabricGameTest {

    private static final String[] BLOCK_IDS = {
            "beeshelf", "melonshelf", "stoneshelf",
            "hellshelf", "blazing_hellshelf", "glowing_hellshelf", "infused_hellshelf",
            "seashelf", "heart_seashelf", "crystal_seashelf", "infused_seashelf",
            "endshelf", "pearl_endshelf", "draconic_endshelf",
            "deepshelf", "dormant_deepshelf", "echoing_deepshelf", "soul_touched_deepshelf",
            "echoing_sculkshelf", "soul_touched_sculkshelf",
            "sightshelf", "sightshelf_t2",
            "rectifier", "rectifier_t2", "rectifier_t3",
            "filtering_shelf", "treasure_shelf",
            "library", "ender_library"
    };

    private static final String[] STANDALONE_ITEM_IDS = {
            "prismatic_web", "infused_breath", "warden_tendril",
            "scrap_tome", "improved_scrap_tome", "extraction_tome"
    };

    private static final String[] MENU_TYPE_IDS = {
            "enchanting_table", "library"
    };

    private static final String[] BLOCK_ENTITY_TYPE_IDS = {
            "filtering_shelf", "treasure_shelf", "library", "ender_library"
    };

    @GameTest(template = "meridian:empty_3x3")
    public void everyBlockIdResolvesInBlockRegistry(GameTestHelper helper) {
        for (String id : BLOCK_IDS) {
            ResourceLocation loc = Meridian.id(id);
            if (!BuiltInRegistries.BLOCK.containsKey(loc)) {
                helper.fail("Block not registered: " + loc);
                return;
            }
        }
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void everyBlockIdHasCompanionBlockItem(GameTestHelper helper) {
        for (String id : BLOCK_IDS) {
            ResourceLocation loc = Meridian.id(id);
            if (!BuiltInRegistries.ITEM.containsKey(loc)) {
                helper.fail("BlockItem not registered: " + loc);
                return;
            }
            if (!(BuiltInRegistries.ITEM.get(loc) instanceof BlockItem)) {
                helper.fail("Item under block id is not a BlockItem: " + loc);
                return;
            }
        }
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void everyStandaloneItemResolvesInRegistry(GameTestHelper helper) {
        for (String id : STANDALONE_ITEM_IDS) {
            ResourceLocation loc = Meridian.id(id);
            if (!BuiltInRegistries.ITEM.containsKey(loc)) {
                helper.fail("Item not registered: " + loc);
                return;
            }
        }
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void menuTypesResolveInRegistry(GameTestHelper helper) {
        for (String id : MENU_TYPE_IDS) {
            ResourceLocation loc = Meridian.id(id);
            if (!BuiltInRegistries.MENU.containsKey(loc)) {
                helper.fail("MenuType not registered: " + loc);
                return;
            }
        }
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void blockEntityTypesResolveInRegistry(GameTestHelper helper) {
        for (String id : BLOCK_ENTITY_TYPE_IDS) {
            ResourceLocation loc = Meridian.id(id);
            if (!BuiltInRegistries.BLOCK_ENTITY_TYPE.containsKey(loc)) {
                helper.fail("BlockEntityType not registered: " + loc);
                return;
            }
        }
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void blocksMapMatchesRegistry(GameTestHelper helper) {
        for (String id : BLOCK_IDS) {
            ResourceLocation loc = Meridian.id(id);
            if (!MeridianRegistry.BLOCKS.containsKey(loc)) {
                helper.fail("BLOCKS map missing entry: " + loc);
                return;
            }
            if (MeridianRegistry.BLOCKS.get(loc) != BuiltInRegistries.BLOCK.get(loc)) {
                helper.fail("BLOCKS map entry differs from BuiltInRegistries: " + loc);
                return;
            }
        }
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void registerIsIdempotent(GameTestHelper helper) {
        int blockCount = BuiltInRegistries.BLOCK.size();
        int itemCount = BuiltInRegistries.ITEM.size();
        int menuCount = BuiltInRegistries.MENU.size();
        int beTypeCount = BuiltInRegistries.BLOCK_ENTITY_TYPE.size();

        MeridianRegistry.register();

        if (BuiltInRegistries.BLOCK.size() != blockCount) {
            helper.fail("BLOCK registry size changed after second register()");
            return;
        }
        if (BuiltInRegistries.ITEM.size() != itemCount) {
            helper.fail("ITEM registry size changed after second register()");
            return;
        }
        if (BuiltInRegistries.MENU.size() != menuCount) {
            helper.fail("MENU registry size changed after second register()");
            return;
        }
        if (BuiltInRegistries.BLOCK_ENTITY_TYPE.size() != beTypeCount) {
            helper.fail("BLOCK_ENTITY_TYPE registry size changed after second register()");
            return;
        }
        helper.succeed();
    }
}
