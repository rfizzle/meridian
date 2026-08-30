// Tier: 3 (Fabric Gametest)
package com.rfizzle.meridian.gametest;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.MeridianRegistry;
import com.rfizzle.meridian.enchanting.MeridianEnchantmentLogic;
import com.rfizzle.meridian.enchanting.MeridianEnchantmentMenu;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EnchantingTableBlock;

/**
 * Integration coverage for XP tomes paying enchanting table cost deficits (#162).
 *
 * <p>Note the two paths charge differently: the enchant path gates on {@code costs[slot]} (the
 * level requirement) but only consumes {@code slot+1} levels, so a tome only ever pays the
 * shortfall between the bar and {@code slot+1}. The craft path gates on and consumes the full
 * {@code costs[CRAFTING_SLOT]}, so the tome pays the full bar shortfall. The tests below assert
 * conservation against each path's actual consumption.
 */
public class XpTomeEnchantIntegrationGameTest implements FabricGameTest {

    /** Longer than the default: these tests wait on deferred ticks (advancement grants, menu round-trips). */
    private static final int TIMEOUT = 100;

    private static final BlockPos TABLE_POS = new BlockPos(4, 1, 4);

    private static void placeTable(GameTestHelper helper) {
        helper.setBlock(TABLE_POS, Blocks.ENCHANTING_TABLE.defaultBlockState());
        for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
            helper.setBlock(TABLE_POS.offset(offset), Blocks.BOOKSHELF.defaultBlockState());
        }
    }

    private MeridianEnchantmentMenu openMenu(GameTestHelper helper, Player player) {
        BlockPos absTable = helper.absolutePos(TABLE_POS);
        return new MeridianEnchantmentMenu(
                1, player.getInventory(),
                ContainerLevelAccess.create(helper.getLevel(), absTable));
    }

    private static int tomeStored(Player player) {
        int total = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof com.rfizzle.meridian.tome.XpTomeItem) {
                total += stack.getOrDefault(MeridianRegistry.STORED_XP, 0);
            }
        }
        return total;
    }

    // --- AC: insufficient bar + sufficient tome → enchant succeeds, deficit debited ---

    @GameTest(template = "meridian:shelf_scan_9x4x9")
    public void enchantSucceedsWithTome(GameTestHelper helper) {
        placeTable(helper);

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.experienceLevel = 0;
        MeridianEnchantmentMenu menu = openMenu(helper, player);
        menu.getSlot(0).set(new ItemStack(Items.DIAMOND_SWORD));
        menu.getSlot(1).set(new ItemStack(Items.LAPIS_LAZULI, 64));

        int requirement = menu.costs[0];
        if (requirement <= 0) {
            helper.fail("Slot 0 cost should be > 0 with bookshelves, got " + requirement);
            return;
        }

        // Bank exactly the level requirement in a tome; the bar is empty.
        ItemStack tome = new ItemStack(MeridianRegistry.XP_TOME_T3);
        tome.set(MeridianRegistry.STORED_XP, requirement);
        player.getInventory().add(tome);

        boolean clicked = menu.clickMenuButton(player, 0);
        if (!clicked) {
            helper.fail("Enchant should succeed when a tome covers the level requirement");
            return;
        }
        if (!menu.getSlot(0).getItem().isEnchanted()) {
            helper.fail("Sword should be enchanted after a tome-funded enchant");
            return;
        }
        if (player.experienceLevel != 0) {
            helper.fail("Player bar should stay at 0, got " + player.experienceLevel);
            return;
        }
        // Slot 0 consumes exactly 1 level, drawn entirely from the tome.
        int expected = requirement - 1;
        if (tomeStored(player) != expected) {
            helper.fail("Tome should hold " + expected + " after a 1-level draw, got " + tomeStored(player));
            return;
        }
        helper.succeed();
    }

    // --- Conservation: a tome-funded enchant must not wipe the bar's sub-level progress ---

    @GameTest(template = "meridian:shelf_scan_9x4x9")
    public void enchantPreservesSubLevelProgress(GameTestHelper helper) {
        placeTable(helper);

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.experienceLevel = 0;
        player.experienceProgress = 0.5F; // half a level of XP banked below the level bar
        MeridianEnchantmentMenu menu = openMenu(helper, player);
        menu.getSlot(0).set(new ItemStack(Items.DIAMOND_SWORD));
        menu.getSlot(1).set(new ItemStack(Items.LAPIS_LAZULI, 64));

        int requirement = menu.costs[0];
        if (requirement <= 0) {
            helper.fail("Slot 0 cost should be > 0 with bookshelves");
            return;
        }
        ItemStack tome = new ItemStack(MeridianRegistry.XP_TOME_T3);
        tome.set(MeridianRegistry.STORED_XP, requirement);
        player.getInventory().add(tome);

        boolean clicked = menu.clickMenuButton(player, 0);
        if (!clicked) {
            helper.fail("Enchant should succeed with a tome covering the requirement");
            return;
        }
        // The bar had nothing to charge (0 levels), so the tome paid the whole 1-level cost and the
        // sub-level progress must be untouched — no vanilla negative-clamp wipe.
        if (player.experienceLevel != 0) {
            helper.fail("Bar level should stay 0, got " + player.experienceLevel);
            return;
        }
        if (Math.abs(player.experienceProgress - 0.5F) > 1.0E-4F) {
            helper.fail("Sub-level XP progress should be preserved (0.5), got " + player.experienceProgress);
            return;
        }
        if (tomeStored(player) != requirement - 1) {
            helper.fail("Tome should hold " + (requirement - 1) + ", got " + tomeStored(player));
            return;
        }
        helper.succeed();
    }

    // --- AC: bar + tome together still short → click rejected, no state change ---

    @GameTest(template = "meridian:shelf_scan_9x4x9")
    public void enchantRejectedWhenInsufficient(GameTestHelper helper) {
        placeTable(helper);

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.experienceLevel = 0;
        MeridianEnchantmentMenu menu = openMenu(helper, player);
        menu.getSlot(0).set(new ItemStack(Items.DIAMOND_SWORD));
        menu.getSlot(1).set(new ItemStack(Items.LAPIS_LAZULI, 64));

        int requirement = menu.costs[0];
        if (requirement <= 0) {
            helper.fail("Slot 0 cost should be > 0 with bookshelves, got " + requirement);
            return;
        }

        // One level short of the requirement, with an empty bar.
        int stored = requirement - 1;
        ItemStack tome = new ItemStack(MeridianRegistry.XP_TOME_T3);
        if (stored > 0) {
            tome.set(MeridianRegistry.STORED_XP, stored);
        }
        player.getInventory().add(tome);

        boolean clicked = menu.clickMenuButton(player, 0);
        if (clicked) {
            helper.fail("Enchant should be rejected when bar + tome is below the requirement");
            return;
        }
        if (menu.getSlot(0).getItem().isEnchanted()) {
            helper.fail("Sword must not be enchanted on a rejected click");
            return;
        }
        if (tomeStored(player) != stored) {
            helper.fail("Tome balance must be untouched on rejection, got " + tomeStored(player));
            return;
        }
        helper.succeed();
    }

    // --- AC: no tome present → behavior identical to today (rejected on empty bar) ---

    @GameTest(template = "meridian:shelf_scan_9x4x9")
    public void enchantNoTomeUnchanged(GameTestHelper helper) {
        placeTable(helper);

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.experienceLevel = 0;
        MeridianEnchantmentMenu menu = openMenu(helper, player);
        menu.getSlot(0).set(new ItemStack(Items.DIAMOND_SWORD));
        menu.getSlot(1).set(new ItemStack(Items.LAPIS_LAZULI, 64));

        if (menu.costs[0] <= 0) {
            helper.fail("Slot 0 cost should be > 0 with bookshelves");
            return;
        }

        boolean clicked = menu.clickMenuButton(player, 0);
        if (clicked) {
            helper.fail("Enchant should be rejected with 0 bar and no tome (unchanged behavior)");
            return;
        }
        if (menu.getSlot(0).getItem().isEnchanted()) {
            helper.fail("Sword must not be enchanted");
            return;
        }
        helper.succeed();
    }

    // --- AC: crafting path — bar + tome covers the full cost, XP conserved ---

    @GameTest(template = "meridian:shelf_scan_9x4x9", timeoutTicks = TIMEOUT)
    public void craftSucceedsWithTomeConservesXp(GameTestHelper helper) {
        Block endshelf = BuiltInRegistries.BLOCK.get(Meridian.id("endshelf"));
        if (endshelf == Blocks.AIR) {
            helper.fail("endshelf block not found in registry");
            return;
        }
        helper.setBlock(TABLE_POS, Blocks.ENCHANTING_TABLE.defaultBlockState());
        int placed = 0;
        for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
            helper.setBlock(TABLE_POS.offset(offset), endshelf.defaultBlockState());
            if (++placed >= 10) break;
        }

        Block seashelf = BuiltInRegistries.BLOCK.get(Meridian.id("seashelf"));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        // infused_seashelf costs 45 levels; split 40 from the bar + 5 from a tome.
        player.experienceLevel = 40;
        MeridianEnchantmentMenu menu = openMenu(helper, player);
        menu.getSlot(0).set(new ItemStack(seashelf));
        menu.getSlot(1).set(new ItemStack(Items.LAPIS_LAZULI, 64));

        if (menu.currentRecipe().isEmpty()) {
            helper.fail("Seashelf infusion recipe should match with 10 endshelves");
            return;
        }
        int xpCost = menu.costs[MeridianEnchantmentLogic.CRAFTING_SLOT];
        if (xpCost <= player.experienceLevel) {
            helper.fail("Test expects the craft cost (" + xpCost + ") to exceed the bar (40)");
            return;
        }

        ItemStack tome = new ItemStack(MeridianRegistry.XP_TOME_T3);
        tome.set(MeridianRegistry.STORED_XP, xpCost - 40);
        player.getInventory().add(tome);

        int barBefore = player.experienceLevel;
        int tomeBefore = tomeStored(player);
        boolean clicked = menu.clickMenuButton(player, MeridianEnchantmentLogic.CRAFTING_SLOT);
        if (!clicked) {
            helper.fail("Craft should succeed when bar + tome covers the cost");
            return;
        }

        ResourceLocation infusedId = Meridian.id("infused_seashelf");
        if (!BuiltInRegistries.ITEM.getKey(menu.getSlot(0).getItem().getItem()).equals(infusedId)) {
            helper.fail("Expected infused_seashelf output, got " + menu.getSlot(0).getItem());
            return;
        }
        if (player.experienceLevel != 0) {
            helper.fail("Bar should be fully spent to 0, got " + player.experienceLevel);
            return;
        }
        int spent = (barBefore - player.experienceLevel) + (tomeBefore - tomeStored(player));
        if (spent != xpCost) {
            helper.fail("XP not conserved: spent " + spent + " for a " + xpCost + "-level craft");
            return;
        }
        helper.succeed();
    }

    // --- AC: crafting path — bar + tome still short → rejected, no state change ---

    @GameTest(template = "meridian:shelf_scan_9x4x9", timeoutTicks = TIMEOUT)
    public void craftRejectedWhenInsufficient(GameTestHelper helper) {
        Block endshelf = BuiltInRegistries.BLOCK.get(Meridian.id("endshelf"));
        if (endshelf == Blocks.AIR) {
            helper.fail("endshelf block not found in registry");
            return;
        }
        helper.setBlock(TABLE_POS, Blocks.ENCHANTING_TABLE.defaultBlockState());
        int placed = 0;
        for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
            helper.setBlock(TABLE_POS.offset(offset), endshelf.defaultBlockState());
            if (++placed >= 10) break;
        }

        Block seashelf = BuiltInRegistries.BLOCK.get(Meridian.id("seashelf"));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.experienceLevel = 40;
        MeridianEnchantmentMenu menu = openMenu(helper, player);
        menu.getSlot(0).set(new ItemStack(seashelf));
        menu.getSlot(1).set(new ItemStack(Items.LAPIS_LAZULI, 64));

        if (menu.currentRecipe().isEmpty()) {
            helper.fail("Seashelf infusion recipe should match with 10 endshelves");
            return;
        }
        int xpCost = menu.costs[MeridianEnchantmentLogic.CRAFTING_SLOT];

        // One level short of the cost even with the tome.
        int stored = xpCost - 40 - 1;
        ItemStack tome = new ItemStack(MeridianRegistry.XP_TOME_T3);
        if (stored > 0) {
            tome.set(MeridianRegistry.STORED_XP, stored);
        }
        player.getInventory().add(tome);

        boolean clicked = menu.clickMenuButton(player, MeridianEnchantmentLogic.CRAFTING_SLOT);
        if (clicked) {
            helper.fail("Craft should be rejected when bar + tome is below the cost");
            return;
        }
        if (player.experienceLevel != 40) {
            helper.fail("Bar must be untouched on rejection, got " + player.experienceLevel);
            return;
        }
        if (tomeStored(player) != Math.max(0, stored)) {
            helper.fail("Tome must be untouched on rejection, got " + tomeStored(player));
            return;
        }
        if (menu.getSlot(0).getItem().is(BuiltInRegistries.ITEM.get(Meridian.id("infused_seashelf")))) {
            helper.fail("Input must not be transformed on a rejected craft");
            return;
        }
        helper.succeed();
    }
}
