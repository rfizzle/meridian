package com.rfizzle.meridian.enchanting.audit;

/**
 * How an enchantment stands relative to Meridian's enchanting-table obtainability gate.
 *
 * <p>Derived purely from three facts read off the enchantment's {@code Holder} — membership in
 * {@code minecraft:in_enchanting_table}, membership in {@code minecraft:treasure}, and whether
 * Meridian's {@code EnchantmentInfoRegistry} has it enabled — by
 * {@link EnchantmentAudit#classify(boolean, boolean, boolean)}. The mapping mirrors the tag/enabled
 * portion of the live table gate in {@code RealEnchantmentHelper#getAvailableEnchantmentResults};
 * keep the two in step if that gate changes.
 */
public enum TableStatus {

    /** In {@code minecraft:in_enchanting_table} and enabled — rolls at Meridian's table. */
    OBTAINABLE,

    /** In {@code minecraft:in_enchanting_table} but disabled via config — cannot roll. */
    DISABLED,

    /** Not in the table tag but in {@code minecraft:treasure} — excluded from the table by design. */
    TREASURE,

    /** In neither obtainability tag — can never roll at the table. The compatibility red flag. */
    NO_TAG;

    /** Whether an enchantment with this status can roll at Meridian's enchanting table. */
    public boolean isTableObtainable() {
        return this == OBTAINABLE;
    }
}
