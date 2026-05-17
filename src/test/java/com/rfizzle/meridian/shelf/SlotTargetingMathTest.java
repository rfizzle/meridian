// Tier: 1 (pure — ShelfSlotMapping uses only java.util.OptionalInt, no MC imports)
package com.rfizzle.meridian.shelf;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SlotTargetingMathTest {

    @ParameterizedTest(name = "h={0}, dy={1} -> slot {2}")
    @CsvSource({
            // Top row (dy >= 0.5): slots 0, 1, 2
            "0.21,  0.75, 0",
            "0.53,  0.75, 1",
            "0.81,  0.75, 2",
            // Bottom row (dy < 0.5): slots 3, 4, 5
            "0.21,  0.25, 3",
            "0.53,  0.25, 4",
            "0.81,  0.25, 5",
    })
    void cellCenter_mapsToExpectedSlot(float h, double dy, int expectedSlot) {
        OptionalInt result = ShelfSlotMapping.computeSlot(h, dy);
        assertTrue(result.isPresent(), "expected slot " + expectedSlot + " but got empty");
        assertEquals(expectedSlot, result.getAsInt());
    }

    @ParameterizedTest(name = "column boundary h={0} -> column {1}")
    @CsvSource({
            // Column 0: [SECTION_MARGIN, COLUMN_LOWER) = [0.0625, 0.375)
            "0.0625, 0",
            "0.374,  0",
            // Column 1: [COLUMN_LOWER, COLUMN_UPPER) = [0.375, 0.6875)
            "0.375,  1",
            "0.687,  1",
            // Column 2: [COLUMN_UPPER, COLUMN_RIGHT_MAX) = [0.6875, 0.9375)
            "0.6875, 2",
            "0.937,  2",
    })
    void columnBoundaries_mapCorrectly(float h, int expectedColumn) {
        assertEquals(expectedColumn, ShelfSlotMapping.columnSection(h));
    }

    @Test
    void leftMargin_returnsEmpty() {
        assertTrue(ShelfSlotMapping.computeSlot(0.03F, 0.5).isEmpty());
    }

    @Test
    void rightMargin_returnsEmpty() {
        assertTrue(ShelfSlotMapping.computeSlot(0.95F, 0.5).isEmpty());
    }

    @Test
    void topMargin_returnsEmpty() {
        assertTrue(ShelfSlotMapping.computeSlot(0.5F, 0.97).isEmpty());
    }

    @Test
    void bottomMargin_returnsEmpty() {
        assertTrue(ShelfSlotMapping.computeSlot(0.5F, 0.03).isEmpty());
    }

    @Test
    void verticalBoundary_atHalf_isTopRow() {
        OptionalInt result = ShelfSlotMapping.computeSlot(0.21F, 0.5);
        assertTrue(result.isPresent());
        assertEquals(0, result.getAsInt(), "dy=0.5 should be top row (slot 0)");
    }

    @Test
    void verticalBoundary_justBelowHalf_isBottomRow() {
        OptionalInt result = ShelfSlotMapping.computeSlot(0.21F, 0.499);
        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt(), "dy=0.499 should be bottom row (slot 3)");
    }

    @Test
    void columnSection_belowMargin_returnsNegative() {
        assertEquals(-1, ShelfSlotMapping.columnSection(0.0F));
        assertEquals(-1, ShelfSlotMapping.columnSection(0.06F));
    }

    @Test
    void columnSection_aboveRightMax_returnsNegative() {
        assertEquals(-1, ShelfSlotMapping.columnSection(0.9375F));
        assertEquals(-1, ShelfSlotMapping.columnSection(1.0F));
    }
}
