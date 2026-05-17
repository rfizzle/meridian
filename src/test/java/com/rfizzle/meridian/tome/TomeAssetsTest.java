package com.rfizzle.meridian.tome;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * T-5.1.2 coverage: for each of the three tome ids, verifies the hand-shipped item-model JSON
 * at {@code assets/meridian/models/item/<id>.json}
 * <ul>
 *   <li>parents {@code minecraft:item/generated} — tomes are flat 2D items, not block models;</li>
 *   <li>points {@code layer0} at {@code meridian:item/tome/<id>} — the {@code tome/}
 *       subfolder segregates tome art from the flat {@code item/} textures (prismatic_web et al.);</li>
 *   <li>has the matching {@code .png} actually sitting on the classpath — drift between a model
 *       and its texture slot is invisible at datagen time but renders as a black-purple checker
 *       at runtime.</li>
 * </ul>
 * Also proves the three {@code item.meridian.<id>} lang keys are non-blank so the
 * inventory UI never shows a raw id.
 */
class TomeAssetsTest {

    private static final List<String> TOME_IDS = List.of(
            "scrap_tome", "improved_scrap_tome", "extraction_tome");

    @TestFactory
    Stream<DynamicTest> modelJson_parentsGeneratedAndPointsAtTomeTexture() {
        return TOME_IDS.stream().map(id -> DynamicTest.dynamicTest(id, () -> {
            String modelPath = "/assets/meridian/models/item/" + id + ".json";
            try (InputStream in = TomeAssetsTest.class.getResourceAsStream(modelPath)) {
                assertNotNull(in, () -> "missing model JSON at " + modelPath);
                JsonObject model = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8))
                        .getAsJsonObject();
                assertEquals("minecraft:item/generated", model.get("parent").getAsString(),
                        () -> id + ".json must parent minecraft:item/generated — tomes render as flat 2D items");
                JsonObject textures = model.getAsJsonObject("textures");
                assertNotNull(textures, () -> id + ".json must declare a textures block");
                assertEquals("meridian:item/tome/" + id, textures.get("layer0").getAsString(),
                        () -> id + ".json layer0 must resolve to the item/tome/ subfolder");
            }
        }));
    }

    @TestFactory
    Stream<DynamicTest> textureFile_existsOnClasspath() {
        return TOME_IDS.stream().map(id -> DynamicTest.dynamicTest(id, () -> {
            String texturePath = "/assets/meridian/textures/item/tome/" + id + ".png";
            try (InputStream in = TomeAssetsTest.class.getResourceAsStream(texturePath)) {
                assertNotNull(in, () -> "missing tome texture at " + texturePath);
            }
        }));
    }

    @TestFactory
    Stream<DynamicTest> langKey_nonBlank() {
        JsonObject lang;
        try (InputStream in = TomeAssetsTest.class.getResourceAsStream("/assets/meridian/lang/en_us.json")) {
            assertNotNull(in, "en_us.json must be on the test classpath");
            lang = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8))
                    .getAsJsonObject();
        } catch (java.io.IOException e) {
            throw new AssertionError("failed to read en_us.json", e);
        }
        return TOME_IDS.stream().map(id -> DynamicTest.dynamicTest(id, () -> {
            String key = "item.meridian." + id;
            assertTrue(lang.has(key), () -> "missing lang key: " + key);
            assertFalse(lang.get(key).getAsString().isBlank(),
                    () -> "lang value for " + key + " must not be blank");
        }));
    }
}
