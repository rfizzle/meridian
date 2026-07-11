package com.rfizzle.meridian.gametest.enchantments;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.enchanting.MiningEnchantMath;
import com.rfizzle.meridian.gametest.MockPlayers;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class MiningEnchantmentGameTest implements FabricGameTest {

    private Holder<Enchantment> lookup(GameTestHelper helper, String id) {
        Registry<Enchantment> reg = helper.getLevel().registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT);
        return reg.getHolder(Meridian.id(id)).orElse(null);
    }

    // --- Grind: hardness-scaled break speed, inert on soft blocks ---

    @GameTest(template = "meridian:empty_3x3")
    public void grindSpeedsUpHardBlocksOnly(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "grind");
        if (ench == null) { helper.fail("grind not in registry"); return; }

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        // A mock player never ticks physics and reads as airborne, which would
        // (correctly) shrink the bonus through the vanilla off-ground penalty.
        player.setOnGround(true);
        BlockState obsidian = Blocks.OBSIDIAN.defaultBlockState();
        BlockState stone = Blocks.STONE.defaultBlockState();

        player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_PICKAXE));
        float plainObsidian = player.getDestroySpeed(obsidian);
        float plainStone = player.getDestroySpeed(stone);

        ItemStack grindPick = new ItemStack(Items.DIAMOND_PICKAXE);
        grindPick.enchant(ench, 3);
        player.setItemSlot(EquipmentSlot.MAINHAND, grindPick);
        float grindObsidian = player.getDestroySpeed(obsidian);
        float grindStone = player.getDestroySpeed(stone);
        player.discard();

        // Obsidian (hardness 50) hits the cap at level III.
        float expected = MiningEnchantMath.grindBonus(3, 50.0f);
        if (Math.abs(grindObsidian - (plainObsidian + expected)) > 1e-4f) {
            helper.fail("Grind III on obsidian should add " + expected + ": plain="
                    + plainObsidian + ", grind=" + grindObsidian);
            return;
        }
        // Stone (hardness 1.5) is below the soft-block gate — no change.
        if (Math.abs(grindStone - plainStone) > 1e-4f) {
            helper.fail("Grind must not change speed on soft blocks: plain=" + plainStone
                    + ", grind=" + grindStone);
            return;
        }
        helper.succeed();
    }

    // --- Adamant: tier gating on drops eligibility ---

    @GameTest(template = "meridian:empty_3x3")
    public void adamantRaisesHarvestTierPerLevel(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "adamant");
        if (ench == null) { helper.fail("adamant not in registry"); return; }

        BlockState goldOre = Blocks.GOLD_ORE.defaultBlockState();     // needs iron tier
        BlockState obsidian = Blocks.OBSIDIAN.defaultBlockState();    // needs diamond tier

        ItemStack plainStonePick = new ItemStack(Items.STONE_PICKAXE);
        if (plainStonePick.isCorrectToolForDrops(goldOre)) {
            helper.fail("Baseline broken: a plain stone pickaxe must not harvest gold ore");
            return;
        }

        ItemStack adamantOne = new ItemStack(Items.STONE_PICKAXE);
        adamantOne.enchant(ench, 1);
        if (!adamantOne.isCorrectToolForDrops(goldOre)) {
            helper.fail("Adamant I on a stone pickaxe should harvest iron-tier blocks");
            return;
        }
        if (adamantOne.isCorrectToolForDrops(obsidian)) {
            helper.fail("Adamant I on a stone pickaxe must not reach diamond-tier blocks");
            return;
        }

        ItemStack adamantTwo = new ItemStack(Items.STONE_PICKAXE);
        adamantTwo.enchant(ench, 2);
        if (!adamantTwo.isCorrectToolForDrops(obsidian)) {
            helper.fail("Adamant II on a stone pickaxe should reach diamond-tier blocks");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void adamantIsInertOffPickaxes(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "adamant");
        if (ench == null) { helper.fail("adamant not in registry"); return; }

        // Force-applied (command-style) onto a non-pickaxe: no tier boost.
        ItemStack sword = new ItemStack(Items.STONE_SWORD);
        sword.enchant(ench, 2);
        if (sword.isCorrectToolForDrops(Blocks.GOLD_ORE.defaultBlockState())) {
            helper.fail("Adamant must not grant harvest tiers to non-pickaxe items");
            return;
        }
        helper.succeed();
    }

    // --- Reclaim: drops route to inventory, no ground items ---

    @GameTest(template = "meridian:empty_3x3")
    public void reclaimRoutesDropsToInventory(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "reclaim");
        if (ench == null) { helper.fail("reclaim not in registry"); return; }

        ServerPlayer player = MockPlayers.serverPlayerInLevel(helper);
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        ItemStack pick = new ItemStack(Items.IRON_PICKAXE);
        pick.enchant(ench, 1);
        player.setItemSlot(EquipmentSlot.MAINHAND, pick);

        BlockPos rel = new BlockPos(1, 2, 1);
        helper.setBlock(rel, Blocks.STONE);
        BlockPos abs = helper.absolutePos(rel);

        player.gameMode.destroyBlock(abs);

        helper.assertBlockPresent(Blocks.AIR, rel);
        boolean inInventory = player.getInventory().items.stream()
                .anyMatch(s -> s.is(Items.COBBLESTONE));
        List<ItemEntity> groundItems = helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                new AABB(abs).inflate(4.0));
        player.discard();

        if (!inInventory) {
            helper.fail("Reclaim should place the cobblestone drop in the player's inventory");
            return;
        }
        if (!groundItems.isEmpty()) {
            helper.fail("Reclaim must not spawn ground items, found " + groundItems.size());
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void unenchantedBreakStillDropsGroundItems(GameTestHelper helper) {
        ServerPlayer player = MockPlayers.serverPlayerInLevel(helper);
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_PICKAXE));

        BlockPos rel = new BlockPos(1, 2, 1);
        helper.setBlock(rel, Blocks.STONE);
        BlockPos abs = helper.absolutePos(rel);

        player.gameMode.destroyBlock(abs);

        List<ItemEntity> groundItems = helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                new AABB(abs).inflate(4.0), item -> item.getItem().is(Items.COBBLESTONE));
        player.discard();

        if (groundItems.isEmpty()) {
            helper.fail("Without Reclaim, the vanilla ground drop must be untouched");
            return;
        }
        helper.succeed();
    }

    // --- Kiln: mined drops come out smelted, composing with Reclaim ---

    @GameTest(template = "meridian:empty_3x3")
    public void kilnSmeltsMinedDropOnTheGround(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "kiln");
        if (ench == null) { helper.fail("kiln not in registry"); return; }

        ServerPlayer player = MockPlayers.serverPlayerInLevel(helper);
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        ItemStack pick = new ItemStack(Items.IRON_PICKAXE);
        pick.enchant(ench, 1);
        player.setItemSlot(EquipmentSlot.MAINHAND, pick);

        BlockPos rel = new BlockPos(1, 2, 1);
        helper.setBlock(rel, Blocks.COBBLESTONE);
        BlockPos abs = helper.absolutePos(rel);

        player.gameMode.destroyBlock(abs);

        AABB area = new AABB(abs).inflate(4.0);
        boolean stoneDropped = !helper.getLevel().getEntitiesOfClass(ItemEntity.class, area,
                item -> item.getItem().is(Items.STONE)).isEmpty();
        boolean cobbleDropped = !helper.getLevel().getEntitiesOfClass(ItemEntity.class, area,
                item -> item.getItem().is(Items.COBBLESTONE)).isEmpty();
        player.discard();

        if (!stoneDropped) {
            helper.fail("Kiln should drop the smelted result (stone) when breaking cobblestone");
            return;
        }
        if (cobbleDropped) {
            helper.fail("Kiln must replace the raw cobblestone drop, not add to it");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void kilnComposesWithReclaimIntoInventory(GameTestHelper helper) {
        Holder<Enchantment> kiln = lookup(helper, "kiln");
        Holder<Enchantment> reclaim = lookup(helper, "reclaim");
        if (kiln == null || reclaim == null) { helper.fail("kiln/reclaim not in registry"); return; }

        ServerPlayer player = MockPlayers.serverPlayerInLevel(helper);
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        ItemStack pick = new ItemStack(Items.IRON_PICKAXE);
        pick.enchant(kiln, 1);
        pick.enchant(reclaim, 1);
        player.setItemSlot(EquipmentSlot.MAINHAND, pick);

        BlockPos rel = new BlockPos(1, 2, 1);
        helper.setBlock(rel, Blocks.COBBLESTONE);
        BlockPos abs = helper.absolutePos(rel);

        player.gameMode.destroyBlock(abs);

        boolean smeltedInInventory = player.getInventory().items.stream().anyMatch(s -> s.is(Items.STONE));
        boolean rawInInventory = player.getInventory().items.stream().anyMatch(s -> s.is(Items.COBBLESTONE));
        List<ItemEntity> groundItems = helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                new AABB(abs).inflate(4.0));
        player.discard();

        if (!smeltedInInventory) {
            helper.fail("Kiln + Reclaim should place the smelted stone straight into the inventory");
            return;
        }
        if (rawInInventory) {
            helper.fail("Kiln + Reclaim must smelt the drop, not bank raw cobblestone");
            return;
        }
        if (!groundItems.isEmpty()) {
            helper.fail("Kiln + Reclaim must not spawn ground items, found " + groundItems.size());
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void kilnLeavesNonSmeltableBlocksAlone(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "kiln");
        if (ench == null) { helper.fail("kiln not in registry"); return; }

        ServerPlayer player = MockPlayers.serverPlayerInLevel(helper);
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        ItemStack shovel = new ItemStack(Items.IRON_SHOVEL);
        shovel.enchant(ench, 1);
        player.setItemSlot(EquipmentSlot.MAINHAND, shovel);

        // Dirt has no smelting recipe — Kiln must leave its drop untouched.
        BlockPos rel = new BlockPos(1, 2, 1);
        helper.setBlock(rel, Blocks.DIRT);
        BlockPos abs = helper.absolutePos(rel);

        player.gameMode.destroyBlock(abs);

        boolean dirtDropped = !helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                new AABB(abs).inflate(4.0), item -> item.getItem().is(Items.DIRT)).isEmpty();
        player.discard();

        if (!dirtDropped) {
            helper.fail("Kiln must drop a non-smeltable block (dirt) normally");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void kilnAppliesToPickaxesAndShovelsOnly(GameTestHelper helper) {
        Holder<Enchantment> kiln = lookup(helper, "kiln");
        if (kiln == null) { helper.fail("kiln not in registry"); return; }

        Enchantment kilnE = kiln.value();
        if (!kilnE.canEnchant(new ItemStack(Items.DIAMOND_PICKAXE))
                || !kilnE.canEnchant(new ItemStack(Items.DIAMOND_SHOVEL))) {
            helper.fail("kiln should apply to pickaxes and shovels");
            return;
        }
        if (kilnE.canEnchant(new ItemStack(Items.DIAMOND_AXE))
                || kilnE.canEnchant(new ItemStack(Items.DIAMOND_HOE))
                || kilnE.canEnchant(new ItemStack(Items.DIAMOND_SWORD))) {
            helper.fail("kiln should NOT apply to axes, hoes, or swords");
            return;
        }
        helper.succeed();
    }

    // --- Definitions: item eligibility follows the issue's item lists ---

    @GameTest(template = "meridian:empty_3x3")
    public void miningEnchantsApplyToTheirItemSets(GameTestHelper helper) {
        Holder<Enchantment> grind = lookup(helper, "grind");
        Holder<Enchantment> adamant = lookup(helper, "adamant");
        Holder<Enchantment> reclaim = lookup(helper, "reclaim");
        if (grind == null || adamant == null || reclaim == null) {
            helper.fail("mining enchantments missing from registry");
            return;
        }

        ItemStack pick = new ItemStack(Items.DIAMOND_PICKAXE);
        ItemStack axe = new ItemStack(Items.DIAMOND_AXE);
        ItemStack shovel = new ItemStack(Items.DIAMOND_SHOVEL);
        ItemStack hoe = new ItemStack(Items.DIAMOND_HOE);
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);

        Enchantment grindE = grind.value();
        if (!grindE.canEnchant(pick) || !grindE.canEnchant(axe) || !grindE.canEnchant(shovel)) {
            helper.fail("grind should apply to pickaxes, axes, and shovels");
            return;
        }
        if (grindE.canEnchant(hoe) || grindE.canEnchant(sword)) {
            helper.fail("grind should NOT apply to hoes or swords");
            return;
        }

        Enchantment adamantE = adamant.value();
        if (!adamantE.canEnchant(pick)) {
            helper.fail("adamant should apply to pickaxes");
            return;
        }
        if (adamantE.canEnchant(axe) || adamantE.canEnchant(shovel)) {
            helper.fail("adamant should ONLY apply to pickaxes");
            return;
        }

        Enchantment reclaimE = reclaim.value();
        if (!reclaimE.canEnchant(pick) || !reclaimE.canEnchant(axe)
                || !reclaimE.canEnchant(shovel) || !reclaimE.canEnchant(hoe)) {
            helper.fail("reclaim should apply to all mining tools");
            return;
        }
        if (reclaimE.canEnchant(sword)) {
            helper.fail("reclaim should NOT apply to swords");
            return;
        }
        helper.succeed();
    }

    // --- Exclusive sets: Grind joins the mining line; the others stay free ---

    @GameTest(template = "meridian:empty_3x3")
    public void grindJoinsMiningExclusiveSet(GameTestHelper helper) {
        Holder<Enchantment> grind = lookup(helper, "grind");
        Holder<Enchantment> excavate = lookup(helper, "excavate");
        Holder<Enchantment> prospect = lookup(helper, "prospect");
        Holder<Enchantment> adamant = lookup(helper, "adamant");
        Holder<Enchantment> reclaim = lookup(helper, "reclaim");
        if (grind == null || excavate == null || prospect == null
                || adamant == null || reclaim == null) {
            helper.fail("enchantments missing from registry");
            return;
        }

        if (Enchantment.areCompatible(grind, excavate) || Enchantment.areCompatible(grind, prospect)) {
            helper.fail("grind must be exclusive with excavate and prospect (mining set)");
            return;
        }
        if (!Enchantment.areCompatible(adamant, grind) || !Enchantment.areCompatible(reclaim, grind)
                || !Enchantment.areCompatible(adamant, reclaim)) {
            helper.fail("adamant and reclaim should be compatible with the rest of the mining kit");
            return;
        }
        helper.succeed();
    }
}
