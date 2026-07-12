package com.rfizzle.meridian.enchanting;

/**
 * Pure balance math for the Ambush / Pinpoint / Sunder / Trophy / Fortuity / Crescendo /
 * Riposte / Joust combat enchantments. Kept free of Minecraft and Fabric imports so plain
 * JUnit tests can exercise the formulas; {@code EnchantmentEffectHandler} and the combat
 * mixins are the only runtime callers. The per-effect tuning constants live here (not in
 * the handler's balance block) for the same reason.
 */
public final class CombatEnchantMath {

    public static final float AMBUSH_DAMAGE_PER_LEVEL = 1.25f;

    public static final float REAP_DAMAGE_PER_LEVEL = 1.25f;

    public static final float PINPOINT_BASE_DAMAGE = 0.5f;
    public static final float PINPOINT_DAMAGE_PER_LEVEL = 1.0f;

    public static final float SUNDER_CHANCE_PER_LEVEL = 0.04f;

    public static final float TROPHY_CHANCE_PER_LEVEL = 0.05f;

    public static final float FORTUITY_LUCK_PER_LEVEL = 1.0f;

    public static final float CRESCENDO_DAMAGE_PER_STACK = 0.5f;
    public static final int CRESCENDO_TIMEOUT_TICKS = 60;

    public static final int RIPOSTE_WINDOW_TICKS = 40;
    public static final float RIPOSTE_DAMAGE_PER_LEVEL = 1.0f;

    /** Horizontal mount speed (blocks/tick) below which Joust treats the mount as stationary. */
    public static final double JOUST_MIN_SPEED = 0.1;
    public static final float JOUST_DAMAGE_PER_SPEED_PER_LEVEL = 3.0f;
    public static final float JOUST_DAMAGE_CAP_PER_LEVEL = 2.0f;

    /** Curse of Blunting: chance per level that a melee strike glances off. */
    public static final float BLUNTING_GLANCE_CHANCE_PER_LEVEL = 0.08f;
    /** Curse of Blunting: fraction of the swing's damage that lands on a glance. */
    public static final float BLUNTING_GLANCE_MULTIPLIER = 0.4f;

    /** Curse of Fumbling: flat chance on taking a hit to drop the held item. */
    public static final float FUMBLING_DROP_CHANCE = 0.15f;

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

    /**
     * Reap's finisher bonus: the mirror of {@link #ambushBonusDamage}. Zero against an
     * unharmed target, scaling up linearly with how far the target's pre-hit health has
     * fallen — full value as it approaches death. Shares Ambush's pre-hit
     * {@link #ambushHealthFraction} snapshot.
     */
    public static float reapBonusDamage(int level, float healthFraction) {
        if (level <= 0) return 0.0f;
        float weakened = 1.0f - Math.min(1.0f, Math.max(0.0f, healthFraction));
        if (weakened <= 0.0f) return 0.0f;
        return REAP_DAMAGE_PER_LEVEL * level * weakened;
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

    /** Stack ceiling for Crescendo's ramp — the per-level damage cap. */
    public static int crescendoMaxStacks(int level) {
        if (level <= 0) return 0;
        return level + 1;
    }

    /** Whether the gap since the streak's last hit is long enough to reset the ramp. */
    public static boolean crescendoStreakExpired(long lastHitTick, long now) {
        return now - lastHitTick > CRESCENDO_TIMEOUT_TICKS;
    }

    /**
     * Crescendo's ramp bonus for a hit carrying {@code stacks} consecutive follow-up hits
     * on the same target. The opening hit of a streak carries zero stacks and no bonus.
     */
    public static float crescendoBonusDamage(int level, int stacks) {
        if (level <= 0 || stacks <= 0) return 0.0f;
        return CRESCENDO_DAMAGE_PER_STACK * Math.min(stacks, crescendoMaxStacks(level));
    }

    /** Whether a Riposte window opened at {@code blockTick} is still live at {@code now}. */
    public static boolean riposteWindowOpen(long blockTick, long now) {
        long elapsed = now - blockTick;
        return elapsed >= 0 && elapsed <= RIPOSTE_WINDOW_TICKS;
    }

    /** Riposte's flat bonus on the first melee hit inside the post-block window. */
    public static float riposteBonusDamage(int level) {
        if (level <= 0) return 0.0f;
        return RIPOSTE_DAMAGE_PER_LEVEL * level;
    }

    /**
     * Joust's bonus for a melee hit landed from a mount moving at {@code mountSpeed}
     * blocks/tick horizontally: linear in speed and level, zero below the stationary
     * threshold, capped per level so charge exploits can't scale unbounded.
     */
    public static float joustBonusDamage(int level, double mountSpeed) {
        if (level <= 0 || mountSpeed < JOUST_MIN_SPEED) return 0.0f;
        float raw = (float) (JOUST_DAMAGE_PER_SPEED_PER_LEVEL * level * mountSpeed);
        return Math.min(JOUST_DAMAGE_CAP_PER_LEVEL * level, raw);
    }

    /** Chance per swing that Curse of Blunting makes a strike glance off, clamped to [0, 1]. */
    public static float bluntingGlanceChance(int level) {
        if (level <= 0) return 0.0f;
        return Math.min(1.0f, BLUNTING_GLANCE_CHANCE_PER_LEVEL * level);
    }

    /**
     * Damage multiplier Curse of Blunting applies to a strike: {@link #BLUNTING_GLANCE_MULTIPLIER}
     * when {@code roll} lands inside the level's glance chance, otherwise 1.0 (unchanged). Taking
     * the roll as a parameter keeps the decision deterministic and unit-testable; the caller feeds
     * {@code random.nextFloat()}.
     */
    public static float bluntingDamageMultiplier(int level, float roll) {
        if (level <= 0) return 1.0f;
        return roll < bluntingGlanceChance(level) ? BLUNTING_GLANCE_MULTIPLIER : 1.0f;
    }
}
