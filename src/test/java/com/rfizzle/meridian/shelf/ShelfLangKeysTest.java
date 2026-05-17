package com.rfizzle.meridian.shelf;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * T-3.2.4 coverage: confirms {@code assets/meridian/lang/en_us.json} parses as valid
 * JSON and carries a {@code block.meridian.<id>} entry with a non-blank English label
 * for every shelf registered by {@link MeridianShelves}. The source-of-truth ID list mirrors the
 * 25-shelf roster from {@code MeridianShelvesTest} — keep the two in lockstep.
 */
class ShelfLangKeysTest {

    private static final String LANG_RESOURCE = "/assets/meridian/lang/en_us.json";

    private static final List<String> EXPECTED_SHELF_IDS = List.of(
            "beeshelf", "melonshelf",
            "stoneshelf",
            "hellshelf", "blazing_hellshelf", "glowing_hellshelf", "infused_hellshelf",
            "seashelf", "heart_seashelf", "crystal_seashelf", "infused_seashelf",
            "endshelf", "pearl_endshelf", "draconic_endshelf",
            "deepshelf", "dormant_deepshelf", "echoing_deepshelf", "soul_touched_deepshelf",
            "echoing_sculkshelf", "soul_touched_sculkshelf",
            "sightshelf", "sightshelf_t2",
            "rectifier", "rectifier_t2", "rectifier_t3");

    private static JsonObject lang;

    @BeforeAll
    static void loadLangFile() throws Exception {
        try (InputStream in = ShelfLangKeysTest.class.getResourceAsStream(LANG_RESOURCE)) {
            assertNotNull(in, "en_us.json must be on the test classpath");
            lang = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8))
                    .getAsJsonObject();
        }
    }

    @TestFactory
    Stream<DynamicTest> everyShelf_hasNonBlankLangKey() {
        return EXPECTED_SHELF_IDS.stream().map(id -> DynamicTest.dynamicTest(id, () -> {
            String key = "block.meridian." + id;
            assertTrue(lang.has(key), () -> "missing lang key: " + key);
            String value = lang.get(key).getAsString();
            assertFalse(value.isBlank(), () -> "lang value for " + key + " must not be blank");
        }));
    }
}
