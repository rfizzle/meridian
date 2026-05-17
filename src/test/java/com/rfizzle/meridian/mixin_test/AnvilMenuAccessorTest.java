package com.rfizzle.meridian.mixin_test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.DataSlot;
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
 * T-4.1.1 — structural verification for {@code AnvilMenuAccessor}. Mirrors the pattern in
 * {@link EnchantmentMenuAccessorTest}: fabric-loader-junit is not on the test classpath, so the
 * Mixin transformer does not apply the accessor at test time. Instead we inspect the target
 * fields + the accessor class via reflection and ASM, defending against the failure modes the
 * runtime transformer would catch —
 *
 * <ul>
 *   <li>Drift in vanilla field names (e.g. a Mojang rename silently detaches the accessor).
 *   <li>A missing or mis-declared {@code @Accessor} annotation.
 *   <li>Accessor method names that violate the /dev-companion prefix rule.
 *   <li>The mixin config forgetting to list the accessor.
 * </ul>
 */
class AnvilMenuAccessorTest {

    private static final String MIXIN_DESC = Type.getDescriptor(org.spongepowered.asm.mixin.Mixin.class);
    private static final String ACCESSOR_DESC =
            Type.getDescriptor(org.spongepowered.asm.mixin.gen.Accessor.class);
    private static final String TARGET_INTERNAL_NAME = Type.getInternalName(AnvilMenu.class);
    private static final String MODID_PREFIX = "meridian$";

    /** field name → expected field type. */
    private static final Map<String, Class<?>> EXPECTED_FIELDS = new HashMap<>();
    static {
        EXPECTED_FIELDS.put("cost", DataSlot.class);
        EXPECTED_FIELDS.put("repairItemCountCost", int.class);
    }

    @Test
    void targetFields_stillExistOnAnvilMenu() throws NoSuchFieldException {
        for (Map.Entry<String, Class<?>> entry : EXPECTED_FIELDS.entrySet()) {
            Field f = AnvilMenu.class.getDeclaredField(entry.getKey());
            assertEquals(entry.getValue(), f.getType(),
                    "field '" + entry.getKey() + "' type drift — if Mojang retyped it, "
                            + "fix the accessor and this test together");
        }
    }

    @Test
    void accessor_annotatesAnvilMenu() throws Exception {
        ClassNode node = readAccessorClass();
        AnnotationNode mixinAnn = findAnnotation(node.visibleAnnotations, MIXIN_DESC);
        if (mixinAnn == null) {
            mixinAnn = findAnnotation(node.invisibleAnnotations, MIXIN_DESC);
        }
        assertNotNull(mixinAnn, "@Mixin annotation must be present for the transformer to apply the accessor");

        List<?> values = extractArrayValue(mixinAnn, "value");
        assertNotNull(values, "@Mixin must specify value = AnvilMenu.class");
        assertEquals(1, values.size(), "a single target — accessor should not widen beyond AnvilMenu");
        assertEquals(TARGET_INTERNAL_NAME, ((Type) values.get(0)).getInternalName(),
                "@Mixin target must be AnvilMenu — a rename here silently detaches the accessor");
    }

    @Test
    void accessor_hasGetterForEachField() throws Exception {
        ClassNode node = readAccessorClass();
        Map<String, List<MethodNode>> byField = new HashMap<>();
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
            byField.computeIfAbsent(fieldName, k -> new ArrayList<>()).add(m);
        }
        assertEquals(EXPECTED_FIELDS.keySet(), byField.keySet(),
                "every expected field must have at least one @Accessor method");
    }

    @Test
    void repairItemCountCost_hasBothGetterAndSetter() throws Exception {
        ClassNode node = readAccessorClass();
        boolean hasGetter = false;
        boolean hasSetter = false;
        for (MethodNode m : node.methods) {
            AnnotationNode ann = findAnnotation(m.visibleAnnotations, ACCESSOR_DESC);
            if (ann == null) ann = findAnnotation(m.invisibleAnnotations, ACCESSOR_DESC);
            if (ann == null) continue;
            if (!"repairItemCountCost".equals(extractValue(ann, "value"))) continue;

            Type[] args = Type.getArgumentTypes(m.desc);
            Type ret = Type.getReturnType(m.desc);
            if (args.length == 0 && ret.getSort() == Type.INT) {
                hasGetter = true;
            } else if (args.length == 1 && args[0].getSort() == Type.INT
                    && ret.getSort() == Type.VOID) {
                hasSetter = true;
            }
        }
        assertTrue(hasGetter, "repairItemCountCost needs an @Accessor int getter");
        assertTrue(hasSetter, "repairItemCountCost needs an @Accessor void setter — "
                + "the dispatcher overwrites this field when a handler fires");
    }

    @Test
    void costGetter_returnsDataSlot() throws Exception {
        ClassNode node = readAccessorClass();
        boolean hasGetter = false;
        for (MethodNode m : node.methods) {
            AnnotationNode ann = findAnnotation(m.visibleAnnotations, ACCESSOR_DESC);
            if (ann == null) ann = findAnnotation(m.invisibleAnnotations, ACCESSOR_DESC);
            if (ann == null) continue;
            if (!"cost".equals(extractValue(ann, "value"))) continue;

            Type ret = Type.getReturnType(m.desc);
            if (Type.getArgumentTypes(m.desc).length == 0
                    && Type.getDescriptor(DataSlot.class).equals(ret.getDescriptor())) {
                hasGetter = true;
            }
        }
        assertTrue(hasGetter, "cost must expose a DataSlot getter — callers mutate via dataSlot.set(int)");
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
            assertTrue(mixins.contains("AnvilMenuAccessor"),
                    "mixin config must list AnvilMenuAccessor — "
                            + "otherwise the transformer never applies the @Accessor interface");
        }
    }

    // --- ASM helpers ---

    private static ClassNode readAccessorClass() throws Exception {
        // String path — a class literal on a mixin-package class trips Knot's IllegalClassLoadError.
        String path = "com/rfizzle/meridian/mixin/AnvilMenuAccessor.class";
        try (InputStream in = AnvilMenuAccessorTest.class.getClassLoader().getResourceAsStream(path)) {
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
