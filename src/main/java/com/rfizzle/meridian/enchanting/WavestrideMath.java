package com.rfizzle.meridian.enchanting;

/**
 * Pure movement gate for the Wavestride mount enchantment. Kept free of Minecraft and Fabric
 * imports so plain JUnit tests can exercise the threshold; {@code WavestrideMixin} is the only
 * runtime caller.
 *
 * <p>Wavestride lets a moving mount stand on the water surface (via {@code canStandOnFluid}); a
 * stationary mount sinks and swims as normal. "Moving" is decided purely from the mount's
 * horizontal speed this tick, so the surface becomes solid only while the mount is actually
 * galloping and dissolves the instant it stops.
 */
public final class WavestrideMath {

    private WavestrideMath() {}

    /**
     * Minimum horizontal speed (blocks/tick) at which a Wavestride mount strides on water. Set
     * just above the idle drift a floating mount accumulates in still water, so deliberate
     * movement strides while a parked mount sinks.
     */
    public static final double STRIDE_SPEED_THRESHOLD = 0.01;

    /** Whether a mount moving at {@code horizontalSpeed} (blocks/tick) is striding rather than idle. */
    public static boolean strides(double horizontalSpeed) {
        return horizontalSpeed >= STRIDE_SPEED_THRESHOLD;
    }
}
