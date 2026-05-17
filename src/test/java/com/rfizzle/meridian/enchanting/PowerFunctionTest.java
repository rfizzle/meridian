package com.rfizzle.meridian.enchanting;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PowerFunctionTest {

    @Test
    void linearPowerFunction_computesBaseAndSlope() {
        var linear = new PowerFunction.LinearPowerFunction(1, 11);
        assertEquals(12, linear.getPower(1));
        assertEquals(23, linear.getPower(2));
        assertEquals(56, linear.getPower(5));
    }

    @Test
    void linearPowerFunction_zeroPerLevel_returnsBase() {
        var linear = new PowerFunction.LinearPowerFunction(50, 0);
        assertEquals(50, linear.getPower(1));
        assertEquals(50, linear.getPower(10));
    }

    @Test
    void linearPowerFunction_negativeBase() {
        var linear = new PowerFunction.LinearPowerFunction(-10, 20);
        assertEquals(10, linear.getPower(1));
        assertEquals(30, linear.getPower(2));
    }

    @Test
    void fixedPowerFunction_alwaysReturnsSameValue() {
        var fixed = new PowerFunction.FixedPowerFunction(150);
        assertEquals(150, fixed.getPower(1));
        assertEquals(150, fixed.getPower(5));
        assertEquals(150, fixed.getPower(100));
    }

    @Test
    void fixedPowerFunction_zero() {
        var fixed = new PowerFunction.FixedPowerFunction(0);
        assertEquals(0, fixed.getPower(1));
    }

    @Test
    void defaultMaxPowerFunction_alwaysReturns200() {
        assertEquals(200, PowerFunction.DefaultMaxPowerFunction.INSTANCE.getPower(1));
        assertEquals(200, PowerFunction.DefaultMaxPowerFunction.INSTANCE.getPower(100));
    }

    @Test
    void typeEnumOrder_matchesCodecOrdinals() {
        assertEquals(0, PowerFunction.Type.DEFAULT_MIN.ordinal());
        assertEquals(1, PowerFunction.Type.DEFAULT_MAX.ordinal());
        assertEquals(2, PowerFunction.Type.LINEAR.ordinal());
        assertEquals(3, PowerFunction.Type.FIXED.ordinal());
    }
}
