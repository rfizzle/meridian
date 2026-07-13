// Tier: 1 (pure JUnit)
package com.rfizzle.meridian.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rfizzle.meridian.enchanting.RealEnchantmentHelper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validates the committed {@code meridian:rarity/*} tag output against the enchantment
 * definitions: every non-curse enchantment appears in exactly one rarity tag, the tag matches
 * its weight bucket per {@link RealEnchantmentHelper#rarityBucket(int)}, and curses appear in
 * none. Guards the published cross-mod contract (issue #140) — a new enchantment JSON with a
 * stale datagen run fails here (and in {@code verifyDatagenIdempotent}).
 */
class RarityTagPartitionTest {

    private static final Path ENCHANTMENT_DIR = Path.of(
            "src/main/resources/data/meridian/enchantment");

    private static final Path RARITY_TAGS_DIR = Path.of(
            "src/main/generated/data/meridian/tags/enchantment/rarity");

    private static final List<String> RARITY_ORDER = List.of(
            "common", "uncommon", "rare", "very_rare");

    private static final Set<String> CURSES = Set.of(
            "meridian:curse_of_decay", "meridian:curse_of_sealing",
            "meridian:curse_of_echoes", "meridian:curse_of_hunger",
            "meridian:curse_of_attraction", "meridian:curse_of_leaden",
            "meridian:curse_of_blunting", "meridian:curse_of_fumbling",
            "meridian:curse_of_wavering", "meridian:curse_of_timidity",
            "meridian:curse_of_molting", "meridian:curse_of_skittishness",
            "meridian:curse_of_obscurity", "meridian:curse_of_toll",
            "meridian:curse_of_dissonance", "meridian:curse_of_waterlogging");

    @Test
    void rarityTagsPartitionTheNonCurseCatalogByWeightBucket() throws IOException {
        Map<String, Integer> weightsById = loadEnchantmentWeights();
        assertFalse(weightsById.isEmpty(), "no enchantment definitions found");

        Map<String, Set<String>> tagMembers = new HashMap<>();
        for (String rarity : RARITY_ORDER) {
            tagMembers.put(rarity, loadTagMembers(RARITY_TAGS_DIR.resolve(rarity + ".json")));
        }

        Set<String> seen = new HashSet<>();
        for (Map.Entry<String, Set<String>> tag : tagMembers.entrySet()) {
            for (String id : tag.getValue()) {
                assertTrue(seen.add(id), id + " appears in more than one rarity tag");
                assertFalse(CURSES.contains(id), id + " is a curse and must not carry a rarity");
                Integer weight = weightsById.get(id);
                assertTrue(weight != null, tag.getKey() + " contains unknown enchantment " + id);
                assertEquals(RARITY_ORDER.get(RealEnchantmentHelper.rarityBucket(weight)),
                        tag.getKey(), id + " (weight " + weight + ") is in the wrong rarity tag");
            }
        }

        Set<String> expected = new HashSet<>(weightsById.keySet());
        expected.removeAll(CURSES);
        assertEquals(expected, seen,
                "rarity tags must cover the full non-curse enchantment catalog exactly once");
    }

    private static Map<String, Integer> loadEnchantmentWeights() throws IOException {
        Map<String, Integer> weights = new TreeMap<>();
        try (Stream<Path> files = Files.list(ENCHANTMENT_DIR)) {
            for (Path file : files.filter(f -> f.getFileName().toString().endsWith(".json")).toList()) {
                String name = file.getFileName().toString();
                JsonObject json = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
                weights.put("meridian:" + name.substring(0, name.length() - ".json".length()),
                        json.getAsJsonPrimitive("weight").getAsInt());
            }
        }
        return weights;
    }

    private static Set<String> loadTagMembers(Path tagFile) throws IOException {
        assertTrue(Files.isRegularFile(tagFile), "missing generated rarity tag: " + tagFile);
        JsonObject json = JsonParser.parseString(Files.readString(tagFile)).getAsJsonObject();
        Set<String> members = new HashSet<>();
        for (JsonElement value : json.getAsJsonArray("values")) {
            members.add(value.isJsonObject()
                    ? value.getAsJsonObject().getAsJsonPrimitive("id").getAsString()
                    : value.getAsString());
        }
        return members;
    }
}
