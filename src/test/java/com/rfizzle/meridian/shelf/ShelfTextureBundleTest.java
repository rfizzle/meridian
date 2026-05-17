package com.rfizzle.meridian.shelf;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * T-3.2.3 coverage: confirms every shelf PNG ported from Zenith is on the classpath under the
 * 1.21.1-singular {@code textures/block/} path, that {@code blazing_hellshelf.png.mcmeta} ships
 * alongside the animated frame strip, and that the cut Apotheosis subsystems
 * ({@code blocks/reforging/}, {@code blocks/augmenting/}) leave no asset traces behind.
 */
class ShelfTextureBundleTest {

    private static final String BLOCK_TEXTURE_DIR = "/assets/meridian/textures/block/";

    private static final List<String> EXPECTED_SHELF_TEXTURES = List.of(
            "beeshelf.png",
            "blazing_hellshelf.png",
            "crystal_seashelf.png",
            "deepshelf.png",
            "draconic_endshelf.png",
            "echoing_deepshelf.png",
            "echoing_sculkshelf.png",
            "endshelf.png",
            "glowing_hellshelf.png",
            "heart_seashelf.png",
            "hellshelf.png",
            "melonshelf.png",
            "pearl_endshelf.png",
            "rectifier.png",
            "rectifier_t2.png",
            "rectifier_t2_top.png",
            "rectifier_t3.png",
            "sculkshelf_top.png",
            "seashelf.png",
            "sight_side.png",
            "sight_top.png",
            "sightshelf_t2.png",
            "sightshelf_t2_top.png",
            "soul_touched_deepshelf.png",
            "soul_touched_sculkshelf.png",
            "stoneshelf.png",
            "treasure_shelf_side.png",
            "treasure_shelf_top.png");

    private static Path blockTextureDir() throws Exception {
        URL url = ShelfTextureBundleTest.class.getResource(BLOCK_TEXTURE_DIR);
        assertNotNull(url, "block texture dir must be on the test classpath");
        return Paths.get(url.toURI());
    }

    @TestFactory
    Stream<DynamicTest> everyExpectedShelfTexture_isPresent() throws Exception {
        Path dir = blockTextureDir();
        return EXPECTED_SHELF_TEXTURES.stream().map(name -> DynamicTest.dynamicTest(name, () -> {
            Path file = dir.resolve(name);
            assertTrue(Files.isRegularFile(file), "missing shelf texture: " + name);
        }));
    }

    @Test
    void blazingHellshelf_hasAnimationMcmeta() throws Exception {
        Path mcmeta = blockTextureDir().resolve("blazing_hellshelf.png.mcmeta");
        assertTrue(Files.isRegularFile(mcmeta),
                "animated blazing_hellshelf must ship its .mcmeta frame definition");
    }

    @Test
    void cutSubsystemDirs_areAbsent() throws Exception {
        Path dir = blockTextureDir();
        assertFalse(Files.exists(dir.resolve("reforging")),
                "blocks/reforging/ belongs to the cut Apotheosis reforging system — must not be bundled");
        assertFalse(Files.exists(dir.resolve("augmenting")),
                "blocks/augmenting/ belongs to the cut Apotheosis augmenting system — must not be bundled");
    }
}
