package com.rfizzle.meridian.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MeridianConfigTest {

    private static final Pattern HEX_COLOR = Pattern.compile("^#[0-9A-Fa-f]{6}$");

    @Test
    void defaultConfig_hasValidValues() {
        MeridianConfig cfg = new MeridianConfig();

        assertEquals(1, cfg.configVersion);

        assertNotNull(cfg.enchantingTable);
        assertNotNull(cfg.shelves);
        assertNotNull(cfg.anvil);
        assertNotNull(cfg.library);
        assertNotNull(cfg.tomes);
        assertNotNull(cfg.warden);
        assertNotNull(cfg.display);

        // enchantingTable.maxEterna — clamp [1, 100]
        assertTrue(cfg.enchantingTable.maxEterna >= 1 && cfg.enchantingTable.maxEterna <= 100,
                "maxEterna default must be in [1, 100]");

        // shelves — clampUnit (0–1)
        assertTrue(inUnit(cfg.shelves.sculkShelfShriekerChance), "sculkShelfShriekerChance must be in [0, 1]");
        assertTrue(inUnit(cfg.shelves.sculkParticleChance), "sculkParticleChance must be in [0, 1]");

        // anvil.prismaticWebLevelCost — clampNonNegative
        assertTrue(cfg.anvil.prismaticWebLevelCost >= 0, "prismaticWebLevelCost must be >= 0");

        // library.ioRateLimitTicks — clampNonNegative
        assertTrue(cfg.library.ioRateLimitTicks >= 0, "ioRateLimitTicks must be >= 0");

        // tomes — clampNonNegative / clampUnit
        assertTrue(cfg.tomes.scrapTomeXpCost >= 0, "scrapTomeXpCost must be >= 0");
        assertTrue(cfg.tomes.improvedScrapTomeXpCost >= 0, "improvedScrapTomeXpCost must be >= 0");
        assertTrue(cfg.tomes.extractionTomeXpCost >= 0, "extractionTomeXpCost must be >= 0");
        assertTrue(cfg.tomes.extractionTomeItemDamage >= 0, "extractionTomeItemDamage must be >= 0");
        assertTrue(inUnit(cfg.tomes.extractionTomeRepairPercent), "extractionTomeRepairPercent must be in [0, 1]");

        // warden — clampUnit
        assertTrue(inUnit(cfg.warden.tendrilDropChance), "tendrilDropChance must be in [0, 1]");
        assertTrue(inUnit(cfg.warden.tendrilLootingBonus), "tendrilLootingBonus must be in [0, 1]");

        // display.overLeveledColor — hex pattern
        assertNotNull(cfg.display.overLeveledColor);
        assertTrue(HEX_COLOR.matcher(cfg.display.overLeveledColor).matches(),
                "overLeveledColor must match ^#[0-9A-Fa-f]{6}$");
    }

    @Test
    void defaultConfig_matchesDesignJsonValues() {
        MeridianConfig cfg = new MeridianConfig();

        assertEquals(false, cfg.enchantingTable.allowTreasureWithoutShelf);
        assertEquals(50, cfg.enchantingTable.maxEterna);
        assertEquals(true, cfg.enchantingTable.showLevelIndicator);

        assertEquals(0.02, cfg.shelves.sculkShelfShriekerChance);
        assertEquals(0.05, cfg.shelves.sculkParticleChance);

        assertEquals(true, cfg.anvil.prismaticWebRemovesCurses);
        assertEquals(30, cfg.anvil.prismaticWebLevelCost);
        assertEquals(true, cfg.anvil.ironBlockRepairsAnvil);

        assertEquals(0, cfg.library.ioRateLimitTicks);

        assertEquals(3, cfg.tomes.scrapTomeXpCost);
        assertEquals(5, cfg.tomes.improvedScrapTomeXpCost);
        assertEquals(10, cfg.tomes.extractionTomeXpCost);
        assertEquals(50, cfg.tomes.extractionTomeItemDamage);
        assertEquals(0.25, cfg.tomes.extractionTomeRepairPercent);

        assertEquals(1.0, cfg.warden.tendrilDropChance);
        assertEquals(0.10, cfg.warden.tendrilLootingBonus);


        assertEquals(true, cfg.display.showBookTooltips);
        assertEquals("#FF6600", cfg.display.overLeveledColor);
        assertEquals(false, cfg.display.enableInlineEnchDescs);
    }

    @Test
    void load_missingFile_writesDefaultsAndReturns(@TempDir Path tmp) {
        Path path = tmp.resolve("meridian.json");
        MeridianConfig loaded = MeridianConfig.load(path);

        assertTrue(Files.exists(path), "load() should have created the missing file");
        assertEquals(50, loaded.enchantingTable.maxEterna);
        assertEquals(0.02, loaded.shelves.sculkShelfShriekerChance);
        assertEquals(30, loaded.anvil.prismaticWebLevelCost);
        assertEquals(0, loaded.library.ioRateLimitTicks);
        assertEquals(3, loaded.tomes.scrapTomeXpCost);
        assertEquals(1.0, loaded.warden.tendrilDropChance);
        assertEquals("#FF6600", loaded.display.overLeveledColor);
    }

    @Test
    void load_partialFile_fillsDefaults(@TempDir Path tmp) throws IOException {
        Path path = tmp.resolve("meridian.json");
        Files.writeString(path, "{\"enchantingTable\":{\"maxEterna\":42}}");

        MeridianConfig loaded = MeridianConfig.load(path);

        // User-specified value survives — but since Gson only populates fields present
        // in the JSON, sibling fields in the same section fall back to primitive zeros.
        // The contract here is "missing sections are filled"; missing fields within a
        // present section fall out of later validation/default behavior.
        assertEquals(42, loaded.enchantingTable.maxEterna);

        // All other sections populated with defaults.
        assertNotNull(loaded.shelves);
        assertEquals(0.02, loaded.shelves.sculkShelfShriekerChance);
        assertEquals(0.05, loaded.shelves.sculkParticleChance);

        assertNotNull(loaded.anvil);
        assertEquals(true, loaded.anvil.prismaticWebRemovesCurses);
        assertEquals(30, loaded.anvil.prismaticWebLevelCost);
        assertEquals(true, loaded.anvil.ironBlockRepairsAnvil);

        assertNotNull(loaded.library);
        assertEquals(0, loaded.library.ioRateLimitTicks);

        assertNotNull(loaded.tomes);
        assertEquals(3, loaded.tomes.scrapTomeXpCost);
        assertEquals(5, loaded.tomes.improvedScrapTomeXpCost);
        assertEquals(10, loaded.tomes.extractionTomeXpCost);
        assertEquals(50, loaded.tomes.extractionTomeItemDamage);
        assertEquals(0.25, loaded.tomes.extractionTomeRepairPercent);

        assertNotNull(loaded.warden);
        assertEquals(1.0, loaded.warden.tendrilDropChance);
        assertEquals(0.10, loaded.warden.tendrilLootingBonus);

        assertNotNull(loaded.display);
        assertEquals(true, loaded.display.showBookTooltips);
        assertEquals("#FF6600", loaded.display.overLeveledColor);
    }

    @Test
    void load_clampsOutOfRange(@TempDir Path tmp) throws IOException {
        Path path = tmp.resolve("meridian.json");
        Files.writeString(path, """
                {
                  "enchantingTable": {"maxEterna": 0},
                  "shelves": {"sculkShelfShriekerChance": -0.5, "sculkParticleChance": 0.5},
                  "anvil": {"prismaticWebLevelCost": -10},
                  "library": {"ioRateLimitTicks": -3},
                  "tomes": {
                    "scrapTomeXpCost": -1,
                    "improvedScrapTomeXpCost": -2,
                    "extractionTomeXpCost": -3,
                    "extractionTomeItemDamage": -4,
                    "extractionTomeRepairPercent": 2.5
                  },
                  "warden": {"tendrilDropChance": -0.25, "tendrilLootingBonus": 2.0}
                }
                """);

        MeridianConfig loaded = MeridianConfig.load(path);

        assertEquals(1, loaded.enchantingTable.maxEterna);
        assertEquals(0.0, loaded.shelves.sculkShelfShriekerChance);
        assertEquals(0, loaded.anvil.prismaticWebLevelCost);
        assertEquals(0, loaded.library.ioRateLimitTicks);
        assertEquals(0, loaded.tomes.scrapTomeXpCost);
        assertEquals(0, loaded.tomes.improvedScrapTomeXpCost);
        assertEquals(0, loaded.tomes.extractionTomeXpCost);
        assertEquals(0, loaded.tomes.extractionTomeItemDamage);
        assertEquals(1.0, loaded.tomes.extractionTomeRepairPercent);
        assertEquals(0.0, loaded.warden.tendrilDropChance);
        assertEquals(1.0, loaded.warden.tendrilLootingBonus);
    }

    @Test
    void load_clampsMaxEternaAboveCeiling(@TempDir Path tmp) throws IOException {
        Path path = tmp.resolve("meridian.json");
        Files.writeString(path, "{\"enchantingTable\":{\"maxEterna\":500}}");

        MeridianConfig loaded = MeridianConfig.load(path);

        assertEquals(100, loaded.enchantingTable.maxEterna);
    }

    @Test
    void load_invalidOverLeveledColor_fallsBack(@TempDir Path tmp) throws IOException {
        Path path = tmp.resolve("meridian.json");
        Files.writeString(path, "{\"display\":{\"overLeveledColor\":\"not-hex\"}}");

        MeridianConfig loaded = MeridianConfig.load(path);

        assertEquals("#FF6600", loaded.display.overLeveledColor);
    }

    @Test
    void load_validCustomOverLeveledColor_preserved(@TempDir Path tmp) throws IOException {
        Path path = tmp.resolve("meridian.json");
        Files.writeString(path, "{\"display\":{\"overLeveledColor\":\"#1A2B3C\"}}");

        MeridianConfig loaded = MeridianConfig.load(path);

        assertEquals("#1A2B3C", loaded.display.overLeveledColor);
    }

    @Test
    void migrate_versionOne_isNoOp() {
        MeridianConfig cfg = new MeridianConfig();
        cfg.configVersion = 1;
        cfg.enchantingTable.maxEterna = 42;

        boolean migrated = cfg.migrate();

        assertEquals(false, migrated);
        assertEquals(1, cfg.configVersion);
        assertEquals(42, cfg.enchantingTable.maxEterna);
    }

    @Test
    void saveAndLoad_roundTrip_preservesValues(@TempDir Path tmp) {
        Path path = tmp.resolve("meridian.json");
        MeridianConfig original = new MeridianConfig();
        original.enchantingTable.maxEterna = 42;
        original.enchantingTable.allowTreasureWithoutShelf = true;
        original.shelves.sculkShelfShriekerChance = 0.12;
        original.anvil.prismaticWebLevelCost = 15;
        original.library.ioRateLimitTicks = 20;
        original.tomes.extractionTomeRepairPercent = 0.5;
        original.warden.tendrilLootingBonus = 0.33;
        original.display.overLeveledColor = "#AABBCC";
        original.display.enableInlineEnchDescs = true;
        original.save(path);

        MeridianConfig reloaded = MeridianConfig.load(path);

        assertEquals(42, reloaded.enchantingTable.maxEterna);
        assertEquals(true, reloaded.enchantingTable.allowTreasureWithoutShelf);
        assertEquals(0.12, reloaded.shelves.sculkShelfShriekerChance);
        assertEquals(15, reloaded.anvil.prismaticWebLevelCost);
        assertEquals(20, reloaded.library.ioRateLimitTicks);
        assertEquals(0.5, reloaded.tomes.extractionTomeRepairPercent);
        assertEquals(0.33, reloaded.warden.tendrilLootingBonus);
        assertEquals("#AABBCC", reloaded.display.overLeveledColor);
        assertEquals(true, reloaded.display.enableInlineEnchDescs);
    }

    @Test
    void load_powerFunctionOverride_linear(@TempDir Path tmp) throws IOException {
        Path path = tmp.resolve("meridian.json");
        Files.writeString(path, """
                {
                  "enchantmentOverrides": {
                    "minecraft:sharpness": {
                      "maxLevel": 10,
                      "minPowerFunction": { "type": "linear", "base": 1, "perLevel": 11 },
                      "maxPowerFunction": { "type": "linear", "base": 21, "perLevel": 11 }
                    }
                  }
                }
                """);

        MeridianConfig loaded = MeridianConfig.load(path);

        MeridianConfig.EnchantmentOverride o = loaded.enchantmentOverrides.get("minecraft:sharpness");
        assertNotNull(o);
        assertEquals(10, o.maxLevel);
        assertNotNull(o.minPowerFunction);
        assertEquals("linear", o.minPowerFunction.type);
        assertEquals(1, o.minPowerFunction.base);
        assertEquals(11, o.minPowerFunction.perLevel);
        assertNotNull(o.maxPowerFunction);
        assertEquals("linear", o.maxPowerFunction.type);
        assertEquals(21, o.maxPowerFunction.base);
        assertEquals(11, o.maxPowerFunction.perLevel);
    }

    @Test
    void load_powerFunctionOverride_fixed(@TempDir Path tmp) throws IOException {
        Path path = tmp.resolve("meridian.json");
        Files.writeString(path, """
                {
                  "enchantmentOverrides": {
                    "minecraft:mending": {
                      "maxPowerFunction": { "type": "fixed", "value": 150 }
                    }
                  }
                }
                """);

        MeridianConfig loaded = MeridianConfig.load(path);

        MeridianConfig.EnchantmentOverride o = loaded.enchantmentOverrides.get("minecraft:mending");
        assertNotNull(o);
        assertNotNull(o.maxPowerFunction);
        assertEquals("fixed", o.maxPowerFunction.type);
        assertEquals(150, o.maxPowerFunction.value);
    }

    @Test
    void load_powerFunctionOverride_invalidType_resetsToDefault(@TempDir Path tmp) throws IOException {
        Path path = tmp.resolve("meridian.json");
        Files.writeString(path, """
                {
                  "enchantmentOverrides": {
                    "minecraft:sharpness": {
                      "minPowerFunction": { "type": "quadratic", "base": 1 }
                    }
                  }
                }
                """);

        MeridianConfig loaded = MeridianConfig.load(path);

        MeridianConfig.EnchantmentOverride o = loaded.enchantmentOverrides.get("minecraft:sharpness");
        assertNotNull(o);
        assertNotNull(o.minPowerFunction);
        assertEquals("default", o.minPowerFunction.type);
    }

    @Test
    void load_powerFunctionOverride_nullWhenAbsent(@TempDir Path tmp) throws IOException {
        Path path = tmp.resolve("meridian.json");
        Files.writeString(path, """
                {
                  "enchantmentOverrides": {
                    "minecraft:sharpness": { "maxLevel": 8 }
                  }
                }
                """);

        MeridianConfig loaded = MeridianConfig.load(path);

        MeridianConfig.EnchantmentOverride o = loaded.enchantmentOverrides.get("minecraft:sharpness");
        assertNotNull(o);
        assertEquals(8, o.maxLevel);
        assertTrue(o.minPowerFunction == null, "minPowerFunction should be null when not specified");
        assertTrue(o.maxPowerFunction == null, "maxPowerFunction should be null when not specified");
    }

    @Test
    void saveAndLoad_powerFunctionOverride_roundTrips(@TempDir Path tmp) {
        Path path = tmp.resolve("meridian.json");
        MeridianConfig original = new MeridianConfig();

        MeridianConfig.EnchantmentOverride o = new MeridianConfig.EnchantmentOverride();
        o.maxLevel = 10;
        MeridianConfig.PowerFunctionConfig minPf = new MeridianConfig.PowerFunctionConfig();
        minPf.type = "linear";
        minPf.base = 1;
        minPf.perLevel = 11;
        o.minPowerFunction = minPf;
        MeridianConfig.PowerFunctionConfig maxPf = new MeridianConfig.PowerFunctionConfig();
        maxPf.type = "fixed";
        maxPf.value = 150;
        o.maxPowerFunction = maxPf;
        original.enchantmentOverrides.put("minecraft:sharpness", o);

        original.save(path);
        MeridianConfig reloaded = MeridianConfig.load(path);

        MeridianConfig.EnchantmentOverride ro = reloaded.enchantmentOverrides.get("minecraft:sharpness");
        assertNotNull(ro);
        assertEquals(10, ro.maxLevel);
        assertNotNull(ro.minPowerFunction);
        assertEquals("linear", ro.minPowerFunction.type);
        assertEquals(1, ro.minPowerFunction.base);
        assertEquals(11, ro.minPowerFunction.perLevel);
        assertNotNull(ro.maxPowerFunction);
        assertEquals("fixed", ro.maxPowerFunction.type);
        assertEquals(150, ro.maxPowerFunction.value);
    }

    private static boolean inUnit(double v) {
        return v >= 0.0 && v <= 1.0;
    }
}
