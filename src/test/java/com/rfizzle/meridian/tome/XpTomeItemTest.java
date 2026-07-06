package com.rfizzle.meridian.tome;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tier-1 coverage for {@link XpTomeItem#clampDebit(int, int)} — the pure clamp that decides how
 * much a single tome contributes toward a level deficit (#162). Inventory-touching helpers
 * ({@code inventoryBalance}, {@code debitInventory}) are covered by gametests.
 */
class XpTomeItemTest {

    @Test
    void clampDebit_fullDebit_whenStoredCoversRequest() {
        assertEquals(3, XpTomeItem.clampDebit(5, 3));
    }

    @Test
    void clampDebit_clampsToStored_whenRequestExceedsBalance() {
        assertEquals(2, XpTomeItem.clampDebit(2, 5));
    }

    @Test
    void clampDebit_exactBalance_debitsAll() {
        assertEquals(4, XpTomeItem.clampDebit(4, 4));
    }

    @Test
    void clampDebit_zeroStored_isNoOp() {
        assertEquals(0, XpTomeItem.clampDebit(0, 5));
    }

    @Test
    void clampDebit_zeroRequest_isNoOp() {
        assertEquals(0, XpTomeItem.clampDebit(5, 0));
    }

    @Test
    void clampDebit_negativeInputs_neverGoNegative() {
        assertEquals(0, XpTomeItem.clampDebit(-1, 5));
        assertEquals(0, XpTomeItem.clampDebit(5, -1));
    }
}
