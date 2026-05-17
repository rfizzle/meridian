package com.rfizzle.meridian.library;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Tier: 2
class LibraryTierBlockEntityTest {

    private static BlockState bookshelf;

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        bookshelf = Blocks.CHEST.defaultBlockState();
    }

    @Test
    void basicConstants_matchDesign() {
        assertEquals(16, BasicLibraryBlockEntity.MAX_LEVEL,
                "Basic tier cap of 16 is baked into shipped NBT — must not drift");
        assertEquals(32_768, BasicLibraryBlockEntity.MAX_POINTS,
                "MAX_POINTS = 2^(MAX_LEVEL - 1) per the points formula");
    }

    @Test
    void enderConstants_matchDesign() {
        assertEquals(31, EnderLibraryBlockEntity.MAX_LEVEL,
                "Ender tier cap of 31 — the largest level whose points fit in a signed int");
        assertEquals(1_073_741_824, EnderLibraryBlockEntity.MAX_POINTS,
                "MAX_POINTS = 2^30 — clamping must not silently truncate to int overflow");
    }

    @Test
    void basicConstruction_exposesExpectedTier() {
        BasicLibraryBlockEntity be = new BasicLibraryBlockEntity(BlockEntityType.CHEST, BlockPos.ZERO, bookshelf);
        assertEquals(BasicLibraryBlockEntity.MAX_LEVEL, be.getMaxLevel(),
                "subclass must propagate its MAX_LEVEL up to the parent");
        assertEquals(BasicLibraryBlockEntity.MAX_POINTS, be.getMaxPoints(),
                "parent must derive maxPoints from the passed-in maxLevel via points()");
        assertTrue(be.getPoints().isEmpty(), "fresh BE has empty point map");
        assertTrue(be.getMaxLevels().isEmpty(), "fresh BE has empty max-level map");
    }

    @Test
    void enderConstruction_exposesExpectedTier() {
        EnderLibraryBlockEntity be = new EnderLibraryBlockEntity(BlockEntityType.CHEST, BlockPos.ZERO, bookshelf);
        assertEquals(EnderLibraryBlockEntity.MAX_LEVEL, be.getMaxLevel());
        assertEquals(EnderLibraryBlockEntity.MAX_POINTS, be.getMaxPoints());
    }

    @Test
    void tiers_areDistinctTypes() {
        assertNotNull(BasicLibraryBlockEntity.class.getSuperclass());
        assertNotNull(EnderLibraryBlockEntity.class.getSuperclass());
        assertEquals(EnchantmentLibraryBlockEntity.class, BasicLibraryBlockEntity.class.getSuperclass());
        assertEquals(EnchantmentLibraryBlockEntity.class, EnderLibraryBlockEntity.class.getSuperclass());
    }
}
