// Tier: 3 (Fabric Gametest)
package com.rfizzle.meridian.gametest;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.MeridianRegistry;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class TomeRegistryGameTest implements FabricGameTest {

    private static final String[] TOME_IDS = {"scrap_tome", "improved_scrap_tome", "extraction_tome"};

    @GameTest(template = "meridian:empty_3x3")
    public void allTomeItemsRegistered(GameTestHelper helper) {
        for (String id : TOME_IDS) {
            ResourceLocation loc = Meridian.id(id);
            if (!BuiltInRegistries.ITEM.containsKey(loc)) {
                helper.fail("Tome item not registered: " + loc);
                return;
            }
        }
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void allTomesStackToSixteen(GameTestHelper helper) {
        Item[] tomes = {
                MeridianRegistry.SCRAP_TOME,
                MeridianRegistry.IMPROVED_SCRAP_TOME,
                MeridianRegistry.EXTRACTION_TOME
        };
        for (Item tome : tomes) {
            ItemStack stack = new ItemStack(tome);
            if (stack.getMaxStackSize() != 16) {
                helper.fail(tome + " maxStackSize=" + stack.getMaxStackSize() + ", expected 16");
                return;
            }
        }
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void noTomeHasDurabilityComponent(GameTestHelper helper) {
        Item[] tomes = {
                MeridianRegistry.SCRAP_TOME,
                MeridianRegistry.IMPROVED_SCRAP_TOME,
                MeridianRegistry.EXTRACTION_TOME
        };
        for (Item tome : tomes) {
            ItemStack stack = new ItemStack(tome);
            if (stack.has(DataComponents.MAX_DAMAGE)) {
                helper.fail(tome + " should not have a durability component");
                return;
            }
        }
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void tomesSingletonMatchRegistry(GameTestHelper helper) {
        if (BuiltInRegistries.ITEM.get(Meridian.id("scrap_tome")) != MeridianRegistry.SCRAP_TOME) {
            helper.fail("SCRAP_TOME singleton doesn't match registry");
            return;
        }
        if (BuiltInRegistries.ITEM.get(Meridian.id("improved_scrap_tome")) != MeridianRegistry.IMPROVED_SCRAP_TOME) {
            helper.fail("IMPROVED_SCRAP_TOME singleton doesn't match registry");
            return;
        }
        if (BuiltInRegistries.ITEM.get(Meridian.id("extraction_tome")) != MeridianRegistry.EXTRACTION_TOME) {
            helper.fail("EXTRACTION_TOME singleton doesn't match registry");
            return;
        }
        helper.succeed();
    }
}
