package com.rfizzle.meridian.enchanting;

/**
 * Pure XP-tax math for Curse of Toll — the inverse of Insight/Animus. Where those raise the
 * experience a kill or a broken block yields, Toll skims a flat fraction off whatever experience
 * the wearer actually collects from an orb, so enchanting progress itself is what the curse costs.
 *
 * <p>The reduction applies to the value vanilla hands to {@code Player#giveExperiencePoints} after
 * Mending has already claimed its share of the orb, so it taxes the experience the player keeps,
 * never the durability repair. Kept Fabric-free so it is unit-testable without a running game.
 */
public final class TollExperienceMath {

    /** Fraction of collected experience removed per level of Curse of Toll (0.15 = 15% per level). */
    public static final float REDUCTION_PER_LEVEL = 0.15f;

    private TollExperienceMath() {}

    /**
     * The experience a wearer keeps from an orb worth {@code rawXp} while carrying Curse of Toll at
     * {@code level}. At {@code level <= 0} this returns {@code rawXp} unchanged; each level removes
     * {@link #REDUCTION_PER_LEVEL} of the raw value, and the result never drops below 0.
     *
     * @param rawXp the experience vanilla would otherwise award (post-Mending remainder)
     * @param level the wearer's Curse of Toll level (0 = not worn)
     * @return the reduced experience to award, floored at 0
     */
    public static int reduce(int rawXp, int level) {
        if (level <= 0 || rawXp <= 0) return rawXp;
        float kept = 1.0f - REDUCTION_PER_LEVEL * level;
        if (kept <= 0f) return 0;
        return Math.max(0, Math.round(rawXp * kept));
    }
}
