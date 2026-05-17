package com.rfizzle.meridian.config;

import com.rfizzle.meridian.Meridian;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class MeridianConfig {
    private static final String CONFIG_FILENAME = "meridian.json";
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();
    private static final Pattern HEX_COLOR = Pattern.compile("^#[0-9A-Fa-f]{6}$");
    private static final String DEFAULT_OVER_LEVELED_COLOR = "#FF6600";
    private static final Set<String> VALID_POWER_FUNCTION_TYPES = Set.of("default", "linear", "fixed");
    private static final int CURRENT_VERSION = 1;

    public int configVersion = 1;
    public EnchantingTable enchantingTable = new EnchantingTable();
    public Shelves shelves = new Shelves();
    public Anvil anvil = new Anvil();
    public Library library = new Library();
    public Tomes tomes = new Tomes();
    public Warden warden = new Warden();
    public Display display = new Display();
    public Map<String, EnchantmentOverride> enchantmentOverrides = new HashMap<>();

    public static MeridianConfig load() {
        return load(configPath());
    }

    static MeridianConfig load(Path path) {
        if (!Files.exists(path)) {
            Meridian.LOGGER.info("Config file missing; creating default at {}", path);
            MeridianConfig config = new MeridianConfig();
            config.save(path);
            return config;
        }
        try (Reader reader = Files.newBufferedReader(path)) {
            MeridianConfig config = GSON.fromJson(reader, MeridianConfig.class);
            if (config == null) {
                Meridian.LOGGER.warn("Config file at {} was empty; using defaults", path);
                MeridianConfig fresh = new MeridianConfig();
                fresh.save(path);
                return fresh;
            }
            boolean migrated = config.migrate();
            config.fillDefaults();
            config.validate();
            if (migrated) {
                config.save(path);
            }
            return config;
        } catch (JsonSyntaxException e) {
            Meridian.LOGGER.error("Failed to parse config at {}; using defaults (existing file left untouched)", path, e);
            MeridianConfig fallback = new MeridianConfig();
            fallback.fillDefaults();
            fallback.validate();
            return fallback;
        } catch (IOException e) {
            Meridian.LOGGER.error("Failed to read config at {}; using defaults", path, e);
            MeridianConfig fallback = new MeridianConfig();
            fallback.fillDefaults();
            fallback.validate();
            return fallback;
        }
    }

    public void save() {
        save(configPath());
    }

    void save(Path path) {
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            Meridian.LOGGER.error("Failed to save config to {}", path, e);
        }
    }

    private static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILENAME);
    }

    boolean migrate() {
        if (configVersion >= CURRENT_VERSION) return false;
        int from = configVersion;
        while (configVersion < CURRENT_VERSION) {
            switch (configVersion) {
                // case 1 -> migrateV1toV2();
                default -> configVersion = CURRENT_VERSION;
            }
        }
        Meridian.LOGGER.info("Migrated config from version {} to {}", from, configVersion);
        return true;
    }

    private void fillDefaults() {
        if (enchantingTable == null) enchantingTable = new EnchantingTable();
        if (shelves == null) shelves = new Shelves();
        if (anvil == null) anvil = new Anvil();
        if (library == null) library = new Library();
        if (tomes == null) tomes = new Tomes();
        if (warden == null) warden = new Warden();
        if (display == null) display = new Display();
        if (enchantmentOverrides == null) enchantmentOverrides = new HashMap<>();
    }

    private void validate() {
        enchantingTable.maxEterna = clampIntRange("enchantingTable.maxEterna", enchantingTable.maxEterna, 1, 100);
        enchantingTable.globalMinEnchantability = clampIntRange(
                "enchantingTable.globalMinEnchantability", enchantingTable.globalMinEnchantability, 0, 100);

        shelves.sculkShelfShriekerChance = clampUnit("shelves.sculkShelfShriekerChance", shelves.sculkShelfShriekerChance);
        shelves.sculkParticleChance = clampUnit("shelves.sculkParticleChance", shelves.sculkParticleChance);

        anvil.prismaticWebLevelCost = clampNonNegative("anvil.prismaticWebLevelCost", anvil.prismaticWebLevelCost);

        library.ioRateLimitTicks = clampNonNegative("library.ioRateLimitTicks", library.ioRateLimitTicks);

        tomes.scrapTomeXpCost = clampNonNegative("tomes.scrapTomeXpCost", tomes.scrapTomeXpCost);
        tomes.improvedScrapTomeXpCost = clampNonNegative("tomes.improvedScrapTomeXpCost", tomes.improvedScrapTomeXpCost);
        tomes.extractionTomeXpCost = clampNonNegative("tomes.extractionTomeXpCost", tomes.extractionTomeXpCost);
        tomes.extractionTomeItemDamage = clampNonNegative("tomes.extractionTomeItemDamage", tomes.extractionTomeItemDamage);
        tomes.extractionTomeRepairPercent = clampUnit("tomes.extractionTomeRepairPercent", tomes.extractionTomeRepairPercent);

        warden.tendrilDropChance = clampUnit("warden.tendrilDropChance", warden.tendrilDropChance);
        warden.tendrilLootingBonus = clampUnit("warden.tendrilLootingBonus", warden.tendrilLootingBonus);

        if (display.overLeveledColor == null || !HEX_COLOR.matcher(display.overLeveledColor).matches()) {
            Meridian.LOGGER.warn("clamped {} from {} to {}",
                    "display.overLeveledColor", display.overLeveledColor, DEFAULT_OVER_LEVELED_COLOR);
            display.overLeveledColor = DEFAULT_OVER_LEVELED_COLOR;
        }

        enchantmentOverrides.entrySet().removeIf(e -> e.getValue() == null);
        for (var entry : enchantmentOverrides.entrySet()) {
            String id = entry.getKey();
            EnchantmentOverride o = entry.getValue();
            if (o.maxLevel != -1) {
                o.maxLevel = clampIntRange("enchantmentOverrides." + id + ".maxLevel", o.maxLevel, 1, 127);
            }
            if (o.maxLootLevel != -1) {
                o.maxLootLevel = clampIntRange("enchantmentOverrides." + id + ".maxLootLevel", o.maxLootLevel, 1, 127);
            }
            if (o.levelCap != -1) {
                o.levelCap = clampIntRange("enchantmentOverrides." + id + ".levelCap", o.levelCap, 1, 127);
            }
            validatePowerFunctionConfig("enchantmentOverrides." + id + ".minPowerFunction", o.minPowerFunction);
            validatePowerFunctionConfig("enchantmentOverrides." + id + ".maxPowerFunction", o.maxPowerFunction);
        }
    }

    private static double clampNonNegative(String name, double value) {
        if (value < 0) {
            Meridian.LOGGER.warn("clamped {} from {} to {}", name, value, 0.0);
            return 0.0;
        }
        return value;
    }

    private static int clampNonNegative(String name, int value) {
        if (value < 0) {
            Meridian.LOGGER.warn("clamped {} from {} to {}", name, value, 0);
            return 0;
        }
        return value;
    }

    private static double clampUnit(String name, double value) {
        if (value < 0) {
            Meridian.LOGGER.warn("clamped {} from {} to {}", name, value, 0.0);
            return 0.0;
        }
        if (value > 1) {
            Meridian.LOGGER.warn("clamped {} from {} to {}", name, value, 1.0);
            return 1.0;
        }
        return value;
    }

    private static int clampIntRange(String name, int value, int min, int max) {
        if (value < min) {
            Meridian.LOGGER.warn("clamped {} from {} to {}", name, value, min);
            return min;
        }
        if (value > max) {
            Meridian.LOGGER.warn("clamped {} from {} to {}", name, value, max);
            return max;
        }
        return value;
    }

    private static void validatePowerFunctionConfig(String name, PowerFunctionConfig pfc) {
        if (pfc == null) return;
        if (pfc.type == null || !VALID_POWER_FUNCTION_TYPES.contains(pfc.type)) {
            Meridian.LOGGER.warn("{}.type '{}' is not valid (expected one of {}); resetting to 'default'",
                    name, pfc.type, VALID_POWER_FUNCTION_TYPES);
            pfc.type = "default";
        }
    }

    public static class EnchantingTable {
        public boolean allowTreasureWithoutShelf = false;
        public int maxEterna = 50;
        public boolean showLevelIndicator = true;
        public int globalMinEnchantability = 1;
    }

    public static class Shelves {
        public double sculkShelfShriekerChance = 0.02;
        public double sculkParticleChance = 0.05;
    }

    public static class Anvil {
        public boolean prismaticWebRemovesCurses = true;
        public int prismaticWebLevelCost = 30;
        public boolean ironBlockRepairsAnvil = true;
    }

    public static class Library {
        public int ioRateLimitTicks = 0;
    }

    public static class Tomes {
        public int scrapTomeXpCost = 3;
        public int improvedScrapTomeXpCost = 5;
        public int extractionTomeXpCost = 10;
        public int extractionTomeItemDamage = 50;
        public double extractionTomeRepairPercent = 0.25;
    }

    public static class Warden {
        public double tendrilDropChance = 1.0;
        public double tendrilLootingBonus = 0.10;
    }

    public static class Display {
        public boolean showBookTooltips = true;
        public String overLeveledColor = "#FF6600";
        public boolean enableInlineEnchDescs = false;
    }

    /**
     * Per-enchantment override entry. Use -1 for any field to keep the vanilla default.
     * Keys in the {@code enchantmentOverrides} map are enchantment IDs (e.g. "minecraft:sharpness").
     */
    public static class EnchantmentOverride {
        public boolean enabled = true;
        public int maxLevel = -1;
        public int maxLootLevel = -1;
        public int levelCap = -1;
        public PowerFunctionConfig minPowerFunction;
        public PowerFunctionConfig maxPowerFunction;
    }

    /**
     * Configures a custom power function for an enchantment override.
     *
     * <p>Supported types:
     * <ul>
     *   <li>{@code "linear"} — {@code base + perLevel * level}
     *   <li>{@code "fixed"} — constant {@code value} regardless of level
     *   <li>{@code "default"} — vanilla behavior (min extrapolation or 200 ceiling)
     * </ul>
     */
    public static class PowerFunctionConfig {
        public String type = "default";
        public int base = 0;
        public int perLevel = 0;
        public int value = 0;
    }
}
