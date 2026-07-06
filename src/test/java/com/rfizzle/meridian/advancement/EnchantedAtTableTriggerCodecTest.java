// Tier: 2 (fabric-loader-junit)
package com.rfizzle.meridian.advancement;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.SharedConstants;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tier-2 coverage for the {@code clues} bound added to {@link EnchantedAtTableTrigger.TriggerInstance}
 * so {@code high_clues} can ride the same trigger as {@code high_rectification} and the existing
 * Arcana/Quanta pair. Confirms the field defaults to {@link MinMaxBounds.Doubles#ANY} when absent
 * (so the 15 legacy criteria that never mention it keep parsing) and survives an encode→decode
 * round-trip when present.
 */
class EnchantedAtTableTriggerCodecTest {

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void absentCluesFieldDefaultsToAny() {
        JsonObject json = new JsonObject();
        JsonObject arcana = new JsonObject();
        arcana.addProperty("min", 60.0);
        json.add("arcana", arcana);

        DataResult<EnchantedAtTableTrigger.TriggerInstance> result =
                EnchantedAtTableTrigger.TriggerInstance.CODEC.parse(JsonOps.INSTANCE, json);
        EnchantedAtTableTrigger.TriggerInstance inst = result.getOrThrow();

        assertEquals(MinMaxBounds.Doubles.ANY, inst.clues(),
                "clues must default to ANY when the JSON omits it");
    }

    @Test
    void explicitCluesBoundSurvivesRoundTrip() {
        JsonObject json = new JsonObject();
        JsonObject clues = new JsonObject();
        clues.addProperty("min", 8.0);
        json.add("clues", clues);

        EnchantedAtTableTrigger.TriggerInstance decoded =
                EnchantedAtTableTrigger.TriggerInstance.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();

        JsonObject reencoded = EnchantedAtTableTrigger.TriggerInstance.CODEC
                .encodeStart(JsonOps.INSTANCE, decoded).getOrThrow().getAsJsonObject();

        assertEquals(JsonParser.parseString("{\"min\":8.0}"), reencoded.get("clues"),
                "clues bound must re-encode to the same JSON");

        // The bound matches a value at/above the threshold and rejects one below it.
        assertTrue(decoded.clues().matches(8.0f));
        assertTrue(decoded.clues().matches(20.0f));
        assertEquals(false, decoded.clues().matches(7.0f));
    }
}
