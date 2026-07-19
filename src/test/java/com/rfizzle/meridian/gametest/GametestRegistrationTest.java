package com.rfizzle.meridian.gametest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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

    private static final Path MAIN_MANIFEST = Path.of("src/main/resources/fabric.mod.json");
    private static final Path GAMETEST_MANIFEST = Path.of("src/gametest/resources/fabric.mod.json");
    private static final Path GAMETEST_SOURCE_ROOT = Path.of("src/gametest/java");

    private static JsonObject loadJson(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " must exist");
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    /** The fully-qualified class names declared in the gametest manifest. */
    private static Set<String> declaredClasses() throws IOException {
        JsonObject root = loadJson(GAMETEST_MANIFEST);
        assertEquals("meridian-gametest", root.get("id").getAsString(),
                "The gametest manifest must declare its own mod id, separate from the shipped mod");

        JsonObject entrypoints = root.getAsJsonObject("entrypoints");
        assertNotNull(entrypoints, "Gametest manifest must have entrypoints");
        JsonArray declared = entrypoints.getAsJsonArray("fabric-gametest");
        assertNotNull(declared, "Gametest manifest must declare a fabric-gametest entrypoint");

        Set<String> classes = new TreeSet<>();
        for (JsonElement element : declared) {
            assertTrue(classes.add(element.getAsString()),
                    "Duplicate fabric-gametest entry: " + element.getAsString());
        }
        return classes;
    }

    /** Every class under the gametest source root that carries at least one {@code @GameTest}. */
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
        try {
            return Files.readAllLines(javaFile, StandardCharsets.UTF_8).stream()
                    .anyMatch(line -> line.stripLeading().startsWith("@GameTest"));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String toClassName(Path javaFile) {
        String relative = GAMETEST_SOURCE_ROOT.relativize(javaFile).toString();
        return relative.substring(0, relative.length() - ".java".length())
                .replace(java.io.File.separatorChar, '.');
    }

    @Test
    void everyAnnotatedGametestClassIsDeclared() throws IOException {
        Set<String> undeclared = new TreeSet<>(annotatedClasses());
        undeclared.removeAll(declaredClasses());
        assertTrue(undeclared.isEmpty(),
                "These classes carry @GameTest but are absent from the fabric-gametest entrypoint, "
                        + "so they would never run: " + undeclared);
    }

    @Test
    void everyDeclaredEntryResolvesToASourceFile() throws IOException {
        Set<String> stale = new TreeSet<>(declaredClasses());
        stale.removeAll(annotatedClasses());
        assertTrue(stale.isEmpty(),
                "These fabric-gametest entries name no @GameTest-bearing source file under "
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
}
