package com.rfizzle.meridian.enchanting;

/**
 * Pure balance math for the fishing-rod enchantments. Kept free of Minecraft and Fabric
 * imports so plain JUnit tests can exercise the formulas; the {@code FishingHookLootMixin}
 * shell rolls the RNG and calls {@link TwinHookHandler} to spawn the extra catch.
 */
public final class FishingEnchantMath {

    /**
     * Chance, per Twin Hook level, that a catch reels in one extra copy. Level I is
     * {@value}, level II twice that — a Rare-tier yield bonus, well short of a guarantee.
     */
    public static final double TWIN_HOOK_CHANCE_PER_LEVEL = 0.20;

    private FishingEnchantMath() {}

    /** Probability in {@code [0,1]} that a Twin Hook catch at {@code level} duplicates. Level {@code <= 0} is zero. */
    public static double twinHookChance(int level) {
        if (level <= 0) return 0.0;
        return Math.min(1.0, TWIN_HOOK_CHANCE_PER_LEVEL * level);
    }

    /**
     * True when a catch at {@code level} should reel in a second copy, given a uniform
     * {@code roll} in {@code [0,1)}. Pure so the roll (from the game's RNG) is supplied
     * by the caller and the decision unit-tests without a {@code RandomSource}.
     */
    public static boolean shouldDuplicate(int level, double roll) {
        return roll < twinHookChance(level);
    }
}
