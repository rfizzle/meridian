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
 * T-3.3.2 — rectifier tiers contribute {@code rectification} only. Values are copied verbatim
 * from Zenith per DESIGN.md's asset-sourcing rule (the DESIGN summary table is informational;
 * the shipped Zenith JSON is the source of truth): {@code rectifier=10, rectifier_t2=15,
 * rectifier_t3=25}. Like the sightshelf tiers, rectifiers leave the Eterna ceiling alone so
 * their slot carries no side-effect beyond the named stat.
 */
class RectifierStatsTest {

    private static final String STATS_DIR = "/data/meridian/enchanting_stats/";

    private static Path resource(String filename) throws Exception {
        URL url = RectifierStatsTest.class.getResource(STATS_DIR + filename);
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
    void rectifier_contributesTenRectification_withNoEternaCap() throws Exception {
        EnchantingStats stats = loadShippedStats("rectifier.json");

        assertEquals(10F, stats.rectification(), 1e-6F, "rectifier ships with rectification=10");
        assertEquals(0F, stats.maxEterna(), 1e-6F,
                "rectifier must not raise the Eterna ceiling — its slot is rectification-only");
        assertEquals(0F, stats.eterna(), 1e-6F,
                "rectifier must not contribute Eterna");
        assertEquals(0F, stats.quanta(), 1e-6F, "rectifier must not contribute Quanta");
        assertEquals(0F, stats.arcana(), 1e-6F, "rectifier must not contribute Arcana");
        assertEquals(0, stats.clues(), "rectifier must not contribute clues");
    }

    @Test
    void rectifierT2_contributesFifteenRectification_withNoEternaCap() throws Exception {
        EnchantingStats stats = loadShippedStats("rectifier_t2.json");

        assertEquals(15F, stats.rectification(), 1e-6F, "rectifier_t2 ships with rectification=15");
        assertEquals(0F, stats.maxEterna(), 1e-6F,
                "rectifier_t2 must not raise the Eterna ceiling");
        assertEquals(0F, stats.eterna(), 1e-6F, "rectifier_t2 must not contribute Eterna");
    }

    @Test
    void rectifierT3_contributesMaxRectification_withNoEternaCap() throws Exception {
        EnchantingStats stats = loadShippedStats("rectifier_t3.json");

        // Zenith ships 25; DESIGN.md's summary table lists 20 but the doc explicitly names the
        // Zenith JSON as the source of truth. The test tracks shipped behaviour.
        assertEquals(25F, stats.rectification(), 1e-6F,
                "rectifier_t3 ships with rectification=25 (verbatim from Zenith)");
        assertEquals(0F, stats.maxEterna(), 1e-6F,
                "rectifier_t3 must not raise the Eterna ceiling");
        assertEquals(0F, stats.eterna(), 1e-6F, "rectifier_t3 must not contribute Eterna");
    }

    @Test
    void rectifierT3_inRange_aggregatesToMaxRectification() throws Exception {
        EnchantingStatRegistry reg = new EnchantingStatRegistry();
        EnchantingStats rectifierT3 = loadShippedStats("rectifier_t3.json");

        List<BlockPos> offsets = List.of(new BlockPos(0, 0, 0));
        StatCollection result = reg.gatherStatsFromOffsets(offsets, pos -> rectifierT3);

        assertEquals(25F, result.rectification(), 1e-6F,
                "a rectifier_t3 in range contributes its full rectification stat to the scan");
        assertEquals(0F, result.maxEterna(), 1e-6F,
                "a lone rectifier_t3 leaves the Eterna ceiling at zero");
        assertEquals(0F, result.eterna(), 1e-6F,
                "a lone rectifier_t3 contributes zero Eterna");
        assertEquals(0, result.clues(), "a lone rectifier_t3 contributes zero clues");
    }

    @Test
    void rectifierTiers_stack_withHigherTierDominatingEternaCap() throws Exception {
        EnchantingStatRegistry reg = new EnchantingStatRegistry();
        EnchantingStats r1 = loadShippedStats("rectifier.json");
        EnchantingStats r2 = loadShippedStats("rectifier_t2.json");
        EnchantingStats r3 = loadShippedStats("rectifier_t3.json");

        List<BlockPos> offsets = List.of(
                new BlockPos(0, 0, 0),
                new BlockPos(1, 0, 0),
                new BlockPos(2, 0, 0));
        StatCollection result = reg.gatherStatsFromOffsets(offsets, pos -> {
            if (pos.getX() == 0) return r1;
            if (pos.getX() == 1) return r2;
            return r3;
        });

        assertEquals(50F, result.rectification(), 1e-6F,
                "rectification sums across all tiers in range (10 + 15 + 25)");
        assertEquals(0F, result.maxEterna(), 1e-6F,
                "rectifier tiers never raise the Eterna ceiling, regardless of mix");
    }
}
