package com.rfizzle.meridian.anvil;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.config.MeridianConfig;
import com.rfizzle.meridian.tome.ExtractionTomeItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * T-5.2.4 — Extraction Tome's item-repair side-path. Fires when the left slot holds a damageable,
 * damaged item, the right slot holds an {@link ExtractionTomeItem}, and the left has no
 * enchantments (so the extraction path declined). The output is a copy of the left item with its
 * damage reduced by {@code floor(maxDamage * config.tomes.extractionTomeRepairPercent)}, floored at
 * zero — the tome is consumed, the original left stack is cleared by vanilla's {@code onTake}, and
 * the player pulls the repaired copy out of the result slot.
 *
 * <p>Registered after {@link ExtractionTomeHandler} so extraction always wins when both could
 * plausibly apply — an enchanted damaged item still flows through extraction (enchants salvaged
 * to a book, source preserved with a damage tick) rather than silently losing its enchants to a
 * repair. The two handlers deliberately share the right-slot instance check but split on the
 * left-slot state.
 *
 * <p>Declines ({@link Optional#empty()}) when any of the following holds:
 * <ul>
 *   <li>{@link Meridian#getConfig()} has not loaded yet.</li>
 *   <li>Either slot is empty.</li>
 *   <li>Right slot is not an Extraction Tome.</li>
 *   <li>Left stack carries any enchantments (extraction path owns it).</li>
 *   <li>Left stack is not damageable, or its damage is already zero.</li>
 *   <li>The configured repair percent rounds down to zero for the left's max durability (avoids
 *       burning a tome for a no-op tick on trivially small-durability items).</li>
 * </ul>
 */
public final class ExtractionTomeFuelSlotRepairHandler implements AnvilHandler {

    private static final int TOME_CONSUMED = 1;

    private final Supplier<MeridianConfig> configSupplier;

    /** Production constructor — reads the live {@link Meridian#getConfig()} at claim time. */
    public ExtractionTomeFuelSlotRepairHandler() {
        this(Meridian::getConfig);
    }

    /** Test constructor — lets fixtures inject a specific config without mutating the singleton. */
    ExtractionTomeFuelSlotRepairHandler(Supplier<MeridianConfig> configSupplier) {
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
        if (!existing.isEmpty()) {
            return Optional.empty();
        }

        if (!left.isDamageableItem() || left.getDamageValue() <= 0) {
            return Optional.empty();
        }

        int restored = repairAmount(left.getMaxDamage(), config.tomes.extractionTomeRepairPercent);
        if (restored <= 0) {
            return Optional.empty();
        }

        ItemStack repaired = left.copy();
        repaired.setDamageValue(Math.max(0, left.getDamageValue() - restored));

        return Optional.of(new AnvilResult(
                repaired,
                config.tomes.extractionTomeXpCost,
                TOME_CONSUMED));
    }

    /**
     * Durability points restored per claim: {@code floor(maxDamage * repairPercent)}, clamped to
     * zero on non-positive inputs. Package-private so the unit tests can assert the rounding
     * contract directly without having to round-trip through an {@link ItemStack}.
     */
    static int repairAmount(int maxDamage, double repairPercent) {
        if (maxDamage <= 0 || repairPercent <= 0.0) {
            return 0;
        }
        return (int) Math.floor(maxDamage * repairPercent);
    }
}
