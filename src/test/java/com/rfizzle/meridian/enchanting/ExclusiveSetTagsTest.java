package com.rfizzle.meridian.enchanting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validates that exclusive_set tag files are well-formed and that required entries
 * resolve to present enchantment JSONs. Entries marked {@code "required": false} are
 * skipped — those reference enchantments not yet implemented (Phase 2+).
 */
class ExclusiveSetTagsTest {

    private static final String TAGS_DIR = "/data/meridian/tags/enchantment/exclusive_set/";
    private static final String ENCHANT_DIR = "/data/meridian/enchantment/";

    private static Path tagsDir() throws Exception {
        URL url = ExclusiveSetTagsTest.class.getResource(TAGS_DIR);
        assertNotNull(url, "exclusive_set tag resource dir must be on the test classpath");
        return Paths.get(url.toURI());
    }

    private static Path enchantDir() throws Exception {
        URL url = ExclusiveSetTagsTest.class.getResource(ENCHANT_DIR);
        assertNotNull(url, "enchantment resource dir must be on the test classpath");
        return Paths.get(url.toURI());
    }

    private static List<Path> allTagFiles() throws Exception {
        try (Stream<Path> files = Files.walk(tagsDir())) {
            return files.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".json"))
                    .sorted()
                    .toList();
        }
    }

    private static Set<String> allEnchantIds() throws Exception {
        Path root = enchantDir();
        Set<String> ids = new LinkedHashSet<>();
        try (Stream<Path> files = Files.walk(root)) {
            files.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".json"))
                    .forEach(p -> {
                        String rel = root.relativize(p).toString().replace('\\', '/');
                        String base = rel.substring(0, rel.length() - ".json".length());
                        ids.add("meridian:" + base);
                    });
        }
        return ids;
    }

    private static List<String> readRequiredValues(Path file) throws Exception {
        JsonElement element;
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            element = JsonParser.parseReader(reader);
        }
        assertTrue(element.isJsonObject(), file.getFileName() + " must be a JSON object");
        JsonObject obj = element.getAsJsonObject();
        assertTrue(obj.has("values") && obj.get("values").isJsonArray(),
                file.getFileName() + " must have a \"values\" array");
        JsonArray arr = obj.getAsJsonArray("values");
        List<String> out = new ArrayList<>(arr.size());
        for (JsonElement e : arr) {
            if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isString()) {
                out.add(e.getAsString());
            } else if (e.isJsonObject() && e.getAsJsonObject().has("id")) {
                JsonObject entry = e.getAsJsonObject();
                if (entry.has("required") && !entry.get("required").getAsBoolean()) {
                    continue;
                }
                out.add(entry.get("id").getAsString());
            } else {
                throw new AssertionError(file.getFileName() + " entry must be a string or {\"id\":...} object: " + e);
            }
        }
        return out;
    }

    @Test
    void tagDir_isNonEmpty() throws Exception {
        assertFalse(allTagFiles().isEmpty(),
                "expected exclusive_set tags under " + TAGS_DIR);
    }

    @Test
    void everyRequiredEntry_resolvesToPresentEnchant() throws Exception {
        Set<String> present = allEnchantIds();
        List<String> unresolved = new ArrayList<>();
        for (Path file : allTagFiles()) {
            for (String entry : readRequiredValues(file)) {
                if (entry.startsWith("minecraft:")) continue;
                if (entry.startsWith("meridian:")) {
                    if (!present.contains(entry)) {
                        unresolved.add(file.getFileName() + " -> " + entry);
                    }
                    continue;
                }
                unresolved.add(file.getFileName() + " -> " + entry + " (unknown namespace)");
            }
        }
        assertTrue(unresolved.isEmpty(),
                "exclusive_set entries must resolve to present or vanilla enchants: " + unresolved);
    }

    @TestFactory
    Stream<DynamicTest> everyTagFile_parsesAndHasValues() throws Exception {
        Path root = tagsDir();
        return allTagFiles().stream()
                .map(file -> DynamicTest.dynamicTest(
                        root.relativize(file).toString().replace('\\', '/'),
                        () -> readRequiredValues(file)));
    }
}
