package com.rfizzle.meridian.enchanting;

/**
 * Pure balance math for the Ambush / Pinpoint / Sunder / Trophy / Fortuity combat
 * enchantments. Kept free of Minecraft and Fabric imports so plain JUnit tests can
 * exercise the formulas; {@code EnchantmentEffectHandler} and the combat mixins are
 * the only runtime callers. The per-effect tuning constants live here (not in the
 * handler's balance block) for the same reason.
 */
public final class CombatEnchantMath {

    public static final float AMBUSH_DAMAGE_PER_LEVEL = 1.25f;

    public static final float PINPOINT_BASE_DAMAGE = 0.5f;
    public static final float PINPOINT_DAMAGE_PER_LEVEL = 1.0f;

    public static final float SUNDER_CHANCE_PER_LEVEL = 0.04f;

    public static final float TROPHY_CHANCE_PER_LEVEL = 0.05f;

    public static final float FORTUITY_LUCK_PER_LEVEL = 1.0f;

    private CombatEnchantMath() {}

    /**
     * Fraction of max health the victim had <em>before</em> the triggering hit landed,
     * clamped to [0, 1]. The handler snapshots the pre-hit value in {@code ALLOW_DAMAGE};
     * absorption overflow can push the raw value above max.
     */
    public static float ambushHealthFraction(float preHitHealth, float maxHealth) {
        if (maxHealth <= 0.0f) return 0.0f;
        return Math.min(1.0f, Math.max(0.0f, preHitHealth) / maxHealth);
    }

    /**
     * Ambush's opener bonus: full value against an unharmed target, scaling down
     * linearly with the target's pre-hit health fraction.
     */
    public static float ambushBonusDamage(int level, float healthFraction) {
        if (level <= 0 || healthFraction <= 0.0f) return 0.0f;
        return AMBUSH_DAMAGE_PER_LEVEL * level * Math.min(1.0f, healthFraction);
    }

    /** Pinpoint's flat bonus, applied on top of the vanilla critical-hit multiplier. */
    public static float pinpointBonusDamage(int level) {
        if (level <= 0) return 0.0f;
        return PINPOINT_BASE_DAMAGE + PINPOINT_DAMAGE_PER_LEVEL * level;
    }

    /** Chance per hit that Sunder knocks a piece of the victim's equipment loose. */
    public static float sunderChance(int level) {
        if (level <= 0) return 0.0f;
        return Math.min(1.0f, SUNDER_CHANCE_PER_LEVEL * level);
    }

    /** Chance per kill that Trophy drops the victim's head. */
    public static float trophyChance(int level) {
        if (level <= 0) return 0.0f;
        return Math.min(1.0f, TROPHY_CHANCE_PER_LEVEL * level);
    }

    /** Extra luck fed into the victim's death-loot rolls by Fortuity. */
    public static float fortuityLuckBonus(int level) {
        if (level <= 0) return 0.0f;
        return FORTUITY_LUCK_PER_LEVEL * level;
    }
}
