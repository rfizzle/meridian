package com.rfizzle.meridian.gametest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the gametest entrypoint wiring. Fabric only runs a gametest class that is listed in a
 * {@code fabric-gametest} entrypoint — an unlisted class compiles and is silently never run — so
 * the list is guarded against drift here rather than discovered as missing coverage later.
 *
 * <p>The entrypoints live in the gametest source set's own manifest, declaring a separate
 * {@code meridian-gametest} mod. That manifest is only on the {@code runGametest} classpath, so the
 * shipped jar never advertises entrypoints whose classes it does not carry.
 */
class GametestRegistrationTest {

    /**
     * Any annotation Fabric needs a registered class for: {@code @GameTest} and
     * {@code @GameTestGenerator}, whether imported or written fully qualified.
     */
    private static final Pattern GAMETEST_ANNOTATION =
            Pattern.compile("@(?:[\\w.]+\\.)?GameTest(?:Generator)?\\b");

    private static final Path MAIN_MANIFEST = resolve("meridian.main.manifest", "src/main/resources/fabric.mod.json");
    private static final Path GAMETEST_MANIFEST =
            resolve("meridian.gametest.manifest", "src/gametest/resources/fabric.mod.json");
    private static final Path GAMETEST_SOURCE_ROOT = resolve("meridian.gametest.sources", "src/gametest/java");

    private static Set<String> declared;
    private static Set<String> annotated;

    /**
     * Gradle injects absolute paths so the test does not depend on the launcher's working
     * directory; the repo-relative fallback keeps it runnable from the project root directly.
     */
    private static Path resolve(String property, String fallback) {
        String injected = System.getProperty(property);
        return injected != null ? Path.of(injected) : Path.of(fallback);
    }

    @BeforeAll
    static void scanOnce() throws IOException {
        declared = declaredClasses();
        annotated = annotatedClasses();
    }

    private static JsonObject loadJson(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " must exist");
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    /** The fully-qualified class names declared in the gametest manifest. */
    private static Set<String> declaredClasses() throws IOException {
        JsonObject root = loadJson(GAMETEST_MANIFEST);
        assertTrue(root.has("id"), GAMETEST_MANIFEST + " must declare a mod id");
        assertEquals("meridian-gametest", root.get("id").getAsString(),
                "The gametest manifest must declare its own mod id, separate from the shipped mod");

        JsonObject entrypoints = root.getAsJsonObject("entrypoints");
        assertNotNull(entrypoints, "Gametest manifest must have entrypoints");
        JsonArray entries = entrypoints.getAsJsonArray("fabric-gametest");
        assertNotNull(entries, "Gametest manifest must declare a fabric-gametest entrypoint");

        Set<String> classes = new TreeSet<>();
        for (JsonElement element : entries) {
            // Schema v1 also permits an {"adapter":…,"value":…} object; this manifest uses plain
            // strings, and asserting so beats a bare UnsupportedOperationException from Gson.
            assertTrue(element.isJsonPrimitive(),
                    "fabric-gametest entries must be plain class-name strings, got: " + element);
            assertTrue(classes.add(element.getAsString()),
                    "Duplicate fabric-gametest entry: " + element.getAsString());
        }
        return classes;
    }

    /** Every class under the gametest source root carrying an annotation that needs registration. */
    private static Set<String> annotatedClasses() throws IOException {
        assertTrue(Files.isDirectory(GAMETEST_SOURCE_ROOT), GAMETEST_SOURCE_ROOT + " must exist");
        try (Stream<Path> files = Files.walk(GAMETEST_SOURCE_ROOT)) {
            return files.filter(path -> path.toString().endsWith(".java"))
                    .filter(GametestRegistrationTest::hasGameTestAnnotation)
                    .map(GametestRegistrationTest::toClassName)
                    .collect(TreeSet::new, Set::add, Set::addAll);
        }
    }

    private static boolean hasGameTestAnnotation(Path javaFile) {
        try (Stream<String> lines = Files.lines(javaFile, StandardCharsets.UTF_8)) {
            return lines.anyMatch(line -> GAMETEST_ANNOTATION.matcher(line).find());
        } catch (IOException e) {
            throw new UncheckedIOException("Unreadable gametest source: " + javaFile, e);
        }
    }

    private static String toClassName(Path javaFile) {
        String relative = GAMETEST_SOURCE_ROOT.relativize(javaFile).toString();
        return relative.substring(0, relative.length() - ".java".length())
                .replace(java.io.File.separatorChar, '.');
    }

    @Test
    void everyAnnotatedGametestClassIsDeclared() {
        Set<String> undeclared = new TreeSet<>(annotated);
        undeclared.removeAll(declared);
        assertTrue(undeclared.isEmpty(),
                "These classes carry a gametest annotation but are absent from the fabric-gametest "
                        + "entrypoint, so they would never run: " + undeclared);
    }

    @Test
    void everyDeclaredEntryResolvesToASourceFile() {
        Set<String> stale = new TreeSet<>(declared);
        stale.removeAll(annotated);
        assertTrue(stale.isEmpty(),
                "These fabric-gametest entries name no gametest-annotated source file under "
                        + GAMETEST_SOURCE_ROOT + ": " + stale);
    }

    /**
     * The shipped manifest must not advertise gametest entrypoints. Their classes live in the
     * gametest source set and are absent from the jar and from the {@code runServer} classpath, so
     * declaring them here crashes any launch whose runtime carries fabric-gametest-api.
     */
    @Test
    void mainManifestDeclaresNoGametestEntrypoints() throws IOException {
        JsonObject entrypoints = loadJson(MAIN_MANIFEST).getAsJsonObject("entrypoints");
        assertNotNull(entrypoints, "fabric.mod.json must have entrypoints");
        assertFalse(entrypoints.has("fabric-gametest"),
                "The shipped manifest must not declare fabric-gametest entrypoints — they belong "
                        + "in " + GAMETEST_MANIFEST);

        // The entrypoints the shipped jar does own stay put.
        for (String key : List.of("main", "client", "fabric-datagen")) {
            assertTrue(entrypoints.has(key), "fabric.mod.json must still declare: " + key);
        }
    }

    /**
     * The gametest mod loads beside the real one, so a drifted toolchain bound fails the whole
     * {@code runGametest} launch with a dependency error that reads like a Loom problem.
     */
    @Test
    void gametestManifestToolchainBoundsMatchTheShippedManifest() throws IOException {
        JsonObject mainDepends = loadJson(MAIN_MANIFEST).getAsJsonObject("depends");
        JsonObject gametestDepends = loadJson(GAMETEST_MANIFEST).getAsJsonObject("depends");
        assertNotNull(gametestDepends, "Gametest manifest must declare depends");

        for (String key : List.of("fabricloader", "minecraft", "java", "fabric-api")) {
            assertTrue(gametestDepends.has(key), "Gametest manifest must pin: " + key);
            assertEquals(mainDepends.get(key).getAsString(), gametestDepends.get(key).getAsString(),
                    "Gametest manifest toolchain bound must match the shipped manifest: " + key);
        }
        assertTrue(gametestDepends.has("meridian"),
                "The gametest mod must depend on the mod it tests");
    }
}
