package com.rfizzle.meridian.anvil;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.config.MeridianConfig;
import com.rfizzle.meridian.tome.ImprovedScrapTomeItem;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Improved Scrap Tome — mid-tier salvage. Left slot holds any enchanted item, right slot holds an
 * {@link ImprovedScrapTomeItem}; the output is a fresh enchanted book carrying
 * <strong>every</strong> enchantment from the left stack, each at its original level. Vanilla's
 * {@code AnvilMenu#onTake} decrements both input slots by their respective consumption counts, so
 * the left item is destroyed and the tome consumed when the player pulls the output.
 *
 * <p>Unlike {@link ScrapTomeHandler} there is no RNG: the salvage is fully deterministic because
 * every enchantment transfers. That's the whole pitch of this tier — you pay a higher XP cost and
 * still destroy the item, but you no longer gamble on which enchantment survives.
 *
 * <p>Declines ({@link Optional#empty()}) when any of the following holds, allowing vanilla or a
 * later dispatcher entry to own the pair:
 * <ul>
 *   <li>{@link Meridian#getConfig()} has not loaded yet.</li>
 *   <li>Either slot is empty.</li>
 *   <li>Right slot is not an Improved Scrap Tome.</li>
 *   <li>Left stack carries no enchantments (nothing to scrap — Zenith's matching guard).</li>
 * </ul>
 *
 * <p>XP cost is the flat {@code config.tomes.improvedScrapTomeXpCost} — intentionally richer than
 * Scrap's cost so the two tiers stay meaningfully distinct in the progression.
 */
public final class ImprovedScrapTomeHandler implements AnvilHandler {

    private static final int TOME_CONSUMED = 1;

    private final Supplier<MeridianConfig> configSupplier;

    /** Production constructor — reads the live {@link Meridian#getConfig()} at claim time. */
    public ImprovedScrapTomeHandler() {
        this(Meridian::getConfig);
    }

    /** Test constructor — lets fixtures inject a specific config without mutating the singleton. */
    ImprovedScrapTomeHandler(Supplier<MeridianConfig> configSupplier) {
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
        if (!(right.getItem() instanceof ImprovedScrapTomeItem)) {
            return Optional.empty();
        }

        ItemEnchantments existing = EnchantmentHelper.getEnchantmentsForCrafting(left);
        if (existing.isEmpty()) {
            return Optional.empty();
        }

        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        for (Holder<Enchantment> holder : existing.keySet()) {
            mutable.set(holder, existing.getLevel(holder));
        }
        book.set(DataComponents.STORED_ENCHANTMENTS, mutable.toImmutable());

        return Optional.of(new AnvilResult(book, config.tomes.improvedScrapTomeXpCost, TOME_CONSUMED));
    }
}
