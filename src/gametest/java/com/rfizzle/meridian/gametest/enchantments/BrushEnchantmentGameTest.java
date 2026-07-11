package com.rfizzle.meridian.gametest.enchantments;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.enchanting.BrushEnchantMath;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Meticulous: brushing suspicious blocks is faster per level (fewer strokes to excavate), and the
 * archaeology loot roll is quality-biased toward rarer entries. Both are exercised by driving a real
 * {@link BrushableBlockEntity} through {@code brush} — the same server path the brush item uses.
 */
public class BrushEnchantmentGameTest implements FabricGameTest {

    /**
     * A controlled loot table with a steep luck gradient: the common entry ({@code weight 20,
     * quality -5}) dominates at luck 0, while the rare entry ({@code weight 1, quality +5}) climbs
     * sharply with luck. The luck Meticulous adds pulls rolls toward the rare entry — the exact
     * quality-weighting the enchant promises.
     */
    private static final ResourceKey<LootTable> BIAS_TABLE =
            ResourceKey.create(Registries.LOOT_TABLE, Meridian.id("gametest/meticulous_bias"));

    private static final BlockPos SUPPORT = new BlockPos(1, 1, 1);
    private static final BlockPos BLOCK = new BlockPos(1, 2, 1);

    private Holder<Enchantment> lookup(GameTestHelper helper, String id) {
        Registry<Enchantment> reg = helper.getLevel().registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT);
        return reg.getHolder(Meridian.id(id)).orElse(null);
    }

    private ItemStack brush(GameTestHelper helper, Holder<Enchantment> meticulous, int level) {
        ItemStack brush = new ItemStack(Items.BRUSH);
        if (meticulous != null && level > 0) brush.enchant(meticulous, level);
        return brush;
    }

    /** Strokes a fresh suspicious-sand block takes to excavate while {@code player} holds its brush. */
    private int strokesToExcavate(GameTestHelper helper, Player player) {
        helper.setBlock(BLOCK, Blocks.SUSPICIOUS_SAND);
        BrushableBlockEntity be = (BrushableBlockEntity) helper.getBlockEntity(BLOCK);
        int strokes = 0;
        long gameTime = 100;
        while (strokes < 64) {
            strokes++;
            if (be.brush(gameTime, player, Direction.UP)) return strokes;
            gameTime += 10;
        }
        return -1;
    }

    // --- Speed: fewer strokes per level, exactly the Math contract ---

    @GameTest(template = "meridian:empty_3x3")
    public void meticulousShortensBrushingPerLevel(GameTestHelper helper) {
        Holder<Enchantment> meticulous = lookup(helper, "meticulous");
        if (meticulous == null) { helper.fail("meticulous not in registry"); return; }

        helper.setBlock(SUPPORT, Blocks.STONE);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        player.setItemSlot(EquipmentSlot.MAINHAND, brush(helper, meticulous, 0));
        int plain = strokesToExcavate(helper, player);

        player.setItemSlot(EquipmentSlot.MAINHAND, brush(helper, meticulous, 2));
        int enchanted = strokesToExcavate(helper, player);
        player.discard();

        if (plain != BrushEnchantMath.BRUSH_COMPLETION_BASE) {
            helper.fail("An unenchanted brush should take the vanilla "
                    + BrushEnchantMath.BRUSH_COMPLETION_BASE + " strokes, took " + plain);
            return;
        }
        if (enchanted != BrushEnchantMath.brushCompletionCount(2)) {
            helper.fail("Meticulous II should excavate in " + BrushEnchantMath.brushCompletionCount(2)
                    + " strokes, took " + enchanted);
            return;
        }
        if (enchanted >= plain) {
            helper.fail("Meticulous must make brushing faster: enchanted=" + enchanted + ", plain=" + plain);
            return;
        }
        helper.succeed();
    }

    // --- Loot bias: quality-weighting toward the rarer entry ---

    @GameTest(template = "meridian:empty_3x3")
    public void meticulousBiasesArchaeologyLootTowardRarer(GameTestHelper helper) {
        Holder<Enchantment> meticulous = lookup(helper, "meticulous");
        if (meticulous == null) { helper.fail("meticulous not in registry"); return; }

        helper.setBlock(SUPPORT, Blocks.STONE);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        AttributeInstance luck = player.getAttribute(Attributes.LUCK);
        if (luck != null) luck.setBaseValue(0.0);
        final int samples = 30;

        // Same fixed seed set for both runs, so the comparison is deterministic across runs. The
        // added luck can only raise the rare entry's weight, so Meticulous surfaces it more often.
        player.setItemSlot(EquipmentSlot.MAINHAND, brush(helper, meticulous, 0));
        int plainRare = countRareRolls(helper, player, samples);

        player.setItemSlot(EquipmentSlot.MAINHAND, brush(helper, meticulous, 2));
        int enchantedRare = countRareRolls(helper, player, samples);
        player.discard();

        if (enchantedRare <= plainRare) {
            helper.fail("Meticulous II must bias archaeology rolls toward the rarer entry: plain="
                    + plainRare + ", enchanted=" + enchantedRare + " of " + samples);
            return;
        }
        helper.succeed();
    }

    private int countRareRolls(GameTestHelper helper, Player player, int samples) {
        int rare = 0;
        for (long seed = 0; seed < samples; seed++) {
            // Clear to air first so each iteration gets a fresh block entity (a same-block setBlock
            // keeps the old one, whose brush cooldown would gate out the roll).
            helper.setBlock(BLOCK, Blocks.AIR);
            helper.setBlock(BLOCK, Blocks.SUSPICIOUS_SAND);
            BrushableBlockEntity be = (BrushableBlockEntity) helper.getBlockEntity(BLOCK);
            be.setLootTable(BIAS_TABLE, seed);
            // One stroke is enough to roll the loot (vanilla rolls on the first brush).
            be.brush(100, player, Direction.UP);
            if (be.getItem().is(Items.DIAMOND)) rare++;
        }
        return rare;
    }
}
