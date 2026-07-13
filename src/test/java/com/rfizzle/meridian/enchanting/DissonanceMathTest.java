// Tier: 1 (pure JUnit)
package com.rfizzle.meridian.enchanting;

import com.rfizzle.meridian.api.StatCollection;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validates the Curse of Dissonance sabotage math. The curse drops Eterna and Clues on the
 * wearer's own table and leaves every other axis alone, floored so a strong reduction on a weak
 * table can never go negative.
 */
class DissonanceMathTest {

    private static final ResourceKey<Enchantment> SOME_ENCHANT =
            ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.withDefaultNamespace("sharpness"));

    private static StatCollection stats(float eterna, int clues) {
        // (eterna, quanta, arcana, rectification, clues, maxEterna, blacklist, treasureAllowed)
        return new StatCollection(eterna, 40F, 30F, 20F, clues, 50F, Set.of(SOME_ENCHANT), true);
    }

    @Test
    void unequippedLeavesStatsUntouched() {
        StatCollection base = stats(30F, 3);
        assertSame(base, DissonanceMath.apply(base, 0), "level 0 must return the same collection");
        assertSame(base, DissonanceMath.apply(base, -1), "a non-positive level must not reduce");
    }

    @Test
    void levelOneReducesEternaAndClues() {
        StatCollection reduced = DissonanceMath.apply(stats(30F, 3), 1);
        assertEquals(30F - DissonanceMath.ETERNA_REDUCTION_PER_LEVEL, reduced.eterna(), 1e-6,
                "eterna must drop by the per-level amount");
        assertEquals(3 - DissonanceMath.CLUES_REDUCTION_PER_LEVEL, reduced.clues(),
                "clues must drop by the per-level amount");
    }

    @Test
    void otherAxesAreUntouched() {
        StatCollection base = stats(30F, 3);
        StatCollection reduced = DissonanceMath.apply(base, 1);
        assertEquals(base.quanta(), reduced.quanta(), 1e-6, "quanta must not move");
        assertEquals(base.arcana(), reduced.arcana(), 1e-6, "arcana must not move");
        assertEquals(base.rectification(), reduced.rectification(), 1e-6, "rectification must not move");
        assertEquals(base.maxEterna(), reduced.maxEterna(), 1e-6, "maxEterna must pass through");
        assertEquals(base.blacklist(), reduced.blacklist(), "blacklist must pass through");
        assertEquals(base.treasureAllowed(), reduced.treasureAllowed(), "treasure flag must pass through");
    }

    @Test
    void reductionFloorsAtZero() {
        // Eterna 3 and clues 0 can't cover a full level-1 reduction — both must floor at 0.
        StatCollection reduced = DissonanceMath.apply(stats(3F, 0), 1);
        assertEquals(0F, reduced.eterna(), 1e-6, "eterna must floor at 0, never negative");
        assertEquals(0, reduced.clues(), "clues must floor at 0, never negative");
    }

    @Test
    void restoringMatchesTheOriginal() {
        // Applying then re-gathering (level 0) yields the untouched baseline — the "unequip restores"
        // property at the math layer.
        StatCollection base = stats(30F, 3);
        StatCollection restored = DissonanceMath.apply(base, 0);
        assertTrue(restored.eterna() == base.eterna() && restored.clues() == base.clues(),
                "removing the curse must restore the full stats");
    }
}
