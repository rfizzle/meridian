package com.rfizzle.meridian.anvil;

import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Claim returned by an anvil handler when it wants to replace vanilla's output for a
 * left+right slot combination. The TAIL hook in
 * {@link com.rfizzle.meridian.mixin.AnvilMenuMixin} consumes this:
 * {@code output} lands in slot 0 of {@code resultSlots}, {@code xpCost} overwrites the
 * menu's cost {@code DataSlot}, and {@code rightConsumed} becomes the menu's
 * {@code repairItemCountCost}.
 *
 * <p>{@code leftReplacement} is the stack that should occupy the left input slot after the
 * player takes the output. Most handlers leave this as {@code null} — vanilla's
 * {@code AnvilMenu#onTake} already clears the left slot, which is what Prismatic Web,
 * iron-block repair, and the two Scrap tome tiers all want (the source item is consumed).
 * The Extraction Tome (S-5.2.3) sets {@code leftReplacement} to a copy of the source item
 * with enchantments stripped and a damage tick applied; the mixin writes that stack back into
 * slot 0 at {@code onTake} tail so the player recovers the item.
 *
 * <p>The sentinel is {@code null} rather than {@link ItemStack#EMPTY} because the 3-arg
 * convenience constructor is called from unit tests that run outside a Minecraft bootstrap —
 * dereferencing {@code ItemStack.EMPTY} there would eagerly load {@code ItemStack}'s static
 * initializer and fail.
 */
public record AnvilResult(ItemStack output, int xpCost, int rightConsumed, @Nullable ItemStack leftReplacement) {

    /**
     * Shorthand for the common case: no left replacement — vanilla's slot-0 clear is correct.
     * Keeps every pre-Extraction-Tome handler call-site one line.
     */
    public AnvilResult(ItemStack output, int xpCost, int rightConsumed) {
        this(output, xpCost, rightConsumed, null);
    }
}
