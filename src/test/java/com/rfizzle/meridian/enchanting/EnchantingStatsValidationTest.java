package com.rfizzle.meridian.enchanting;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * T-3.2.2 coverage: every {@code data/meridian/enchanting_stats/*.json} file shipped in
 * this mod must parse through {@link EnchantingStatRegistry.StatEntry#CODEC} without error and
 * round-trip into the in-memory registry. The ported Zenith files use the nested {@code stats}
 * wrapper; the pre-existing {@code vanilla_provider.json} uses the flat schema — the codec
 * accepts both.
 */
class EnchantingStatsValidationTest {

    private static final String RESOURCE_DIR = "/data/meridian/enchanting_stats/";

    private static Path resourceDir() throws Exception {
        URL url = EnchantingStatsValidationTest.class.getResource(RESOURCE_DIR);
        assertNotNull(url, "enchanting_stats resource dir must be on the test classpath");
        return Paths.get(url.toURI());
    }

    private static List<Path> jsonFiles() throws Exception {
        try (Stream<Path> files = Files.list(resourceDir())) {
            return files.filter(p -> p.getFileName().toString().endsWith(".json")).sorted().toList();
        }
    }

    @TestFactory
    Stream<DynamicTest> everyShippedFile_parsesWithoutError() throws Exception {
        return jsonFiles().stream().map(file -> DynamicTest.dynamicTest(file.getFileName().toString(), () -> {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                JsonElement json = JsonParser.parseReader(reader);
                DataResult<EnchantingStatRegistry.StatEntry> result =
                        EnchantingStatRegistry.StatEntry.CODEC.parse(JsonOps.INSTANCE, json);
                assertTrue(result.error().isEmpty(),
                        "parse failed for " + file.getFileName() + ": "
                                + result.error().map(DataResult.Error::message).orElse(""));
                EnchantingStatRegistry.StatEntry entry = result.result().orElseThrow();
                assertTrue(entry.block().isPresent() ^ entry.tag().isPresent(),
                        "entry must carry exactly one of block/tag: " + file.getFileName());
            }
        }));
    }

    @Test
    void loaderRegistersEveryShippedFile() throws Exception {
        EnchantingStatRegistry reg = new EnchantingStatRegistry();
        List<Path> files = jsonFiles();
        assertFalse(files.isEmpty(), "expected shipped stat JSONs on the classpath");

        for (Path file : files) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                JsonElement json = JsonParser.parseReader(reader);
                reg.parseAndRegister(
                        ResourceLocation.fromNamespaceAndPath(
                                "meridian", file.getFileName().toString().replace(".json", "")),
                        json);
            }
        }

        assertEquals(files.size(), reg.blockEntryCount() + reg.tagEntryCount(),
                "every shipped JSON should register as exactly one block or tag entry");
    }

    /**
     * Spot-check a handful of ported Zenith values to confirm the nested-schema path lands the right
     * numbers — protects against silent shape drift when the flexible codec is touched.
     */
    @Test
    void portedZenithValues_retainZenithNumbers() throws Exception {
        EnchantingStatRegistry reg = new EnchantingStatRegistry();
        for (Path file : jsonFiles()) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                JsonElement json = JsonParser.parseReader(reader);
                reg.parseAndRegister(
                        ResourceLocation.fromNamespaceAndPath(
                                "meridian", file.getFileName().toString().replace(".json", "")),
                        json);
            }
        }

        // beeshelf: { eterna: -15, quanta: 100 }
        Predicate<TagKey<Block>> inNoTags = tag -> false;
        EnchantingStats beeshelf = reg.resolveWith(
                ResourceLocation.fromNamespaceAndPath("meridian", "beeshelf"), inNoTags);
        assertEquals(-15F, beeshelf.eterna(), 0.0001F);
        assertEquals(100F, beeshelf.quanta(), 0.0001F);

        // draconic_endshelf: { maxEterna: 50, eterna: 10 }
        EnchantingStats draconic = reg.resolveWith(
                ResourceLocation.fromNamespaceAndPath("meridian", "draconic_endshelf"), inNoTags);
        assertEquals(50F, draconic.maxEterna(), 0.0001F);
        assertEquals(10F, draconic.eterna(), 0.0001F);

        // rectifier_t3: { rectification: 25 }
        EnchantingStats rect3 = reg.resolveWith(
                ResourceLocation.fromNamespaceAndPath("meridian", "rectifier_t3"), inNoTags);
        assertEquals(25F, rect3.rectification(), 0.0001F);

        // sightshelf_t2: { clues: 2 }
        EnchantingStats sight2 = reg.resolveWith(
                ResourceLocation.fromNamespaceAndPath("meridian", "sightshelf_t2"), inNoTags);
        assertEquals(2, sight2.clues());

        // vanilla_provider (flat schema) still resolves via its tag binding.
        TagKey<Block> powerProvider = TagKey.create(
                Registries.BLOCK, ResourceLocation.parse("minecraft:enchantment_power_provider"));
        EnchantingStats vanilla = reg.resolveWith(
                ResourceLocation.fromNamespaceAndPath("minecraft", "bookshelf"),
                tag -> tag.equals(powerProvider));
        assertEquals(15F, vanilla.maxEterna(), 0.0001F);
        assertEquals(1F, vanilla.eterna(), 0.0001F);
    }
}
