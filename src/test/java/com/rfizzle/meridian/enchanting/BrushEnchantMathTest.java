package com.rfizzle.meridian.enchanting;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Tier: 1 (pure JUnit)
class BrushEnchantMathTest {

    private static final float EPS = 1e-6f;

    // --- brushCompletionCount: speed ---

    @Test
    void completionCount_levelZero_isVanillaBase() {
        assertEquals(BrushEnchantMath.BRUSH_COMPLETION_BASE, BrushEnchantMath.brushCompletionCount(0));
    }

    @Test
    void completionCount_negativeLevel_isVanillaBase() {
        assertEquals(BrushEnchantMath.BRUSH_COMPLETION_BASE, BrushEnchantMath.brushCompletionCount(-3));
    }

    @Test
    void completionCount_levelOne_isBaseMinusOneReduction() {
        assertEquals(10 - 3, BrushEnchantMath.brushCompletionCount(1));
    }

    @Test
    void completionCount_levelTwo_isBaseMinusTwoReductions() {
        assertEquals(10 - 6, BrushEnchantMath.brushCompletionCount(2));
    }

    @Test
    void completionCount_strictlyDecreasesWithLevel() {
        assertTrue(BrushEnchantMath.brushCompletionCount(2) < BrushEnchantMath.brushCompletionCount(1),
                "level II must excavate in fewer strokes than level I");
        assertTrue(BrushEnchantMath.brushCompletionCount(1) < BrushEnchantMath.brushCompletionCount(0),
                "level I must excavate in fewer strokes than unenchanted");
    }

    @Test
    void completionCount_neverBelowFloor() {
        // A high level must clamp at the floor, never zero or negative (which would soft-lock brushing).
        assertEquals(BrushEnchantMath.BRUSH_MIN_COMPLETION, BrushEnchantMath.brushCompletionCount(100));
        assertTrue(BrushEnchantMath.brushCompletionCount(100) >= BrushEnchantMath.BRUSH_MIN_COMPLETION);
    }

    // --- meticulousLuckBonus: loot bias ---

    @Test
    void luckBonus_levelZero_isZero() {
        assertEquals(0.0f, BrushEnchantMath.meticulousLuckBonus(0), EPS);
    }

    @Test
    void luckBonus_negativeLevel_isZero() {
        assertEquals(0.0f, BrushEnchantMath.meticulousLuckBonus(-1), EPS);
    }

    @Test
    void luckBonus_scalesPerLevel() {
        assertEquals(BrushEnchantMath.METICULOUS_LUCK_PER_LEVEL, BrushEnchantMath.meticulousLuckBonus(1), EPS);
        assertEquals(2 * BrushEnchantMath.METICULOUS_LUCK_PER_LEVEL, BrushEnchantMath.meticulousLuckBonus(2), EPS);
    }
}
