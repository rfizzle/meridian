package com.rfizzle.meridian;

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
import java.util.TreeMap;
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
 *
 * <p>Suites are detected by their {@code implements FabricGameTest} clause — the same predicate the
 * loader uses — rather than by an annotation regex (which matches {@code @GameTest} inside comments
 * and string literals) or a filename suffix (which lets a mis-named suite vanish from both sides of
 * the comparison at once). A suite that inherits the interface from an abstract base is rejected
 * on purpose: the manifest needs the concrete class named anyway, and the naming check below fails
 * it loudly instead of letting it register implicitly.
 */
class GametestRegistrationTest {

    /** Matches a class's {@code implements} clause naming FabricGameTest. */
    private static final Pattern IMPLEMENTS_FABRIC_GAMETEST =
            Pattern.compile("implements\\s+[^{]*\\bFabricGameTest\\b");

    private static final Path MAIN_MANIFEST = resolve("meridian.main.manifest", "src/main/resources/fabric.mod.json");
    private static final Path GAMETEST_MANIFEST =
            resolve("meridian.gametest.manifest", "src/gametest/resources/fabric.mod.json");
    private static final Path GAMETEST_SOURCE_ROOT = resolve("meridian.gametest.sources", "src/gametest/java");

    private static Set<String> declared;
    /** Fully-qualified name of every class under the gametest tree, mapped to its source text. */
    private static TreeMap<String, String> sources;

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
        sources = gametestSources();
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

    private static TreeMap<String, String> gametestSources() throws IOException {
        assertTrue(Files.isDirectory(GAMETEST_SOURCE_ROOT), GAMETEST_SOURCE_ROOT + " must exist");
        TreeMap<String, String> result = new TreeMap<>();
        try (Stream<Path> files = Files.walk(GAMETEST_SOURCE_ROOT)) {
            files.filter(path -> path.toString().endsWith(".java")).forEach(path -> {
                try {
                    result.put(toClassName(path), Files.readString(path, StandardCharsets.UTF_8));
                } catch (IOException e) {
                    throw new UncheckedIOException("Unreadable gametest source: " + path, e);
                }
            });
        }
        return result;
    }

    private static boolean isSuite(String source) {
        return IMPLEMENTS_FABRIC_GAMETEST.matcher(source).find();
    }

    /** Every class under the gametest source root that implements {@code FabricGameTest}. */
    private static Set<String> suitesOnDisk() {
        Set<String> suites = new TreeSet<>();
        sources.forEach((className, source) -> {
            if (isSuite(source)) {
                suites.add(className);
            }
        });
        return suites;
    }

    private static String toClassName(Path javaFile) {
        String relative = GAMETEST_SOURCE_ROOT.relativize(javaFile).toString();
        return relative.substring(0, relative.length() - ".java".length())
                .replace(java.io.File.separatorChar, '.');
    }

    @Test
    void everySuiteOnDiskIsDeclared() {
        Set<String> undeclared = new TreeSet<>(suitesOnDisk());
        undeclared.removeAll(declared);
        assertTrue(undeclared.isEmpty(),
                "These classes implement FabricGameTest but are absent from the fabric-gametest "
                        + "entrypoint, so they would never run: " + undeclared);
    }

    @Test
    void everyDeclaredEntryResolvesToASuiteOnDisk() {
        Set<String> stale = new TreeSet<>(declared);
        stale.removeAll(suitesOnDisk());
        assertTrue(stale.isEmpty(),
                "These fabric-gametest entries name no FabricGameTest class under "
                        + GAMETEST_SOURCE_ROOT + " — the gametest run would fail to load them: " + stale);
    }

    /**
     * Matching suites by interface closes the "helper flagged as unregistered" hole; enforcing the
     * name closes the other one, where a suite called {@code FooTests} goes missing from the source
     * tree scan and the manifest at the same time and the guards above stay green.
     */
    @Test
    void suiteNamingConventionHoldsInBothDirections() {
        Set<String> misnamedSuites = new TreeSet<>();
        Set<String> impostors = new TreeSet<>();
        sources.forEach((className, source) -> {
            boolean suite = isSuite(source);
            boolean named = className.endsWith("GameTest");
            if (suite && !named) {
                misnamedSuites.add(className);
            } else if (!suite && named) {
                impostors.add(className);
            }
        });
        assertTrue(misnamedSuites.isEmpty(),
                "FabricGameTest implementors must be named *GameTest: " + misnamedSuites);
        assertTrue(impostors.isEmpty(),
                "classes named *GameTest must implement FabricGameTest: " + impostors);
    }

    /**
     * Suites live in {@code com.rfizzle.meridian.gametest} or a subpackage of it other than
     * {@code util}; non-suite helpers live in {@code gametest.util}. A suite in {@code <mod>.event}
     * is invisible to anyone looking for the test suite and to tooling that scopes by package.
     */
    @Test
    void suitesLiveUnderTheGametestPackage() {
        Set<String> strays = new TreeSet<>();
        for (String suite : suitesOnDisk()) {
            if (!suite.startsWith("com.rfizzle.meridian.gametest.")
                    || suite.startsWith("com.rfizzle.meridian.gametest.util.")) {
                strays.add(suite);
            }
        }
        assertTrue(strays.isEmpty(),
                "Gametest suites belong under com.rfizzle.meridian.gametest (not util): " + strays);
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
     * The gametest mod depends on the main mod and nothing else. Loader, Minecraft, Java, and
     * Fabric API are enforced transitively — {@code meridian-gametest} cannot load unless
     * {@code meridian} did, and {@code meridian} cannot load without them — so restating their
     * floors here would only add a second place to update on every toolchain bump, where a missed
     * edit surfaces as a confusing load failure inside {@code runGametest}.
     */
    @Test
    void gametestManifestDependsOnlyOnTheMod() throws IOException {
        JsonObject gametestDepends = loadJson(GAMETEST_MANIFEST).getAsJsonObject("depends");
        assertNotNull(gametestDepends, "Gametest manifest must declare depends");

        assertTrue(gametestDepends.has("meridian"),
                "The gametest mod must depend on the mod it tests");
        assertEquals(Set.of("meridian"), gametestDepends.keySet(),
                "Gametest manifest depends must name only the main mod — the toolchain floors are "
                        + "enforced transitively and belong solely in " + MAIN_MANIFEST);
    }
}
