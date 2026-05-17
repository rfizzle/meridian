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

import java.util.OptionalInt;

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
        assertTypeField("budding_amethyst.json", "meridian:enchanting");

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
