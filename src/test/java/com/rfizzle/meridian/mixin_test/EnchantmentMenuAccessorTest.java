package com.rfizzle.meridian.mixin_test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.EnchantmentMenu;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * T-2.5.3 — structural verification that {@code EnchantmentMenuAccessor}
 * correctly exposes the three vanilla fields the stat-driven menu consumes
 * ({@code enchantSlots}, {@code random}, {@code enchantmentSeed}).
 *
 * <p>fabric-loader-junit is not on the test classpath (matches fizzle-difficulty),
 * so the Mixin transformer does not run during unit tests — we cannot actually
 * cast a constructed {@link EnchantmentMenu} to the accessor here. Instead we
 * inspect the accessor class + target class via ASM / reflection, defending
 * against the failure modes the transformer would catch at runtime:
 *
 * <ul>
 *   <li>Drift in vanilla field names (e.g. a Mojang rename silently detaches the accessor).
 *   <li>A missing / mis-declared {@code @Accessor} annotation.
 *   <li>An accessor method name that violates the /dev-companion prefix rule.
 *   <li>The config file forgetting to list the accessor so the transformer never sees it.
 * </ul>
 */
class EnchantmentMenuAccessorTest {

    private static final String MIXIN_DESC = Type.getDescriptor(org.spongepowered.asm.mixin.Mixin.class);
    private static final String ACCESSOR_DESC =
            Type.getDescriptor(org.spongepowered.asm.mixin.gen.Accessor.class);
    private static final String TARGET_INTERNAL_NAME = Type.getInternalName(EnchantmentMenu.class);
    private static final String MODID_PREFIX = "meridian$";

    /** field name → expected accessor return type. */
    private static final Map<String, Class<?>> EXPECTED_FIELDS = new HashMap<>();
    static {
        EXPECTED_FIELDS.put("enchantSlots", Container.class);
        EXPECTED_FIELDS.put("random", RandomSource.class);
        EXPECTED_FIELDS.put("enchantmentSeed", DataSlot.class);
    }

    @Test
    void targetFields_stillExistOnEnchantmentMenu() throws NoSuchFieldException {
        for (Map.Entry<String, Class<?>> entry : EXPECTED_FIELDS.entrySet()) {
            Field f = EnchantmentMenu.class.getDeclaredField(entry.getKey());
            assertEquals(entry.getValue(), f.getType(),
                    "field '" + entry.getKey() + "' type drift — if Mojang retyped it, "
                            + "fix the accessor and this test together");
        }
    }

    @Test
    void accessor_annotatesEnchantmentMenu() throws Exception {
        ClassNode node = readAccessorClass();
        AnnotationNode mixinAnn = findAnnotation(node.visibleAnnotations, MIXIN_DESC);
        if (mixinAnn == null) {
            mixinAnn = findAnnotation(node.invisibleAnnotations, MIXIN_DESC);
        }
        assertNotNull(mixinAnn, "@Mixin annotation must be present for the transformer to apply the accessor");

        List<?> values = extractArrayValue(mixinAnn, "value");
        assertNotNull(values, "@Mixin must specify value = EnchantmentMenu.class");
        assertEquals(1, values.size(), "a single target — accessor should not widen beyond EnchantmentMenu");
        assertEquals(TARGET_INTERNAL_NAME, ((Type) values.get(0)).getInternalName(),
                "@Mixin target must be EnchantmentMenu — a rename here silently detaches the accessor");
    }

    @Test
    void accessor_hasOneAccessorMethodPerField() throws Exception {
        ClassNode node = readAccessorClass();
        Map<String, MethodNode> byField = new HashMap<>();
        for (MethodNode m : node.methods) {
            AnnotationNode ann = findAnnotation(m.visibleAnnotations, ACCESSOR_DESC);
            if (ann == null) ann = findAnnotation(m.invisibleAnnotations, ACCESSOR_DESC);
            if (ann == null) continue;

            String fieldName = (String) extractValue(ann, "value");
            assertNotNull(fieldName,
                    "accessor '" + m.name + "' must declare @Accessor(\"<field>\") — "
                            + "the prefixed method name blocks Mixin's default name-derivation");
            assertTrue(EXPECTED_FIELDS.containsKey(fieldName),
                    "unexpected accessor target '" + fieldName + "' — keep the mixin surface minimal");
            assertTrue(m.name.startsWith(MODID_PREFIX),
                    "accessor method '" + m.name + "' must start with '" + MODID_PREFIX
                            + "' per /dev-companion's mixin naming rule");

            MethodNode prev = byField.put(fieldName, m);
            assertTrue(prev == null, "duplicate accessor for field '" + fieldName + "'");
        }
        assertEquals(EXPECTED_FIELDS.keySet(), byField.keySet(),
                "every expected field must have an @Accessor method");
    }

    @Test
    void accessor_returnTypesMatchFieldTypes() throws Exception {
        ClassNode node = readAccessorClass();
        for (MethodNode m : node.methods) {
            AnnotationNode ann = findAnnotation(m.visibleAnnotations, ACCESSOR_DESC);
            if (ann == null) ann = findAnnotation(m.invisibleAnnotations, ACCESSOR_DESC);
            if (ann == null) continue;

            String fieldName = (String) extractValue(ann, "value");
            Class<?> expectedType = EXPECTED_FIELDS.get(fieldName);
            Type returnType = Type.getReturnType(m.desc);
            assertEquals(Type.getDescriptor(expectedType), returnType.getDescriptor(),
                    "accessor '" + m.name + "' return type must match field '" + fieldName + "'");
            assertEquals(0, Type.getArgumentTypes(m.desc).length,
                    "getter '" + m.name + "' must take zero arguments");
        }
    }

    @Test
    void mixinConfig_listsThisAccessor() throws Exception {
        try (InputStream in = getClass().getClassLoader()
                .getResourceAsStream("meridian.mixins.json")) {
            assertNotNull(in, "meridian.mixins.json must be on the test classpath");
            JsonObject root = JsonParser.parseReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            List<String> mixins = new ArrayList<>();
            root.getAsJsonArray("mixins").forEach(e -> mixins.add(e.getAsString()));
            assertTrue(mixins.contains("EnchantmentMenuAccessor"),
                    "mixin config must list EnchantmentMenuAccessor — "
                            + "otherwise the transformer never applies the @Accessor interfaces");
        }
    }

    // --- ASM helpers ---

    private static ClassNode readAccessorClass() throws Exception {
        // String path — a class literal on a mixin-package class trips Knot's IllegalClassLoadError.
        String path = "com/rfizzle/meridian/mixin/EnchantmentMenuAccessor.class";
        try (InputStream in = EnchantmentMenuAccessorTest.class.getClassLoader().getResourceAsStream(path)) {
            assertNotNull(in, "accessor class file must be loadable from the classpath");
            ClassReader reader = new ClassReader(in);
            ClassNode node = new ClassNode();
            reader.accept(node, 0);
            return node;
        }
    }

    private static AnnotationNode findAnnotation(List<AnnotationNode> annotations, String descriptor) {
        if (annotations == null) return null;
        for (AnnotationNode a : annotations) {
            if (descriptor.equals(a.desc)) return a;
        }
        return null;
    }

    private static Object extractValue(AnnotationNode annotation, String key) {
        if (annotation.values == null) return null;
        for (int i = 0; i + 1 < annotation.values.size(); i += 2) {
            if (key.equals(annotation.values.get(i))) {
                return annotation.values.get(i + 1);
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> extractArrayValue(AnnotationNode annotation, String key) {
        Object v = extractValue(annotation, key);
        return v == null ? null : (List<T>) v;
    }
}
