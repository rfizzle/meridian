package com.rfizzle.meridian.tome;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * T-5.1.3 — proves the hand-shipped {@code scrap_tome.json} recipe parses into the 1.21.1 shaped-
 * recipe shape (using the new {@code id} result field, not the legacy {@code item}) and matches
 * Zenith's BBB/BAB/BBB book-and-anvil pattern with a count of 8. The Improved Scrap and Extraction
 * tomes intentionally stay on the {@code meridian:enchanting} custom recipe type already
 * shipped in T-4.6.4 — only the entry-tier scrap tome is reachable through a vanilla crafting grid.
 */
class ScrapTomeRecipeTest {

    private static final String RECIPE_PATH =
            "/data/meridian/recipe/scrap_tome.json";

    @Test
    void recipeJson_parsesAsShapedWithExpectedPatternAndResult() throws Exception {
        JsonObject recipe = loadRecipe();

        assertEquals("minecraft:crafting_shaped", recipe.get("type").getAsString(),
                "scrap_tome must ship as a vanilla shaped recipe so no custom recipe type is needed");

        var pattern = recipe.getAsJsonArray("pattern");
        assertEquals(3, pattern.size(), "pattern is 3x3");
        assertEquals("BBB", pattern.get(0).getAsString());
        assertEquals("BAB", pattern.get(1).getAsString());
        assertEquals("BBB", pattern.get(2).getAsString());

        JsonObject key = recipe.getAsJsonObject("key");
        assertEquals("minecraft:book",
                key.getAsJsonObject("B").get("item").getAsString(),
                "B must resolve to vanilla book (Zenith parity)");
        assertEquals("minecraft:anvil",
                key.getAsJsonObject("A").get("item").getAsString(),
                "A must resolve to vanilla anvil (Zenith parity)");

        JsonObject result = recipe.getAsJsonObject("result");
        // 1.21.1 renamed the field from "item" to "id" in recipe results — hand-shipped JSON
        // must match the new shape or it won't parse under the data loader.
        assertTrue(result.has("id"),
                "result must use the 1.21.1 `id` field (not legacy `item`) or the data loader rejects it");
        assertEquals("meridian:scrap_tome", result.get("id").getAsString(),
                "result id must point at our registered scrap tome item");
        assertEquals(8, result.get("count").getAsInt(),
                "Zenith parity: 8 books + 1 anvil yields 8 scrap tomes per craft");
    }

    private static JsonObject loadRecipe() throws Exception {
        try (InputStream in = ScrapTomeRecipeTest.class.getResourceAsStream(RECIPE_PATH)) {
            if (in == null) {
                fail("recipe JSON missing at " + RECIPE_PATH + " — hand-shipped resource should be on the classpath");
            }
            assertNotNull(in);
            try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        }
    }
}
