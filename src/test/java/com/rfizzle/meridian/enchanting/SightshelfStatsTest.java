package com.rfizzle.meridian.enchanting;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * T-3.3.1 — sightshelf and sightshelf_t2 contribute clues only. Their shipped JSONs omit
 * {@code maxEterna}, which the codec zero-fills, so placing a sightshelf beside lower-tier
 * stone shelves does not raise the enchanting ceiling by itself. Stacking two sightshelf_t2s
 * adds four raw clues; the aggregator passes them through unbounded (matching Zenith's
 * behavior where clue count has no ceiling).
 */
class SightshelfStatsTest {

    private static final String STATS_DIR = "/data/meridian/enchanting_stats/";

    private static Path resource(String filename) throws Exception {
        URL url = SightshelfStatsTest.class.getResource(STATS_DIR + filename);
        assertNotNull(url, "missing shipped JSON on test classpath: " + filename);
        return Paths.get(url.toURI());
    }

    private static EnchantingStats loadShippedStats(String filename) throws Exception {
        EnchantingStatRegistry reg = new EnchantingStatRegistry();
        try (Reader reader = Files.newBufferedReader(resource(filename), StandardCharsets.UTF_8)) {
            JsonElement json = JsonParser.parseReader(reader);
            String blockPath = filename.replace(".json", "");
            reg.parseAndRegister(
                    ResourceLocation.fromNamespaceAndPath("meridian", blockPath),
                    json);
            return reg.resolveWith(
                    ResourceLocation.fromNamespaceAndPath("meridian", blockPath),
                    tag -> false);
        }
    }

    @Test
    void sightshelf_contributesOneClue_withNoEternaCap() throws Exception {
        EnchantingStats stats = loadShippedStats("sightshelf.json");

        assertEquals(1, stats.clues(), "sightshelf ships with clues=1");
        assertEquals(0F, stats.maxEterna(), 1e-6F,
                "sightshelf must not raise the Eterna ceiling — its slot is clue-only");
        assertEquals(0F, stats.eterna(), 1e-6F,
                "sightshelf must not contribute Eterna");
    }

    @Test
    void sightshelfT2_contributesTwoClues_withNoEternaCap() throws Exception {
        EnchantingStats stats = loadShippedStats("sightshelf_t2.json");

        assertEquals(2, stats.clues(), "sightshelf_t2 ships with clues=2");
        assertEquals(0F, stats.maxEterna(), 1e-6F,
                "sightshelf_t2 must not raise the Eterna ceiling — its slot is clue-only");
        assertEquals(0F, stats.eterna(), 1e-6F,
                "sightshelf_t2 must not contribute Eterna");
    }

    @Test
    void twoSightshelfT2_clampedToThreeClues() throws Exception {
        EnchantingStatRegistry reg = new EnchantingStatRegistry();
        EnchantingStats sightT2 = loadShippedStats("sightshelf_t2.json");

        List<BlockPos> offsets = List.of(new BlockPos(0, 0, 0), new BlockPos(1, 0, 0));
        StatCollection result = reg.gatherStatsFromOffsets(offsets, pos -> sightT2);

        assertEquals(EnchantingStatRegistry.MAX_CLUES, result.clues(),
                "two sightshelf_t2 raw clues (4) clamped to MAX_CLUES (3)");
        assertEquals(0F, result.maxEterna(), 1e-6F,
                "a pair of sightshelves leaves the Eterna ceiling at zero");
        assertEquals(0F, result.eterna(), 1e-6F,
                "a pair of sightshelves contributes zero Eterna");
    }
}
