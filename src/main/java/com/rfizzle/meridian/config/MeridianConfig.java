package com.rfizzle.meridian.config;

import com.rfizzle.meridian.Meridian;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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
    // Serializer for the server→client sync wire form (#149). Compact (no pretty-printing) and
    // drops the client-only {@link #display} block, so the synced view is exactly the
    // server-authoritative gameplay surface. The client keeps reading its own local
    // {@code display} preferences; {@link #fromJson(String)} reads this form back.
    private static final Gson SYNC_GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .addSerializationExclusionStrategy(new ExclusionStrategy() {
                @Override
                public boolean shouldSkipField(FieldAttributes f) {
                    return MeridianConfig.class.equals(f.getDeclaringClass()) && "display".equals(f.getName());
                }

                @Override
                public boolean shouldSkipClass(Class<?> clazz) {
                    return false;
                }
            })
            .create();
    private static final Pattern HEX_COLOR = Pattern.compile("^#[0-9A-Fa-f]{6}$");
    private static final String DEFAULT_OVER_LEVELED_COLOR = "#FF6600";
    private static final Set<String> VALID_POWER_FUNCTION_TYPES = Set.of("default", "linear", "fixed");

    public int configVersion = ConfigMigrator.CURRENT_VERSION;
    public EnchantingTable enchantingTable = new EnchantingTable();
    public TableCrafting tableCrafting = new TableCrafting();
    public Shelves shelves = new Shelves();
    public Anvil anvil = new Anvil();
    public Library library = new Library();
    public Tomes tomes = new Tomes();
    public Everfeast everfeast = new Everfeast();
    public Warden warden = new Warden();
    public Combat combat = new Combat();
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
        try {
            JsonElement element = JsonParser.parseString(Files.readString(path));
            if (element == null || !element.isJsonObject()) {
                Meridian.LOGGER.warn("Config file at {} was empty or not a JSON object; using defaults", path);
                MeridianConfig fresh = new MeridianConfig();
                fresh.save(path);
                return fresh;
            }
            // Migrate the raw JSON tree before deserialize so a renamed key survives (a lenient
            // Gson deserialize would drop it). A file without configVersion is treated as v0.
            JsonObject raw = element.getAsJsonObject();
            boolean migrated = ConfigMigrator.migrate(raw);
            MeridianConfig config = GSON.fromJson(raw, MeridianConfig.class);
            config.fillDefaults();
            config.clamp();
            if (migrated) {
                config.save(path);
            }
            return config;
        } catch (JsonSyntaxException e) {
            Meridian.LOGGER.error("Failed to parse config at {}; using defaults (existing file left untouched)", path, e);
            MeridianConfig fallback = new MeridianConfig();
            fallback.fillDefaults();
            fallback.clamp();
            return fallback;
        } catch (IOException e) {
            Meridian.LOGGER.error("Failed to read config at {}; using defaults", path, e);
            MeridianConfig fallback = new MeridianConfig();
            fallback.fillDefaults();
            fallback.clamp();
            return fallback;
        }
    }

    public void save() {
        save(configPath());
    }

    void save(Path path) {
        // Write to a sibling temp file then atomically rename, so a crash or kill mid-write can
        // never leave a truncated/corrupt config.json in place. Fall back to a plain move where the
        // filesystem can't do an atomic rename, and clean up the orphan temp on failure.
        Path tmp = path.resolveSibling(path.getFileName() + ".tmp");
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(tmp, GSON.toJson(this));
            try {
                Files.move(tmp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            Meridian.LOGGER.error("Failed to save config to {}", path, e);
            try {
                Files.deleteIfExists(tmp);
            } catch (IOException cleanup) {
                Meridian.LOGGER.warn("Failed to clean up orphan temp config {}", tmp, cleanup);
            }
        }
    }

    private static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILENAME);
    }

    /**
     * Serializes the server-authoritative gameplay surface for the config-sync payload (#149):
     * every section except the client-only {@link #display} block. Read back with
     * {@link #fromJson(String)}.
     */
    public String toSyncJson() {
        return SYNC_GSON.toJson(this);
    }

    /**
     * Reconstructs a config from a {@link #toSyncJson()} string received from the server. The JSON is
     * already at the current schema version (both sides run the same mod build), so migration is
     * deliberately skipped — only {@link #fillDefaults()} null-healing and {@link #clamp()}
     * warn-and-clamp run, so a hostile or malformed payload can never yield an out-of-range config.
     * A null/unparseable tree falls back to a fresh default rather than throwing.
     */
    public static MeridianConfig fromJson(String json) {
        MeridianConfig config;
        try {
            config = GSON.fromJson(json, MeridianConfig.class);
        } catch (JsonSyntaxException e) {
            Meridian.LOGGER.warn("Failed to parse synced config JSON; using defaults", e);
            config = null;
        }
        if (config == null) {
            config = new MeridianConfig();
        }
        config.fillDefaults();
        config.clamp();
        return config;
    }

    private void fillDefaults() {
        if (enchantingTable == null) enchantingTable = new EnchantingTable();
        if (tableCrafting == null) tableCrafting = new TableCrafting();
        if (shelves == null) shelves = new Shelves();
        if (anvil == null) anvil = new Anvil();
        if (library == null) library = new Library();
        if (tomes == null) tomes = new Tomes();
        if (everfeast == null) everfeast = new Everfeast();
        if (warden == null) warden = new Warden();
        if (combat == null) combat = new Combat();
        if (display == null) display = new Display();
        if (enchantmentOverrides == null) enchantmentOverrides = new HashMap<>();
    }

    /**
     * Warn-and-clamp every field into its valid range, logging each correction. Public so the
     * ModMenu screen can clamp before {@link #save()} — an out-of-range value typed into the config
     * GUI is corrected rather than persisted verbatim — matching the mc-config skill's pattern.
     */
    public void clamp() {
        enchantingTable.maxEterna = clampIntRange("enchantingTable.maxEterna", enchantingTable.maxEterna, 1, 100);
        enchantingTable.globalMinEnchantability = clampIntRange(
                "enchantingTable.globalMinEnchantability", enchantingTable.globalMinEnchantability, 0, 100);

        shelves.sculkShelfShriekerChance = clampUnit("shelves.sculkShelfShriekerChance", shelves.sculkShelfShriekerChance);
        shelves.sculkParticleChance = clampUnit("shelves.sculkParticleChance", shelves.sculkParticleChance);

        anvil.prismaticWebLevelCost = clampNonNegative("anvil.prismaticWebLevelCost", anvil.prismaticWebLevelCost);
        anvil.temperedCoreLevelCost = clampNonNegative("anvil.temperedCoreLevelCost", anvil.temperedCoreLevelCost);

        library.ioRateLimitTicks = clampNonNegative("library.ioRateLimitTicks", library.ioRateLimitTicks);

        tomes.scrapTomeXpCost = clampNonNegative("tomes.scrapTomeXpCost", tomes.scrapTomeXpCost);
        tomes.improvedScrapTomeXpCost = clampNonNegative("tomes.improvedScrapTomeXpCost", tomes.improvedScrapTomeXpCost);
        tomes.extractionTomeXpCost = clampNonNegative("tomes.extractionTomeXpCost", tomes.extractionTomeXpCost);
        tomes.extractionTomeItemDamage = clampNonNegative("tomes.extractionTomeItemDamage", tomes.extractionTomeItemDamage);
        tomes.extractionTomeRepairPercent = clampUnit("tomes.extractionTomeRepairPercent", tomes.extractionTomeRepairPercent);

        everfeast.bites = clampIntRange("everfeast.bites", everfeast.bites, 1, 4096);

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

    public static class TableCrafting {
        /**
         * Whether the vanilla-item duplication recipes ({@code "module": "duplication"} — totem of
         * undying, echo shard, golden apples, …) are available at the table. Already-crafted items
         * are unaffected.
         */
        public boolean allowDuplication = true;
    }

    public static class Shelves {
        public double sculkShelfShriekerChance = 0.02;
        public double sculkParticleChance = 0.05;
    }

    public static class Anvil {
        public boolean prismaticWebRemovesCurses = true;
        public int prismaticWebLevelCost = 30;
        public boolean ironBlockRepairsAnvil = true;
        public int temperedCoreLevelCost = 10;
        public boolean temperedCoreEnabled = true;
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

    public static class Everfeast {
        /**
         * Whether the Everfeast ration and Everfull Flask recipes ({@code "module": "everfeast"})
         * are available at the table. Existing Everfeast items in a world keep working when off.
         */
        public boolean enabled = true;
        /** Bites a newly-infused Everfeast ration is created with; existing rations keep theirs. */
        public int bites = 128;
    }

    public static class Warden {
        public double tendrilDropChance = 1.0;
        public double tendrilLootingBonus = 0.10;
    }

    public static class Combat {
        /** Whether Sunder may knock equipment off player victims; mobs are always eligible. */
        public boolean sunderAffectsPlayers = false;
        /** Whether Seeker bolts may lock onto player targets; mobs are always eligible. */
        public boolean seekerTargetsPlayers = false;
        /** Whether Harpoon may drag player victims toward the thrower; mobs are always eligible. */
        public boolean harpoonAffectsPlayers = false;
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
