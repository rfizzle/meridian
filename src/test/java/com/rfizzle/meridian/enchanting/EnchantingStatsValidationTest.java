package com.rfizzle.meridian.enchanting;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.rfizzle.meridian.api.StatCollection;
import net.minecraft.core.BlockPos;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    // --- #84: Zenith documented setups reproduce their stat totals ---

    private static final TagKey<Block> BASIC_SKULLS = TagKey.create(
            Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("meridian", "basic_skulls"));

    /** Deepshelf of Arcane Treasures (treasure_shelf): code-driven +10% arcana / −10% quanta. */
    private static final EnchantingStats TREASURE = new EnchantingStats(0F, 0F, -10F, 10F, 0F, 0);

    private static EnchantingStatRegistry loadedRegistry() throws Exception {
        EnchantingStatRegistry reg = new EnchantingStatRegistry();
        for (Path file : jsonFiles()) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                reg.parseAndRegister(
                        ResourceLocation.fromNamespaceAndPath(
                                "meridian", file.getFileName().toString().replace(".json", "")),
                        JsonParser.parseReader(reader));
            }
        }
        return reg;
    }

    /** Resolves a shipped block-keyed shelf to its JSON-defined per-shelf stats. */
    private static EnchantingStats shelf(EnchantingStatRegistry reg, String name) {
        return reg.resolveWith(
                ResourceLocation.fromNamespaceAndPath("meridian", name), tag -> false);
    }

    /** A candle block holding {@code candles} candles → +1.25% arcana each (matches CandleBlockMixin). */
    private static EnchantingStats candleBlock(int candles) {
        return new EnchantingStats(0F, 0F, 0F, 1.25F * candles, 0F, 0);
    }

    /**
     * Runs a setup's per-shelf contributions through the real raw aggregation + the table baselines,
     * exactly as {@link MeridianEnchantmentMenu#recompute} does. {@code itemEnchantability} is 0 to
     * match Zenith's documented totals ("tables start with 0 arcana"); a real item adds {@code ench/2}.
     */
    private static StatCollection compute(EnchantingStatRegistry reg, List<EnchantingStats> shelves) {
        Map<BlockPos, EnchantingStats> byPos = new HashMap<>();
        List<BlockPos> offsets = new ArrayList<>();
        for (int i = 0; i < shelves.size(); i++) {
            BlockPos pos = new BlockPos(i, 0, 0);
            offsets.add(pos);
            byPos.put(pos, shelves.get(i));
        }
        StatCollection raw = reg.rawStatsFromOffsets(offsets, byPos::get, pos -> true, pos -> null);
        return raw.applyBaselines(0);
    }

    private static List<EnchantingStats> repeat(EnchantingStats stats, int n) {
        List<EnchantingStats> out = new ArrayList<>();
        for (int i = 0; i < n; i++) out.add(stats);
        return out;
    }

    @Test
    void setup_libraryOfAlexandria_hitsDocumentedTotals() throws Exception {
        EnchantingStatRegistry reg = loadedRegistry();
        List<EnchantingStats> shelves = new ArrayList<>();
        shelves.add(shelf(reg, "draconic_endshelf"));
        shelves.addAll(repeat(TREASURE, 2));
        shelves.addAll(repeat(shelf(reg, "echoing_sculkshelf"), 4));
        shelves.addAll(repeat(shelf(reg, "pearl_endshelf"), 4));

        StatCollection s = compute(reg, shelves);
        assertEquals(50F, s.eterna(), 0.0001F, "Eterna");
        assertEquals(45F, s.quanta(), 0.0001F, "Quanta (low end of Zenith's 45–50)");
        assertEquals(100F, s.arcana(), 0.0001F, "Arcana");
        assertEquals(0F, s.rectification(), 0.0001F, "Rectification");
        assertEquals(5, s.clues(), "Clues (4 echoing + 1 table base)");
    }

    @Test
    void setup_maxValues_hitsDocumentedTotals() throws Exception {
        EnchantingStatRegistry reg = loadedRegistry();
        List<EnchantingStats> shelves = new ArrayList<>();
        shelves.add(shelf(reg, "draconic_endshelf"));
        shelves.addAll(repeat(shelf(reg, "rectifier_t3"), 3));
        shelves.addAll(repeat(shelf(reg, "soul_touched_sculkshelf"), 5));
        shelves.addAll(repeat(shelf(reg, "echoing_sculkshelf"), 4));
        shelves.add(TREASURE);
        shelves.add(candleBlock(4)); // 4 candles → +5% arcana

        StatCollection s = compute(reg, shelves);
        assertEquals(50F, s.eterna(), 0.0001F, "Eterna");
        assertEquals(100F, s.quanta(), 0.0001F, "Quanta");
        assertEquals(100F, s.arcana(), 0.0001F, "Arcana");
        assertEquals(100F, s.rectification(), 0.0001F, "Rectification");
        assertEquals(5, s.clues(), "Clues (4 echoing + 1 table base)");
    }

    @Test
    void setup_potionCharm_hitsDocumentedTotals() throws Exception {
        EnchantingStatRegistry reg = loadedRegistry();
        EnchantingStats skull = reg.resolveWith(
                ResourceLocation.fromNamespaceAndPath("minecraft", "skeleton_skull"),
                tag -> tag.equals(BASIC_SKULLS));
        assertEquals(5F, skull.quanta(), 0.0001F, "skeleton skull resolves via basic_skulls tag → 5% quanta");

        List<EnchantingStats> shelves = new ArrayList<>();
        shelves.addAll(repeat(shelf(reg, "draconic_endshelf"), 5));
        shelves.add(TREASURE);
        shelves.add(skull);
        shelves.addAll(repeat(candleBlock(4), 5)); // 20 candles → +25% arcana

        StatCollection s = compute(reg, shelves);
        assertEquals(50F, s.eterna(), 0.0001F, "Eterna");
        // raw quanta = -10 (treasure) + 5 (skull) = -5; +15 base = 10 (NOT 15 — the fix for Gap C)
        assertEquals(10F, s.quanta(), 0.0001F, "Quanta in the 8.5–13.5% infusion window");
        assertEquals(35F, s.arcana(), 0.0001F, "Arcana in the 32.5–37.5% infusion window");
        assertEquals(1, s.clues(), "Clues (table base only)");
    }

    @Test
    void setup_potionCharm_echoingDeepshelfVariant_hitsDocumentedTotals() throws Exception {
        EnchantingStatRegistry reg = loadedRegistry();
        EnchantingStats skull = reg.resolveWith(
                ResourceLocation.fromNamespaceAndPath("minecraft", "skeleton_skull"),
                tag -> tag.equals(BASIC_SKULLS));

        List<EnchantingStats> shelves = new ArrayList<>();
        shelves.addAll(repeat(shelf(reg, "draconic_endshelf"), 5));
        shelves.add(TREASURE);
        shelves.add(skull);
        shelves.add(shelf(reg, "echoing_deepshelf")); // +15% arcana
        shelves.add(candleBlock(4));
        shelves.add(candleBlock(2)); // 6 candles → +7.5% arcana

        StatCollection s = compute(reg, shelves);
        assertEquals(50F, s.eterna(), 0.0001F, "Eterna");
        assertEquals(10F, s.quanta(), 0.0001F, "Quanta in the 8.5–13.5% infusion window");
        assertEquals(32.5F, s.arcana(), 0.0001F, "Arcana at the low end of the 32.5–37.5% window");
        assertEquals(1, s.clues(), "Clues (table base only)");
    }
}
