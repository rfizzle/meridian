// Tier: 3 (Fabric Gametest)
package com.rfizzle.meridian.gametest;

import com.rfizzle.meridian.Meridian;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.advancements.Advancement;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.RegistryOps;

import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

public class AdvancementCodecGameTest implements FabricGameTest {

    private static final String ADVANCEMENT_DIR = "/data/meridian/advancement/";
    private static final String LANG_FILE = "/assets/meridian/lang/en_us.json";
    private static final List<String> ADVANCEMENT_IDS = List.of(
            "root", "stone_tier", "tier_three", "library", "ender_library",
            "tome_apprentice", "tome_master", "warden_tendril", "infused_breath", "apotheosis",
            "sculk_mastery", "stable_enchanting", "all_seeing", "curator", "treasure_seeker",
            "web_spinner", "high_arcana", "high_quanta");

    @GameTest(template = "meridian:empty_3x3")
    public void advancementDirectoryShipsExpectedRoster(GameTestHelper helper) {
        try {
            URL url = getClass().getResource(ADVANCEMENT_DIR);
            if (url == null) {
                helper.fail("advancement resource dir not on classpath");
                return;
            }
            List<String> shipped;
            try (Stream<Path> files = Files.list(Paths.get(url.toURI()))) {
                shipped = files
                        .filter(p -> p.getFileName().toString().endsWith(".json"))
                        .map(p -> p.getFileName().toString().replace(".json", ""))
                        .sorted()
                        .toList();
            }
            List<String> expected = ADVANCEMENT_IDS.stream().sorted().toList();
            if (!expected.equals(shipped)) {
                helper.fail("Expected roster " + expected + " but found " + shipped);
                return;
            }
        } catch (Exception e) {
            helper.fail("Failed to read advancement dir: " + e.getMessage());
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void everyAdvancementParsesWithCriteriaAndDisplay(GameTestHelper helper) {
        RegistryOps<JsonElement> ops = RegistryOps.create(
                com.mojang.serialization.JsonOps.INSTANCE,
                helper.getLevel().registryAccess());
        try {
            URL url = getClass().getResource(ADVANCEMENT_DIR);
            if (url == null) {
                helper.fail("advancement resource dir not on classpath");
                return;
            }
            List<Path> jsonFiles;
            try (Stream<Path> files = Files.list(Paths.get(url.toURI()))) {
                jsonFiles = files.filter(p -> p.getFileName().toString().endsWith(".json")).sorted().toList();
            }
            for (Path file : jsonFiles) {
                try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    JsonElement json = JsonParser.parseReader(reader);
                    DataResult<Advancement> result = Advancement.CODEC.parse(ops, json);
                    if (result.error().isPresent()) {
                        helper.fail("Parse failed for " + file.getFileName() + ": "
                                + result.error().get().message());
                        return;
                    }
                    Advancement adv = result.result().orElseThrow();
                    if (adv.criteria().isEmpty()) {
                        helper.fail(file.getFileName() + " must declare at least one criterion");
                        return;
                    }
                    if (adv.display().isEmpty()) {
                        helper.fail(file.getFileName() + " must carry a display block");
                        return;
                    }
                }
            }
        } catch (Exception e) {
            helper.fail("Exception during advancement parse: " + e.getMessage());
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void everyAdvancementHasLangKeys(GameTestHelper helper) {
        try {
            URL langUrl = getClass().getResource(LANG_FILE);
            if (langUrl == null) {
                helper.fail("lang file not on classpath");
                return;
            }
            JsonObject lang;
            try (Reader reader = Files.newBufferedReader(Paths.get(langUrl.toURI()), StandardCharsets.UTF_8)) {
                lang = JsonParser.parseReader(reader).getAsJsonObject();
            }
            for (String id : ADVANCEMENT_IDS) {
                String titleKey = "advancements.meridian." + id + ".title";
                String descKey = "advancements.meridian." + id + ".description";
                if (!lang.has(titleKey)) {
                    helper.fail("lang must carry " + titleKey);
                    return;
                }
                if (!lang.has(descKey)) {
                    helper.fail("lang must carry " + descKey);
                    return;
                }
                if (lang.get(titleKey).getAsString().isBlank()) {
                    helper.fail(titleKey + " must be non-blank");
                    return;
                }
                if (lang.get(descKey).getAsString().isBlank()) {
                    helper.fail(descKey + " must be non-blank");
                    return;
                }
            }
        } catch (Exception e) {
            helper.fail("Exception reading lang file: " + e.getMessage());
            return;
        }
        helper.succeed();
    }
}
