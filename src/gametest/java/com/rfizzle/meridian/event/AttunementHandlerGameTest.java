// Tier: 3 (Fabric Gametest)
package com.rfizzle.meridian.event;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.config.MeridianConfig;
import com.rfizzle.meridian.gametest.MockPlayers;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EnchantingTableBlock;

/**
 * Drives {@link AttunementHandler#repairPulse} directly with explicit config instances (no shared
 * config mutation, no waiting out real tick intervals), against a real table-and-shelf room built
 * in the test structure. The config parameter seam is what keeps these tests out of the
 * config-mutation gametest batches.
 */
public class AttunementHandlerGameTest implements FabricGameTest {

    private static final BlockPos TABLE = new BlockPos(2, 1, 2);

    private Holder<Enchantment> attunement(GameTestHelper helper) {
        Registry<Enchantment> reg = helper.getLevel().registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT);
        return reg.getHolder(Meridian.id("attunement")).orElse(null);
    }

    /** Table at {@link #TABLE} ringed with vanilla bookshelves — shelf scan reaches Eterna 15. */
    private void buildQualifyingRoom(GameTestHelper helper) {
        helper.setBlock(TABLE, Blocks.ENCHANTING_TABLE);
        for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
            helper.setBlock(TABLE.offset(offset), Blocks.BOOKSHELF);
        }
    }

    private ServerPlayer playerNearTable(GameTestHelper helper) {
        ServerPlayer player = MockPlayers.serverPlayerInLevel(helper);
        BlockPos abs = helper.absolutePos(TABLE.above());
        player.teleportTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5);
        return player;
    }

    @GameTest(template = "meridian:empty_5x5x5")
    public void repairsInventoryByLevelNearSetup(GameTestHelper helper) {
        Holder<Enchantment> ench = attunement(helper);
        if (ench == null) { helper.fail("attunement not in registry"); return; }

        buildQualifyingRoom(helper);
        ServerPlayer player = playerNearTable(helper);
        try {
            ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
            pickaxe.enchant(ench, 1);
            pickaxe.setDamageValue(10);
            ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
            sword.enchant(ench, 2);
            sword.setDamageValue(10);
            ItemStack shovel = new ItemStack(Items.DIAMOND_SHOVEL); // damaged but not attuned
            shovel.setDamageValue(10);
            ItemStack helmet = new ItemStack(Items.DIAMOND_HELMET); // attuned but undamaged
            helmet.enchant(ench, 1);
            player.getInventory().setItem(0, pickaxe);
            player.getInventory().setItem(1, sword);
            player.getInventory().setItem(2, shovel);
            player.getInventory().setItem(3, helmet);

            AttunementHandler.repairPulse(player, new MeridianConfig.Attunement());

            helper.assertValueEqual(player.getInventory().getItem(0).getDamageValue(), 9,
                    "Attunement I should repair 1 durability per pulse");
            helper.assertValueEqual(player.getInventory().getItem(1).getDamageValue(), 8,
                    "Attunement II should repair 2 durability per pulse");
            helper.assertValueEqual(player.getInventory().getItem(2).getDamageValue(), 10,
                    "an unenchanted item must not repair");
            helper.assertValueEqual(player.getInventory().getItem(3).getDamageValue(), 0,
                    "an undamaged item must stay at zero damage");
            helper.succeed();
        } finally {
            player.discard();
        }
    }

    @GameTest(template = "meridian:empty_5x5x5")
    public void noRepairBeyondRadius(GameTestHelper helper) {
        Holder<Enchantment> ench = attunement(helper);
        if (ench == null) { helper.fail("attunement not in registry"); return; }

        buildQualifyingRoom(helper);
        ServerPlayer player = MockPlayers.serverPlayerInLevel(helper);
        BlockPos corner = helper.absolutePos(new BlockPos(0, 2, 0));
        player.teleportTo(corner.getX() + 0.5, corner.getY(), corner.getZ() + 0.5);
        try {
            ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
            pickaxe.enchant(ench, 1);
            pickaxe.setDamageValue(10);
            player.getInventory().setItem(0, pickaxe);

            // The corner is ~3 blocks from the table; a 1-block radius puts it out of range.
            MeridianConfig.Attunement config = new MeridianConfig.Attunement();
            config.radius = 1;
            AttunementHandler.repairPulse(player, config);

            helper.assertValueEqual(player.getInventory().getItem(0).getDamageValue(), 10,
                    "no repair when every table is beyond the configured radius");
            helper.succeed();
        } finally {
            player.discard();
        }
    }

    @GameTest(template = "meridian:empty_5x5x5")
    public void noRepairWithoutAnyTable(GameTestHelper helper) {
        Holder<Enchantment> ench = attunement(helper);
        if (ench == null) { helper.fail("attunement not in registry"); return; }

        ServerPlayer player = playerNearTable(helper); // no room built — plain air
        try {
            ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
            pickaxe.enchant(ench, 1);
            pickaxe.setDamageValue(10);
            player.getInventory().setItem(0, pickaxe);

            AttunementHandler.repairPulse(player, new MeridianConfig.Attunement());

            helper.assertValueEqual(player.getInventory().getItem(0).getDamageValue(), 10,
                    "no repair without an enchanting table in range");
            helper.succeed();
        } finally {
            player.discard();
        }
    }

    @GameTest(template = "meridian:empty_5x5x5")
    public void noRepairBelowEternaThreshold(GameTestHelper helper) {
        Holder<Enchantment> ench = attunement(helper);
        if (ench == null) { helper.fail("attunement not in registry"); return; }

        helper.setBlock(TABLE, Blocks.ENCHANTING_TABLE); // bare table, no shelves: Eterna 0
        ServerPlayer player = playerNearTable(helper);
        try {
            ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
            pickaxe.enchant(ench, 1);
            pickaxe.setDamageValue(10);
            player.getInventory().setItem(0, pickaxe);

            AttunementHandler.repairPulse(player, new MeridianConfig.Attunement());

            helper.assertValueEqual(player.getInventory().getItem(0).getDamageValue(), 10,
                    "a shelfless table below the Eterna threshold must not repair");
            helper.succeed();
        } finally {
            player.discard();
        }
    }
}
