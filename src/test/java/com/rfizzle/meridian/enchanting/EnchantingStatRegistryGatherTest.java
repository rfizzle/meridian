package com.rfizzle.meridian.enchanting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.EnchantingTableBlock;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnchantingStatRegistryGatherTest {

    private static List<BlockPos> offsets(int count) {
        List<BlockPos> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            list.add(new BlockPos(i, 0, 0));
        }
        return list;
    }

    @Test
    void gather_fifteenUnitContributors_sumsToFifteenEterna() {
        EnchantingStatRegistry reg = new EnchantingStatRegistry();
        // Vanilla-shelf tuple (maxEterna=15, eterna=1): 15 shelves sum to exactly the cap.
        EnchantingStats vanilla = new EnchantingStats(15F, 1F, 0F, 0F, 0F, 0);

        StatCollection result = reg.gatherStatsFromOffsets(offsets(15), pos -> vanilla);

        assertEquals(15F, result.eterna());
        assertEquals(0F, result.quanta());
        assertEquals(0F, result.arcana());
        assertEquals(0F, result.rectification());
        assertEquals(0, result.clues());
        assertEquals(15F, result.maxEterna(),
                "maxEterna tracks the highest contributor — all 15 report maxEterna=15");
        assertTrue(result.blacklist().isEmpty(), "blacklist stays empty until T-2.2.4");
        assertFalse(result.treasureAllowed(), "treasureAllowed stays false until T-2.2.4");
    }

    @Test
    void gather_emptyOffsets_returnsZeroCollection() {
        EnchantingStatRegistry reg = new EnchantingStatRegistry();

        StatCollection result = reg.gatherStatsFromOffsets(List.of(), pos -> EnchantingStats.ZERO);

        assertEquals(0F, result.eterna());
        assertEquals(0F, result.quanta());
        assertEquals(0F, result.arcana());
        assertEquals(0F, result.rectification());
        assertEquals(0, result.clues());
        assertEquals(0F, result.maxEterna());
    }

    @Test
    void gather_zeroLookups_contributeNothing() {
        EnchantingStatRegistry reg = new EnchantingStatRegistry();

        StatCollection result = reg.gatherStatsFromOffsets(
                offsets(32), pos -> EnchantingStats.ZERO);

        assertEquals(StatCollection.EMPTY.eterna(), result.eterna());
        assertEquals(StatCollection.EMPTY.maxEterna(), result.maxEterna());
    }

    @Test
    void gather_mixedStats_sumsIndependently() {
        EnchantingStatRegistry reg = new EnchantingStatRegistry();
        EnchantingStats hellshelf = new EnchantingStats(22.5F, 1.5F, 3F, 0F, 0F, 0);
        EnchantingStats seashelf = new EnchantingStats(22.5F, 1.5F, 0F, 2F, 0F, 0);

        Function<BlockPos, EnchantingStats> lookup = pos -> switch (pos.getX() % 2) {
            case 0 -> hellshelf;
            default -> seashelf;
        };

        StatCollection result = reg.gatherStatsFromOffsets(offsets(4), lookup);

        // 2 hellshelves + 2 seashelves — same maxEterna tier, step-ladder has one group
        assertEquals(6F, result.eterna(), 1e-6,
                "eterna sums within the same maxEterna tier, capped at that tier's ceiling");
        assertEquals(6F, result.quanta(), 1e-6);
        assertEquals(4F, result.arcana(), 1e-6);
        assertEquals(22.5F, result.maxEterna(), 1e-6,
                "maxEterna is the max across contributors, not a sum");
    }

    @Test
    void gather_mixedMaxEterna_trackAsMax() {
        EnchantingStatRegistry reg = new EnchantingStatRegistry();
        EnchantingStats stoneTier = new EnchantingStats(22.5F, 1.5F, 0F, 0F, 0F, 0);
        EnchantingStats endTier = new EnchantingStats(45F, 2.5F, 0F, 0F, 0F, 0);

        Function<BlockPos, EnchantingStats> lookup = pos ->
                pos.getX() == 0 ? stoneTier : endTier;

        StatCollection result = reg.gatherStatsFromOffsets(offsets(3), lookup);

        assertEquals(45F, result.maxEterna(), 1e-6);
    }

    @Test
    void gather_cluesAccumulateAsIntegers() {
        EnchantingStatRegistry reg = new EnchantingStatRegistry();
        EnchantingStats sight = new EnchantingStats(0F, 0F, 0F, 0F, 0F, 1);
        EnchantingStats sightT2 = new EnchantingStats(0F, 0F, 0F, 0F, 0F, 2);

        Function<BlockPos, EnchantingStats> lookup = pos ->
                pos.getX() == 0 ? sight : sightT2;

        StatCollection result = reg.gatherStatsFromOffsets(offsets(2), lookup);

        assertEquals(3, result.clues(),
                "clues sum to 3 (1 + 2 from the two shelves)");
    }

    @Test
    void gather_negativeContributions_subtract() {
        EnchantingStatRegistry reg = new EnchantingStatRegistry();
        // Stoneshelf drains arcana/rectification but doesn't raise maxEterna, so its
        // negative eterna sum floors to 0 after clamping. Arcana clamps to [0, 100].
        EnchantingStats stoneshelf = new EnchantingStats(0F, -1.5F, 0F, -7.5F, 0F, 0);

        StatCollection result = reg.gatherStatsFromOffsets(offsets(2), pos -> stoneshelf);

        assertEquals(0F, result.eterna(), 1e-6,
                "eterna clamps to the [0, maxEterna] floor when contributions go negative");
        assertEquals(0F, result.arcana(), 1e-6,
                "arcana clamps to the [0, 100] floor when contributions go negative");
    }

    @Test
    void gather_nullLookupValue_isSkipped() {
        EnchantingStatRegistry reg = new EnchantingStatRegistry();

        StatCollection result = reg.gatherStatsFromOffsets(offsets(3), pos -> null);

        assertEquals(0F, result.eterna());
        assertEquals(0F, result.maxEterna());
    }

    @Test
    void emptyCollection_constantIsSane() {
        assertSame(StatCollection.EMPTY.blacklist(), StatCollection.EMPTY.blacklist());
        assertTrue(StatCollection.EMPTY.blacklist().isEmpty());
        assertFalse(StatCollection.EMPTY.treasureAllowed());
    }

    @Test
    void vanillaBookshelfOffsets_areAccessible() {
        assertNotNull(EnchantingTableBlock.BOOKSHELF_OFFSETS,
                "gatherStats(Level, BlockPos) depends on vanilla BOOKSHELF_OFFSETS being public static final");
        assertFalse(EnchantingTableBlock.BOOKSHELF_OFFSETS.isEmpty(),
                "BOOKSHELF_OFFSETS should contain the ring of shelf positions");
    }

    @Test
    void midpoint_halvesXAndZ_preservesY() {
        // Matches vanilla EnchantingTableBlock#isValidBookShelf: midpoint = (x/2, y, z/2)
        assertEquals(new BlockPos(1, 0, -1),
                EnchantingStatRegistry.midpoint(new BlockPos(2, 0, -2)));
        assertEquals(new BlockPos(0, 1, 1),
                EnchantingStatRegistry.midpoint(new BlockPos(1, 1, 2)));
        assertEquals(new BlockPos(-1, 1, 0),
                EnchantingStatRegistry.midpoint(new BlockPos(-2, 1, 1)));
        // Java integer division rounds toward zero: -1/2 == 0
        assertEquals(new BlockPos(0, 0, 0),
                EnchantingStatRegistry.midpoint(new BlockPos(-1, 0, 1)));
    }

    @Test
    void gather_blockedMidpoint_shelfContributesZero() {
        EnchantingStatRegistry reg = new EnchantingStatRegistry();
        // Vanilla tuple so the eterna sum (14) stays below maxEterna=15 and the transmitter
        // check is the only thing that removes a contribution.
        EnchantingStats vanilla = new EnchantingStats(15F, 1F, 0F, 0F, 0F, 0);

        // Block the midpoint for the shelf at offset.x == 2 — all others still transmit.
        Predicate<BlockPos> transmitterCheck = pos -> pos.getX() != 2;

        StatCollection result = reg.gatherStatsFromOffsets(
                offsets(15), pos -> vanilla, transmitterCheck);

        assertEquals(14F, result.eterna(), 1e-6,
                "A shelf whose midpoint fails the transmitter check contributes zero");
    }

    @Test
    void gather_removingMidpointBlockage_restoresContribution() {
        EnchantingStatRegistry reg = new EnchantingStatRegistry();
        EnchantingStats unit = new EnchantingStats(22.5F, 1.5F, 3F, 0F, 0F, 0);
        List<BlockPos> singleOffset = List.of(new BlockPos(2, 0, 0));

        StatCollection blocked = reg.gatherStatsFromOffsets(
                singleOffset, pos -> unit, pos -> false);
        assertEquals(0F, blocked.eterna(), 1e-6);
        assertEquals(0F, blocked.quanta(), 1e-6);
        assertEquals(0F, blocked.maxEterna(), 1e-6,
                "A blocked shelf contributes no maxEterna either");

        StatCollection clear = reg.gatherStatsFromOffsets(
                singleOffset, pos -> unit, pos -> true);
        assertEquals(1.5F, clear.eterna(), 1e-6);
        assertEquals(3F, clear.quanta(), 1e-6);
        assertEquals(22.5F, clear.maxEterna(), 1e-6);
    }

    @Test
    void gather_eternaSumExceedsMaxEterna_clampsToMax() {
        EnchantingStatRegistry reg = new EnchantingStatRegistry();
        // Each shelf reports maxEterna=15, eterna=2 → raw sum 30, clamped to 15.
        EnchantingStats overcharged = new EnchantingStats(15F, 2F, 0F, 0F, 0F, 0);

        StatCollection result = reg.gatherStatsFromOffsets(offsets(15), pos -> overcharged);

        assertEquals(15F, result.maxEterna(), 1e-6);
        assertEquals(15F, result.eterna(), 1e-6,
                "eterna is clamped to maxEterna when contributions exceed the cap");
    }

    @Test
    void gather_mixedMaxEterna_clampsEternaToHighestMax() {
        EnchantingStatRegistry reg = new EnchantingStatRegistry();
        EnchantingStats lowTier = new EnchantingStats(15F, 1F, 0F, 0F, 0F, 0);
        EnchantingStats highTier = new EnchantingStats(45F, 5F, 2F, 0F, 0F, 0);

        // 10 low-tier + 5 high-tier → eterna sum = 10*1 + 5*5 = 35; maxEterna = 45;
        // clamp = min(35, 45) = 35 (still below the highest cap).
        Function<BlockPos, EnchantingStats> lookup = pos -> pos.getX() < 10 ? lowTier : highTier;

        StatCollection result = reg.gatherStatsFromOffsets(offsets(15), lookup);

        assertEquals(45F, result.maxEterna(), 1e-6,
                "maxEterna uses the highest contributor, not a sum or an average");
        assertEquals(35F, result.eterna(), 1e-6);
        assertEquals(10F, result.quanta(), 1e-6, "quanta is uncapped and sums across shelves");
    }

    @Test
    void gather_stepLadder_lowTierOverflowDoesNotCarryToHigherTier() {
        EnchantingStatRegistry reg = new EnchantingStatRegistry();
        // 14 vanilla bookshelves overcontribute (maxE=15, e=2 each → raw 28, but capped at 15)
        // 1 hellshelf (maxE=22.5, e=1.5)
        // Step-ladder: tier 15 → min(15, 0+28)=15; tier 22.5 → min(22.5, 15+1.5)=16.5
        EnchantingStats vanilla = new EnchantingStats(15F, 2F, 0F, 0F, 0F, 0);
        EnchantingStats hellshelf = new EnchantingStats(22.5F, 1.5F, 0F, 0F, 0F, 0);

        Function<BlockPos, EnchantingStats> lookup = pos ->
                pos.getX() == 0 ? hellshelf : vanilla;

        StatCollection result = reg.gatherStatsFromOffsets(offsets(15), lookup);

        assertEquals(16.5F, result.eterna(), 1e-6,
                "low-tier overflow is capped at tier ceiling before higher tiers add");
        assertEquals(22.5F, result.maxEterna(), 1e-6);
    }

    @Test
    void gather_stepLadder_threeTierProgression() {
        EnchantingStatRegistry reg = new EnchantingStatRegistry();
        // 5 vanilla (maxE=15, e=1), 5 hellshelf (maxE=22.5, e=1.5), 5 endshelf (maxE=45, e=2.5)
        // Step-ladder: tier 15 → min(15, 0+5)=5; tier 22.5 → min(22.5, 5+7.5)=12.5; tier 45 → min(45, 12.5+12.5)=25
        EnchantingStats vanilla = new EnchantingStats(15F, 1F, 0F, 0F, 0F, 0);
        EnchantingStats hellshelf = new EnchantingStats(22.5F, 1.5F, 0F, 0F, 0F, 0);
        EnchantingStats endshelf = new EnchantingStats(45F, 2.5F, 0F, 0F, 0F, 0);

        Function<BlockPos, EnchantingStats> lookup = pos -> {
            int x = pos.getX();
            if (x < 5) return vanilla;
            if (x < 10) return hellshelf;
            return endshelf;
        };

        StatCollection result = reg.gatherStatsFromOffsets(offsets(15), lookup);

        assertEquals(25F, result.eterna(), 1e-6,
                "three-tier step-ladder accumulates through each ceiling");
        assertEquals(45F, result.maxEterna(), 1e-6);
    }

    @Test
    void gather_stepLadder_negativeMaxEternaAddsDirectly() {
        EnchantingStatRegistry reg = new EnchantingStatRegistry();
        // Beeshelf (maxE=0, e=-15) drains eterna without capping
        // Combined with vanilla shelves (maxE=15, e=1)
        EnchantingStats beeshelf = new EnchantingStats(0F, -15F, 0F, 0F, 0F, 0);
        EnchantingStats vanilla = new EnchantingStats(15F, 1F, 0F, 0F, 0F, 0);

        Function<BlockPos, EnchantingStats> lookup = pos ->
                pos.getX() == 0 ? beeshelf : vanilla;

        // Step-ladder: tier 0 → eterna += -15 → eterna=-15; tier 15 → min(15, -15+14)=min(15,-1)=-1
        // Final clamp: max(0, -1) = 0
        StatCollection result = reg.gatherStatsFromOffsets(offsets(15), lookup);

        assertEquals(0F, result.eterna(), 1e-6,
                "negative contributions from zero-maxEterna shelves reduce the running total");
    }

    @Test
    void gather_singleShelfWithHighClues_clampedToMax() {
        EnchantingStatRegistry reg = new EnchantingStatRegistry();
        EnchantingStats overclued = new EnchantingStats(0F, 0F, 0F, 0F, 0F, 5);

        StatCollection result = reg.gatherStatsFromOffsets(
                List.of(new BlockPos(0, 0, 0)), pos -> overclued);

        assertEquals(EnchantingStatRegistry.MAX_CLUES, result.clues(),
                "clues exceeding MAX_CLUES are clamped to the cap");
    }

    @Test
    void gather_defaultOverload_assumesAllTransmittersClear() {
        EnchantingStatRegistry reg = new EnchantingStatRegistry();
        EnchantingStats vanilla = new EnchantingStats(15F, 1F, 0F, 0F, 0F, 0);

        // The 2-arg overload must still treat every offset as transmitter-clear so the
        // existing pre-transmitter-check behaviour is preserved.
        StatCollection result = reg.gatherStatsFromOffsets(offsets(15), pos -> vanilla);

        assertEquals(15F, result.eterna(), 1e-6);
    }

    // --- T-2.2.4: BlacklistSource / TreasureFlagSource hooks ---------------------------------

    private static ResourceKey<Enchantment> testEnchantKey(String path) {
        return ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.parse(path));
    }

    /** Records how often {@code getEnchantmentBlacklist()} is called — T-2.2.4 asserts once-per-BE. */
    private static final class CountingBlacklistShelf implements BlacklistSource {
        final Set<ResourceKey<Enchantment>> entries;
        final AtomicInteger calls = new AtomicInteger();

        CountingBlacklistShelf(Set<ResourceKey<Enchantment>> entries) {
            this.entries = entries;
        }

        @Override
        public Set<ResourceKey<Enchantment>> getEnchantmentBlacklist() {
            calls.incrementAndGet();
            return entries;
        }
    }

    private static final class TreasureShelfStub implements TreasureFlagSource {
    }

    @Test
    void gather_filteringShelfBlacklistUnionsIntoResult() {
        EnchantingStatRegistry reg = new EnchantingStatRegistry();
        EnchantingStats vanilla = new EnchantingStats(15F, 1F, 0F, 0F, 0F, 0);
        ResourceKey<Enchantment> sharpness = testEnchantKey("minecraft:sharpness");
        ResourceKey<Enchantment> mending = testEnchantKey("minecraft:mending");
        CountingBlacklistShelf shelf = new CountingBlacklistShelf(Set.of(sharpness, mending));

        Function<BlockPos, Object> contextLookup = pos ->
                pos.getX() == 0 ? shelf : null;

        StatCollection result = reg.gatherStatsFromOffsets(
                offsets(3), pos -> vanilla, pos -> true, contextLookup);

        assertEquals(Set.of(sharpness, mending), result.blacklist(),
                "a filtering shelf's blacklist must flow through to the aggregated result");
        assertEquals(1, shelf.calls.get(),
                "getEnchantmentBlacklist() is called exactly once per in-range BE per scan");
    }

    @Test
    void gather_treasureShelfSetsTreasureAllowed() {
        EnchantingStatRegistry reg = new EnchantingStatRegistry();

        Function<BlockPos, Object> contextLookup = pos ->
                pos.getX() == 1 ? new TreasureShelfStub() : null;

        StatCollection result = reg.gatherStatsFromOffsets(
                offsets(3), pos -> EnchantingStats.ZERO, pos -> true, contextLookup);

        assertTrue(result.treasureAllowed(),
                "a single in-range TreasureFlagSource flips the treasure flag");
    }

    @Test
    void gather_multipleFilteringShelves_unionBlacklists() {
        EnchantingStatRegistry reg = new EnchantingStatRegistry();
        ResourceKey<Enchantment> sharpness = testEnchantKey("minecraft:sharpness");
        ResourceKey<Enchantment> smite = testEnchantKey("minecraft:smite");
        ResourceKey<Enchantment> fortune = testEnchantKey("minecraft:fortune");

        CountingBlacklistShelf shelfA = new CountingBlacklistShelf(Set.of(sharpness, smite));
        CountingBlacklistShelf shelfB = new CountingBlacklistShelf(Set.of(smite, fortune));

        Function<BlockPos, Object> contextLookup = pos -> switch (pos.getX()) {
            case 0 -> shelfA;
            case 1 -> shelfB;
            default -> null;
        };

        StatCollection result = reg.gatherStatsFromOffsets(
                offsets(3), pos -> EnchantingStats.ZERO, pos -> true, contextLookup);

        assertEquals(Set.of(sharpness, smite, fortune), result.blacklist(),
                "multiple filtering shelves union their blacklist entries");
        assertEquals(1, shelfA.calls.get(),
                "each filtering shelf BE is queried exactly once");
        assertEquals(1, shelfB.calls.get(),
                "each filtering shelf BE is queried exactly once");
    }

    @Test
    void gather_noContextLookup_isNoOpForHooks() {
        EnchantingStatRegistry reg = new EnchantingStatRegistry();
        EnchantingStats vanilla = new EnchantingStats(15F, 1F, 0F, 0F, 0F, 0);

        StatCollection result = reg.gatherStatsFromOffsets(
                offsets(3), pos -> vanilla, pos -> true, pos -> null);

        assertTrue(result.blacklist().isEmpty(),
                "no context BEs in range → blacklist is empty");
        assertFalse(result.treasureAllowed(),
                "no context BEs in range → treasure flag stays false");
    }

    @Test
    void gather_blockedMidpoint_skipsContextToo() {
        EnchantingStatRegistry reg = new EnchantingStatRegistry();
        ResourceKey<Enchantment> sharpness = testEnchantKey("minecraft:sharpness");
        CountingBlacklistShelf shelf = new CountingBlacklistShelf(Set.of(sharpness));

        // Shelf sits at x=0 but its midpoint is blocked. It should contribute neither stats nor context.
        Predicate<BlockPos> transmitter = pos -> pos.getX() != 0;
        Function<BlockPos, Object> contextLookup = pos ->
                pos.getX() == 0 ? shelf : null;

        StatCollection result = reg.gatherStatsFromOffsets(
                offsets(3), pos -> EnchantingStats.ZERO, transmitter, contextLookup);

        assertTrue(result.blacklist().isEmpty(),
                "a filtering shelf whose midpoint is blocked contributes no blacklist");
        assertEquals(0, shelf.calls.get(),
                "the hook is not invoked for a shelf gated out by the transmitter check");
    }

    @Test
    void gather_filteringShelfWithEmptyBlacklist_leavesResultEmpty() {
        EnchantingStatRegistry reg = new EnchantingStatRegistry();
        CountingBlacklistShelf emptyShelf = new CountingBlacklistShelf(Set.of());

        Function<BlockPos, Object> contextLookup = pos ->
                pos.getX() == 0 ? emptyShelf : null;

        StatCollection result = reg.gatherStatsFromOffsets(
                offsets(1), pos -> EnchantingStats.ZERO, pos -> true, contextLookup);

        assertTrue(result.blacklist().isEmpty(),
                "an empty filtering shelf contributes no entries (still counts as a valid source)");
        assertEquals(1, emptyShelf.calls.get(),
                "hook invoked once even when the returned set is empty");
    }

    @Test
    void gather_contextLookupInvokedOncePerOffset() {
        EnchantingStatRegistry reg = new EnchantingStatRegistry();
        Map<BlockPos, Integer> invocations = new HashMap<>();

        Function<BlockPos, Object> contextLookup = pos -> {
            invocations.merge(pos, 1, Integer::sum);
            return null;
        };

        reg.gatherStatsFromOffsets(
                offsets(10), pos -> EnchantingStats.ZERO, pos -> true, contextLookup);

        assertEquals(10, invocations.size(),
                "the context lookup is invoked once per in-range offset");
        assertTrue(invocations.values().stream().allMatch(count -> count == 1),
                "the context lookup is invoked exactly once per offset, not repeatedly");
    }
}
