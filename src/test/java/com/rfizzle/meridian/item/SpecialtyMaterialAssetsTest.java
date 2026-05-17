package com.rfizzle.meridian.item;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpecialtyMaterialAssetsTest {

    private static JsonObject lang;

    @BeforeAll
    static void loadLangFile() throws Exception {
        try (InputStream in = SpecialtyMaterialAssetsTest.class
                .getResourceAsStream("/assets/meridian/lang/en_us.json")) {
            assertNotNull(in, "en_us.json must be on the test classpath");
            lang = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8))
                    .getAsJsonObject();
        }
    }

    @Test
    void infusedBreathLangKey_nonBlank() {
        String key = "item.meridian.infused_breath";
        assertTrue(lang.has(key), "missing lang key: " + key);
        assertFalse(lang.get(key).getAsString().isBlank(),
                "lang value for " + key + " must not be blank");
    }

    @Test
    void wardenTendrilLangKey_nonBlank() {
        String key = "item.meridian.warden_tendril";
        assertTrue(lang.has(key), "missing lang key: " + key);
        assertFalse(lang.get(key).getAsString().isBlank(),
                "lang value for " + key + " must not be blank");
    }

    @Test
    void infusedBreathAnimationMcmeta_existsOnClasspath() {
        String path = "/assets/meridian/textures/item/infused_breath.png.mcmeta";
        assertNotNull(SpecialtyMaterialAssetsTest.class.getResourceAsStream(path),
                "missing animation metadata at " + path);
    }
}
