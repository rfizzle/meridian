// Tier: 3 (Fabric Gametest)
package com.rfizzle.meridian.gametest;

import com.rfizzle.meridian.MeridianRegistry;
import com.rfizzle.meridian.api.MeridianAPI;
import com.rfizzle.meridian.library.BasicLibraryBlockEntity;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Blocks;

import java.util.OptionalInt;

/**
 * Verifies the read-only {@link MeridianAPI#getStoredPoints} library query: a placed library
 * answers with its point pool, and any non-library position returns the
 * {@link OptionalInt#empty()} sentinel.
 */
public class LibraryQueryApiGameTest implements FabricGameTest {

    private static final BlockPos LIB_POS = new BlockPos(1, 1, 1);
    private static final BlockPos STONE_POS = new BlockPos(0, 1, 0);

    @GameTest(template = "meridian:empty_3x3")
    public void storedPointsQueryableThroughApi(GameTestHelper helper) {
        helper.setBlock(LIB_POS, MeridianRegistry.BASIC_LIBRARY.defaultBlockState());
        BasicLibraryBlockEntity be = (BasicLibraryBlockEntity) helper.getLevel()
                .getBlockEntity(helper.absolutePos(LIB_POS));
        if (be == null) { helper.fail("Library BE not created"); return; }

        ResourceKey<Enchantment> key = Enchantments.SHARPNESS;
        BlockPos abs = helper.absolutePos(LIB_POS);

        OptionalInt empty = MeridianAPI.getStoredPoints(helper.getLevel(), abs, key);
        if (empty.isEmpty() || empty.getAsInt() != 0) {
            helper.fail("Fresh library should report 0 stored points, got " + empty);
            return;
        }

        Registry<Enchantment> reg =
                helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        be.depositBook(enchantedBook(reg, key, 3));

        OptionalInt stored = MeridianAPI.getStoredPoints(helper.getLevel(), abs, key);
        if (stored.isEmpty()) {
            helper.fail("Library position should not return the not-a-library sentinel");
            return;
        }
        // points(level) = 2^(level-1): a level-3 book deposits 4 points.
        if (stored.getAsInt() != 4) {
            helper.fail("Expected 4 stored points after a level-3 book, got " + stored.getAsInt());
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void nonLibraryPositionsReturnSentinel(GameTestHelper helper) {
        helper.setBlock(STONE_POS, Blocks.STONE.defaultBlockState());
        ResourceKey<Enchantment> key = Enchantments.SHARPNESS;

        OptionalInt stone = MeridianAPI.getStoredPoints(
                helper.getLevel(), helper.absolutePos(STONE_POS), key);
        if (stone.isPresent()) {
            helper.fail("Stone position should return OptionalInt.empty(), got " + stone);
            return;
        }

        OptionalInt air = MeridianAPI.getStoredPoints(
                helper.getLevel(), helper.absolutePos(new BlockPos(2, 2, 2)), key);
        if (air.isPresent()) {
            helper.fail("Air position should return OptionalInt.empty(), got " + air);
            return;
        }
        helper.succeed();
    }

    private static ItemStack enchantedBook(
            Registry<Enchantment> reg, ResourceKey<Enchantment> key, int level) {
        Holder<Enchantment> holder = reg.getHolderOrThrow(key);
        ItemStack stack = new ItemStack(Items.ENCHANTED_BOOK);
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        mutable.set(holder, level);
        stack.set(DataComponents.STORED_ENCHANTMENTS, mutable.toImmutable());
        return stack;
    }
}
