package com.rfizzle.meridian.enchanting;

/**
 * Pure range math for Curse of Echoes — the inverse of Hush. Where Hush suppresses a wearer's
 * movement vibrations entirely, Echoes widens the distance at which any vibration listener (a
 * sculk sensor or the Warden) still accepts them, so the wearer is heard from farther.
 *
 * <p>Vanilla's {@code EuclideanGameEventListenerRegistry} accepts a listener when the squared
 * distance from the event to the listener is within the listener's own squared radius. Echoes
 * inflates that radius by a flat per-level bonus before the comparison. Kept Fabric-free so it
 * is unit-testable without a running game.
 */
public final class EchoesVibrationMath {

    /** Blocks added to a listener's effective detection radius per level of Curse of Echoes. */
    public static final int BONUS_RADIUS_PER_LEVEL = 8;

    private EchoesVibrationMath() {}

    /**
     * The squared detection radius a listener should use for a movement event whose source wears
     * Curse of Echoes at {@code level}. At {@code level <= 0} this is exactly the vanilla squared
     * radius; each level adds {@link #BONUS_RADIUS_PER_LEVEL} blocks before squaring.
     *
     * @param baseRadius the listener's native {@code getListenerRadius()} in blocks
     * @param level      the wearer's Curse of Echoes level (0 = not worn)
     * @return the squared radius to compare the squared event distance against
     */
    public static int effectiveRadiusSq(int baseRadius, int level) {
        int effective = baseRadius;
        if (level > 0) {
            effective += BONUS_RADIUS_PER_LEVEL * level;
        }
        return effective * effective;
    }
}
