package com.rfizzle.meridian;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ModBootTest {
    @Test
    void modId_matchesExpected() {
        assertEquals("meridian", Meridian.MOD_ID);
    }

    @Test
    void logger_isInitialized() {
        assertNotNull(Meridian.LOGGER);
    }

    @Test
    void id_producesNamespacedResourceLocation() {
        assertEquals("meridian", Meridian.id("test").getNamespace());
        assertEquals("test", Meridian.id("test").getPath());
    }
}
