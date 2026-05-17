package com.rfizzle.meridian.shelf;

import java.util.OptionalInt;

/**
 * Pure coordinate math for mapping hit positions on the front face of a chiseled-bookshelf-style
 * block to one of six slot indices. Top row reads left-to-right as 0/1/2, bottom row as 3/4/5.
 * Extracted from {@link FilteringShelfBlock#computeHitSlot} so the math is testable without
 * loading any MC block classes.
 */
final class ShelfSlotMapping {

    private ShelfSlotMapping() {}

    static final float SECTION_MARGIN = 0.0625F;
    static final float COLUMN_LOWER = 0.375F;
    static final float COLUMN_UPPER = 0.6875F;
    static final float COLUMN_RIGHT_MAX = 0.9375F;

    static OptionalInt computeSlot(float horizontal, double dy) {
        int column = columnSection(horizontal);
        if (column < 0) {
            return OptionalInt.empty();
        }
        if (dy < SECTION_MARGIN || dy > 1.0D - SECTION_MARGIN) {
            return OptionalInt.empty();
        }
        int row = dy >= 0.5D ? 0 : 1;
        return OptionalInt.of(row * 3 + column);
    }

    static int columnSection(float h) {
        if (h < SECTION_MARGIN) return -1;
        if (h < COLUMN_LOWER) return 0;
        if (h < COLUMN_UPPER) return 1;
        if (h < COLUMN_RIGHT_MAX) return 2;
        return -1;
    }
}
