package com.rfizzle.meridian.gametest.enchantments;

import com.rfizzle.meridian.Meridian;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Verdure — verifies the shears-gated sapling/apple pools are appended to the vanilla leaf tables
 * and only fire for a Verdure-enchanted tool. Rolls each table many times so the chance-based
 * pools are exercised deterministically (a sapling appearing at least once across thousands of
 * rolls is statistically certain; a plain shears never producing one is exact).
 */
public class VerdureGameTest implements FabricGameTest {

    private static final int ROLLS = 3000;

    private Holder<Enchantment> lookup(GameTestHelper helper, String id) {
        Registry<Enchantment> reg = helper.getLevel().registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT);
        return reg.getHolder(Meridian.id(id)).orElse(null);
    }

    private ItemStack verdureShears(GameTestHelper helper) {
        Holder<Enchantment> verdure = lookup(helper, "verdure");
        if (verdure == null) {
            helper.fail("verdure not in registry");
            return ItemStack.EMPTY;
        }
        ItemStack shears = new ItemStack(Items.SHEARS);
        shears.enchant(verdure, 1);
        return shears;
    }

    /** Total count of {@code item} produced by rolling {@code leaf}'s loot table {@code ROLLS} times with {@code tool}. */
    private int rollFor(GameTestHelper helper, Block leaf, ItemStack tool, net.minecraft.world.item.Item item) {
        ServerLevel level = helper.getLevel();
        LootTable table = level.getServer().reloadableRegistries().getLootTable(leaf.getLootTable());
        Vec3 origin = Vec3.atCenterOf(helper.absolutePos(new BlockPos(1, 1, 1)));

        int total = 0;
        for (int i = 0; i < ROLLS; i++) {
            LootParams params = new LootParams.Builder(level)
                    .withParameter(LootContextParams.ORIGIN, origin)
                    .withParameter(LootContextParams.TOOL, tool)
                    .withParameter(LootContextParams.BLOCK_STATE, leaf.defaultBlockState())
                    .create(LootContextParamSets.BLOCK);
            List<ItemStack> drops = table.getRandomItems(params);
            for (ItemStack drop : drops) {
                if (drop.is(item)) {
                    total += drop.getCount();
                }
            }
        }
        return total;
    }

    @GameTest(template = "meridian:empty_3x3")
    public void verdureShearsDropOakSaplingAndApple(GameTestHelper helper) {
        ItemStack shears = verdureShears(helper);
        if (shears.isEmpty()) return;

        int saplings = rollFor(helper, Blocks.OAK_LEAVES, shears, Items.OAK_SAPLING);
        int apples = rollFor(helper, Blocks.OAK_LEAVES, shears, Items.APPLE);
        if (saplings <= 0) {
            helper.fail("Verdure shears should drop oak saplings from oak leaves");
        } else if (apples <= 0) {
            helper.fail("Verdure shears should drop apples from oak leaves");
        } else {
            helper.succeed();
        }
    }

    @GameTest(template = "meridian:empty_3x3")
    public void verdureShearsDropDarkOakSaplingAndApple(GameTestHelper helper) {
        ItemStack shears = verdureShears(helper);
        if (shears.isEmpty()) return;

        int saplings = rollFor(helper, Blocks.DARK_OAK_LEAVES, shears, Items.DARK_OAK_SAPLING);
        int apples = rollFor(helper, Blocks.DARK_OAK_LEAVES, shears, Items.APPLE);
        if (saplings <= 0) {
            helper.fail("Verdure shears should drop dark oak saplings from dark oak leaves");
        } else if (apples <= 0) {
            helper.fail("Verdure shears should drop apples from dark oak leaves");
        } else {
            helper.succeed();
        }
    }

    @GameTest(template = "meridian:empty_3x3")
    public void verdureShearsDropBirchSaplingButNoApple(GameTestHelper helper) {
        ItemStack shears = verdureShears(helper);
        if (shears.isEmpty()) return;

        int saplings = rollFor(helper, Blocks.BIRCH_LEAVES, shears, Items.BIRCH_SAPLING);
        int apples = rollFor(helper, Blocks.BIRCH_LEAVES, shears, Items.APPLE);
        if (saplings <= 0) {
            helper.fail("Verdure shears should drop birch saplings from birch leaves");
        } else if (apples != 0) {
            helper.fail("Birch leaves must not drop apples — apple is oak/dark_oak only");
        } else {
            helper.succeed();
        }
    }

    @GameTest(template = "meridian:empty_3x3")
    public void plainShearsDropNoSapling(GameTestHelper helper) {
        // Control: an unenchanted shears trips neither the sapling nor the apple pool.
        ItemStack plain = new ItemStack(Items.SHEARS);
        int saplings = rollFor(helper, Blocks.OAK_LEAVES, plain, Items.OAK_SAPLING);
        int apples = rollFor(helper, Blocks.OAK_LEAVES, plain, Items.APPLE);
        if (saplings != 0) {
            helper.fail("Plain shears must not drop oak saplings");
        } else if (apples != 0) {
            helper.fail("Plain shears must not drop apples");
        } else {
            helper.succeed();
        }
    }
}
