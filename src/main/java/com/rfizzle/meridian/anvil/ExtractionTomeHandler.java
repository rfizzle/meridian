package com.rfizzle.meridian.anvil;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.config.MeridianConfig;
import com.rfizzle.meridian.tome.ExtractionTomeItem;
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
 * Extraction Tome — top-tier salvage. Left slot holds any enchanted item, right slot holds an
 * {@link ExtractionTomeItem}; the output is a fresh enchanted book carrying
 * <strong>every</strong> enchantment from the left stack, and — uniquely among the three tome
 * tiers — the source item is <em>preserved</em> on the anvil's left slot, fully unenchanted
 * and with a configurable durability tick applied. The preserved stack is surfaced to the
 * take-path via {@link AnvilResult#leftReplacement()}; {@link com.rfizzle.meridian.mixin.AnvilMenuMixin}'s
 * {@code onTake} TAIL hook writes it back into slot 0 after vanilla clears it.
 *
 * <p>Progression-wise this is the most expensive tome tier precisely because the source item
 * survives. The Scrap / Improved Scrap tiers destroy the item; Extraction gives the best of both
 * worlds (full enchantment salvage + item retention) at a correspondingly higher XP cost.
 *
 * <p>Durability handling: the copy's damage value is raised by
 * {@code config.tomes.extractionTomeItemDamage} but clamped to {@code maxDamage - 1} so the item
 * always has at least one hit-point of durability left. A sword with 1 durability remaining
 * stays at 1 — the tome never outright breaks an item. Non-damageable items (e.g. an enchanted
 * book in the left slot) get unenchanted but not damaged, since there is no damage axis to
 * increment.
 *
 * <p>Declines ({@link Optional#empty()}) when any of the following holds, letting vanilla or a
 * later dispatcher entry own the pair:
 * <ul>
 *   <li>{@link Meridian#getConfig()} has not loaded yet.</li>
 *   <li>Either slot is empty.</li>
 *   <li>Right slot is not an Extraction Tome.</li>
 *   <li>Left stack carries no enchantments (nothing to extract).</li>
 * </ul>
 */
public final class ExtractionTomeHandler implements AnvilHandler {

    private static final int TOME_CONSUMED = 1;

    private final Supplier<MeridianConfig> configSupplier;

    /** Production constructor — reads the live {@link Meridian#getConfig()} at claim time. */
    public ExtractionTomeHandler() {
        this(Meridian::getConfig);
    }

    /** Test constructor — lets fixtures inject a specific config without mutating the singleton. */
    ExtractionTomeHandler(Supplier<MeridianConfig> configSupplier) {
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
        if (!(right.getItem() instanceof ExtractionTomeItem)) {
            return Optional.empty();
        }

        ItemEnchantments existing = EnchantmentHelper.getEnchantmentsForCrafting(left);
        if (existing.isEmpty()) {
            return Optional.empty();
        }

        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        ItemEnchantments.Mutable transferred = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        for (Holder<Enchantment> holder : existing.keySet()) {
            transferred.set(holder, existing.getLevel(holder));
        }
        book.set(DataComponents.STORED_ENCHANTMENTS, transferred.toImmutable());

        ItemStack preserved = stripAndDamage(left, config.tomes.extractionTomeItemDamage);

        return Optional.of(new AnvilResult(
                book,
                config.tomes.extractionTomeXpCost,
                TOME_CONSUMED,
                preserved));
    }

    /**
     * Returns a single-count copy of {@code source} with every enchantment stripped from both
     * the {@code ENCHANTMENTS} and {@code STORED_ENCHANTMENTS} components (so swords and
     * enchanted-book inputs both come out blank), and durability reduced by {@code damageDelta}
     * without ever crossing into the item's broken threshold.
     *
     * <p>Package-private for unit-test access — the handler tests assert the stripping and
     * clamp semantics directly on this helper so the production call-site stays one line.
     */
    static ItemStack stripAndDamage(ItemStack source, int damageDelta) {
        ItemStack preserved = source.copyWithCount(1);
        preserved.set(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (preserved.has(DataComponents.STORED_ENCHANTMENTS)) {
            preserved.set(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
        }
        if (preserved.isDamageableItem() && damageDelta > 0) {
            preserved.setDamageValue(
                    clampDamage(preserved.getDamageValue(), damageDelta, preserved.getMaxDamage()));
        }
        return preserved;
    }

    static int clampDamage(int currentDamage, int damageDelta, int maxDamage) {
        int ceiling = Math.max(0, maxDamage - 1);
        int proposed = currentDamage + damageDelta;
        return Math.min(proposed, ceiling);
    }
}
