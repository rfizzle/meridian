package com.rfizzle.meridian.enchanting;

/**
 * Pure balance math for the Blink / Inexorable / Emberward / Reprieve / Loft / Bastion /
 * Decoy / Everbloom defense and mobility enchantments. Kept free of Minecraft and Fabric
 * imports so plain JUnit tests can exercise the formulas; {@code EnchantmentEffectHandler},
 * {@code LoftHandler}, {@code DecoyManager}, and the Antidote/Everbloom/Inexorable/Loft mixins
 * are the only runtime callers. The per-effect tuning constants live here (not in the handlers)
 * for the same reason.
 */
public final class DefenseEnchantMath {

    /** One Minecraft game day in ticks. */
    public static final int GAME_DAY_TICKS = 24000;

    /** How many game days Blink stays locked out after firing. */
    public static final int BLINK_COOLDOWN_GAME_DAYS = 1;

    public static final long BLINK_COOLDOWN_TICKS = (long) GAME_DAY_TICKS * BLINK_COOLDOWN_GAME_DAYS;

    /**
     * Sentinel for "Blink has never fired" in the persisted last-used attachment. Game time
     * is non-negative, so no real timestamp collides with it.
     */
    public static final long BLINK_NEVER_USED = Long.MIN_VALUE;

    /** Health the wearer is left with after Blink cancels a killing blow (one heart). */
    public static final float BLINK_SURVIVAL_HEALTH = 2.0f;

    public static final int BLINK_WEAKNESS_TICKS = 200;

    /** How far Blink scatters the wearer, mirroring the chorus-fruit teleport envelope. */
    public static final double BLINK_TELEPORT_RANGE = 16.0;
    public static final int BLINK_TELEPORT_ATTEMPTS = 16;

    public static final int EMBERWARD_FIRE_RES_TICKS = 100;

    /** Vanilla's post-hit invulnerability window, set by {@code LivingEntity#hurt}. */
    public static final int VANILLA_HURT_INVULNERABILITY_TICKS = 20;

    /**
     * Reprieve's window extension per level. I-frames are potent — two levels stretch the
     * window by 40%, deliberately short of doubling it.
     */
    public static final int REPRIEVE_BONUS_TICKS_PER_LEVEL = 4;

    /** Blocks of fall distance Loft forgives per level — a raised safe fall height. */
    public static final float LOFT_SAFE_FALL_PER_LEVEL = 1.5f;

    /** Upward velocity of Loft's mid-air jump, a touch over the vanilla ground jump (~0.42). */
    public static final double LOFT_JUMP_VELOCITY = 0.55;

    /**
     * Minimum ticks between two air jumps. Defense in depth against a client rapid-firing
     * jump requests around the re-arm check — a legitimate jump-land-jump cycle is never
     * this fast.
     */
    public static final int LOFT_AIR_JUMP_MIN_INTERVAL_TICKS = 10;

    /** Fraction of max health at or below which Decoy deploys — half health. */
    public static final float DECOY_HEALTH_THRESHOLD = 0.5f;

    /** How long Decoy stays locked out after deploying a decoy. Five minutes. */
    public static final int DECOY_COOLDOWN_TICKS = 6000;

    /** How long a deployed decoy lives before it despawns on its own. Eight seconds. */
    public static final int DECOY_LIFETIME_TICKS = 160;

    /** Radius within which a live decoy taunts hostile mobs onto itself. */
    public static final double DECOY_TAUNT_RADIUS = 12.0;

    /** Resistance pulsed to allies on a Bastion block — level 0 (Resistance I). */
    public static final int BASTION_RESIST_AMPLIFIER = 0;

    /** Blocks around the blocker that a Bastion pulse reaches. */
    public static final double BASTION_ALLY_RADIUS = 8.0;

    public static final int BASTION_BASE_RESIST_TICKS = 40;
    public static final int BASTION_RESIST_TICKS_PER_LEVEL = 80;

    /**
     * Stagger's daze duration per level. A blocked melee hit briefly saps the attacker; the
     * window stays short (three to four seconds) so it punishes the swing without stun-locking.
     */
    public static final int STAGGER_BASE_DAZE_TICKS = 40;
    public static final int STAGGER_DAZE_TICKS_PER_LEVEL = 20;

    /**
     * Weakness stays at level I at every Stagger level — only Slowness deepens with level, so the
     * daze never fully neuters an attacker's damage output.
     */
    public static final int STAGGER_WEAKNESS_AMPLIFIER = 0;

    /**
     * Everbloom's beneficial-duration bonus per level and its cap. Deliberately held well below
     * doubling (a +100% cap) so it lengthens buffs without trivializing potion economy — at
     * max level three it lands at +45%, short of the +50% ceiling.
     */
    public static final float EVERBLOOM_DURATION_BONUS_PER_LEVEL = 0.15f;
    public static final float EVERBLOOM_MAX_DURATION_BONUS = 0.50f;

    private DefenseEnchantMath() {}

    /**
     * Whether a hit that took the wearer from {@code preHealth} to {@code postHealth} crossed
     * Decoy's half-health line. Only the downward crossing counts — a hit that starts already
     * below the line (chip damage while low) does not re-arm the decoy.
     */
    public static boolean decoyThresholdCrossed(float preHealth, float postHealth, float maxHealth) {
        float threshold = maxHealth * DECOY_HEALTH_THRESHOLD;
        return preHealth > threshold && postHealth <= threshold;
    }

    /** Resistance duration in ticks for a Bastion block at the given level. Zero at level 0. */
    public static int bastionResistanceTicks(int level) {
        if (level <= 0) return 0;
        return BASTION_BASE_RESIST_TICKS + BASTION_RESIST_TICKS_PER_LEVEL * level;
    }

    /** Duration in ticks of the Slowness/Weakness daze a Stagger block inflicts. Zero at level 0. */
    public static int staggerDazeTicks(int level) {
        if (level <= 0) return 0;
        return STAGGER_BASE_DAZE_TICKS + STAGGER_DAZE_TICKS_PER_LEVEL * level;
    }

    /**
     * Slowness amplifier for a Stagger daze — Slowness I at level 1, deepening one tier per level
     * ({@code level - 1}). Zero at level 0.
     */
    public static int staggerSlownessAmplifier(int level) {
        if (level <= 0) return 0;
        return level - 1;
    }

    /**
     * The lengthened duration Everbloom grants a beneficial effect. Level 0 and infinite
     * durations ({@code < 0}) are returned unchanged; otherwise the base is scaled by
     * {@code 1 + min(cap, perLevel * level)} and rounded up.
     */
    public static int everbloomExtendedDuration(int baseDuration, int level) {
        if (level <= 0 || baseDuration < 0) return baseDuration;
        float bonus = Math.min(EVERBLOOM_MAX_DURATION_BONUS, EVERBLOOM_DURATION_BONUS_PER_LEVEL * level);
        return (int) Math.ceil(baseDuration * (1.0f + bonus));
    }

    /**
     * Whether Blink may fire given when it last fired ({@link #BLINK_NEVER_USED} if never)
     * and the current game time. Game time is monotonic within a world (unaffected by
     * {@code /time set}), but the timestamp persists with the player — restoring a backup
     * or carrying playerdata into a fresh world can put it in this world's future. A
     * future timestamp reads as off-cooldown rather than locking Blink out for months.
     */
    public static boolean blinkOffCooldown(long lastUsedGameTime, long currentGameTime) {
        if (lastUsedGameTime == BLINK_NEVER_USED || lastUsedGameTime > currentGameTime) return true;
        return currentGameTime - lastUsedGameTime >= BLINK_COOLDOWN_TICKS;
    }

    /**
     * The post-hit invulnerability window for a victim with the given Reprieve level.
     * Level 0 is vanilla's {@link #VANILLA_HURT_INVULNERABILITY_TICKS}.
     */
    public static int reprieveInvulnerabilityTicks(int level) {
        if (level <= 0) return VANILLA_HURT_INVULNERABILITY_TICKS;
        return VANILLA_HURT_INVULNERABILITY_TICKS + REPRIEVE_BONUS_TICKS_PER_LEVEL * level;
    }

    /**
     * Blocks subtracted from the effective fall distance before fall damage is computed.
     * Zero at level 0; never lifts the distance below zero at the call site.
     */
    public static float loftSafeFallReduction(int level) {
        if (level <= 0) return 0.0f;
        return LOFT_SAFE_FALL_PER_LEVEL * level;
    }
}
