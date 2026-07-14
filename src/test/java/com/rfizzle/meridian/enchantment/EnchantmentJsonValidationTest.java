package com.rfizzle.meridian.enchantment;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class EnchantmentJsonValidationTest {

    private static final Path ENCHANTMENT_DIR = Path.of(
            "src/main/resources/data/meridian/enchantment");

    private static final Path GENERATED_TAGS_DIR = Path.of(
            "src/main/generated/data");

    private static final Path LANG_FILE = Path.of(
            "src/main/resources/assets/meridian/lang/en_us.json");

    private static final List<String> ALL_ENCHANTMENT_IDS = List.of(
            "abyss_ward", "adamant", "alacrity", "alchemists_draw", "ambush", "animus",
            "antidote", "attunement", "aurify", "bastion", "beckon", "blight", "blink",
            "bloodrage", "bounty", "bullrush", "bulwark", "cinderwalk", "clamber",
            "clearsight", "cleave", "colossus", "crescendo", "curse_of_decay", "curse_of_sealing",
            "curse_of_echoes", "curse_of_hunger", "curse_of_attraction", "curse_of_leaden",
            "curse_of_blunting", "curse_of_fumbling", "curse_of_wavering", "curse_of_timidity",
            "curse_of_molting", "curse_of_skittishness", "curse_of_obscurity",
            "curse_of_toll", "curse_of_dissonance", "curse_of_waterlogging",
            "abyssal", "ballast",
            "decay", "decoy", "detonation", "diminish", "dowse", "emberward", "endurance", "everbloom", "excavate",
            "falconstrike", "final_gambit", "fortify", "fortuity", "frostguard", "furrow",
            "gale_shot", "gallop", "glacial_lance", "grapnel", "gravitas", "grind", "groom",
            "harpoon", "hush", "impact_ward", "inexorable", "insight", "ironclasp", "ironwing",
            "joust", "keen_edge", "kiln", "loft", "longshot", "luminance",
            "mark", "masons_reach", "meticulous", "nightfall", "outreach", "permafrost", "pin", "pinpoint",
            "plunder", "premonition", "prismatic", "prospect", "pummel",
            "quell", "rally", "reap", "reckless", "reclaim", "renewal",
            "reprieve", "repulse", "resonance", "retribution", "ricochet",
            "rift_strike", "riposte", "saddleguard", "sanctify", "seeker",
            "seismic_slam", "sentinel", "shackle", "siphon", "skybound", "skyfall",
            "slipstream", "snare", "soul_tax", "spellguard", "stagger", "steadfast",
            "stormcall", "stormward", "sunder", "tailwind", "tempest", "tempo", "terrasculpt",
            "tether", "thermal", "thrift", "timberfell", "torrent", "trailblaze", "trample", "trophy",
            "true_flight", "twin_hook",
            "umbral", "undertow", "updraft",
            "vault", "verdure", "vital_mend", "vitality", "voidbane", "volley", "wavestride", "winterward");

    private static final List<String> TREASURE_ENCHANTMENTS = List.of(
            "final_gambit", "snare", "abyss_ward", "rally", "cinderwalk",
            "vital_mend", "attunement", "tether", "ironclasp", "curse_of_decay", "curse_of_sealing", "aurify",
            "curse_of_echoes", "curse_of_hunger", "curse_of_attraction", "curse_of_leaden",
            "curse_of_blunting", "curse_of_fumbling", "curse_of_wavering", "curse_of_timidity",
            "curse_of_molting", "curse_of_skittishness", "curse_of_obscurity",
            "curse_of_toll", "curse_of_dissonance", "curse_of_waterlogging");

    /**
     * Maps each exclusive set tag path (relative to GENERATED_TAGS_DIR) to the
     * Meridian enchantment IDs that must appear in it.
     */
    private static final Map<String, List<String>> EXCLUSIVE_SET_MEMBERS = Map.ofEntries(
            Map.entry("minecraft/tags/enchantment/exclusive_set/damage.json",
                    List.of("voidbane", "sanctify", "sentinel", "rift_strike", "keen_edge",
                            "ambush", "reap", "pinpoint", "longshot", "crescendo", "torrent", "skyfall")),
            Map.entry("minecraft/tags/enchantment/exclusive_set/armor.json",
                    List.of("spellguard")),
            Map.entry("minecraft/tags/enchantment/exclusive_set/boots.json",
                    List.of("cinderwalk")),
            Map.entry("minecraft/tags/enchantment/exclusive_set/mace.json",
                    List.of("tempest", "seismic_slam", "updraft")),
            Map.entry("minecraft/tags/enchantment/exclusive_set/mining.json",
                    List.of("kiln")),
            Map.entry("meridian/tags/enchantment/exclusive_set/aspect.json",
                    List.of("blight", "decay", "shackle", "nightfall")),
            Map.entry("meridian/tags/enchantment/exclusive_set/arrow_impact.json",
                    List.of("gale_shot", "resonance", "permafrost", "detonation", "stormcall")),
            Map.entry("meridian/tags/enchantment/exclusive_set/size.json",
                    List.of("diminish", "colossus")),
            Map.entry("meridian/tags/enchantment/exclusive_set/mining.json",
                    List.of("excavate", "grind", "prospect")),
            Map.entry("meridian/tags/enchantment/exclusive_set/axe.json",
                    List.of("timberfell")),
            Map.entry("meridian/tags/enchantment/exclusive_set/glass_cannon.json",
                    List.of("bloodrage", "reckless", "reprieve")),
            Map.entry("meridian/tags/enchantment/exclusive_set/mobility.json",
                    List.of("loft", "vault")),
            Map.entry("meridian/tags/enchantment/exclusive_set/mending.json",
                    List.of("vital_mend", "attunement")),
            Map.entry("meridian/tags/enchantment/exclusive_set/loot_bonus.json",
                    List.of("fortuity", "plunder")),
            Map.entry("meridian/tags/enchantment/exclusive_set/trophy.json",
                    List.of("trophy", "snare"))
    );

    /**
     * Maps enchantment IDs to the exclusive_set tag reference they must declare in their JSON.
     */
    private static final Map<String, String> ENCHANTMENT_TO_EXCLUSIVE_SET = Map.ofEntries(
            Map.entry("voidbane", "#minecraft:exclusive_set/damage"),
            Map.entry("sanctify", "#minecraft:exclusive_set/damage"),
            Map.entry("sentinel", "#minecraft:exclusive_set/damage"),
            Map.entry("rift_strike", "#minecraft:exclusive_set/damage"),
            Map.entry("keen_edge", "#minecraft:exclusive_set/damage"),
            Map.entry("kiln", "#minecraft:exclusive_set/mining"),
            Map.entry("spellguard", "#minecraft:exclusive_set/armor"),
            Map.entry("cinderwalk", "#minecraft:exclusive_set/boots"),
            Map.entry("tempest", "#minecraft:exclusive_set/mace"),
            Map.entry("seismic_slam", "#minecraft:exclusive_set/mace"),
            Map.entry("updraft", "#minecraft:exclusive_set/mace"),
            Map.entry("blight", "#meridian:exclusive_set/aspect"),
            Map.entry("decay", "#meridian:exclusive_set/aspect"),
            Map.entry("shackle", "#meridian:exclusive_set/aspect"),
            Map.entry("nightfall", "#meridian:exclusive_set/aspect"),
            Map.entry("gale_shot", "#meridian:exclusive_set/arrow_impact"),
            Map.entry("resonance", "#meridian:exclusive_set/arrow_impact"),
            Map.entry("permafrost", "#meridian:exclusive_set/arrow_impact"),
            Map.entry("detonation", "#meridian:exclusive_set/arrow_impact"),
            Map.entry("stormcall", "#meridian:exclusive_set/arrow_impact"),
            Map.entry("diminish", "#meridian:exclusive_set/size"),
            Map.entry("colossus", "#meridian:exclusive_set/size"),
            Map.entry("excavate", "#meridian:exclusive_set/mining"),
            Map.entry("grind", "#meridian:exclusive_set/mining"),
            Map.entry("prospect", "#meridian:exclusive_set/mining"),
            Map.entry("timberfell", "#meridian:exclusive_set/axe"),
            Map.entry("bloodrage", "#meridian:exclusive_set/glass_cannon"),
            Map.entry("reckless", "#meridian:exclusive_set/glass_cannon"),
            Map.entry("reprieve", "#meridian:exclusive_set/glass_cannon"),
            Map.entry("loft", "#meridian:exclusive_set/mobility"),
            Map.entry("vault", "#meridian:exclusive_set/mobility"),
            Map.entry("vital_mend", "#meridian:exclusive_set/mending"),
            Map.entry("attunement", "#meridian:exclusive_set/mending"),
            Map.entry("ambush", "#minecraft:exclusive_set/damage"),
            Map.entry("reap", "#minecraft:exclusive_set/damage"),
            Map.entry("pinpoint", "#minecraft:exclusive_set/damage"),
            Map.entry("longshot", "#minecraft:exclusive_set/damage"),
            Map.entry("crescendo", "#minecraft:exclusive_set/damage"),
            Map.entry("torrent", "#minecraft:exclusive_set/damage"),
            Map.entry("skyfall", "#minecraft:exclusive_set/damage"),
            Map.entry("fortuity", "#meridian:exclusive_set/loot_bonus"),
            Map.entry("plunder", "#meridian:exclusive_set/loot_bonus"),
            Map.entry("trophy", "#meridian:exclusive_set/trophy"),
            Map.entry("snare", "#meridian:exclusive_set/trophy")
    );

    /**
     * Enchantments that must have primary_items different from supported_items, per the spec.
     * Maps ID to expected primary_items tag reference.
     */
    private static final Map<String, String> PRIMARY_ITEMS_OVERRIDES = Map.ofEntries(
            Map.entry("tempo", "#minecraft:enchantable/sword"),
            Map.entry("quell", "#minecraft:enchantable/sword"),
            Map.entry("siphon", "#minecraft:enchantable/sword"),
            Map.entry("shackle", "#minecraft:enchantable/sword"),
            Map.entry("blight", "#minecraft:enchantable/sword"),
            Map.entry("decay", "#minecraft:enchantable/sword"),
            Map.entry("nightfall", "#minecraft:enchantable/sword"),
            Map.entry("outreach", "#minecraft:enchantable/sword"),
            Map.entry("insight", "#minecraft:enchantable/sword"),
            Map.entry("final_gambit", "#minecraft:enchantable/sword"),
            Map.entry("snare", "#minecraft:enchantable/sword"),
            Map.entry("soul_tax", "#minecraft:enchantable/sword"),
            Map.entry("plunder", "#minecraft:enchantable/sword"),
            Map.entry("gale_shot", "#minecraft:enchantable/bow"),
            Map.entry("resonance", "#minecraft:enchantable/bow"),
            Map.entry("permafrost", "#minecraft:enchantable/bow"),
            Map.entry("detonation", "#minecraft:enchantable/bow"),
            Map.entry("ricochet", "#minecraft:enchantable/bow"),
            Map.entry("stormcall", "#minecraft:enchantable/bow"),
            Map.entry("true_flight", "#minecraft:enchantable/bow"),
            Map.entry("longshot", "#minecraft:enchantable/bow"),
            Map.entry("mark", "#minecraft:enchantable/bow"),
            Map.entry("skyfall", "#minecraft:enchantable/bow"),
            Map.entry("repulse", "#minecraft:enchantable/chest_armor"),
            Map.entry("frostguard", "#minecraft:enchantable/chest_armor"),
            Map.entry("bloodrage", "#minecraft:enchantable/chest_armor"),
            Map.entry("excavate", "#meridian:enchantable/pickaxes"),
            Map.entry("alacrity", "#minecraft:enchantable/foot_armor")
    );

    // =========================================================================
    // 1. JSON loading & schema validation
    // =========================================================================

    @Test
    void enchantmentCount_isExpected() throws Exception {
        long count = Files.list(ENCHANTMENT_DIR)
                .filter(p -> p.toString().endsWith(".json"))
                .count();
        assertEquals(145, count, "expected exactly 145 enchantment JSON files");
    }

    @TestFactory
    Stream<DynamicTest> everyEnchantment_fileExists() {
        return ALL_ENCHANTMENT_IDS.stream().map(id -> DynamicTest.dynamicTest(id, () ->
                assertTrue(Files.isRegularFile(ENCHANTMENT_DIR.resolve(id + ".json")),
                        "missing enchantment file: " + id + ".json")));
    }

    @TestFactory
    Stream<DynamicTest> everyEnchantment_parsesAsValidJson() {
        return ALL_ENCHANTMENT_IDS.stream().map(id -> DynamicTest.dynamicTest(id, () -> {
            Path file = ENCHANTMENT_DIR.resolve(id + ".json");
            assertDoesNotThrow(() -> parseJson(file),
                    "failed to parse " + id + ".json as valid JSON");
        }));
    }

    @TestFactory
    Stream<DynamicTest> everyEnchantment_hasRequiredFields() {
        return ALL_ENCHANTMENT_IDS.stream().map(id -> DynamicTest.dynamicTest(id, () -> {
            JsonObject root = parseJson(ENCHANTMENT_DIR.resolve(id + ".json"));

            assertTrue(root.has("description"), id + ": missing 'description'");
            assertTrue(root.get("description").isJsonObject(), id + ": 'description' must be an object");
            JsonObject desc = root.getAsJsonObject("description");
            assertTrue(desc.has("translate"), id + ": description missing 'translate' key");
            assertEquals("enchantment.meridian." + id, desc.get("translate").getAsString(),
                    id + ": description translate key mismatch");

            assertTrue(root.has("supported_items"), id + ": missing 'supported_items'");
            assertTrue(root.has("weight"), id + ": missing 'weight'");
            assertTrue(root.get("weight").getAsInt() > 0, id + ": weight must be positive");
            assertTrue(root.has("max_level"), id + ": missing 'max_level'");
            assertTrue(root.get("max_level").getAsInt() >= 1, id + ": max_level must be >= 1");

            assertTrue(root.has("min_cost"), id + ": missing 'min_cost'");
            JsonObject minCost = root.getAsJsonObject("min_cost");
            assertTrue(minCost.has("base"), id + ": min_cost missing 'base'");
            assertTrue(minCost.has("per_level_above_first"), id + ": min_cost missing 'per_level_above_first'");

            assertTrue(root.has("max_cost"), id + ": missing 'max_cost'");
            JsonObject maxCost = root.getAsJsonObject("max_cost");
            assertTrue(maxCost.has("base"), id + ": max_cost missing 'base'");
            assertTrue(maxCost.has("per_level_above_first"), id + ": max_cost missing 'per_level_above_first'");

            assertTrue(root.has("anvil_cost"), id + ": missing 'anvil_cost'");
            assertTrue(root.get("anvil_cost").getAsInt() >= 1, id + ": anvil_cost must be >= 1");

            assertTrue(root.has("slots"), id + ": missing 'slots'");
            assertTrue(root.get("slots").isJsonArray(), id + ": 'slots' must be an array");
            assertFalse(root.getAsJsonArray("slots").isEmpty(), id + ": 'slots' must not be empty");

            assertTrue(root.has("effects"), id + ": missing 'effects'");
        }));
    }

    @TestFactory
    Stream<DynamicTest> everyEnchantment_minCostLessThanMaxCost() {
        return ALL_ENCHANTMENT_IDS.stream().map(id -> DynamicTest.dynamicTest(id, () -> {
            JsonObject root = parseJson(ENCHANTMENT_DIR.resolve(id + ".json"));
            int minBase = root.getAsJsonObject("min_cost").get("base").getAsInt();
            int maxBase = root.getAsJsonObject("max_cost").get("base").getAsInt();
            assertTrue(minBase < maxBase,
                    id + ": min_cost base (" + minBase + ") must be less than max_cost base (" + maxBase + ")");
        }));
    }

    @TestFactory
    Stream<DynamicTest> noUnexpectedFiles_inEnchantmentDir() throws Exception {
        return Files.list(ENCHANTMENT_DIR)
                .filter(p -> p.toString().endsWith(".json"))
                .map(p -> {
                    String filename = p.getFileName().toString().replace(".json", "");
                    return DynamicTest.dynamicTest(filename, () ->
                            assertTrue(ALL_ENCHANTMENT_IDS.contains(filename),
                                    "unexpected enchantment file: " + filename + ".json (not in spec)"));
                });
    }

    // =========================================================================
    // 2. Exclusive set membership validation
    // =========================================================================

    @TestFactory
    Stream<DynamicTest> everyExclusiveSetEnchantment_declaresCorrectTag() {
        return ENCHANTMENT_TO_EXCLUSIVE_SET.entrySet().stream()
                .map(entry -> DynamicTest.dynamicTest(entry.getKey(), () -> {
                    JsonObject root = parseJson(ENCHANTMENT_DIR.resolve(entry.getKey() + ".json"));
                    assertTrue(root.has("exclusive_set"),
                            entry.getKey() + ": must declare exclusive_set");
                    assertEquals(entry.getValue(), root.get("exclusive_set").getAsString(),
                            entry.getKey() + ": exclusive_set tag mismatch");
                }));
    }

    @TestFactory
    Stream<DynamicTest> everyExclusiveSetTag_containsExpectedMembers() {
        return EXCLUSIVE_SET_MEMBERS.entrySet().stream()
                .map(entry -> DynamicTest.dynamicTest(entry.getKey(), () -> {
                    Path tagFile = GENERATED_TAGS_DIR.resolve(entry.getKey());
                    assertTrue(Files.isRegularFile(tagFile),
                            "tag file missing: " + entry.getKey());
                    JsonObject root = parseJson(tagFile);
                    JsonArray values = root.getAsJsonArray("values");
                    assertNotNull(values, entry.getKey() + ": missing 'values' array");

                    List<String> taggedIds = new ArrayList<>();
                    for (JsonElement el : values) {
                        String id;
                        if (el.isJsonObject()) {
                            id = el.getAsJsonObject().get("id").getAsString();
                        } else {
                            id = el.getAsString();
                        }
                        if (id.startsWith("meridian:")) {
                            taggedIds.add(id.substring("meridian:".length()));
                        }
                    }

                    for (String expectedId : entry.getValue()) {
                        assertTrue(taggedIds.contains(expectedId),
                                entry.getKey() + ": missing member meridian:" + expectedId);
                    }
                }));
    }

    @TestFactory
    Stream<DynamicTest> enchantmentsWithoutExclusiveSet_doNotDeclareOne() {
        Set<String> enchantmentsWithSets = ENCHANTMENT_TO_EXCLUSIVE_SET.keySet();
        return ALL_ENCHANTMENT_IDS.stream()
                .filter(id -> !enchantmentsWithSets.contains(id))
                .map(id -> DynamicTest.dynamicTest(id, () -> {
                    JsonObject root = parseJson(ENCHANTMENT_DIR.resolve(id + ".json"));
                    assertFalse(root.has("exclusive_set"),
                            id + ": should not declare exclusive_set (not in any set per spec)");
                }));
    }

    // =========================================================================
    // 3. Treasure enchantment validation
    // =========================================================================

    @TestFactory
    Stream<DynamicTest> treasureEnchantments_haveNoPrimaryItems() {
        return TREASURE_ENCHANTMENTS.stream()
                .filter(id -> !PRIMARY_ITEMS_OVERRIDES.containsKey(id))
                .map(id -> DynamicTest.dynamicTest(id, () -> {
                    JsonObject root = parseJson(ENCHANTMENT_DIR.resolve(id + ".json"));
                    assertFalse(root.has("primary_items"),
                            id + ": treasure enchantment should not have primary_items "
                                    + "(prevents appearing in enchanting table)");
                }));
    }

    @TestFactory
    Stream<DynamicTest> treasureEnchantments_haveCorrectWeights() {
        Map<String, Integer> expectedWeights = Map.ofEntries(
                Map.entry("final_gambit", 1),
                Map.entry("snare", 1),
                Map.entry("abyss_ward", 1),
                Map.entry("rally", 1),
                Map.entry("cinderwalk", 2),
                Map.entry("vital_mend", 1),
                Map.entry("attunement", 1),
                Map.entry("tether", 1),
                Map.entry("ironclasp", 1),
                Map.entry("curse_of_decay", 2),
                Map.entry("curse_of_sealing", 1),
                Map.entry("aurify", 1),
                Map.entry("curse_of_echoes", 2),
                Map.entry("curse_of_hunger", 2),
                Map.entry("curse_of_attraction", 1),
                Map.entry("curse_of_leaden", 2),
                Map.entry("curse_of_blunting", 2),
                Map.entry("curse_of_fumbling", 1),
                Map.entry("curse_of_wavering", 2),
                Map.entry("curse_of_timidity", 2),
                Map.entry("curse_of_molting", 2),
                Map.entry("curse_of_skittishness", 2),
                Map.entry("curse_of_obscurity", 1),
                Map.entry("curse_of_toll", 2),
                Map.entry("curse_of_dissonance", 1),
                Map.entry("curse_of_waterlogging", 2)
        );
        return expectedWeights.entrySet().stream()
                .map(entry -> DynamicTest.dynamicTest(entry.getKey(), () -> {
                    JsonObject root = parseJson(ENCHANTMENT_DIR.resolve(entry.getKey() + ".json"));
                    assertEquals(entry.getValue().intValue(), root.get("weight").getAsInt(),
                            entry.getKey() + ": weight mismatch");
                }));
    }

    @TestFactory
    Stream<DynamicTest> nonTreasureEnchantments_havePrimaryItemsOrSupportedOnly() {
        return ALL_ENCHANTMENT_IDS.stream()
                .filter(id -> !TREASURE_ENCHANTMENTS.contains(id))
                .map(id -> DynamicTest.dynamicTest(id, () -> {
                    JsonObject root = parseJson(ENCHANTMENT_DIR.resolve(id + ".json"));
                    assertTrue(root.has("supported_items"),
                            id + ": non-treasure enchantment must have supported_items");
                }));
    }

    // =========================================================================
    // 4. Primary vs supported items validation
    // =========================================================================

    @TestFactory
    Stream<DynamicTest> enchantmentsWithPrimaryOverride_haveCorrectPrimaryItems() {
        return PRIMARY_ITEMS_OVERRIDES.entrySet().stream()
                .map(entry -> DynamicTest.dynamicTest(entry.getKey(), () -> {
                    JsonObject root = parseJson(ENCHANTMENT_DIR.resolve(entry.getKey() + ".json"));
                    assertTrue(root.has("primary_items"),
                            entry.getKey() + ": must have primary_items per spec");
                    assertEquals(entry.getValue(), root.get("primary_items").getAsString(),
                            entry.getKey() + ": primary_items mismatch");
                }));
    }

    @TestFactory
    Stream<DynamicTest> enchantmentsWithPrimaryOverride_haveDifferentSupportedItems() {
        return PRIMARY_ITEMS_OVERRIDES.entrySet().stream()
                .map(entry -> DynamicTest.dynamicTest(entry.getKey(), () -> {
                    JsonObject root = parseJson(ENCHANTMENT_DIR.resolve(entry.getKey() + ".json"));
                    String primary = root.get("primary_items").getAsString();
                    String supported = root.get("supported_items").getAsString();
                    assertNotEquals(primary, supported,
                            entry.getKey() + ": primary_items and supported_items should differ "
                                    + "(primary is a subset for the enchanting table)");
                }));
    }

    @TestFactory
    Stream<DynamicTest> enchantmentsWithoutPrimaryOverride_haveNoPrimaryItems() {
        Set<String> overridden = PRIMARY_ITEMS_OVERRIDES.keySet();
        return ALL_ENCHANTMENT_IDS.stream()
                .filter(id -> !overridden.contains(id))
                .map(id -> DynamicTest.dynamicTest(id, () -> {
                    JsonObject root = parseJson(ENCHANTMENT_DIR.resolve(id + ".json"));
                    assertFalse(root.has("primary_items"),
                            id + ": should not have primary_items (supported_items serves both roles)");
                }));
    }

    // =========================================================================
    // 5. Lang key validation
    // =========================================================================

    @Test
    void langFile_containsAllEnchantmentNameKeys() throws Exception {
        JsonObject lang = parseJson(LANG_FILE);
        List<String> missing = new ArrayList<>();
        for (String id : ALL_ENCHANTMENT_IDS) {
            String key = "enchantment.meridian." + id;
            if (!lang.has(key)) missing.add(key);
        }
        assertTrue(missing.isEmpty(),
                "missing enchantment name lang keys: " + missing);
    }

    @Test
    void langFile_containsAllEnchantmentDescKeys() throws Exception {
        JsonObject lang = parseJson(LANG_FILE);
        List<String> missing = new ArrayList<>();
        for (String id : ALL_ENCHANTMENT_IDS) {
            String key = "enchantment.meridian." + id + ".desc";
            if (!lang.has(key)) missing.add(key);
        }
        assertTrue(missing.isEmpty(),
                "missing enchantment .desc lang keys: " + missing);
    }

    @Test
    void langFile_hasNoStaleEnchantmentKeys() throws Exception {
        JsonObject lang = parseJson(LANG_FILE);
        Set<String> validPrefixes = Set.copyOf(ALL_ENCHANTMENT_IDS);
        List<String> stale = new ArrayList<>();
        for (String key : lang.keySet()) {
            if (!key.startsWith("enchantment.meridian.")) continue;
            String remainder = key.substring("enchantment.meridian.".length());
            String id = remainder.contains(".") ? remainder.substring(0, remainder.indexOf('.')) : remainder;
            if (!validPrefixes.contains(id)) stale.add(key);
        }
        assertTrue(stale.isEmpty(),
                "stale enchantment lang keys (no matching JSON): " + stale);
    }

    @TestFactory
    Stream<DynamicTest> everyEnchantmentDesc_isNonEmpty() {
        return ALL_ENCHANTMENT_IDS.stream().map(id -> DynamicTest.dynamicTest(id, () -> {
            JsonObject lang = parseJson(LANG_FILE);
            String key = "enchantment.meridian." + id + ".desc";
            assertTrue(lang.has(key), id + ": missing .desc key");
            String desc = lang.get(key).getAsString();
            assertFalse(desc.isBlank(), id + ": .desc value must not be blank");
        }));
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private static JsonObject parseJson(Path file) throws Exception {
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }
}
