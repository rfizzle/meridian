// Tier: 1 (pure JUnit)
package com.rfizzle.meridian.enchanting;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validates the Curse of Echoes range math — the inverse of Hush. Without the curse the effective
 * detection radius must be exactly vanilla's; with it, the radius grows by a flat per-level bonus
 * so sculk sensors and the Warden hear the wearer from farther.
 */
class EchoesVibrationMathTest {

    @Test
    void unwornLeavesVanillaRadius() {
        int base = 8; // sculk sensor native radius
        assertEquals(base * base, EchoesVibrationMath.effectiveRadiusSq(base, 0),
                "level 0 must equal the vanilla squared radius");
        assertEquals(base * base, EchoesVibrationMath.effectiveRadiusSq(base, -1),
                "a non-positive level must equal the vanilla squared radius");
    }

    @Test
    void oneLevelAddsTheBonusRadius() {
        int base = 8;
        int expected = base + EchoesVibrationMath.BONUS_RADIUS_PER_LEVEL;
        assertEquals(expected * expected, EchoesVibrationMath.effectiveRadiusSq(base, 1));
    }

    @Test
    void higherLevelWidensRadiusMonotonically() {
        int base = 16; // Warden native radius
        int r0 = EchoesVibrationMath.effectiveRadiusSq(base, 0);
        int r1 = EchoesVibrationMath.effectiveRadiusSq(base, 1);
        int r2 = EchoesVibrationMath.effectiveRadiusSq(base, 2);
        assertTrue(r1 > r0, "level 1 must widen past vanilla");
        assertTrue(r2 > r1, "each further level must widen further");
    }

    @Test
    void scalesPerLevelByTheBonusConstant() {
        int base = 8;
        int bonus = EchoesVibrationMath.BONUS_RADIUS_PER_LEVEL;
        assertEquals((base + 3 * bonus) * (base + 3 * bonus),
                EchoesVibrationMath.effectiveRadiusSq(base, 3));
    }
}
