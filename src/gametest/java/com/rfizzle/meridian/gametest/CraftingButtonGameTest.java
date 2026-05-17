// Tier: 3 (Fabric Gametest)
package com.rfizzle.meridian.gametest;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.enchanting.MeridianEnchantmentLogic;
import com.rfizzle.meridian.enchanting.MeridianEnchantmentMenu;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EnchantingTableBlock;

/**
 * Tests the crafting-result click path (button id = {@link MeridianEnchantmentLogic#CRAFTING_SLOT}).
 * Uses endshelf blocks (eterna=2.5, quanta=5, arcana=5, maxEterna=45 each) to reach the
 * infused_seashelf recipe thresholds (E≥22.5, Q≥15, A≥10) after baselines (+15Q, +0A for blocks).
 */
public class CraftingButtonGameTest implements FabricGameTest {

    private static final BlockPos TABLE_POS = new BlockPos(4, 1, 4);

    @GameTest(template = "meridian:shelf_scan_9x4x9", timeoutTicks = 100)
    public void craftingRecipeMatchDetected(GameTestHelper helper) {
        Block endshelf = BuiltInRegistries.BLOCK.get(Meridian.id("endshelf"));
        if (endshelf == Blocks.AIR) {
            helper.fail("endshelf block not found in registry");
            return;
        }

        helper.setBlock(TABLE_POS, Blocks.ENCHANTING_TABLE.defaultBlockState());
        int placed = 0;
        for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
            helper.setBlock(TABLE_POS.offset(offset), endshelf.defaultBlockState());
            placed++;
            if (placed >= 10) break;
        }

        ResourceLocation seashelfId = Meridian.id("seashelf");
        Block seashelfBlock = BuiltInRegistries.BLOCK.get(seashelfId);
        if (seashelfBlock == Blocks.AIR) {
            helper.fail("seashelf block not found — needed as recipe input item");
            return;
        }
        ItemStack seashelfItem = new ItemStack(seashelfBlock);

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.experienceLevel = 30;
        BlockPos absTable = helper.absolutePos(TABLE_POS);
        MeridianEnchantmentMenu menu = new MeridianEnchantmentMenu(
                1, player.getInventory(),
                ContainerLevelAccess.create(helper.getLevel(), absTable));

        menu.getSlot(0).set(seashelfItem);
        menu.getSlot(1).set(new ItemStack(Items.LAPIS_LAZULI, 64));

        if (menu.currentRecipe().isEmpty()) {
            helper.fail("With 10 endshelves + seashelf input, a crafting recipe should match. "
                    + "Stats: costs=" + java.util.Arrays.toString(menu.costs));
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "meridian:shelf_scan_9x4x9", timeoutTicks = 100)
    public void craftingClickProducesOutput(GameTestHelper helper) {
        Block endshelf = BuiltInRegistries.BLOCK.get(Meridian.id("endshelf"));
        if (endshelf == Blocks.AIR) {
            helper.fail("endshelf block not found");
            return;
        }

        helper.setBlock(TABLE_POS, Blocks.ENCHANTING_TABLE.defaultBlockState());
        int placed = 0;
        for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
            helper.setBlock(TABLE_POS.offset(offset), endshelf.defaultBlockState());
            placed++;
            if (placed >= 10) break;
        }

        ResourceLocation seashelfId = Meridian.id("seashelf");
        Block seashelfBlock = BuiltInRegistries.BLOCK.get(seashelfId);
        ItemStack seashelfItem = new ItemStack(seashelfBlock);

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.experienceLevel = 30;
        BlockPos absTable = helper.absolutePos(TABLE_POS);
        MeridianEnchantmentMenu menu = new MeridianEnchantmentMenu(
                1, player.getInventory(),
                ContainerLevelAccess.create(helper.getLevel(), absTable));

        menu.getSlot(0).set(seashelfItem);
        menu.getSlot(1).set(new ItemStack(Items.LAPIS_LAZULI, 64));

        if (menu.currentRecipe().isEmpty()) {
            helper.fail("Recipe not matched — cannot test crafting click");
            return;
        }

        int xpBefore = player.experienceLevel;
        boolean clicked = menu.clickMenuButton(player, MeridianEnchantmentLogic.CRAFTING_SLOT);
        if (!clicked) {
            helper.fail("clickMenuButton(CRAFTING_SLOT) returned false");
            return;
        }

        ItemStack result = menu.getSlot(0).getItem();
        ResourceLocation infusedId = Meridian.id("infused_seashelf");
        if (!BuiltInRegistries.ITEM.getKey(result.getItem()).equals(infusedId)) {
            helper.fail("Expected infused_seashelf in input slot after craft, got "
                    + BuiltInRegistries.ITEM.getKey(result.getItem()));
            return;
        }
        helper.succeed();
    }

    /**
     * S-6.3b — Seashelf with only 3 endshelves (E=7.5) does not reach the infused_seashelf
     * recipe minimum (E≥22.5). Verifies that no crafting recipe matches.
     */
    @GameTest(template = "meridian:shelf_scan_9x4x9", timeoutTicks = 100)
    public void statsBelowMinNoInfusion(GameTestHelper helper) {
        Block endshelf = BuiltInRegistries.BLOCK.get(Meridian.id("endshelf"));
        if (endshelf == Blocks.AIR) {
            helper.fail("endshelf block not found in registry");
            return;
        }

        helper.setBlock(TABLE_POS, Blocks.ENCHANTING_TABLE.defaultBlockState());
        int placed = 0;
        for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
            helper.setBlock(TABLE_POS.offset(offset), endshelf.defaultBlockState());
            placed++;
            if (placed >= 3) break;
        }

        Block seashelfBlock = BuiltInRegistries.BLOCK.get(Meridian.id("seashelf"));
        if (seashelfBlock == Blocks.AIR) {
            helper.fail("seashelf block not found");
            return;
        }

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.experienceLevel = 30;
        BlockPos absTable = helper.absolutePos(TABLE_POS);
        MeridianEnchantmentMenu menu = new MeridianEnchantmentMenu(
                1, player.getInventory(),
                ContainerLevelAccess.create(helper.getLevel(), absTable));

        menu.getSlot(0).set(new ItemStack(seashelfBlock));
        menu.getSlot(1).set(new ItemStack(Items.LAPIS_LAZULI, 64));

        if (menu.currentRecipe().isPresent()) {
            helper.fail("With 3 endshelves (E=7.5), seashelf infusion should not match "
                    + "(requires E>=22.5). Stats: costs=" + java.util.Arrays.toString(menu.costs));
            return;
        }
        helper.succeed();
    }

    /**
     * S-6.3c — Carrot with 5 endshelves (E=12.5) exceeds the golden_carrot recipe's
     * max_requirements (E≤10). Verifies that no crafting recipe matches.
     */
    @GameTest(template = "meridian:shelf_scan_9x4x9", timeoutTicks = 100)
    public void statsAboveMaxNoInfusion(GameTestHelper helper) {
        Block endshelf = BuiltInRegistries.BLOCK.get(Meridian.id("endshelf"));
        if (endshelf == Blocks.AIR) {
            helper.fail("endshelf block not found in registry");
            return;
        }

        helper.setBlock(TABLE_POS, Blocks.ENCHANTING_TABLE.defaultBlockState());
        int placed = 0;
        for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
            helper.setBlock(TABLE_POS.offset(offset), endshelf.defaultBlockState());
            placed++;
            if (placed >= 5) break;
        }

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.experienceLevel = 30;
        BlockPos absTable = helper.absolutePos(TABLE_POS);
        MeridianEnchantmentMenu menu = new MeridianEnchantmentMenu(
                1, player.getInventory(),
                ContainerLevelAccess.create(helper.getLevel(), absTable));

        menu.getSlot(0).set(new ItemStack(Items.CARROT));
        menu.getSlot(1).set(new ItemStack(Items.LAPIS_LAZULI, 64));

        if (menu.currentRecipe().isPresent()) {
            helper.fail("With 5 endshelves (E=12.5), golden carrot infusion should not match "
                    + "(max_requirements E=10). Stats: costs=" + java.util.Arrays.toString(menu.costs));
            return;
        }
        helper.succeed();
    }

    /**
     * S-6.3d — Hellshelf with 10 endshelves (E=25, Q=65, A=50) meets the infused_hellshelf
     * recipe thresholds (E≥22.5, Q≥30, A≥0). Clicking the crafting button produces infused_hellshelf.
     */
    @GameTest(template = "meridian:shelf_scan_9x4x9", timeoutTicks = 100)
    public void shelfUpgradeHellshelfToInfused(GameTestHelper helper) {
        Block endshelf = BuiltInRegistries.BLOCK.get(Meridian.id("endshelf"));
        if (endshelf == Blocks.AIR) {
            helper.fail("endshelf block not found in registry");
            return;
        }

        helper.setBlock(TABLE_POS, Blocks.ENCHANTING_TABLE.defaultBlockState());
        int placed = 0;
        for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
            helper.setBlock(TABLE_POS.offset(offset), endshelf.defaultBlockState());
            placed++;
            if (placed >= 10) break;
        }

        Block hellshelfBlock = BuiltInRegistries.BLOCK.get(Meridian.id("hellshelf"));
        if (hellshelfBlock == Blocks.AIR) {
            helper.fail("hellshelf block not found");
            return;
        }

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.experienceLevel = 30;
        BlockPos absTable = helper.absolutePos(TABLE_POS);
        MeridianEnchantmentMenu menu = new MeridianEnchantmentMenu(
                1, player.getInventory(),
                ContainerLevelAccess.create(helper.getLevel(), absTable));

        menu.getSlot(0).set(new ItemStack(hellshelfBlock));
        menu.getSlot(1).set(new ItemStack(Items.LAPIS_LAZULI, 64));

        if (menu.currentRecipe().isEmpty()) {
            helper.fail("Hellshelf recipe should match with 10 endshelves (E=25, Q=65, A=50). "
                    + "Stats: costs=" + java.util.Arrays.toString(menu.costs));
            return;
        }

        boolean clicked = menu.clickMenuButton(player, MeridianEnchantmentLogic.CRAFTING_SLOT);
        if (!clicked) {
            helper.fail("clickMenuButton(CRAFTING_SLOT) returned false for hellshelf upgrade");
            return;
        }

        ItemStack result = menu.getSlot(0).getItem();
        ResourceLocation infusedId = Meridian.id("infused_hellshelf");
        if (!BuiltInRegistries.ITEM.getKey(result.getItem()).equals(infusedId)) {
            helper.fail("Expected infused_hellshelf, got "
                    + BuiltInRegistries.ITEM.getKey(result.getItem()));
            return;
        }
        helper.succeed();
    }

    /**
     * S-6.3e — Library with Sharpness V infused into ender library via keep_nbt_enchanting recipe.
     * Uses 7 echoing deepshelves + 6 endshelves + 4 draconic endshelves to hit the tight
     * stat window (E=50, Q=45, A=100). Verifies enchantments are preserved on the output.
     */
    @GameTest(template = "meridian:shelf_scan_9x4x9", timeoutTicks = 100)
    public void keepNbtInfusionRetainsEnchantments(GameTestHelper helper) {
        Block echoingDeepshelf = BuiltInRegistries.BLOCK.get(Meridian.id("echoing_deepshelf"));
        Block endshelf = BuiltInRegistries.BLOCK.get(Meridian.id("endshelf"));
        Block draconicEndshelf = BuiltInRegistries.BLOCK.get(Meridian.id("draconic_endshelf"));
        if (echoingDeepshelf == Blocks.AIR || endshelf == Blocks.AIR || draconicEndshelf == Blocks.AIR) {
            helper.fail("Required shelf blocks not found in registry");
            return;
        }

        helper.setBlock(TABLE_POS, Blocks.ENCHANTING_TABLE.defaultBlockState());
        int idx = 0;
        for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
            Block shelf;
            if (idx < 7) shelf = echoingDeepshelf;
            else if (idx < 13) shelf = endshelf;
            else if (idx < 17) shelf = draconicEndshelf;
            else break;
            helper.setBlock(TABLE_POS.offset(offset), shelf.defaultBlockState());
            idx++;
        }

        Block libraryBlock = BuiltInRegistries.BLOCK.get(Meridian.id("library"));
        if (libraryBlock == Blocks.AIR) {
            helper.fail("library block not found");
            return;
        }

        Registry<Enchantment> enchReg = helper.getLevel().registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> sharpness = enchReg.getHolderOrThrow(Enchantments.SHARPNESS);
        ItemStack libraryItem = new ItemStack(libraryBlock);
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        mutable.set(sharpness, 5);
        libraryItem.set(DataComponents.ENCHANTMENTS, mutable.toImmutable());

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.experienceLevel = 50;
        BlockPos absTable = helper.absolutePos(TABLE_POS);
        MeridianEnchantmentMenu menu = new MeridianEnchantmentMenu(
                1, player.getInventory(),
                ContainerLevelAccess.create(helper.getLevel(), absTable));

        menu.getSlot(0).set(libraryItem);
        menu.getSlot(1).set(new ItemStack(Items.LAPIS_LAZULI, 64));

        if (menu.currentRecipe().isEmpty()) {
            helper.fail("Ender library recipe should match (E=50, Q=45, A=100). "
                    + "Stats: costs=" + java.util.Arrays.toString(menu.costs));
            return;
        }

        boolean clicked = menu.clickMenuButton(player, MeridianEnchantmentLogic.CRAFTING_SLOT);
        if (!clicked) {
            helper.fail("clickMenuButton(CRAFTING_SLOT) returned false for ender library upgrade");
            return;
        }

        ItemStack result = menu.getSlot(0).getItem();
        ResourceLocation enderLibId = Meridian.id("ender_library");
        if (!BuiltInRegistries.ITEM.getKey(result.getItem()).equals(enderLibId)) {
            helper.fail("Expected ender_library, got "
                    + BuiltInRegistries.ITEM.getKey(result.getItem()));
            return;
        }

        ItemEnchantments resultEnch = result.get(DataComponents.ENCHANTMENTS);
        if (resultEnch == null || resultEnch.isEmpty()) {
            helper.fail("Ender library should retain enchantments from library input");
            return;
        }
        if (resultEnch.getLevel(sharpness) != 5) {
            helper.fail("Expected Sharpness V on ender library, got level "
                    + resultEnch.getLevel(sharpness));
            return;
        }
        helper.succeed();
    }

    /**
     * S-6.3f — Infusion with 3 scrap_tomes consumes exactly 1, produces 4 improved_scrap_tomes
     * in slot 0, and returns the 2 excess scrap_tomes to the player's inventory.
     * Uses 7 echoing deepshelves + 3 endshelves (E=25, Q=30, A=100) to match the recipe
     * (E≥22.5, Q∈[25,50], A≥35).
     */
    @GameTest(template = "meridian:shelf_scan_9x4x9", timeoutTicks = 100)
    public void infusionConsumesOneProducesCorrectCount(GameTestHelper helper) {
        Block echoingDeepshelf = BuiltInRegistries.BLOCK.get(Meridian.id("echoing_deepshelf"));
        Block endshelf = BuiltInRegistries.BLOCK.get(Meridian.id("endshelf"));
        if (echoingDeepshelf == Blocks.AIR || endshelf == Blocks.AIR) {
            helper.fail("Required shelf blocks not found in registry");
            return;
        }

        helper.setBlock(TABLE_POS, Blocks.ENCHANTING_TABLE.defaultBlockState());
        int idx = 0;
        for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
            Block shelf;
            if (idx < 7) shelf = echoingDeepshelf;
            else if (idx < 10) shelf = endshelf;
            else break;
            helper.setBlock(TABLE_POS.offset(offset), shelf.defaultBlockState());
            idx++;
        }

        ResourceLocation scrapTomeId = Meridian.id("scrap_tome");
        ItemStack scrapTomes = new ItemStack(
                BuiltInRegistries.ITEM.get(scrapTomeId), 3);
        if (scrapTomes.isEmpty()) {
            helper.fail("scrap_tome item not found in registry");
            return;
        }

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.experienceLevel = 30;
        BlockPos absTable = helper.absolutePos(TABLE_POS);
        MeridianEnchantmentMenu menu = new MeridianEnchantmentMenu(
                1, player.getInventory(),
                ContainerLevelAccess.create(helper.getLevel(), absTable));

        menu.getSlot(0).set(scrapTomes);
        menu.getSlot(1).set(new ItemStack(Items.LAPIS_LAZULI, 64));

        if (menu.currentRecipe().isEmpty()) {
            helper.fail("Improved scrap tome recipe should match (E=25, Q=30, A=100). "
                    + "Stats: costs=" + java.util.Arrays.toString(menu.costs));
            return;
        }

        boolean clicked = menu.clickMenuButton(player, MeridianEnchantmentLogic.CRAFTING_SLOT);
        if (!clicked) {
            helper.fail("clickMenuButton(CRAFTING_SLOT) returned false for scrap tome upgrade");
            return;
        }

        ItemStack result = menu.getSlot(0).getItem();
        ResourceLocation improvedId = Meridian.id("improved_scrap_tome");
        if (!BuiltInRegistries.ITEM.getKey(result.getItem()).equals(improvedId)) {
            helper.fail("Expected improved_scrap_tome in slot 0, got "
                    + BuiltInRegistries.ITEM.getKey(result.getItem()));
            return;
        }
        if (result.getCount() != 4) {
            helper.fail("Expected output count 4, got " + result.getCount());
            return;
        }

        int excessFound = 0;
        for (ItemStack invStack : player.getInventory().items) {
            if (BuiltInRegistries.ITEM.getKey(invStack.getItem()).equals(scrapTomeId)) {
                excessFound += invStack.getCount();
            }
        }
        if (excessFound != 2) {
            helper.fail("Expected 2 excess scrap_tomes in player inventory, found " + excessFound);
            return;
        }
        helper.succeed();
    }
}
