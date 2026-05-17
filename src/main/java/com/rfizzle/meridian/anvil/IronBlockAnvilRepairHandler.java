package com.rfizzle.meridian.anvil;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.config.MeridianConfig;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Repairs a damaged or chipped anvil one tier when a block of iron is placed in slot B, matching
 * Zenith's iron-block repair behaviour but with the flat XP cost mandated by DESIGN.md
 * (§ "Anvil tweaks (MVP)" — "Cost: 1 level, 1 iron block consumed"). Damaged → chipped, chipped →
 * normal; a pristine anvil has nothing to repair so the handler declines.
 *
 * <p>Any {@link ItemEnchantments} component on the input anvil is copied onto the output stack so
 * the rare case of an enchanted anvil (picked up with Silk Touch through another mod) survives
 * the repair. All other data components are dropped on purpose — naming via the anvil UI is a
 * vanilla feature, and no other component is meaningful on a block-form anvil.
 *
 * <p>The {@code Items.IRON_BLOCK} check is an exact item match on purpose: Zenith uses the
 * {@code #c:iron_blocks} tag, but DESIGN.md and T-4.2.1 both single out "a block of iron" /
 * {@code IRON_BLOCK}. Narrowing the trigger here keeps iron **ingots**, iron nuggets, and
 * other-metal storage blocks out — the task's explicit "iron ingot in right → declines"
 * acceptance pins that.
 *
 * <p>The handler also respects {@code config.anvil.ironBlockRepairsAnvil} (T-4.2.2): operators can
 * disable iron-block repairs without unregistering the handler, and a missing config (e.g. before
 * {@link Meridian#onInitialize} has finished loading) is treated as "feature off" rather
 * than throwing.
 */
public final class IronBlockAnvilRepairHandler implements AnvilHandler {

    private static final int XP_COST_LEVELS = 1;
    private static final int IRON_CONSUMED = 1;

    private final Supplier<MeridianConfig> configSupplier;

    /** Production constructor — reads the live {@link Meridian#getConfig()} at claim time. */
    public IronBlockAnvilRepairHandler() {
        this(Meridian::getConfig);
    }

    /** Test constructor — lets fixtures inject a specific config without mutating the singleton. */
    IronBlockAnvilRepairHandler(Supplier<MeridianConfig> configSupplier) {
        this.configSupplier = configSupplier;
    }

    @Override
    public Optional<AnvilResult> handle(ItemStack left, ItemStack right, Player player) {
        MeridianConfig config = configSupplier.get();
        if (config == null || !config.anvil.ironBlockRepairsAnvil) {
            return Optional.empty();
        }
        if (left == null || right == null || left.isEmpty() || right.isEmpty()) {
            return Optional.empty();
        }
        if (!right.is(Items.IRON_BLOCK)) {
            return Optional.empty();
        }

        Item next = nextTier(left.getItem());
        if (next == null) {
            return Optional.empty();
        }

        ItemStack output = new ItemStack(next);
        ItemEnchantments enchantments = left.get(DataComponents.ENCHANTMENTS);
        if (enchantments != null && !enchantments.isEmpty()) {
            output.set(DataComponents.ENCHANTMENTS, enchantments);
        }
        return Optional.of(new AnvilResult(output, XP_COST_LEVELS, IRON_CONSUMED));
    }

    private static Item nextTier(Item item) {
        if (item == Items.DAMAGED_ANVIL) return Items.CHIPPED_ANVIL;
        if (item == Items.CHIPPED_ANVIL) return Items.ANVIL;
        return null;
    }
}
