package com.rfizzle.meridian.enchanting;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StatLineFormatterTest {

    @Test
    void format_emptyStats_rendersAllZeros() {
        assertEquals("E: 0  Q: 0  A: 0  R: 0  C: 0", StatLineFormatter.format(StatCollection.EMPTY));
    }

    @Test
    void format_nullStats_rendersAllZeros() {
        assertEquals("E: 0  Q: 0  A: 0  R: 0  C: 0", StatLineFormatter.format(null));
    }

    @Test
    void format_populatedStats_matchesDesignExample() {
        StatCollection stats = new StatCollection(
                50f, 12f, 5f, 10f, 2, 50f, Set.of(), false);
        assertEquals("E: 50  Q: 12  A: 5  R: 10  C: 2", StatLineFormatter.format(stats));
    }

    @Test
    void format_fractionalStats_flooredForDisplay() {
        // Zenith's HUD floors fractional contributions; mixed shelf placement commonly yields
        // non-integer values (e.g. stoneshelf's -1.5 eterna).
        StatCollection stats = new StatCollection(
                22.8f, 3.9f, 0f, 15f, 1, 22.5f, Set.of(), false);
        assertEquals("E: 22  Q: 3  A: 0  R: 15  C: 1", StatLineFormatter.format(stats));
    }

    @Test
    void format_negativeValues_preservedAsNegativeIntegers() {
        // Negative eterna happens with a stoneshelf out-weighing positive shelves; rendering
        // must not swallow the sign.
        StatCollection stats = new StatCollection(
                -1.5f, -2f, 0f, 0f, 0, 0f, Set.of(), false);
        assertEquals("E: -1  Q: -2  A: 0  R: 0  C: 0", StatLineFormatter.format(stats));
    }
}
