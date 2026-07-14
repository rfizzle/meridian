// Tier: 1 (pure JUnit — no Minecraft types)
package com.rfizzle.meridian.enchanting.audit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnchantmentAuditTest {

    @Test
    void inTableAndEnabled_isObtainable() {
        assertEquals(TableStatus.OBTAINABLE, EnchantmentAudit.classify(true, false, true));
        assertTrue(EnchantmentAudit.classify(true, false, true).isTableObtainable());
    }

    @Test
    void inTableButDisabled_isDisabled() {
        // A config-disabled enchantment is reported as not table-obtainable (acceptance #3).
        TableStatus status = EnchantmentAudit.classify(true, false, false);
        assertEquals(TableStatus.DISABLED, status);
        assertFalse(status.isTableObtainable());
    }

    @Test
    void notInTableButTreasure_isTreasure() {
        TableStatus status = EnchantmentAudit.classify(false, true, true);
        assertEquals(TableStatus.TREASURE, status);
        assertFalse(status.isTableObtainable());
    }

    @Test
    void notInAnyTag_isNoTag() {
        TableStatus status = EnchantmentAudit.classify(false, false, true);
        assertEquals(TableStatus.NO_TAG, status);
        assertFalse(status.isTableObtainable());
    }

    @Test
    void tableTagWins_overTreasureTag() {
        // Membership in the table tag decides obtainability even when treasure is also set.
        assertEquals(TableStatus.OBTAINABLE, EnchantmentAudit.classify(true, true, true));
        assertEquals(TableStatus.DISABLED, EnchantmentAudit.classify(true, true, false));
    }

    @Test
    void enabledFlagIrrelevant_whenOutsideTableTag() {
        // Outside the table tag the enabled flag cannot make it obtainable — treasure/no-tag stands.
        assertEquals(TableStatus.TREASURE, EnchantmentAudit.classify(false, true, false));
        assertEquals(TableStatus.NO_TAG, EnchantmentAudit.classify(false, false, false));
    }
}
