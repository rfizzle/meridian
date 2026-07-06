package com.rfizzle.meridian.anvil;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.config.MeridianConfig;
import com.rfizzle.meridian.item.TemperedCoreItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Unbreakable;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Applies a Tempered Core to the left stack, marking it permanently unbreakable via
 * {@code minecraft:unbreakable}. The target keeps its item type, enchantments, name, and
 * every other component — only durability semantics change: remaining damage is healed to
 * full and the item stops taking damage entirely. Any Mending or Unbreaking on the item
 * simply goes inert; nothing is stripped or refunded.
 *
 * <p>The handler declines the pair — leaving vanilla and the rest of the dispatcher chain
 * free to handle it — when any of the following is true:
 * <ul>
 *   <li>Either slot is empty.</li>
 *   <li>Right slot is not a Tempered Core.</li>
 *   <li>Left stack is already unbreakable (one core per item, no double-dipping).</li>
 *   <li>Left stack has no durability to protect (not a damageable item).</li>
 * </ul>
 *
 * <p>Exactly one core is consumed per click regardless of stack size. XP cost comes from
 * {@code config.anvil.temperedCoreLevelCost} — the real gate is obtaining the core.
 */
public final class TemperedCoreHandler implements AnvilHandler {

    private final Supplier<MeridianConfig> configSupplier;

    /** Production constructor — reads the live {@link Meridian#getConfig()} at claim time. */
    public TemperedCoreHandler() {
        this(Meridian::getConfig);
    }

    /** Test constructor — lets fixtures inject a specific config without mutating the singleton. */
    TemperedCoreHandler(Supplier<MeridianConfig> configSupplier) {
        this.configSupplier = configSupplier;
    }

    @Override
    public Optional<AnvilResult> handle(ItemStack left, ItemStack right, Player player) {
        MeridianConfig config = configSupplier.get();
        if (config == null) {
            return Optional.empty();
        }
        if (!config.anvil.temperedCoreEnabled) {
            return Optional.empty();
        }
        if (left == null || right == null || left.isEmpty() || right.isEmpty()) {
            return Optional.empty();
        }
        if (!(right.getItem() instanceof TemperedCoreItem)) {
            return Optional.empty();
        }
        if (left.has(DataComponents.UNBREAKABLE)) {
            return Optional.empty();
        }
        if (!left.isDamageableItem()) {
            return Optional.empty();
        }

        ItemStack output = left.copyWithCount(1);
        output.setDamageValue(0);
        output.set(DataComponents.UNBREAKABLE, new Unbreakable(true));
        return Optional.of(new AnvilResult(output, config.anvil.temperedCoreLevelCost, 1));
    }
}
