package com.rfizzle.meridian.enchanting.recipe;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Codec round-trip + sentinel sanity check for {@link StatRequirements}. */
class StatRequirementsTest {

    @Test
    void noMaxSentinel_carriesMinusOneOnAllAxes() {
        assertEquals(-1F, StatRequirements.NO_MAX.eterna());
        assertEquals(-1F, StatRequirements.NO_MAX.quanta());
        assertEquals(-1F, StatRequirements.NO_MAX.arcana());
    }

    @Test
    void codec_roundTripsThroughJsonOps() {
        StatRequirements original = new StatRequirements(22.5F, 30F, 0F);
        JsonElement json = StatRequirements.CODEC.encodeStart(JsonOps.INSTANCE, original)
                .getOrThrow();
        StatRequirements decoded = StatRequirements.CODEC.parse(JsonOps.INSTANCE, json)
                .getOrThrow();
        assertEquals(original, decoded);
    }

    @Test
    void codec_appliesDefaultsForMissingAxes() {
        // Each axis is optional and defaults to 0; matches Zenith, where leaving an axis off the
        // JSON object is equivalent to "no minimum on that stat."
        JsonElement json = JsonParser.parseString("{\"eterna\": 12}");
        StatRequirements decoded = StatRequirements.CODEC.parse(JsonOps.INSTANCE, json)
                .getOrThrow();
        assertEquals(12F, decoded.eterna());
        assertEquals(0F, decoded.quanta());
        assertEquals(0F, decoded.arcana());
    }

    @Test
    void codec_acceptsZenithStyleMaxRequirementsWithMinusOne() {
        // Zenith JSONs encode "no upper bound on this axis" by writing -1 explicitly, instead of
        // omitting the field. Both shapes must decode cleanly so ported recipe JSONs work as-is.
        JsonElement json = JsonParser.parseString("{\"eterna\": -1, \"quanta\": 25, \"arcana\": -1}");
        StatRequirements decoded = StatRequirements.CODEC.parse(JsonOps.INSTANCE, json)
                .getOrThrow();
        assertTrue(decoded.eterna() < 0);
        assertEquals(25F, decoded.quanta());
        assertTrue(decoded.arcana() < 0);
    }
}
