// Tier: 3 (Fabric Gametest) — TEST-4.5-T3
package com.rfizzle.meridian.gametest;

import com.rfizzle.meridian.MeridianRegistry;
import com.rfizzle.meridian.library.BasicLibraryBlockEntity;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

public class LibraryHopperGameTest implements FabricGameTest {

    private static final BlockPos LIB_POS = new BlockPos(1, 1, 1);

    @GameTest(template = "meridian:empty_3x3")
    public void transferApiFindsLibraryStorage(GameTestHelper helper) {
        helper.setBlock(LIB_POS, MeridianRegistry.BASIC_LIBRARY.defaultBlockState());

        BlockPos absPos = helper.absolutePos(LIB_POS);
        Storage<ItemVariant> storage = ItemStorage.SIDED.find(
                helper.getLevel(), absPos, Direction.UP);
        if (storage == null) {
            helper.fail("ItemStorage.SIDED did not find library storage at " + absPos);
            return;
        }
        helper.succeed();
    }

    @SuppressWarnings("UnstableApiUsage")
    @GameTest(template = "meridian:empty_3x3")
    public void transferApiInsertsEnchantedBookIntoLibrary(GameTestHelper helper) {
        helper.setBlock(LIB_POS, MeridianRegistry.BASIC_LIBRARY.defaultBlockState());
        BlockPos absPos = helper.absolutePos(LIB_POS);

        Storage<ItemVariant> storage = ItemStorage.SIDED.find(
                helper.getLevel(), absPos, Direction.UP);
        if (storage == null) {
            helper.fail("Storage not found");
            return;
        }

        Registry<Enchantment> reg = helper.getLevel().registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT);
        ItemStack book = enchantedBook(reg, Enchantments.SHARPNESS, 3);

        long inserted;
        try (Transaction tx = Transaction.openOuter()) {
            inserted = storage.insert(ItemVariant.of(book), 1, tx);
            tx.commit();
        }
        if (inserted != 1) {
            helper.fail("Expected 1 book inserted, got " + inserted);
            return;
        }

        BasicLibraryBlockEntity lib = (BasicLibraryBlockEntity) helper.getLevel()
                .getBlockEntity(absPos);
        if (lib == null || lib.getPoints().isEmpty()) {
            helper.fail("Library should have points after Transfer API insert");
            return;
        }
        if (lib.getPoints().getInt(Enchantments.SHARPNESS) <= 0) {
            helper.fail("Sharpness points should be > 0");
            return;
        }
        helper.succeed();
    }

    @SuppressWarnings("UnstableApiUsage")
    @GameTest(template = "meridian:empty_3x3")
    public void transferApiRejectsNonBook(GameTestHelper helper) {
        helper.setBlock(LIB_POS, MeridianRegistry.BASIC_LIBRARY.defaultBlockState());
        BlockPos absPos = helper.absolutePos(LIB_POS);

        Storage<ItemVariant> storage = ItemStorage.SIDED.find(
                helper.getLevel(), absPos, Direction.UP);
        if (storage == null) {
            helper.fail("Storage not found");
            return;
        }

        long inserted;
        try (Transaction tx = Transaction.openOuter()) {
            inserted = storage.insert(ItemVariant.of(new ItemStack(Items.DIAMOND_SWORD)), 1, tx);
            tx.commit();
        }
        if (inserted != 0) {
            helper.fail("Non-book insert should return 0, got " + inserted);
            return;
        }

        BasicLibraryBlockEntity lib = (BasicLibraryBlockEntity) helper.getLevel()
                .getBlockEntity(absPos);
        if (lib != null && !lib.getPoints().isEmpty()) {
            helper.fail("Library should have no points after rejected insert");
            return;
        }
        helper.succeed();
    }

    @SuppressWarnings("UnstableApiUsage")
    @GameTest(template = "meridian:empty_3x3")
    public void transferApiAbortRollsBackState(GameTestHelper helper) {
        helper.setBlock(LIB_POS, MeridianRegistry.BASIC_LIBRARY.defaultBlockState());
        BlockPos absPos = helper.absolutePos(LIB_POS);

        Storage<ItemVariant> storage = ItemStorage.SIDED.find(
                helper.getLevel(), absPos, Direction.UP);
        if (storage == null) {
            helper.fail("Storage not found");
            return;
        }

        Registry<Enchantment> reg = helper.getLevel().registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT);
        ItemStack book = enchantedBook(reg, Enchantments.SHARPNESS, 3);

        try (Transaction tx = Transaction.openOuter()) {
            storage.insert(ItemVariant.of(book), 1, tx);
            // abort — do not commit
        }

        BasicLibraryBlockEntity lib = (BasicLibraryBlockEntity) helper.getLevel()
                .getBlockEntity(absPos);
        if (lib != null && !lib.getPoints().isEmpty()) {
            helper.fail("Library should have no points after aborted transaction");
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
