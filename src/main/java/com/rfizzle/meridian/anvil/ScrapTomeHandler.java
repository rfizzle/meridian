package com.rfizzle.meridian.anvil;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.config.MeridianConfig;
import com.rfizzle.meridian.tome.ScrapTomeItem;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.Supplier;

/**
 * Scrap Tome — cheapest salvage tier. Left slot holds any enchanted item, right slot holds a
 * {@link ScrapTomeItem}; the output is a fresh enchanted book carrying <strong>one</strong> of
 * the left stack's enchantments, preserving its original level. Vanilla's
 * {@code AnvilMenu#onTake} decrements both input slots by their respective consumption counts,
 * so the left item is destroyed and the tome consumed when the player pulls the output.
 *
 * <p>The enchantment chosen is deterministic, not uniformly random: the seed mixes
 * {@link Player#getEnchantmentSeed()} with the resource-location hashes of every enchantment on
 * the left stack (Zenith's pattern from {@code ScrappingTomeItem}). The same player scrapping
 * the same loadout always draws the same enchantment — predictable enough for players to plan
 * around, not so predictable that they can brute-force the roll by swapping tomes.
 *
 * <p>Candidate keys are sorted by resource-location string before indexing so that
 * {@link ItemEnchantments#keySet()}'s internal {@code Reference2IntOpenHashMap} iteration order
 * cannot leak into the pick — without the sort the same seed could return different picks across
 * JVM runs.
 *
 * <p>Declines ({@link Optional#empty()}) when any of the following holds, allowing vanilla or a
 * later dispatcher entry to own the pair:
 * <ul>
 *   <li>{@link Meridian#getConfig()} has not loaded yet.</li>
 *   <li>Either slot is empty.</li>
 *   <li>Right slot is not a Scrap Tome.</li>
 *   <li>Left stack carries no enchantments (nothing to scrap — Zenith's matching guard).</li>
 * </ul>
 *
 * <p>XP cost is the flat {@code config.tomes.scrapTomeXpCost} — DESIGN.md intentionally diverges
 * from Zenith's per-enchant pricing so the Scrap Tome stays the cheap/gamble tier regardless of
 * how many enchants the source item carries.
 */
public final class ScrapTomeHandler implements AnvilHandler {

    private static final int TOME_CONSUMED = 1;
    private static final long SEED_SALT = 1831L;

    private final Supplier<MeridianConfig> configSupplier;

    /** Production constructor — reads the live {@link Meridian#getConfig()} at claim time. */
    public ScrapTomeHandler() {
        this(Meridian::getConfig);
    }

    /** Test constructor — lets fixtures inject a specific config without mutating the singleton. */
    ScrapTomeHandler(Supplier<MeridianConfig> configSupplier) {
        this.configSupplier = configSupplier;
    }

    @Override
    public Optional<AnvilResult> handle(ItemStack left, ItemStack right, Player player) {
        MeridianConfig config = configSupplier.get();
        if (config == null) {
            return Optional.empty();
        }
        if (left == null || right == null || left.isEmpty() || right.isEmpty()) {
            return Optional.empty();
        }
        if (!(right.getItem() instanceof ScrapTomeItem)) {
            return Optional.empty();
        }

        ItemEnchantments existing = EnchantmentHelper.getEnchantmentsForCrafting(left);
        if (existing.isEmpty()) {
            return Optional.empty();
        }

        List<Holder<Enchantment>> candidates = sortedKeys(existing);
        if (candidates.isEmpty()) {
            return Optional.empty();
        }

        Random rand = new Random(seedFor(player, candidates));
        Holder<Enchantment> picked = candidates.get(rand.nextInt(candidates.size()));
        int level = existing.getLevel(picked);

        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        mutable.set(picked, level);
        book.set(DataComponents.STORED_ENCHANTMENTS, mutable.toImmutable());

        return Optional.of(new AnvilResult(book, config.tomes.scrapTomeXpCost, TOME_CONSUMED));
    }

    /**
     * Deterministic seed for the salvage roll. Package-private so {@link ScrapTomeHandlerTest}
     * can mirror the computation when asserting which enchantment survives a given loadout.
     *
     * <p>Without a {@link Player} (tests, or the rare pre-init dispatch) the player contribution
     * is zero — the seed still depends on the enchantment set, which is enough for repeatable
     * unit tests.
     */
    static long seedFor(Player player, List<Holder<Enchantment>> candidates) {
        long seed = SEED_SALT;
        for (Holder<Enchantment> holder : candidates) {
            seed ^= locationHash(holder);
        }
        if (player != null) {
            seed ^= player.getEnchantmentSeed();
        }
        return seed;
    }

    /**
     * Stable ordering for the candidate list. Sorting by the registered resource location means
     * the pick index always maps to the same {@link Holder} regardless of how the underlying
     * {@link ItemEnchantments} stored the set — important for the seeded-roll contract.
     */
    static List<Holder<Enchantment>> sortedKeys(ItemEnchantments enchantments) {
        List<Holder<Enchantment>> list = new ArrayList<>(enchantments.keySet());
        list.sort(Comparator.comparing(ScrapTomeHandler::locationString));
        return list;
    }

    private static String locationString(Holder<Enchantment> holder) {
        return holder.unwrapKey().map(k -> k.location().toString()).orElse("");
    }

    private static long locationHash(Holder<Enchantment> holder) {
        return holder.unwrapKey().map(ResourceKey::location)
                .map(loc -> (long) loc.hashCode())
                .orElse(0L);
    }
}
