// Tier: 2 (fabric-loader-junit)
package com.rfizzle.meridian.enchanting;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatJsonCodecSweepTest {

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @TestFactory
    Stream<DynamicTest> allShippedStatJsons_parseThroughCodec() throws IOException {
        Path statsDir = statJsonDir();
        assertTrue(Files.isDirectory(statsDir), "enchanting_stats directory must exist: " + statsDir);

        return Files.list(statsDir)
                .filter(p -> p.toString().endsWith(".json"))
                .sorted()
                .map(path -> DynamicTest.dynamicTest(path.getFileName().toString(), () -> {
                    JsonElement json;
                    try (Reader reader = Files.newBufferedReader(path)) {
                        json = JsonParser.parseReader(reader);
                    }
                    DataResult<EnchantingStatRegistry.StatEntry> result =
                            EnchantingStatRegistry.StatEntry.CODEC.parse(JsonOps.INSTANCE, json);

                    assertTrue(result.result().isPresent(),
                            "failed to parse " + path.getFileName() + ": "
                                    + result.error().map(DataResult.Error::message).orElse("unknown"));

                    EnchantingStatRegistry.StatEntry entry = result.result().get();
                    assertTrue(entry.block().isPresent() || entry.tag().isPresent(),
                            path.getFileName() + " must specify either 'block' or 'tag'");
                }));
    }

    @TestFactory
    Stream<DynamicTest> tagReferences_areValidTagKeyFormat() throws IOException {
        Path statsDir = statJsonDir();

        return Files.list(statsDir)
                .filter(p -> p.toString().endsWith(".json"))
                .sorted()
                .flatMap(path -> {
                    try {
                        JsonElement json;
                        try (Reader reader = Files.newBufferedReader(path)) {
                            json = JsonParser.parseReader(reader);
                        }
                        DataResult<EnchantingStatRegistry.StatEntry> result =
                                EnchantingStatRegistry.StatEntry.CODEC.parse(JsonOps.INSTANCE, json);
                        if (result.result().isEmpty()) return Stream.empty();
                        EnchantingStatRegistry.StatEntry entry = result.result().get();
                        if (entry.tag().isEmpty()) return Stream.empty();

                        return Stream.of(DynamicTest.dynamicTest(
                                path.getFileName() + " tag format",
                                () -> assertNotNull(entry.tag().get().location(),
                                        "tag key must have a valid ResourceLocation")));
                    } catch (IOException e) {
                        return Stream.empty();
                    }
                });
    }

    private static Path statJsonDir() {
        String genDir = System.getProperty("meridian.generated.dir");
        assertNotNull(genDir, "meridian.generated.dir system property must be set");
        return Path.of(genDir).getParent().resolve("resources/data/meridian/enchanting_stats");
    }
}
