package com.rfizzle.meridian.enchanting.audit;

/**
 * Pure classification of an enchantment's enchanting-table obtainability.
 *
 * <p>The three inputs are exactly what the client audit shell reads off each enchantment
 * {@code Holder}: membership in {@code minecraft:in_enchanting_table}, membership in
 * {@code minecraft:treasure}, and Meridian's per-enchantment {@code enabled} flag. The result
 * mirrors the tag/enabled portion of the live table gate in
 * {@code RealEnchantmentHelper#getAvailableEnchantmentResults} — the audit deliberately omits that
 * gate's per-item {@code canEnchant} and power-window checks, because it is a registry-wide,
 * item-agnostic scan rather than a roll against a specific stack.
 *
 * <p>The {@code enabled} flag only decides the outcome for a table-tagged enchantment, since that is
 * the only case where it is the deciding factor: a treasure-only or untagged enchantment is barred
 * from the table by its tags regardless, so those statuses outrank the flag and a disabled
 * treasure-only enchantment still reports as {@link TableStatus#TREASURE}.
 */
public final class EnchantmentAudit {

    private EnchantmentAudit() {
    }

    /**
     * Classifies an enchantment from its table-tag membership, treasure-tag membership, and enabled
     * flag. An enchantment in the table tag is {@link TableStatus#OBTAINABLE} when enabled and
     * {@link TableStatus#DISABLED} when not; one outside the table tag is {@link TableStatus#TREASURE}
     * when it is a treasure enchantment and {@link TableStatus#NO_TAG} otherwise.
     */
    public static TableStatus classify(boolean inTable, boolean isTreasure, boolean enabled) {
        if (inTable) {
            return enabled ? TableStatus.OBTAINABLE : TableStatus.DISABLED;
        }
        return isTreasure ? TableStatus.TREASURE : TableStatus.NO_TAG;
    }
}
