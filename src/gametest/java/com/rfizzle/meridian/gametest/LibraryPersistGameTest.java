// Tier: 3 (Fabric Gametest) — TEST-4.3-T3
package com.rfizzle.meridian.gametest;

import com.rfizzle.meridian.MeridianRegistry;
import com.rfizzle.meridian.library.BasicLibraryBlockEntity;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class LibraryPersistGameTest implements FabricGameTest {

    private static final BlockPos LIB_POS = new BlockPos(1, 1, 1);

    @GameTest(template = "meridian:empty_3x3")
    public void libraryStatePreservedThroughSaveLoad(GameTestHelper helper) {
        BlockState state = MeridianRegistry.BASIC_LIBRARY.defaultBlockState();
        helper.setBlock(LIB_POS, state);

        BlockPos absPos = helper.absolutePos(LIB_POS);
        BasicLibraryBlockEntity be = (BasicLibraryBlockEntity) helper.getLevel()
                .getBlockEntity(absPos);
        if (be == null) {
            helper.fail("Library BE not created");
            return;
        }

        Registry<Enchantment> reg = helper.getLevel().registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT);
        be.depositBook(enchantedBook(reg, Enchantments.SHARPNESS, 5));
        be.depositBook(enchantedBook(reg, Enchantments.UNBREAKING, 3));

        ResourceKey<Enchantment> sharpKey = Enchantments.SHARPNESS;
        ResourceKey<Enchantment> unbrkKey = Enchantments.UNBREAKING;
        int sharpPts = be.getPoints().getInt(sharpKey);
        int sharpMax = be.getMaxLevels().getInt(sharpKey);
        int unbrkPts = be.getPoints().getInt(unbrkKey);
        int unbrkMax = be.getMaxLevels().getInt(unbrkKey);

        HolderLookup.Provider registries = helper.getLevel().registryAccess();
        CompoundTag saved = be.saveWithFullMetadata(registries);

        BlockEntity reloaded = BlockEntity.loadStatic(absPos, state, saved, registries);
        if (!(reloaded instanceof BasicLibraryBlockEntity reloadedLib)) {
            helper.fail("loadStatic did not produce BasicLibraryBlockEntity, got "
                    + (reloaded == null ? "null" : reloaded.getClass().getSimpleName()));
            return;
        }

        if (reloadedLib.getPoints().getInt(sharpKey) != sharpPts) {
            helper.fail("Sharpness points not preserved: expected " + sharpPts
                    + " got " + reloadedLib.getPoints().getInt(sharpKey));
            return;
        }
        if (reloadedLib.getMaxLevels().getInt(sharpKey) != sharpMax) {
            helper.fail("Sharpness maxLevels not preserved: expected " + sharpMax
                    + " got " + reloadedLib.getMaxLevels().getInt(sharpKey));
            return;
        }
        if (reloadedLib.getPoints().getInt(unbrkKey) != unbrkPts) {
            helper.fail("Unbreaking points not preserved: expected " + unbrkPts
                    + " got " + reloadedLib.getPoints().getInt(unbrkKey));
            return;
        }
        if (reloadedLib.getMaxLevels().getInt(unbrkKey) != unbrkMax) {
            helper.fail("Unbreaking maxLevels not preserved: expected " + unbrkMax
                    + " got " + reloadedLib.getMaxLevels().getInt(unbrkKey));
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void freshLibraryHasEmptyState(GameTestHelper helper) {
        helper.setBlock(LIB_POS, MeridianRegistry.BASIC_LIBRARY.defaultBlockState());
        BasicLibraryBlockEntity be = (BasicLibraryBlockEntity) helper.getLevel()
                .getBlockEntity(helper.absolutePos(LIB_POS));
        if (be == null) {
            helper.fail("Library BE not created");
            return;
        }
        if (!be.getPoints().isEmpty()) {
            helper.fail("Fresh library should have empty points map");
            return;
        }
        if (!be.getMaxLevels().isEmpty()) {
            helper.fail("Fresh library should have empty maxLevels map");
            return;
        }
        helper.succeed();
    }

    private static ItemStack enchantedBook(Registry<Enchantment> reg,
                                           ResourceKey<Enchantment> key, int level) {
        Holder<Enchantment> holder = reg.getHolderOrThrow(key);
        ItemStack stack = new ItemStack(Items.ENCHANTED_BOOK);
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        mutable.set(holder, level);
        stack.set(DataComponents.STORED_ENCHANTMENTS, mutable.toImmutable());
        return stack;
    }
}
