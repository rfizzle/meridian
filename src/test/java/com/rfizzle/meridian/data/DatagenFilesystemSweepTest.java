package com.rfizzle.meridian.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatagenFilesystemSweepTest {

    private static final Path GENERATED_DIR = Path.of(
            System.getProperty("meridian.generated.dir", "src/main/generated"));

    private static final Path BLOCKSTATES =
            GENERATED_DIR.resolve("assets/meridian/blockstates");
    private static final Path LOOT_TABLES =
            GENERATED_DIR.resolve("data/meridian/loot_table/blocks");
    private static final Path RECIPES =
            GENERATED_DIR.resolve("data/meridian/recipe");
    private static final Path ADVANCEMENTS =
            GENERATED_DIR.resolve("data/meridian/advancement/recipes/building_blocks");

    private static final List<String> SHELF_IDS = List.of(
            "beeshelf", "melonshelf", "stoneshelf",
            "hellshelf", "blazing_hellshelf", "glowing_hellshelf", "infused_hellshelf",
            "seashelf", "heart_seashelf", "crystal_seashelf", "infused_seashelf",
            "endshelf", "pearl_endshelf", "draconic_endshelf",
            "deepshelf", "dormant_deepshelf", "echoing_deepshelf", "soul_touched_deepshelf",
            "echoing_sculkshelf", "soul_touched_sculkshelf",
            "sightshelf", "sightshelf_t2",
            "rectifier", "rectifier_t2", "rectifier_t3");

    private static final List<String> NON_SHELF_BLOCK_IDS = List.of(
            "filtering_shelf", "treasure_shelf", "library", "ender_library");

    private static final List<String> ALL_BLOCK_IDS = Stream.concat(
            SHELF_IDS.stream(), NON_SHELF_BLOCK_IDS.stream()).toList();

    private static final List<String> GENERATED_RECIPE_IDS = List.of(
            "beeshelf", "melonshelf", "stoneshelf",
            "hellshelf", "blazing_hellshelf", "glowing_hellshelf",
            "seashelf", "heart_seashelf", "crystal_seashelf",
            "endshelf", "pearl_endshelf", "draconic_endshelf",
            "dormant_deepshelf", "echoing_deepshelf", "soul_touched_deepshelf",
            "echoing_sculkshelf", "soul_touched_sculkshelf",
            "filtering_shelf", "treasure_shelf",
            "sightshelf", "sightshelf_t2",
            "rectifier", "rectifier_t2", "rectifier_t3");

    private static JsonObject parseJson(Path file) throws Exception {
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    // --- Blockstates: one per shelf ---

    @TestFactory
    Stream<DynamicTest> everyShelf_hasBlockstateJson() {
        return SHELF_IDS.stream().map(id -> DynamicTest.dynamicTest(id, () ->
                assertTrue(Files.isRegularFile(BLOCKSTATES.resolve(id + ".json")),
                        "missing blockstate: " + id + ".json")));
    }

    @Test
    void blockstateCount_matchesShelfCount() throws Exception {
        long count = Files.list(BLOCKSTATES)
                .filter(p -> p.toString().endsWith(".json")).count();
        assertEquals(SHELF_IDS.size(), count,
                "blockstate count should equal shelf count (" + SHELF_IDS.size() + ")");
    }

    // --- Loot tables: one per registered block ---

    @TestFactory
    Stream<DynamicTest> everyBlock_hasLootTable() {
        return ALL_BLOCK_IDS.stream().map(id -> DynamicTest.dynamicTest(id, () ->
                assertTrue(Files.isRegularFile(LOOT_TABLES.resolve(id + ".json")),
                        "missing loot table: " + id + ".json")));
    }

    @TestFactory
    Stream<DynamicTest> everyLootTable_hasItemPool() {
        return ALL_BLOCK_IDS.stream().map(id -> DynamicTest.dynamicTest(id, () -> {
            JsonObject root = parseJson(LOOT_TABLES.resolve(id + ".json"));
            assertEquals("minecraft:block", root.get("type").getAsString());
            JsonArray pools = root.getAsJsonArray("pools");
            assertNotNull(pools, "loot table must have pools array");
            assertFalse(pools.isEmpty(), "pools must not be empty");
            boolean hasItemEntry = false;
            for (JsonElement pool : pools) {
                for (JsonElement entry : pool.getAsJsonObject().getAsJsonArray("entries")) {
                    if ("minecraft:item".equals(entry.getAsJsonObject().get("type").getAsString())) {
                        hasItemEntry = true;
                    }
                }
            }
            assertTrue(hasItemEntry, "loot table must contain at least one minecraft:item entry");
        }));
    }

    @Test
    void lootTableCount_matchesBlockCount() throws Exception {
        long count = Files.list(LOOT_TABLES)
                .filter(p -> p.toString().endsWith(".json")).count();
        assertEquals(ALL_BLOCK_IDS.size(), count,
                "loot table count should equal block count (" + ALL_BLOCK_IDS.size() + ")");
    }

    // --- Recipes: 24 generated shaped recipes ---

    @TestFactory
    Stream<DynamicTest> everyGeneratedRecipe_exists() {
        return GENERATED_RECIPE_IDS.stream().map(id -> DynamicTest.dynamicTest(id, () ->
                assertTrue(Files.isRegularFile(RECIPES.resolve(id + ".json")),
                        "missing generated recipe: " + id + ".json")));
    }

    @Test
    void generatedRecipeCount_matchesExpected() throws Exception {
        long count = Files.list(RECIPES)
                .filter(p -> Files.isRegularFile(p) && p.toString().endsWith(".json")).count();
        assertEquals(GENERATED_RECIPE_IDS.size(), count,
                "generated recipe count should be " + GENERATED_RECIPE_IDS.size());
    }

    // --- Advancements: one per generated recipe ---

    @TestFactory
    Stream<DynamicTest> everyGeneratedRecipe_hasAdvancement() {
        return GENERATED_RECIPE_IDS.stream().map(id -> DynamicTest.dynamicTest(id, () ->
                assertTrue(Files.isRegularFile(ADVANCEMENTS.resolve(id + ".json")),
                        "missing recipe advancement: " + id + ".json")));
    }

    // --- Negative: no custom-recipe-type files in generated output ---

    @Test
    void noEnchantingRecipesInGenerated() {
        Path enchanting = RECIPES.resolve("enchanting");
        assertFalse(Files.exists(enchanting),
                "enchanting/ subdirectory must not appear in generated output — "
                        + "custom recipes are hand-shipped in src/main/resources/");
    }

    @Test
    void noKeepNbtEnchantingRecipesInGenerated() {
        Path keepNbt = RECIPES.resolve("keep_nbt_enchanting");
        assertFalse(Files.exists(keepNbt),
                "keep_nbt_enchanting/ subdirectory must not appear in generated output");
    }
}
