package com.rfizzle.meridian.enchanting;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.config.MeridianConfig;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.util.random.WeightedRandom;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Core math for translating enchanting stats into concrete table outputs. Ported from
 * Zenith's {@code RealEnchantmentHelper}, re-authored against 1.21.1's dynamic enchantment
 * registry. The stat semantics are those of our DESIGN.md: {@code eterna} is the target
 * maximum level (0–50) rather than Zenith's doubled "power" scale, so slot 2 maps directly
 * to {@code round(eterna)} with no extra multiplier.
 */
public final class RealEnchantmentHelper {

    /** Fallback when called before {@link Meridian#onInitialize} has populated the config. */
    public static final int DEFAULT_MAX_ETERNA = 50;

    private RealEnchantmentHelper() {
    }

    /**
     * Computes the XP-level cost for the given preview slot.
     *
     * <p>Slot 2 returns {@code round(eterna)} deterministically. Slot 1 and slot 0 scale down
     * to {@code [60%, 80%]} and {@code [20%, 40%]} of slot 2 respectively, drawn from
     * {@code rand}. The returned cost is floored at 1 for slots 0 and 1 to stay inside the
     * vanilla "usable slot" invariant.
     *
     * <p>The {@code eterna} input is clamped to {@code [0, config.enchantingTable.maxEterna]}
     * before the level conversion so that shelf stats that overshoot the configured ceiling
     * cannot produce a cost higher than the operator allows.
     *
     * @param rand   pre-seeded random (callers seed with the player's enchantment seed)
     * @param slot   preview slot index in {@code [0, 2]}; slot 2 is the highest tier
     * @param eterna aggregated eterna stat for the table
     * @param stack  item being enchanted — retained for API parity with the caller side; the
     *               cost formula itself does not depend on the stack
     * @return the XP level cost for the slot, {@code >= 0}
     */
    public static int getEnchantmentCost(RandomSource rand, int slot, float eterna, ItemStack stack) {
        int maxEternaCfg = resolveMaxEterna();
        float clamped = Mth.clamp(eterna, 0F, (float) maxEternaCfg);
        int level = Math.round(clamped);
        if (slot == 2) {
            return level;
        }
        float lowBound = 0.6F - 0.4F * (1 - slot);
        float highBound = 0.8F - 0.4F * (1 - slot);
        return Math.max(1, Math.round(level * Mth.nextFloat(rand, lowBound, highBound)));
    }

    /**
     * Rolls the enchantments applied at a preview slot, mirroring Zenith's stat-driven selection
     * against the 1.21.1 dynamic enchantment registry.
     *
     * <p>Quanta widens a symmetric random window around {@code level} via
     * {@link #getQuantaFactor}: the effective power becomes
     * {@code clamp(round(level * (1 + factor)), 1, 200)}, where {@code factor} is a
     * roughly-normal variate in {@code [-quanta/100, quanta/100]}. Rectification truncates the
     * lower tail — at {@code rectification=100} the factor is {@code >= 0} and outcomes are
     * monotonically {@code >= level}.
     *
     * <p>Arcana shifts the rarity weighting and unlocks extra picks at 33 and 66. Incompatible
     * enchantments are pruned after each pick so two mutually-exclusive rolls never coexist.
     *
     * @param rand             pre-seeded random; callers use the player's enchantment seed
     * @param stack            item being enchanted; the enchantability value gates whether any
     *                         enchantments are rolled, and its supported-items set filters the
     *                         candidate pool
     * @param level            slot power (typically this is the slot cost from
     *                         {@link #getEnchantmentCost})
     * @param quanta           quanta stat in {@code [0, 100]}; widens the random power window
     * @param arcana           arcana stat in {@code [0, 100]}; tilts rarity weights
     * @param rectification    rectification stat in {@code [0, 100]}; truncates the negative
     *                         tail of the quanta variance
     * @param treasureAllowed  when {@code false}, enchantments in
     *                         {@link EnchantmentTags#TREASURE} are filtered out
     * @param blacklist        enchantments that must never appear in the output
     * @param registryAccess   source of the dynamic enchantment registry
     * @return the chosen enchantments; empty when the item is unenchantable or nothing rolls
     */
    public static List<EnchantmentInstance> selectEnchantment(
            RandomSource rand,
            ItemStack stack,
            int level,
            float quanta,
            float arcana,
            float rectification,
            boolean treasureAllowed,
            Set<ResourceKey<Enchantment>> blacklist,
            RegistryAccess registryAccess
    ) {
        Registry<Enchantment> registry = registryAccess.registryOrThrow(Registries.ENCHANTMENT);
        return selectEnchantment(
                rand, stack, level, quanta, arcana, rectification, treasureAllowed, blacklist, registry);
    }

    static List<EnchantmentInstance> selectEnchantment(
            RandomSource rand,
            ItemStack stack,
            int level,
            float quanta,
            float arcana,
            float rectification,
            boolean treasureAllowed,
            Set<ResourceKey<Enchantment>> blacklist,
            Registry<Enchantment> registry
    ) {
        List<EnchantmentInstance> chosen = new ArrayList<>();
        if (stack.isEmpty()) {
            return chosen;
        }
        int enchantability = stack.getItem().getEnchantmentValue();
        if (enchantability <= 0) {
            enchantability = 1;
        }

        float quantaFactor = getQuantaFactor(rand, quanta, rectification);
        int scaledLevel = Mth.clamp(Math.round(level * (1F + quantaFactor)), 1, 200);

        Set<ResourceKey<Enchantment>> safeBlacklist = blacklist == null ? Set.of() : blacklist;
        List<EnchantmentInstance> candidates = getAvailableEnchantmentResults(
                scaledLevel, stack, registry, treasureAllowed, safeBlacklist);
        removePresentEnchantments(candidates, stack);

        if (candidates.isEmpty()) {
            return chosen;
        }

        Arcana arcanaTier = Arcana.getForThreshold(arcana);
        List<ArcanaEnchantmentData> pool = new ArrayList<>(candidates.size());
        for (EnchantmentInstance inst : candidates) {
            pool.add(new ArcanaEnchantmentData(arcanaTier, inst));
        }

        for (int i = 0; i <= 66; i += 33) {
            if (arcana >= i && !pool.isEmpty()) {
                pickInto(rand, pool, chosen);
                if (!chosen.isEmpty()) {
                    removeIncompatible(pool, lastOf(chosen));
                }
            }
        }

        int randomBound = Math.max(50, (int) (scaledLevel * 1.15F));
        while (rand.nextInt(randomBound) <= scaledLevel && !pool.isEmpty()) {
            pickInto(rand, pool, chosen);
            removeIncompatible(pool, lastOf(chosen));
            scaledLevel /= 2;
        }

        if (stack.getItem() instanceof EnchantableItem enchantable) {
            StatCollection stats = new StatCollection(
                    level, quanta, arcana, rectification, 0, level, safeBlacklist, treasureAllowed);
            chosen = enchantable.selectEnchantments(chosen, rand, stack, level, stats);
        }
        return chosen;
    }

    /**
     * Builds the candidate list for {@link #selectEnchantment} from the 1.21.1 dynamic
     * enchantment registry.
     *
     * <p>Filters applied:
     * <ul>
     *     <li>{@link EnchantmentTags#IN_ENCHANTING_TABLE} OR ({@code allowTreasure} AND
     *         {@link EnchantmentTags#TREASURE}) — treasure-only enchantments (e.g. Mending)
     *         are unlocked when a treasure shelf is present.</li>
     *     <li>{@code blacklist} — filtering-shelf book contents.</li>
     *     <li>{@link Enchantment#canEnchant(ItemStack)} — supported-items check.</li>
     * </ul>
     *
     * <p>For each surviving enchantment, the highest level whose cost window admits
     * {@code power} is picked (matching Zenith's semantics: require
     * {@code power >= minCost(L)} and either {@code power <= maxCost(L)} or {@code L} is the
     * minimum level).
     */
    public static List<EnchantmentInstance> getAvailableEnchantmentResults(
            int power,
            ItemStack stack,
            Registry<Enchantment> registry,
            boolean allowTreasure,
            Set<ResourceKey<Enchantment>> blacklist
    ) {
        List<EnchantmentInstance> list = new ArrayList<>();
        for (Holder.Reference<Enchantment> holder : (Iterable<Holder.Reference<Enchantment>>) registry.holders()::iterator) {
            boolean inTable = holder.is(EnchantmentTags.IN_ENCHANTING_TABLE);
            boolean isTreasure = holder.is(EnchantmentTags.TREASURE);
            if (!inTable && !(allowTreasure && isTreasure)) {
                continue;
            }
            if (blacklist.contains(holder.key())) {
                continue;
            }
            Enchantment ench = holder.value();
            if (!ench.canEnchant(stack) && !stack.is(Items.BOOK)) {
                continue;
            }
            EnchantmentInfo info = EnchantmentInfoRegistry.getInfo(holder);
            if (!info.enabled()) {
                continue;
            }
            for (int lvl = info.getMaxLevel(); lvl > ench.getMinLevel() - 1; lvl--) {
                if (power >= info.getMinPower(lvl)
                        && (power <= info.getMaxPower(lvl) || lvl == ench.getMinLevel())) {
                    list.add(new EnchantmentInstance(holder, lvl));
                    break;
                }
            }
        }
        return list;
    }

    /**
     * Derives a preview slot's display enchantment plus the tooltip clue list from the slot's
     * full selection pool (the result of {@link #selectEnchantment}).
     *
     * <p>When the pool is non-empty, a random entry is removed and becomes the slot's
     * {@link ClueBuild#primary}. That primary is the enchant the client stores in its
     * {@code enchantClue}/{@code levelClue} arrays and shows as the slot's tooltip headline.
     * If {@code cluesCount > 0} it also becomes the first entry of {@link ClueBuild#clues}
     * — so the first clue is always the exact enchant the slot will apply for the player.
     * Remaining clues are pulled at random from what is left in the pool until
     * {@code cluesCount} entries are collected or the pool runs dry.
     *
     * <p>{@link ClueBuild#exhaustedList} is {@code true} when the pool emptied during clue
     * building — a signal to the client that no more clues can be revealed at a higher Clues
     * stat. An empty input pool short-circuits to
     * {@code {primary: null, clues: [], exhaustedList: true}} without consuming randomness.
     *
     * <p>Matches Zenith's inline clue-list build inside {@code ApothEnchantmentMenu.slotsChanged}.
     *
     * @param rand        pre-seeded random shared with {@link #selectEnchantment} in the caller
     * @param slotPicks   the selection pool for this slot; never mutated by this method
     * @param cluesCount  clamped clues stat in {@code [0, ∞)}; values {@code <= 0} still permit
     *                    a primary pick (so the slot has something to display) but return an
     *                    empty clue list
     * @return the primary, clue list, and exhaustion flag for this slot
     */
    public static ClueBuild buildClueList(
            RandomSource rand, List<EnchantmentInstance> slotPicks, int cluesCount) {
        if (slotPicks == null || slotPicks.isEmpty()) {
            return new ClueBuild(null, List.of(), true);
        }
        List<EnchantmentInstance> pool = new ArrayList<>(slotPicks);
        EnchantmentInstance primary = pool.remove(rand.nextInt(pool.size()));
        List<EnchantmentInstance> clues = new ArrayList<>();
        int remaining = cluesCount;
        if (remaining > 0) {
            clues.add(primary);
            remaining--;
        }
        while (remaining > 0 && !pool.isEmpty()) {
            clues.add(pool.remove(rand.nextInt(pool.size())));
            remaining--;
        }
        return new ClueBuild(primary, List.copyOf(clues), pool.isEmpty());
    }

    /**
     * Result of {@link #buildClueList}: the slot's display enchant ({@code primary}), the
     * clue list sized up to {@code cluesCount}, and whether the selection pool was drained
     * during clue building. When {@code primary} is non-null and {@code cluesCount > 0} the
     * invariant {@code primary == clues.get(0)} holds.
     */
    public record ClueBuild(
            EnchantmentInstance primary,
            List<EnchantmentInstance> clues,
            boolean exhaustedList) {
    }

    /**
     * Quanta factor in {@code [-quanta/100, quanta/100]} drawn from a truncated-gaussian
     * distribution whose negative tail is re-rolled uniformly into
     * {@code [rectification/100 - 1, 1]}. With {@code rectification=0} the distribution is
     * symmetric (mean 0, stdev ~ quanta/300); with {@code rectification=100} the distribution
     * floors at 0 and the result is always non-negative.
     */
    public static float getQuantaFactor(RandomSource rand, float quanta, float rectification) {
        float gaussian = (float) rand.nextGaussian();
        float factor = Mth.clamp(gaussian / 3F, -1F, 1F);
        float rectPercent = rectification / 100F;
        if (factor < rectPercent - 1F) {
            factor = Mth.nextFloat(rand, rectPercent - 1F, 1F);
        }
        return quanta * factor / 100F;
    }

    /** Removes enchantments in {@code list} that are incompatible with {@code data}. */
    static void removeIncompatible(List<ArcanaEnchantmentData> list, EnchantmentInstance data) {
        Iterator<ArcanaEnchantmentData> iterator = list.iterator();
        while (iterator.hasNext()) {
            ArcanaEnchantmentData next = iterator.next();
            if (!Enchantment.areCompatible(data.enchantment, next.data.enchantment)) {
                iterator.remove();
            }
        }
    }

    private static void pickInto(
            RandomSource rand, List<ArcanaEnchantmentData> pool, List<EnchantmentInstance> chosen) {
        WeightedRandom.getRandomItem(rand, pool).ifPresent(entry -> chosen.add(entry.data));
    }

    private static void removePresentEnchantments(List<EnchantmentInstance> list, ItemStack stack) {
        ItemEnchantments existing = EnchantmentHelper.getEnchantmentsForCrafting(stack);
        if (existing.isEmpty()) {
            return;
        }
        list.removeIf(inst -> existing.getLevel(inst.enchantment) > 0);
    }

    private static EnchantmentInstance lastOf(List<EnchantmentInstance> list) {
        return list.get(list.size() - 1);
    }

    private static int resolveMaxEterna() {
        MeridianConfig config = Meridian.getConfig();
        return config != null ? config.enchantingTable.maxEterna : DEFAULT_MAX_ETERNA;
    }

    /**
     * Weighted-random entry for {@link WeightedRandom#getRandomItem}. The weight is pulled from
     * the Arcana rarity bucket matching the wrapped enchantment's own weight — so increasing
     * arcana tilts the pool toward rarer picks without mutating the enchantment registry.
     */
    public static final class ArcanaEnchantmentData extends WeightedEntry.IntrusiveBase {
        final EnchantmentInstance data;

        public ArcanaEnchantmentData(Arcana arcana, EnchantmentInstance data) {
            super(arcana.getRarities()[rarityBucket(data.enchantment.value().getWeight())]);
            this.data = data;
        }

        public EnchantmentInstance getInstance() {
            return data;
        }
    }

    /**
     * Buckets a 1.21.1 enchantment weight into the 4-tier rarity index used by {@link Arcana}.
     * Mirrors vanilla's pre-1.20 Rarity weights (COMMON=10, UNCOMMON=5, RARE=2, VERY_RARE=1) so
     * the EMPTY arcana tier produces vanilla-equivalent rolls.
     */
    static int rarityBucket(int weight) {
        if (weight >= 10) return 0;
        if (weight >= 5) return 1;
        if (weight >= 2) return 2;
        return 3;
    }

    /**
     * Arcana rarity tiers — 4 per-rarity weights that feed
     * {@link WeightedRandom#getRandomItem}. Each tier replaces vanilla's per-enchantment weight
     * with a rarity-bucket weight so higher arcana pushes rolls toward rarer tiers.
     *
     * <p>Values are lifted verbatim from Zenith so rolls match reference output at the same
     * stat inputs.
     */
    public enum Arcana {
        EMPTY(0, 10, 5, 2, 1),
        LITTLE(10, 8, 5, 3, 1),
        FEW(20, 7, 5, 4, 2),
        SOME(30, 5, 5, 4, 2),
        LESS(40, 5, 5, 4, 3),
        MEDIUM(50, 5, 5, 5, 5),
        MORE(60, 3, 4, 5, 5),
        VALUE(70, 2, 4, 5, 5),
        EXTRA(80, 2, 4, 5, 7),
        ALMOST(90, 1, 3, 5, 8),
        MAX(99, 1, 2, 5, 10);

        private static final Arcana[] VALUES = values();

        private final float threshold;
        private final int[] rarities;

        Arcana(float threshold, int... rarities) {
            this.threshold = threshold;
            this.rarities = rarities;
        }

        public int[] getRarities() {
            return rarities;
        }

        public static Arcana getForThreshold(float threshold) {
            for (int i = VALUES.length - 1; i >= 0; i--) {
                if (threshold >= VALUES[i].threshold) {
                    return VALUES[i];
                }
            }
            return EMPTY;
        }
    }
}
