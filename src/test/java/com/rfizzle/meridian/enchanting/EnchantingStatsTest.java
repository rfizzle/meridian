package com.rfizzle.meridian.enchanting;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class EnchantingStatsTest {

    @Test
    void zero_hasAllFieldsAtZero() {
        assertEquals(0F, EnchantingStats.ZERO.maxEterna());
        assertEquals(0F, EnchantingStats.ZERO.eterna());
        assertEquals(0F, EnchantingStats.ZERO.quanta());
        assertEquals(0F, EnchantingStats.ZERO.arcana());
        assertEquals(0F, EnchantingStats.ZERO.rectification());
        assertEquals(0, EnchantingStats.ZERO.clues());
    }

    @Test
    void codec_roundTripsFullStats() {
        EnchantingStats stats = new EnchantingStats(50F, 5F, 3F, 2F, 10F, 1);

        JsonElement encoded = EnchantingStats.CODEC.encodeStart(JsonOps.INSTANCE, stats).getOrThrow();
        EnchantingStats decoded = EnchantingStats.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();

        assertEquals(stats, decoded);
    }

    @Test
    void codec_missingFieldsDefaultToZero() {
        JsonElement empty = JsonParser.parseString("{}");

        EnchantingStats decoded = EnchantingStats.CODEC.parse(JsonOps.INSTANCE, empty).getOrThrow();

        assertEquals(EnchantingStats.ZERO, decoded);
    }

    @Test
    void codec_partialJson_fillsMissingWithZero() {
        JsonElement partial = JsonParser.parseString("{\"maxEterna\":15,\"eterna\":1}");

        EnchantingStats decoded = EnchantingStats.CODEC.parse(JsonOps.INSTANCE, partial).getOrThrow();

        assertEquals(new EnchantingStats(15F, 1F, 0F, 0F, 0F, 0), decoded);
    }

    @Test
    void codec_allowsNegativeFloats() {
        EnchantingStats stats = new EnchantingStats(-5F, -1.5F, -2F, -3F, -4F, 0);

        JsonElement encoded = EnchantingStats.CODEC.encodeStart(JsonOps.INSTANCE, stats).getOrThrow();
        EnchantingStats decoded = EnchantingStats.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();

        assertEquals(stats, decoded);
    }

    @Test
    void codec_cluesIsInteger() {
        JsonElement floatClues = JsonParser.parseString("{\"clues\":2.7}");

        EnchantingStats decoded = EnchantingStats.CODEC.parse(JsonOps.INSTANCE, floatClues).getOrThrow();

        assertEquals(2, decoded.clues());
    }

    @Test
    void recordEquality_allowsUseAsMapKey() {
        EnchantingStats a = new EnchantingStats(1F, 2F, 3F, 4F, 5F, 6);
        EnchantingStats b = new EnchantingStats(1F, 2F, 3F, 4F, 5F, 6);
        EnchantingStats c = new EnchantingStats(1F, 2F, 3F, 4F, 5F, 7);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);

        Map<EnchantingStats, String> map = new HashMap<>();
        map.put(a, "ok");
        assertEquals("ok", map.get(b));
    }

    @Test
    void zero_isSingleton() {
        assertSame(EnchantingStats.ZERO, EnchantingStats.ZERO);
        assertEquals(EnchantingStats.ZERO, new EnchantingStats(0F, 0F, 0F, 0F, 0F, 0));
    }
}
