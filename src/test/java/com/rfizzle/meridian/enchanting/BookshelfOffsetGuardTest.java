// Tier: 2 (fabric-loader-junit)
package com.rfizzle.meridian.enchanting;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.EnchantingTableBlock;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookshelfOffsetGuardTest {

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void bookshelfOffsets_containsExpectedEntries() {
        List<BlockPos> offsets = EnchantingTableBlock.BOOKSHELF_OFFSETS;
        assertEquals(32, offsets.size(),
                "vanilla 1.21.1 defines 32 shelf positions around the table (two rings at y=0 and y=1)");
    }

    @Test
    void bookshelfOffsets_noDuplicates() {
        List<BlockPos> offsets = EnchantingTableBlock.BOOKSHELF_OFFSETS;
        Set<BlockPos> unique = new HashSet<>(offsets);
        assertEquals(offsets.size(), unique.size(),
                "every offset position must be unique");
    }

    @Test
    void bookshelfOffsets_allWithinExpectedRange() {
        for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
            assertTrue(Math.abs(offset.getX()) <= 2,
                    "offset X must be in [-2, 2]; got " + offset);
            assertTrue(Math.abs(offset.getZ()) <= 2,
                    "offset Z must be in [-2, 2]; got " + offset);
            assertTrue(offset.getY() >= 0 && offset.getY() <= 1,
                    "offset Y must be 0 or 1; got " + offset);
        }
    }

    @Test
    void midpoint_matchesVanillaLosRule() {
        for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
            BlockPos mid = EnchantingStatRegistry.midpoint(offset);
            assertEquals(offset.getX() / 2, mid.getX(),
                    "midpoint X must be half of offset X for " + offset);
            assertEquals(offset.getY(), mid.getY(),
                    "midpoint Y must equal offset Y for " + offset);
            assertEquals(offset.getZ() / 2, mid.getZ(),
                    "midpoint Z must be half of offset Z for " + offset);
        }
    }

    @Test
    void everyOffset_hasMidpointCloserToTable() {
        BlockPos table = BlockPos.ZERO;
        for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
            BlockPos mid = EnchantingStatRegistry.midpoint(offset);
            double offsetDist = table.distSqr(offset);
            double midDist = table.distSqr(mid);
            assertTrue(midDist < offsetDist,
                    "midpoint " + mid + " must be closer to table than offset " + offset);
        }
    }
}
