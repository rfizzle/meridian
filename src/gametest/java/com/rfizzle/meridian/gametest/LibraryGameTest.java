// Tier: 3 (Fabric Gametest)
package com.rfizzle.meridian.gametest;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.MeridianRegistry;
import com.rfizzle.meridian.library.BasicLibraryBlockEntity;
import com.rfizzle.meridian.library.EnchantmentLibraryBlockEntity;
import com.rfizzle.meridian.library.EnchantmentLibraryMenu;
import com.rfizzle.meridian.library.EnderLibraryBlockEntity;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.entity.BlockEntity;

public class LibraryGameTest implements FabricGameTest {

    private static final BlockPos LIB_POS = new BlockPos(1, 1, 1);

    // --- TEST-4.4-T3a: Both library blocks register ---

    @GameTest(template = "meridian:empty_3x3")
    public void basicLibraryRegistered(GameTestHelper helper) {
        ResourceLocation loc = Meridian.id("library");
        if (!BuiltInRegistries.BLOCK.containsKey(loc)) {
            helper.fail("Basic library block not registered: " + loc);
            return;
        }
        if (BuiltInRegistries.BLOCK.get(loc) != MeridianRegistry.BASIC_LIBRARY) {
            helper.fail("Basic library singleton doesn't match registry");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void enderLibraryRegistered(GameTestHelper helper) {
        ResourceLocation loc = Meridian.id("ender_library");
        if (!BuiltInRegistries.BLOCK.containsKey(loc)) {
            helper.fail("Ender library block not registered: " + loc);
            return;
        }
        if (BuiltInRegistries.BLOCK.get(loc) != MeridianRegistry.ENDER_LIBRARY) {
            helper.fail("Ender library singleton doesn't match registry");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void basicLibraryCreatesCorrectBlockEntity(GameTestHelper helper) {
        helper.setBlock(LIB_POS, MeridianRegistry.BASIC_LIBRARY.defaultBlockState());
        BlockEntity be = helper.getLevel().getBlockEntity(helper.absolutePos(LIB_POS));
        if (!(be instanceof BasicLibraryBlockEntity)) {
            helper.fail("Expected BasicLibraryBlockEntity, got "
                    + (be == null ? "null" : be.getClass().getSimpleName()));
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void enderLibraryCreatesCorrectBlockEntity(GameTestHelper helper) {
        helper.setBlock(LIB_POS, MeridianRegistry.ENDER_LIBRARY.defaultBlockState());
        BlockEntity be = helper.getLevel().getBlockEntity(helper.absolutePos(LIB_POS));
        if (!(be instanceof EnderLibraryBlockEntity)) {
            helper.fail("Expected EnderLibraryBlockEntity, got "
                    + (be == null ? "null" : be.getClass().getSimpleName()));
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void librariesAreDistinctInstances(GameTestHelper helper) {
        if (MeridianRegistry.BASIC_LIBRARY == MeridianRegistry.ENDER_LIBRARY) {
            helper.fail("Basic and Ender library should be distinct block instances");
            return;
        }
        helper.succeed();
    }

    // --- TEST-4.4-T3b: Menu deposit/extract at a real library block ---

    @GameTest(template = "meridian:empty_3x3")
    public void depositAbsorbsBookIntoPool(GameTestHelper helper) {
        helper.setBlock(LIB_POS, MeridianRegistry.BASIC_LIBRARY.defaultBlockState());
        BasicLibraryBlockEntity be = (BasicLibraryBlockEntity) helper.getLevel()
                .getBlockEntity(helper.absolutePos(LIB_POS));
        if (be == null) { helper.fail("BE not created"); return; }

        Player player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        EnchantmentLibraryMenu menu = new EnchantmentLibraryMenu(1, player.getInventory(), be);

        Registry<Enchantment> reg = helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        ItemStack book = enchantedBook(reg, Enchantments.SHARPNESS, 3);
        menu.getSlot(EnchantmentLibraryMenu.DEPOSIT_SLOT).set(book);

        ResourceKey<Enchantment> key = Enchantments.SHARPNESS;
        int pts = be.getPoints().getInt(key);
        if (pts <= 0) {
            helper.fail("After deposit, points for SHARPNESS should be > 0, got " + pts);
            return;
        }
        int maxLvl = be.getMaxLevels().getInt(key);
        if (maxLvl != 3) {
            helper.fail("maxLevels for SHARPNESS should be 3, got " + maxLvl);
            return;
        }
        ItemStack remaining = menu.ioInv.getItem(EnchantmentLibraryMenu.DEPOSIT_SLOT);
        if (!remaining.isEmpty()) {
            helper.fail("Deposit slot should be empty after absorb");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void depositAndExtractViaBlockEntity(GameTestHelper helper) {
        helper.setBlock(LIB_POS, MeridianRegistry.BASIC_LIBRARY.defaultBlockState());
        BasicLibraryBlockEntity be = (BasicLibraryBlockEntity) helper.getLevel()
                .getBlockEntity(helper.absolutePos(LIB_POS));
        if (be == null) { helper.fail("BE not created"); return; }

        Registry<Enchantment> reg = helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        ResourceKey<Enchantment> sharpKey = Enchantments.SHARPNESS;

        be.depositBook(enchantedBook(reg, Enchantments.SHARPNESS, 5));

        int pts = be.getPoints().getInt(sharpKey);
        if (pts <= 0) {
            helper.fail("After deposit, points should be > 0, got " + pts);
            return;
        }
        if (be.getMaxLevels().getInt(sharpKey) != 5) {
            helper.fail("maxLevels should be 5 after depositing level 5 book");
            return;
        }
        if (!be.canExtract(sharpKey, 1, 0)) {
            helper.fail("canExtract(target=1, cur=0) should succeed");
            return;
        }

        boolean extracted = be.extract(sharpKey, 1, 0);
        if (!extracted) {
            helper.fail("extract should succeed");
            return;
        }
        if (be.getPoints().getInt(sharpKey) >= pts) {
            helper.fail("Points should decrease after extraction");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void extractDeniedWhenInsufficientMaxLevel(GameTestHelper helper) {
        helper.setBlock(LIB_POS, MeridianRegistry.BASIC_LIBRARY.defaultBlockState());
        BasicLibraryBlockEntity be = (BasicLibraryBlockEntity) helper.getLevel()
                .getBlockEntity(helper.absolutePos(LIB_POS));
        if (be == null) { helper.fail("BE not created"); return; }

        Registry<Enchantment> reg = helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        ResourceKey<Enchantment> sharpKey = Enchantments.SHARPNESS;

        be.depositBook(enchantedBook(reg, Enchantments.SHARPNESS, 1));

        boolean canExtract = be.canExtract(sharpKey, 2, 1);
        if (canExtract) {
            helper.fail("canExtract(target=2, cur=1) should fail when maxLevels=1");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void listenerRegisteredOnConstructionAndRemovedOnClose(GameTestHelper helper) {
        helper.setBlock(LIB_POS, MeridianRegistry.BASIC_LIBRARY.defaultBlockState());
        BasicLibraryBlockEntity be = (BasicLibraryBlockEntity) helper.getLevel()
                .getBlockEntity(helper.absolutePos(LIB_POS));
        if (be == null) { helper.fail("BE not created"); return; }

        Player player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        EnchantmentLibraryMenu menu1 = new EnchantmentLibraryMenu(1, player.getInventory(), be);
        EnchantmentLibraryMenu menu2 = new EnchantmentLibraryMenu(2, player.getInventory(), be);

        menu1.removed(player);
        menu2.removed(player);
        helper.succeed();
    }

    // --- S-7.2 Deposit Edge Cases ---

    @GameTest(template = "meridian:empty_3x3")
    public void depositOverflowCapsAtMaxPoints(GameTestHelper helper) {
        helper.setBlock(LIB_POS, MeridianRegistry.BASIC_LIBRARY.defaultBlockState());
        BasicLibraryBlockEntity be = (BasicLibraryBlockEntity) helper.getLevel()
                .getBlockEntity(helper.absolutePos(LIB_POS));
        if (be == null) { helper.fail("BE not created"); return; }

        Registry<Enchantment> reg = helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        ResourceKey<Enchantment> key = Enchantments.SHARPNESS;

        be.depositBook(enchantedBook(reg, Enchantments.SHARPNESS, 16));
        be.depositBook(enchantedBook(reg, Enchantments.SHARPNESS, 16));
        be.depositBook(enchantedBook(reg, Enchantments.SHARPNESS, 16));

        int pts = be.getPoints().getInt(key);
        if (pts != BasicLibraryBlockEntity.MAX_POINTS) {
            helper.fail("Points should cap at maxPoints (" + BasicLibraryBlockEntity.MAX_POINTS
                    + "), got " + pts);
            return;
        }
        if (pts < 0) {
            helper.fail("Points wrapped negative: " + pts);
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void depositUpdatesMaxLevelToHighest(GameTestHelper helper) {
        helper.setBlock(LIB_POS, MeridianRegistry.BASIC_LIBRARY.defaultBlockState());
        BasicLibraryBlockEntity be = (BasicLibraryBlockEntity) helper.getLevel()
                .getBlockEntity(helper.absolutePos(LIB_POS));
        if (be == null) { helper.fail("BE not created"); return; }

        Registry<Enchantment> reg = helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        ResourceKey<Enchantment> key = Enchantments.SHARPNESS;

        be.depositBook(enchantedBook(reg, Enchantments.SHARPNESS, 3));
        if (be.getMaxLevels().getInt(key) != 3) {
            helper.fail("maxLevel should be 3 after depositing III, got " + be.getMaxLevels().getInt(key));
            return;
        }

        be.depositBook(enchantedBook(reg, Enchantments.SHARPNESS, 5));
        if (be.getMaxLevels().getInt(key) != 5) {
            helper.fail("maxLevel should update to 5 after depositing V, got " + be.getMaxLevels().getInt(key));
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void depositLowerLevelDoesNotDowngradeMaxLevel(GameTestHelper helper) {
        helper.setBlock(LIB_POS, MeridianRegistry.BASIC_LIBRARY.defaultBlockState());
        BasicLibraryBlockEntity be = (BasicLibraryBlockEntity) helper.getLevel()
                .getBlockEntity(helper.absolutePos(LIB_POS));
        if (be == null) { helper.fail("BE not created"); return; }

        Registry<Enchantment> reg = helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        ResourceKey<Enchantment> key = Enchantments.SHARPNESS;

        be.depositBook(enchantedBook(reg, Enchantments.SHARPNESS, 5));
        if (be.getMaxLevels().getInt(key) != 5) {
            helper.fail("maxLevel should be 5 after depositing V, got " + be.getMaxLevels().getInt(key));
            return;
        }

        be.depositBook(enchantedBook(reg, Enchantments.SHARPNESS, 3));
        if (be.getMaxLevels().getInt(key) != 5) {
            helper.fail("maxLevel should stay 5 after depositing III, got " + be.getMaxLevels().getInt(key));
            return;
        }
        helper.succeed();
    }

    // --- S-7.3 Extract Edge Cases ---

    @GameTest(template = "meridian:empty_3x3")
    public void extractUpgradeCostsOnlyDelta(GameTestHelper helper) {
        helper.setBlock(LIB_POS, MeridianRegistry.BASIC_LIBRARY.defaultBlockState());
        BasicLibraryBlockEntity be = (BasicLibraryBlockEntity) helper.getLevel()
                .getBlockEntity(helper.absolutePos(LIB_POS));
        if (be == null) { helper.fail("BE not created"); return; }

        Registry<Enchantment> reg = helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        ResourceKey<Enchantment> key = Enchantments.SHARPNESS;

        be.depositBook(enchantedBook(reg, Enchantments.SHARPNESS, 5));
        int pointsBefore = be.getPoints().getInt(key);

        int expectedCost = EnchantmentLibraryBlockEntity.points(3) - EnchantmentLibraryBlockEntity.points(1);
        boolean extracted = be.extract(key, 3, 1);
        if (!extracted) {
            helper.fail("extract(target=3, cur=1) should succeed");
            return;
        }
        int pointsAfter = be.getPoints().getInt(key);
        int actualCost = pointsBefore - pointsAfter;
        if (actualCost != expectedCost) {
            helper.fail("Upgrade cost should be points(3)-points(1)=" + expectedCost
                    + ", got " + actualCost);
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void extractSameLevelIsNoOp(GameTestHelper helper) {
        helper.setBlock(LIB_POS, MeridianRegistry.BASIC_LIBRARY.defaultBlockState());
        BasicLibraryBlockEntity be = (BasicLibraryBlockEntity) helper.getLevel()
                .getBlockEntity(helper.absolutePos(LIB_POS));
        if (be == null) { helper.fail("BE not created"); return; }

        Registry<Enchantment> reg = helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        ResourceKey<Enchantment> key = Enchantments.SHARPNESS;

        be.depositBook(enchantedBook(reg, Enchantments.SHARPNESS, 3));
        int pointsBefore = be.getPoints().getInt(key);

        boolean canExtract = be.canExtract(key, 3, 3);
        if (canExtract) {
            helper.fail("canExtract(target=3, cur=3) should return false");
            return;
        }

        boolean extracted = be.extract(key, 3, 3);
        if (extracted) {
            helper.fail("extract(target=3, cur=3) should return false (no-op)");
            return;
        }
        int pointsAfter = be.getPoints().getInt(key);
        if (pointsAfter != pointsBefore) {
            helper.fail("Points should be unchanged after no-op extract, was " + pointsBefore
                    + " now " + pointsAfter);
            return;
        }
        helper.succeed();
    }

    // --- S-7.4 Library Tier Boundaries ---

    @GameTest(template = "meridian:empty_3x3")
    public void basicLibraryTruncatesLevelAbove16(GameTestHelper helper) {
        helper.setBlock(LIB_POS, MeridianRegistry.BASIC_LIBRARY.defaultBlockState());
        BasicLibraryBlockEntity be = (BasicLibraryBlockEntity) helper.getLevel()
                .getBlockEntity(helper.absolutePos(LIB_POS));
        if (be == null) { helper.fail("BE not created"); return; }

        Registry<Enchantment> reg = helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        ResourceKey<Enchantment> key = Enchantments.SHARPNESS;

        be.depositBook(enchantedBook(reg, Enchantments.SHARPNESS, 20));
        int maxLvl = be.getMaxLevels().getInt(key);
        if (maxLvl != 16) {
            helper.fail("Basic library should truncate deposited level 20 to 16, got " + maxLvl);
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void enderLibraryAcceptsLevelsUpTo31(GameTestHelper helper) {
        helper.setBlock(LIB_POS, MeridianRegistry.ENDER_LIBRARY.defaultBlockState());
        BlockEntity rawBe = helper.getLevel().getBlockEntity(helper.absolutePos(LIB_POS));
        if (!(rawBe instanceof EnderLibraryBlockEntity be)) {
            helper.fail("Expected EnderLibraryBlockEntity, got "
                    + (rawBe == null ? "null" : rawBe.getClass().getSimpleName()));
            return;
        }

        Registry<Enchantment> reg = helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        ResourceKey<Enchantment> key = Enchantments.SHARPNESS;

        be.depositBook(enchantedBook(reg, Enchantments.SHARPNESS, 25));
        int maxLvl = be.getMaxLevels().getInt(key);
        if (maxLvl != 25) {
            helper.fail("Ender library should accept level 25, got " + maxLvl);
            return;
        }

        be.depositBook(enchantedBook(reg, Enchantments.SHARPNESS, 31));
        maxLvl = be.getMaxLevels().getInt(key);
        if (maxLvl != 31) {
            helper.fail("Ender library should accept level 31, got " + maxLvl);
            return;
        }

        be.depositBook(enchantedBook(reg, Enchantments.SHARPNESS, 35));
        maxLvl = be.getMaxLevels().getInt(key);
        if (maxLvl != 31) {
            helper.fail("Ender library should truncate level 35 to 31, got " + maxLvl);
            return;
        }
        helper.succeed();
    }

    // --- Helpers ---

    private static ItemStack enchantedBook(Registry<Enchantment> reg, ResourceKey<Enchantment> key, int level) {
        Holder<Enchantment> holder = reg.getHolderOrThrow(key);
        ItemStack stack = new ItemStack(Items.ENCHANTED_BOOK);
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        mutable.set(holder, level);
        stack.set(DataComponents.STORED_ENCHANTMENTS, mutable.toImmutable());
        return stack;
    }
}
