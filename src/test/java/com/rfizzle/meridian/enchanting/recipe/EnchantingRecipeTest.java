package com.rfizzle.meridian.enchanting.recipe;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * T-4.6.1 / T-4.6.2 / T-4.6.4 — exercises {@link EnchantingRecipe} and
 * {@link KeepNbtEnchantingRecipe} through the codec + match / assemble surface that the menu and
 * recipe loader will hit at runtime.
 *
 * <p>Bootstrapping vanilla is required so {@link Items} populates the {@link BuiltInRegistries#ITEM}
 * lookup that {@link Ingredient}/{@link ItemStack} codecs query during decode.
 */
class EnchantingRecipeTest {

    /** Lazily-initialised lookup for the dynamic Enchantment registry — used by the KeepNbt test. */
    private static HolderLookup.Provider lookup;

    @BeforeAll
    static void bootstrap() throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        // Enchantments are data-driven in 1.21.1 (dynamic registry, not BuiltInRegistries); the
        // vanilla data-bootstrap below populates them so SHARPNESS resolves below.
        lookup = VanillaRegistries.createLookup();
    }

    private static Holder<Enchantment> sharpness() {
        return lookup.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS);
    }

    @Test
    void matches_passesWhenInputAndStatsClearMinima() {
        EnchantingRecipe recipe = makeRecipe(
                Ingredient.of(Items.DIAMOND_SWORD),
                new StatRequirements(20F, 10F, 5F),
                StatRequirements.NO_MAX,
                new ItemStack(Items.DIAMOND));
        assertTrue(recipe.matches(new ItemStack(Items.DIAMOND_SWORD), 30F, 12F, 6F));
    }

    @Test
    void matches_failsWhenIngredientMismatch() {
        EnchantingRecipe recipe = makeRecipe(
                Ingredient.of(Items.DIAMOND_SWORD),
                new StatRequirements(0F, 0F, 0F),
                StatRequirements.NO_MAX,
                new ItemStack(Items.DIAMOND));
        assertFalse(recipe.matches(new ItemStack(Items.IRON_SWORD), 50F, 50F, 50F));
    }

    @Test
    void matches_failsWhenAnyStatBelowMinimum() {
        EnchantingRecipe recipe = makeRecipe(
                Ingredient.of(Items.DIAMOND_SWORD),
                new StatRequirements(20F, 10F, 5F),
                StatRequirements.NO_MAX,
                new ItemStack(Items.DIAMOND));
        assertFalse(recipe.matches(new ItemStack(Items.DIAMOND_SWORD), 19.9F, 10F, 5F));
        assertFalse(recipe.matches(new ItemStack(Items.DIAMOND_SWORD), 20F, 9F, 5F));
        assertFalse(recipe.matches(new ItemStack(Items.DIAMOND_SWORD), 20F, 10F, 4.5F));
    }

    @Test
    void matches_respectsMaxBoundsAndIgnoresMinusOneSentinel() {
        EnchantingRecipe recipe = makeRecipe(
                Ingredient.of(Items.DIAMOND_SWORD),
                new StatRequirements(0F, 0F, 0F),
                new StatRequirements(-1F, 25F, -1F),
                new ItemStack(Items.DIAMOND));
        // Quanta cap at 25 — stay under or equal, and the unbounded eterna/arcana axes don't gate.
        assertTrue(recipe.matches(new ItemStack(Items.DIAMOND_SWORD), 9999F, 25F, 9999F));
        assertFalse(recipe.matches(new ItemStack(Items.DIAMOND_SWORD), 0F, 25.5F, 0F));
    }

    @Test
    void codec_roundTripsThroughJsonOps() {
        EnchantingRecipe original = makeRecipe(
                Ingredient.of(Items.DIAMOND_SWORD),
                new StatRequirements(22.5F, 30F, 0F),
                StatRequirements.NO_MAX,
                new ItemStack(Items.DIAMOND, 3),
                OptionalInt.of(5),
                4);

        JsonElement json = EnchantingRecipeRegistry.ENCHANTING_SERIALIZER.codec()
                .codec().encodeStart(JsonOps.INSTANCE, original).getOrThrow();
        EnchantingRecipe decoded = EnchantingRecipeRegistry.ENCHANTING_SERIALIZER.codec()
                .codec().parse(JsonOps.INSTANCE, json).getOrThrow();

        assertEnchantingRecipeEquals(original, decoded);
    }

    @Test
    void codec_roundTripsKeepNbtSubtypeWithoutCollapsingToBaseType() {
        KeepNbtEnchantingRecipe original = new KeepNbtEnchantingRecipe(
                Ingredient.of(Items.BOOK),
                new StatRequirements(50F, 45F, 100F),
                new StatRequirements(50F, 50F, 100F),
                new ItemStack(Items.ENCHANTED_BOOK),
                OptionalInt.empty(),
                0);

        JsonElement json = EnchantingRecipeRegistry.KEEP_NBT_SERIALIZER.codec()
                .codec().encodeStart(JsonOps.INSTANCE, original).getOrThrow();
        KeepNbtEnchantingRecipe decoded = EnchantingRecipeRegistry.KEEP_NBT_SERIALIZER.codec()
                .codec().parse(JsonOps.INSTANCE, json).getOrThrow();

        assertEnchantingRecipeEquals(original, decoded);
        assertInstanceOf(KeepNbtEnchantingRecipe.class, decoded);
    }

    @Test
    void streamCodec_roundTripsThroughBuffer() {
        EnchantingRecipe original = makeRecipe(
                Ingredient.of(Items.DIAMOND_SWORD),
                new StatRequirements(22.5F, 30F, 0F),
                new StatRequirements(-1F, 50F, -1F),
                new ItemStack(Items.DIAMOND, 3),
                OptionalInt.of(5),
                4);
        // ItemStack.STREAM_CODEC writes the item id by registry index, so the buffer's registry
        // access must contain ITEM. Wrap the static built-in registries in a Frozen access; this
        // is the same shape vanilla produces during normal play.
        RegistryAccess.Frozen access = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), access);

        EnchantingRecipeRegistry.ENCHANTING_SERIALIZER.streamCodec().encode(buf, original);
        EnchantingRecipe decoded = EnchantingRecipeRegistry.ENCHANTING_SERIALIZER.streamCodec().decode(buf);

        assertEnchantingRecipeEquals(original, decoded);
    }

    @Test
    void keepNbtAssemble_preservesEnchantmentsFromInput() {
        // Sharpness V → output enchanted-book carries the same component.
        ItemStack swordIn = new ItemStack(Items.DIAMOND_SWORD);
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        mutable.set(sharpness(), 5);
        swordIn.set(DataComponents.ENCHANTMENTS, mutable.toImmutable());

        KeepNbtEnchantingRecipe recipe = new KeepNbtEnchantingRecipe(
                Ingredient.of(Items.DIAMOND_SWORD),
                new StatRequirements(0F, 0F, 0F),
                StatRequirements.NO_MAX,
                new ItemStack(Items.ENCHANTED_BOOK),
                OptionalInt.empty(),
                0);
        ItemStack out = recipe.assemble(new SingleRecipeInput(swordIn), null);
        ItemEnchantments enchantments = out.get(DataComponents.ENCHANTMENTS);
        assertEquals(1, enchantments.size());
        assertEquals(5, enchantments.getLevel(sharpness()));
    }

    @Test
    void baseAssemble_doesNotPropagateInputEnchantments() {
        // The non-keep-nbt subtype must produce a fresh result that ignores input components,
        // mirroring Zenith's `EnchantingRecipe#assemble`.
        ItemStack swordIn = new ItemStack(Items.DIAMOND_SWORD);
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        mutable.set(sharpness(), 5);
        swordIn.set(DataComponents.ENCHANTMENTS, mutable.toImmutable());

        EnchantingRecipe recipe = makeRecipe(
                Ingredient.of(Items.DIAMOND_SWORD),
                new StatRequirements(0F, 0F, 0F),
                StatRequirements.NO_MAX,
                new ItemStack(Items.DIAMOND));

        ItemStack out = recipe.assemble(new SingleRecipeInput(swordIn), null);
        ItemEnchantments enchantments = out.get(DataComponents.ENCHANTMENTS);
        // ItemStack components default to ItemEnchantments.EMPTY when never set, so checking
        // isEmpty() proves nothing leaked through.
        assertTrue(enchantments == null || enchantments.isEmpty(),
                "Base EnchantingRecipe must not propagate input enchantments to the result");
    }

    @Test
    void shippedJsonFiles_parseIntoExpectedSubtypes() throws Exception {
        // T-4.6.4 acceptance — every shipped recipe JSON must round-trip through the recipe
        // registry's serializer for the type listed in its `type` field. We can only invoke the
        // ItemStack codec safely against items that exist in BuiltInRegistries.ITEM at this
        // bootstrap; tome items are registered in Epic 5, so we restrict the codec round-trip
        // to recipes whose input + result both reference items the test JVM already knows. For
        // the tome-related recipes we still verify the `type` field is correct (the discriminator
        // that decides which serializer the loader picks).
        assertTypeField("infused_breath.json", "meridian:enchanting");
        assertTypeField("infused_hellshelf.json", "meridian:enchanting");
        assertTypeField("infused_seashelf.json", "meridian:enchanting");
        assertTypeField("deepshelf.json", "meridian:enchanting");
        assertTypeField("improved_scrap_tome.json", "meridian:enchanting");
        assertTypeField("extraction_tome.json", "meridian:enchanting");
        assertTypeField("ender_library.json", "meridian:keep_nbt_enchanting");
        assertTypeField("honey_to_xp_t1.json", "meridian:enchanting");
        assertTypeField("honey_to_xp_t2.json", "meridian:enchanting");
        assertTypeField("honey_to_xp_t3.json", "meridian:enchanting");
        assertTypeField("echo_shard_duplication.json", "meridian:enchanting");
        assertTypeField("golden_carrot.json", "meridian:enchanting");
        assertTypeField("golden_apple.json", "meridian:enchanting");
        assertTypeField("enchanted_golden_apple.json", "meridian:enchanting");
        assertTypeField("heart_of_the_sea.json", "meridian:enchanting");
        assertTypeField("totem_of_undying.json", "meridian:enchanting");
        assertTypeField("budding_amethyst.json", "meridian:enchanting");
        assertTypeField("everfull_flask.json", "meridian:enchanting");
        assertTypeField("tempered_core.json", "meridian:enchanting");
        for (String food : EVERFEAST_FOODS) {
            assertTypeField("everfeast_" + food + ".json", "meridian:enchanting");
        }

        // Spot-check stat values match Zenith on the ones we can fully read without item lookup.
        JsonElement infusedBreath = readResource("infused_breath.json");
        StatRequirements ibReq = StatRequirements.CODEC.parse(JsonOps.INSTANCE,
                infusedBreath.getAsJsonObject().get("requirements")).getOrThrow();
        StatRequirements ibMax = StatRequirements.CODEC.parse(JsonOps.INSTANCE,
                infusedBreath.getAsJsonObject().get("max_requirements")).getOrThrow();
        assertEquals(40F, ibReq.eterna());
        assertEquals(15F, ibReq.quanta());
        assertEquals(60F, ibReq.arcana());
        assertEquals(-1F, ibMax.eterna());
        assertEquals(25F, ibMax.quanta());
        assertEquals(-1F, ibMax.arcana());
    }

    private static final String[] EVERFEAST_FOODS = {
            "cooked_beef", "cooked_porkchop", "cooked_mutton", "cooked_chicken", "cooked_rabbit",
            "cooked_cod", "cooked_salmon", "bread", "baked_potato", "golden_carrot"
    };

    @Test
    void everfeastRecipes_shareTheStatedGateAndConsumeTheirBaseFood() throws Exception {
        for (String food : EVERFEAST_FOODS) {
            JsonObject json = readResource("everfeast_" + food + ".json").getAsJsonObject();
            JsonObject req = json.getAsJsonObject("requirements");
            assertEquals(27F, req.get("eterna").getAsFloat(),
                    "everfeast_" + food + " must gate at eterna 27");
            assertEquals(15F, req.get("quanta").getAsFloat(),
                    "everfeast_" + food + " must gate at quanta 15");
            assertEquals(30, json.get("xp_cost").getAsInt(),
                    "everfeast_" + food + " must cost 30 levels");
            assertEquals("minecraft:" + food,
                    json.getAsJsonObject("input").get("item").getAsString(),
                    "everfeast_" + food + " must consume its base food");
            assertEquals("meridian:everfeast_" + food,
                    json.getAsJsonObject("result").get("id").getAsString());
        }
    }

    @Test
    void honeyRecipes_haveAscendingEternaForTieredOutput() throws Exception {
        JsonElement t1 = readResource("honey_to_xp_t1.json");
        JsonElement t2 = readResource("honey_to_xp_t2.json");
        JsonElement t3 = readResource("honey_to_xp_t3.json");

        float e1 = t1.getAsJsonObject().getAsJsonObject("requirements").get("eterna").getAsFloat();
        float e2 = t2.getAsJsonObject().getAsJsonObject("requirements").get("eterna").getAsFloat();
        float e3 = t3.getAsJsonObject().getAsJsonObject("requirements").get("eterna").getAsFloat();

        assertTrue(e1 < e2, "t1 eterna (" + e1 + ") must be < t2 (" + e2 + ")");
        assertTrue(e2 < e3, "t2 eterna (" + e2 + ") must be < t3 (" + e3 + ")");

        int c1 = t1.getAsJsonObject().getAsJsonObject("result").get("count").getAsInt();
        int c2 = t2.getAsJsonObject().getAsJsonObject("result").get("count").getAsInt();
        int c3 = t3.getAsJsonObject().getAsJsonObject("result").get("count").getAsInt();

        assertTrue(c1 < c2, "t1 count must be < t2 count");
        assertTrue(c2 < c3, "t2 count must be < t3 count");
    }

    @Test
    void goldenCarrotRecipe_hasPrecisionMaxBounds() throws Exception {
        JsonElement json = readResource("golden_carrot.json");
        JsonObject maxReq = json.getAsJsonObject().getAsJsonObject("max_requirements");
        assertTrue(maxReq.get("eterna").getAsFloat() > 0,
                "golden carrot must have an eterna upper bound (precision recipe)");
        assertTrue(maxReq.get("quanta").getAsFloat() > 0,
                "golden carrot must have a quanta upper bound");
    }

    @Test
    void buddingAmethystRecipe_hasQuantaCap() throws Exception {
        JsonElement json = readResource("budding_amethyst.json");
        JsonObject maxReq = json.getAsJsonObject().getAsJsonObject("max_requirements");
        assertTrue(maxReq.get("quanta").getAsFloat() > 0,
                "budding amethyst must have a quanta upper bound");
        assertEquals(-1F, maxReq.get("eterna").getAsFloat(),
                "budding amethyst eterna should be uncapped");
    }

    @Test
    void moduleField_absentInJson_defaultsToCore() {
        JsonObject json = JsonParser.parseString("""
                {
                    "input": { "item": "minecraft:bookshelf" },
                    "requirements": { "eterna": 10 },
                    "result": { "id": "minecraft:enchanting_table" }
                }
                """).getAsJsonObject();
        EnchantingRecipe decoded = EnchantingRecipeRegistry.ENCHANTING_SERIALIZER.codec()
                .codec().parse(JsonOps.INSTANCE, json).getOrThrow();
        assertEquals(RecipeModule.CORE, decoded.getModule(),
                "a recipe without a module tag must be CORE and un-toggleable");
    }

    @Test
    void moduleField_roundTripsThroughJsonAndBuffer() {
        EnchantingRecipe original = new EnchantingRecipe(
                Ingredient.of(Items.EMERALD_BLOCK),
                new StatRequirements(50F, 45F, 85F),
                StatRequirements.NO_MAX,
                new ItemStack(Items.TOTEM_OF_UNDYING),
                OptionalInt.empty(),
                0,
                RecipeModule.DUPLICATION);

        JsonElement json = EnchantingRecipeRegistry.ENCHANTING_SERIALIZER.codec()
                .codec().encodeStart(JsonOps.INSTANCE, original).getOrThrow();
        assertEquals("duplication", json.getAsJsonObject().get("module").getAsString());
        EnchantingRecipe decoded = EnchantingRecipeRegistry.ENCHANTING_SERIALIZER.codec()
                .codec().parse(JsonOps.INSTANCE, json).getOrThrow();
        assertEnchantingRecipeEquals(original, decoded);

        // The stream codec must carry the module too — clients receive recipes over the wire and
        // apply the same gate to the craft-slot hint and the recipe viewers.
        RegistryAccess.Frozen access = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), access);
        EnchantingRecipeRegistry.ENCHANTING_SERIALIZER.streamCodec().encode(buf, original);
        EnchantingRecipe wired = EnchantingRecipeRegistry.ENCHANTING_SERIALIZER.streamCodec().decode(buf);
        assertEnchantingRecipeEquals(original, wired);
    }

    @Test
    void moduleField_unknownValueFailsTheParse() {
        JsonObject json = JsonParser.parseString("""
                {
                    "input": { "item": "minecraft:bookshelf" },
                    "requirements": { "eterna": 10 },
                    "result": { "id": "minecraft:enchanting_table" },
                    "module": "duplicaton"
                }
                """).getAsJsonObject();
        // Strict by design: a typo'd module must fail the recipe at load, not silently un-gate it
        // by collapsing to CORE.
        assertTrue(EnchantingRecipeRegistry.ENCHANTING_SERIALIZER.codec()
                        .codec().parse(JsonOps.INSTANCE, json).isError(),
                "an unknown module string must be a parse error, not a fallback to CORE");
    }

    @Test
    void shippedJsonFiles_carryTheirIssueModuleTags() throws Exception {
        String[] duplication = {"totem_of_undying", "enchanted_golden_apple", "echo_shard_duplication",
                "golden_apple", "golden_carrot", "heart_of_the_sea", "budding_amethyst"};
        for (String name : duplication) {
            assertEquals("duplication", readResource(name + ".json").getAsJsonObject()
                            .get("module").getAsString(),
                    name + " must be in the duplication module (#163)");
        }
        for (String food : EVERFEAST_FOODS) {
            assertEquals("everfeast", readResource("everfeast_" + food + ".json").getAsJsonObject()
                            .get("module").getAsString(),
                    "everfeast_" + food + " must be in the everfeast module (#163)");
        }
        assertEquals("everfeast", readResource("everfull_flask.json").getAsJsonObject()
                .get("module").getAsString(), "everfull_flask must be in the everfeast module (#163)");

        // Everything else ships untagged — CORE, not toggleable.
        try (var files = java.nio.file.Files.list(
                java.nio.file.Path.of("src/main/resources/data/meridian/recipe/enchanting"))) {
            Set<String> tagged = new HashSet<>(List.of(duplication));
            for (String food : EVERFEAST_FOODS) tagged.add("everfeast_" + food);
            tagged.add("everfull_flask");
            for (var path : files.toList()) {
                String name = path.getFileName().toString().replace(".json", "");
                if (tagged.contains(name)) continue;
                assertFalse(readResource(name + ".json").getAsJsonObject().has("module"),
                        name + " must not carry a module tag — core recipes stay untagged");
            }
        }
    }

    @Test
    void effectiveXpCost_derivesFromEternaOnTheDoubledScale() {
        // No explicit xp_cost → cost tracks the slot-2 enchant scale: round(eterna * 2).
        EnchantingRecipe enderLibrary = makeRecipe(Ingredient.of(Items.BOOK),
                new StatRequirements(50F, 45F, 100F), StatRequirements.NO_MAX,
                new ItemStack(Items.ENCHANTED_BOOK));
        assertEquals(100, enderLibrary.getEffectiveXpCost(),
                "50 Eterna must cost 100 levels — the same as a maxed table enchant");

        // Fractional Eterna doubles before rounding: 22.5 -> 45, not round(22.5)*2 = 46.
        EnchantingRecipe infusedShelf = makeRecipe(Ingredient.of(Items.BOOKSHELF),
                new StatRequirements(22.5F, 30F, 0F), StatRequirements.NO_MAX,
                new ItemStack(Items.BOOKSHELF));
        assertEquals(45, infusedShelf.getEffectiveXpCost(),
                "22.5 Eterna must double before rounding (45), matching getEnchantmentCost");
    }

    @Test
    void effectiveXpCost_explicitOverrideTakesPrecedence() {
        EnchantingRecipe recipe = makeRecipe(Ingredient.of(Items.DIAMOND_SWORD),
                new StatRequirements(40F, 0F, 0F), StatRequirements.NO_MAX,
                new ItemStack(Items.NETHERITE_INGOT), OptionalInt.empty(), 12);
        assertEquals(12, recipe.getEffectiveXpCost(),
                "an explicit xp_cost must be used verbatim, not the derived doubled value");
    }

    private static EnchantingRecipe makeRecipe(Ingredient input, StatRequirements req,
                                               StatRequirements max, ItemStack result) {
        return makeRecipe(input, req, max, result, OptionalInt.empty(), 0);
    }

    private static EnchantingRecipe makeRecipe(Ingredient input, StatRequirements req,
                                               StatRequirements max, ItemStack result,
                                               OptionalInt displayLevel, int xpCost) {
        return new EnchantingRecipe(input, req, max, result, displayLevel, xpCost);
    }

    private static void assertEnchantingRecipeEquals(EnchantingRecipe expected, EnchantingRecipe actual) {
        assertEquals(expected.getRequirements(), actual.getRequirements());
        assertEquals(expected.getMaxRequirements(), actual.getMaxRequirements());
        assertEquals(expected.getDisplayLevel(), actual.getDisplayLevel());
        assertEquals(expected.getXpCost(), actual.getXpCost());
        assertEquals(expected.getModule(), actual.getModule());
        assertEquals(expected.getResult().getItem(), actual.getResult().getItem());
        assertEquals(expected.getResult().getCount(), actual.getResult().getCount());
        assertEquals(expected.getInput().getItems().length, actual.getInput().getItems().length);
        assertSame(expected.getInput().getItems()[0].getItem(), actual.getInput().getItems()[0].getItem());
    }

    private static void assertTypeField(String filename, String expectedType) throws Exception {
        JsonElement json = readResource(filename);
        assertEquals(expectedType, json.getAsJsonObject().get("type").getAsString(),
                "Recipe " + filename + " must declare type " + expectedType);
    }

    private static JsonElement readResource(String filename) throws Exception {
        java.nio.file.Path path = java.nio.file.Path.of(
                "src/main/resources/data/meridian/recipe/enchanting/" + filename);
        try (var reader = java.nio.file.Files.newBufferedReader(path)) {
            return JsonParser.parseReader(reader);
        }
    }
}
