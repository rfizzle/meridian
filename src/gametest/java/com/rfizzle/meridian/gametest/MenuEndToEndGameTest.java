// Tier: 3 (Fabric Gametest)
package com.rfizzle.meridian.gametest;

import com.rfizzle.meridian.enchanting.MeridianEnchantmentMenu;
import com.rfizzle.meridian.enchanting.StatCollection;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EnchantingTableBlock;

public class MenuEndToEndGameTest implements FabricGameTest {

    private static final BlockPos TABLE_POS = new BlockPos(4, 1, 4);

    @GameTest(template = "meridian:shelf_scan_9x4x9")
    public void menuTypeIsMeridianEnchantmentMenu(GameTestHelper helper) {
        helper.setBlock(TABLE_POS, Blocks.ENCHANTING_TABLE.defaultBlockState());

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos absTable = helper.absolutePos(TABLE_POS);
        MeridianEnchantmentMenu menu = new MeridianEnchantmentMenu(
                1, player.getInventory(),
                ContainerLevelAccess.create(helper.getLevel(), absTable));

        if (!(menu instanceof MeridianEnchantmentMenu)) {
            helper.fail("Menu is not MeridianEnchantmentMenu");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "meridian:shelf_scan_9x4x9")
    public void clickSlotZeroAppliesEnchant(GameTestHelper helper) {
        helper.setBlock(TABLE_POS, Blocks.ENCHANTING_TABLE.defaultBlockState());
        for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
            helper.setBlock(TABLE_POS.offset(offset), Blocks.BOOKSHELF.defaultBlockState());
        }

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.experienceLevel = 30;
        BlockPos absTable = helper.absolutePos(TABLE_POS);
        MeridianEnchantmentMenu menu = new MeridianEnchantmentMenu(
                1, player.getInventory(),
                ContainerLevelAccess.create(helper.getLevel(), absTable));

        menu.getSlot(0).set(new ItemStack(Items.DIAMOND_SWORD));
        menu.getSlot(1).set(new ItemStack(Items.LAPIS_LAZULI, 64));

        if (menu.costs[0] <= 0) {
            helper.fail("Slot 0 cost should be > 0 with 15 bookshelves, got " + menu.costs[0]);
            return;
        }

        int xpBefore = player.experienceLevel;
        boolean clicked = menu.clickMenuButton(player, 0);
        if (!clicked) {
            helper.fail("clickMenuButton(0) returned false");
            return;
        }

        ItemStack result = menu.getSlot(0).getItem();
        if (!result.isEnchanted()) {
            helper.fail("Sword should be enchanted after clicking slot 0");
            return;
        }
        if (player.experienceLevel >= xpBefore) {
            helper.fail("XP should have decreased after enchanting");
            return;
        }
        helper.succeed();
    }

    // --- S-5.1b: Menu has item slot (slot 0) and lapis slot (slot 1) ---

    @GameTest(template = "meridian:shelf_scan_9x4x9")
    public void menuHasItemAndLapisSlots(GameTestHelper helper) {
        helper.setBlock(TABLE_POS, Blocks.ENCHANTING_TABLE.defaultBlockState());

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos absTable = helper.absolutePos(TABLE_POS);
        MeridianEnchantmentMenu menu = new MeridianEnchantmentMenu(
                1, player.getInventory(),
                ContainerLevelAccess.create(helper.getLevel(), absTable));

        menu.getSlot(0).set(new ItemStack(Items.DIAMOND_SWORD));
        if (menu.getSlot(0).getItem().isEmpty()) {
            helper.fail("Slot 0 (item slot) should accept a diamond sword");
            return;
        }

        menu.getSlot(1).set(new ItemStack(Items.LAPIS_LAZULI, 3));
        if (menu.getSlot(1).getItem().isEmpty()) {
            helper.fail("Slot 1 (lapis slot) should accept lapis lazuli");
            return;
        }

        helper.succeed();
    }

    // --- S-5.1c: Stats are computed and stored on menu when item is placed ---

    @GameTest(template = "meridian:shelf_scan_9x4x9")
    public void statsComputedWhenItemPlaced(GameTestHelper helper) {
        helper.setBlock(TABLE_POS, Blocks.ENCHANTING_TABLE.defaultBlockState());
        for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
            helper.setBlock(TABLE_POS.offset(offset), Blocks.BOOKSHELF.defaultBlockState());
        }

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos absTable = helper.absolutePos(TABLE_POS);
        MeridianEnchantmentMenu menu = new MeridianEnchantmentMenu(
                1, player.getInventory(),
                ContainerLevelAccess.create(helper.getLevel(), absTable));

        menu.getSlot(0).set(new ItemStack(Items.DIAMOND_SWORD));
        menu.getSlot(1).set(new ItemStack(Items.LAPIS_LAZULI, 64));

        StatCollection stats = menu.getLastStats();
        if (stats.eterna() <= 0) {
            helper.fail("Stats should have eterna > 0 with bookshelves, got " + stats.eterna());
            return;
        }
        if (stats.quanta() <= 0) {
            helper.fail("Stats should have quanta > 0 (baseline +15), got " + stats.quanta());
            return;
        }

        helper.succeed();
    }

    // --- S-5.1d: Costs array has 3 entries corresponding to 3 enchanting slots ---

    @GameTest(template = "meridian:shelf_scan_9x4x9")
    public void costsArrayHasThreeEntries(GameTestHelper helper) {
        helper.setBlock(TABLE_POS, Blocks.ENCHANTING_TABLE.defaultBlockState());
        for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
            helper.setBlock(TABLE_POS.offset(offset), Blocks.BOOKSHELF.defaultBlockState());
        }

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.experienceLevel = 30;
        BlockPos absTable = helper.absolutePos(TABLE_POS);
        MeridianEnchantmentMenu menu = new MeridianEnchantmentMenu(
                1, player.getInventory(),
                ContainerLevelAccess.create(helper.getLevel(), absTable));

        if (menu.costs.length != 3) {
            helper.fail("Costs array should have 3 entries, got " + menu.costs.length);
            return;
        }

        menu.getSlot(0).set(new ItemStack(Items.DIAMOND_SWORD));
        menu.getSlot(1).set(new ItemStack(Items.LAPIS_LAZULI, 64));

        boolean anyCostSet = menu.costs[0] > 0 || menu.costs[1] > 0 || menu.costs[2] > 0;
        if (!anyCostSet) {
            helper.fail("At least one cost slot should be > 0 with bookshelves and a sword");
            return;
        }

        helper.succeed();
    }

    @GameTest(template = "meridian:shelf_scan_9x4x9")
    public void clickSlotZeroDecrementsLapis(GameTestHelper helper) {
        helper.setBlock(TABLE_POS, Blocks.ENCHANTING_TABLE.defaultBlockState());
        for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
            helper.setBlock(TABLE_POS.offset(offset), Blocks.BOOKSHELF.defaultBlockState());
        }

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.experienceLevel = 30;
        BlockPos absTable = helper.absolutePos(TABLE_POS);
        MeridianEnchantmentMenu menu = new MeridianEnchantmentMenu(
                1, player.getInventory(),
                ContainerLevelAccess.create(helper.getLevel(), absTable));

        menu.getSlot(0).set(new ItemStack(Items.DIAMOND_SWORD));
        menu.getSlot(1).set(new ItemStack(Items.LAPIS_LAZULI, 64));

        int lapisBefore = menu.getSlot(1).getItem().getCount();
        menu.clickMenuButton(player, 0);
        int lapisAfter = menu.getSlot(1).getItem().getCount();

        if (lapisAfter >= lapisBefore) {
            helper.fail("Lapis should decrease after enchanting, was " + lapisBefore + " now " + lapisAfter);
            return;
        }
        helper.succeed();
    }

    // --- S-5.2a: Slot 0 costs 1 lapis, slot 1 costs 2, slot 2 costs 3 ---

    @GameTest(template = "meridian:shelf_scan_9x4x9")
    public void lapisCostMatchesSlotIndex(GameTestHelper helper) {
        helper.setBlock(TABLE_POS, Blocks.ENCHANTING_TABLE.defaultBlockState());
        for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
            helper.setBlock(TABLE_POS.offset(offset), Blocks.BOOKSHELF.defaultBlockState());
        }

        for (int slot = 0; slot < 3; slot++) {
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            player.experienceLevel = 30;
            BlockPos absTable = helper.absolutePos(TABLE_POS);
            MeridianEnchantmentMenu menu = new MeridianEnchantmentMenu(
                    1, player.getInventory(),
                    ContainerLevelAccess.create(helper.getLevel(), absTable));

            menu.getSlot(0).set(new ItemStack(Items.DIAMOND_SWORD));
            menu.getSlot(1).set(new ItemStack(Items.LAPIS_LAZULI, 64));

            if (menu.costs[slot] <= 0) {
                helper.fail("Slot " + slot + " cost should be > 0 with 15 bookshelves");
                return;
            }

            int lapisBefore = menu.getSlot(1).getItem().getCount();
            boolean clicked = menu.clickMenuButton(player, slot);
            if (!clicked) {
                helper.fail("clickMenuButton(" + slot + ") returned false");
                return;
            }
            int lapisAfter = menu.getSlot(1).getItem().getCount();
            int consumed = lapisBefore - lapisAfter;
            int expected = slot + 1;
            if (consumed != expected) {
                helper.fail("Slot " + slot + " should consume " + expected + " lapis, consumed " + consumed);
                return;
            }
        }

        helper.succeed();
    }

    // --- S-5.2b: Cannot enchant without sufficient lapis ---

    @GameTest(template = "meridian:shelf_scan_9x4x9")
    public void cannotEnchantWithoutLapis(GameTestHelper helper) {
        helper.setBlock(TABLE_POS, Blocks.ENCHANTING_TABLE.defaultBlockState());
        for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
            helper.setBlock(TABLE_POS.offset(offset), Blocks.BOOKSHELF.defaultBlockState());
        }

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.experienceLevel = 30;
        BlockPos absTable = helper.absolutePos(TABLE_POS);
        MeridianEnchantmentMenu menu = new MeridianEnchantmentMenu(
                1, player.getInventory(),
                ContainerLevelAccess.create(helper.getLevel(), absTable));

        menu.getSlot(0).set(new ItemStack(Items.DIAMOND_SWORD));

        boolean clicked = menu.clickMenuButton(player, 0);
        if (clicked) {
            helper.fail("Enchanting should fail with no lapis in slot 1");
            return;
        }

        helper.succeed();
    }

    // --- S-5.2c: Cannot enchant without sufficient player XP levels ---

    @GameTest(template = "meridian:shelf_scan_9x4x9")
    public void cannotEnchantWithoutXp(GameTestHelper helper) {
        helper.setBlock(TABLE_POS, Blocks.ENCHANTING_TABLE.defaultBlockState());
        for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
            helper.setBlock(TABLE_POS.offset(offset), Blocks.BOOKSHELF.defaultBlockState());
        }

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.experienceLevel = 0;
        BlockPos absTable = helper.absolutePos(TABLE_POS);
        MeridianEnchantmentMenu menu = new MeridianEnchantmentMenu(
                1, player.getInventory(),
                ContainerLevelAccess.create(helper.getLevel(), absTable));

        menu.getSlot(0).set(new ItemStack(Items.DIAMOND_SWORD));
        menu.getSlot(1).set(new ItemStack(Items.LAPIS_LAZULI, 64));

        boolean clicked = menu.clickMenuButton(player, 0);
        if (clicked) {
            helper.fail("Enchanting should fail with 0 XP levels");
            return;
        }

        helper.succeed();
    }

    // --- S-5.2d: Successful enchant consumes correct lapis and XP ---

    @GameTest(template = "meridian:shelf_scan_9x4x9")
    public void enchantConsumesCorrectLapisAndXp(GameTestHelper helper) {
        helper.setBlock(TABLE_POS, Blocks.ENCHANTING_TABLE.defaultBlockState());
        for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
            helper.setBlock(TABLE_POS.offset(offset), Blocks.BOOKSHELF.defaultBlockState());
        }

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.experienceLevel = 30;
        BlockPos absTable = helper.absolutePos(TABLE_POS);
        MeridianEnchantmentMenu menu = new MeridianEnchantmentMenu(
                1, player.getInventory(),
                ContainerLevelAccess.create(helper.getLevel(), absTable));

        menu.getSlot(0).set(new ItemStack(Items.DIAMOND_SWORD));
        menu.getSlot(1).set(new ItemStack(Items.LAPIS_LAZULI, 64));

        int lapisBefore = menu.getSlot(1).getItem().getCount();
        int xpBefore = player.experienceLevel;
        boolean clicked = menu.clickMenuButton(player, 0);
        if (!clicked) {
            helper.fail("clickMenuButton(0) returned false");
            return;
        }

        int lapisConsumed = lapisBefore - menu.getSlot(1).getItem().getCount();
        if (lapisConsumed != 1) {
            helper.fail("Slot 0 should consume exactly 1 lapis, consumed " + lapisConsumed);
            return;
        }

        int xpConsumed = xpBefore - player.experienceLevel;
        if (xpConsumed != 1) {
            helper.fail("Slot 0 should consume exactly 1 XP level, consumed " + xpConsumed);
            return;
        }

        helper.succeed();
    }

    // --- S-5.3a: Diamond sword receives at least 1 enchantment ---

    @GameTest(template = "meridian:shelf_scan_9x4x9")
    public void diamondSwordReceivesEnchantment(GameTestHelper helper) {
        helper.setBlock(TABLE_POS, Blocks.ENCHANTING_TABLE.defaultBlockState());
        for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
            helper.setBlock(TABLE_POS.offset(offset), Blocks.BOOKSHELF.defaultBlockState());
        }

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.experienceLevel = 30;
        BlockPos absTable = helper.absolutePos(TABLE_POS);
        MeridianEnchantmentMenu menu = new MeridianEnchantmentMenu(
                1, player.getInventory(),
                ContainerLevelAccess.create(helper.getLevel(), absTable));

        menu.getSlot(0).set(new ItemStack(Items.DIAMOND_SWORD));
        menu.getSlot(1).set(new ItemStack(Items.LAPIS_LAZULI, 64));

        menu.clickMenuButton(player, 0);

        ItemStack result = menu.getSlot(0).getItem();
        if (!result.isEnchanted()) {
            helper.fail("Diamond sword should have at least 1 enchantment after enchanting");
            return;
        }
        if (!result.is(Items.DIAMOND_SWORD)) {
            helper.fail("Result should still be a diamond sword, got " + result.getItem());
            return;
        }

        helper.succeed();
    }

    // --- S-5.3b: Book receives at least 1 enchantment (becomes enchanted book) ---

    @GameTest(template = "meridian:shelf_scan_9x4x9")
    public void bookReceivesEnchantment(GameTestHelper helper) {
        helper.setBlock(TABLE_POS, Blocks.ENCHANTING_TABLE.defaultBlockState());
        for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
            helper.setBlock(TABLE_POS.offset(offset), Blocks.BOOKSHELF.defaultBlockState());
        }

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.experienceLevel = 30;
        BlockPos absTable = helper.absolutePos(TABLE_POS);
        MeridianEnchantmentMenu menu = new MeridianEnchantmentMenu(
                1, player.getInventory(),
                ContainerLevelAccess.create(helper.getLevel(), absTable));

        menu.getSlot(0).set(new ItemStack(Items.BOOK));
        menu.getSlot(1).set(new ItemStack(Items.LAPIS_LAZULI, 64));

        menu.clickMenuButton(player, 0);

        ItemStack result = menu.getSlot(0).getItem();
        if (!result.is(Items.ENCHANTED_BOOK)) {
            helper.fail("Book should become an enchanted book after enchanting, got " + result.getItem());
            return;
        }

        helper.succeed();
    }

    // --- S-5.3c: Item with 0 enchantability: no enchantment options ---

    @GameTest(template = "meridian:shelf_scan_9x4x9")
    public void nonEnchantableItemHasNoCosts(GameTestHelper helper) {
        helper.setBlock(TABLE_POS, Blocks.ENCHANTING_TABLE.defaultBlockState());
        for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
            helper.setBlock(TABLE_POS.offset(offset), Blocks.BOOKSHELF.defaultBlockState());
        }

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.experienceLevel = 30;
        BlockPos absTable = helper.absolutePos(TABLE_POS);
        MeridianEnchantmentMenu menu = new MeridianEnchantmentMenu(
                1, player.getInventory(),
                ContainerLevelAccess.create(helper.getLevel(), absTable));

        menu.getSlot(0).set(new ItemStack(Items.STICK));
        menu.getSlot(1).set(new ItemStack(Items.LAPIS_LAZULI, 64));

        boolean allZero = menu.costs[0] == 0 && menu.costs[1] == 0 && menu.costs[2] == 0;
        if (!allZero) {
            helper.fail("Non-enchantable item should have all costs=0, got ["
                    + menu.costs[0] + ", " + menu.costs[1] + ", " + menu.costs[2] + "]");
            return;
        }

        helper.succeed();
    }

    // --- S-5.3d: After enchanting, item has enchantments and costs are reset ---

    @GameTest(template = "meridian:shelf_scan_9x4x9")
    public void costsResetAfterEnchanting(GameTestHelper helper) {
        helper.setBlock(TABLE_POS, Blocks.ENCHANTING_TABLE.defaultBlockState());
        for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
            helper.setBlock(TABLE_POS.offset(offset), Blocks.BOOKSHELF.defaultBlockState());
        }

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.experienceLevel = 30;
        BlockPos absTable = helper.absolutePos(TABLE_POS);
        MeridianEnchantmentMenu menu = new MeridianEnchantmentMenu(
                1, player.getInventory(),
                ContainerLevelAccess.create(helper.getLevel(), absTable));

        menu.getSlot(0).set(new ItemStack(Items.DIAMOND_SWORD));
        menu.getSlot(1).set(new ItemStack(Items.LAPIS_LAZULI, 64));

        if (menu.costs[0] <= 0) {
            helper.fail("Cost should be > 0 before enchanting");
            return;
        }

        menu.clickMenuButton(player, 0);

        ItemStack result = menu.getSlot(0).getItem();
        if (!result.isEnchanted()) {
            helper.fail("Sword should be enchanted");
            return;
        }

        boolean allZero = menu.costs[0] == 0 && menu.costs[1] == 0 && menu.costs[2] == 0;
        if (!allZero) {
            helper.fail("Costs should reset to 0 after enchanting (item is already enchanted), got ["
                    + menu.costs[0] + ", " + menu.costs[1] + ", " + menu.costs[2] + "]");
            return;
        }

        helper.succeed();
    }
}
