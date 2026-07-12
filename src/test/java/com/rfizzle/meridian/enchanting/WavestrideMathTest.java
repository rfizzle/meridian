// Tier: 1 (pure JUnit)
package com.rfizzle.meridian.enchanting;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WavestrideMathTest {

    @Test
    void strides_isFalseWhenStationary() {
        assertFalse(WavestrideMath.strides(0.0), "a parked mount must not stride");
    }

    @Test
    void strides_thresholdIsInclusive() {
        assertTrue(WavestrideMath.strides(WavestrideMath.STRIDE_SPEED_THRESHOLD),
                "movement exactly at the threshold strides");
    }

    @Test
    void strides_isFalseJustBelowThreshold() {
        assertFalse(WavestrideMath.strides(WavestrideMath.STRIDE_SPEED_THRESHOLD - 1.0e-6),
                "idle drift below the threshold must not stride");
    }

    @Test
    void strides_isTrueWhenGalloping() {
        assertTrue(WavestrideMath.strides(0.4), "a galloping mount strides");
    }
}
