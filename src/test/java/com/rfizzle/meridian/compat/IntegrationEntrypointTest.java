package com.rfizzle.meridian.compat;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * T-7.1-T1 / T-7.2-T1 / T-7.3-T1 / T-7.4-T1: verifies integration entrypoint wiring is
 * consistent between {@code fabric.mod.json} declarations and compiled source files. EMI/REI/JEI
 * plugins live in the client source set ({@code src/client/java/}), Jade in main.
 */
class IntegrationEntrypointTest {

    private static final Path FABRIC_MOD_JSON = Path.of("src/main/resources/fabric.mod.json");

    private static JsonObject loadFabricModJson() throws Exception {
        assertTrue(Files.exists(FABRIC_MOD_JSON), "fabric.mod.json must exist at project root");
        try (BufferedReader reader = Files.newBufferedReader(FABRIC_MOD_JSON, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    @Test
    void fabricModJson_declaresAllFourIntegrationEntrypoints() throws Exception {
        JsonObject root = loadFabricModJson();
        JsonObject entrypoints = root.getAsJsonObject("entrypoints");
        assertNotNull(entrypoints, "fabric.mod.json must have entrypoints");

        for (String key : List.of("emi", "rei_client", "jei_mod_plugin", "jade")) {
            assertTrue(entrypoints.has(key),
                    "fabric.mod.json must declare integration entrypoint: " + key);
            JsonArray classes = entrypoints.getAsJsonArray(key);
            assertFalse(classes.isEmpty(),
                    "Entrypoint " + key + " must declare at least one class");
        }
    }

    @Test
    void fabricModJson_suggestsAllFourOptionalDeps() throws Exception {
        JsonObject root = loadFabricModJson();
        assertTrue(root.has("suggests"), "fabric.mod.json must have a suggests block");
        JsonObject suggests = root.getAsJsonObject("suggests");

        for (String modId : List.of("emi", "roughlyenoughitems", "jei", "jade")) {
            assertTrue(suggests.has(modId),
                    "suggests must declare optional dep: " + modId);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "src/client/java/com/rfizzle/meridian/compat/emi/EmiEnchantingPlugin.java",
            "src/client/java/com/rfizzle/meridian/compat/rei/ReiEnchantingPlugin.java",
            "src/client/java/com/rfizzle/meridian/compat/jei/JeiEnchantingPlugin.java",
            "src/main/java/com/rfizzle/meridian/compat/jade/JadeEnchantingPlugin.java"
    })
    void integrationPlugin_sourceFileExists(String path) {
        assertTrue(Files.exists(Path.of(path)),
                "Integration plugin source must exist: " + path);
    }

    @Test
    void sharedCommonClasses_loadWithoutOptionalDeps() throws Exception {
        Class.forName("com.rfizzle.meridian.compat.common.TableCraftingDisplay");
        Class.forName("com.rfizzle.meridian.compat.common.TableCraftingDisplayExtractor");
        Class.forName("com.rfizzle.meridian.compat.common.RecipeInfoFormatter");
        Class.forName("com.rfizzle.meridian.compat.common.JadeTooltipFormatter");
    }

    @Test
    void entrypointClassNames_matchExpectedPattern() throws Exception {
        JsonObject root = loadFabricModJson();
        JsonObject entrypoints = root.getAsJsonObject("entrypoints");

        assertEntrypointClass(entrypoints, "emi",
                "com.rfizzle.meridian.compat.emi.EmiEnchantingPlugin");
        assertEntrypointClass(entrypoints, "jade",
                "com.rfizzle.meridian.compat.jade.JadeEnchantingPlugin");
    }

    private static void assertEntrypointClass(JsonObject entrypoints, String key, String expected) {
        JsonArray arr = entrypoints.getAsJsonArray(key);
        boolean found = false;
        for (JsonElement el : arr) {
            if (expected.equals(el.getAsString())) {
                found = true;
                break;
            }
        }
        assertTrue(found, key + " entrypoint must include " + expected);
    }
}
