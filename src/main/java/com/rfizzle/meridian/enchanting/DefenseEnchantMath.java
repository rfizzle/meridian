package com.rfizzle.meridian.enchanting;

/**
 * Pure balance math for the Blink / Inexorable / Emberward / Reprieve / Loft defense and
 * mobility enchantments. Kept free of Minecraft and Fabric imports so plain JUnit tests can
 * exercise the formulas; {@code EnchantmentEffectHandler}, {@code LoftHandler}, and the
 * Inexorable/Loft mixins are the only runtime callers. The per-effect tuning constants live
 * here (not in the handlers) for the same reason.
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

    private DefenseEnchantMath() {}

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
