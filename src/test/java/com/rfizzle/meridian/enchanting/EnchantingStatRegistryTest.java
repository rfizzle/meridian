package com.rfizzle.meridian.enchanting;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnchantingStatRegistryTest {

    private static final ResourceLocation BOOKSHELF = ResourceLocation.parse("minecraft:bookshelf");
    private static final ResourceLocation STONE = ResourceLocation.parse("minecraft:stone");
    private static final TagKey<Block> CUSTOM_TAG =
            TagKey.create(Registries.BLOCK, ResourceLocation.parse("meridian:test_tag"));

    @SafeVarargs
    private static Predicate<TagKey<Block>> inTags(TagKey<Block>... tags) {
        return t -> {
            for (TagKey<Block> accepted : tags) {
                if (t.equals(accepted)) return true;
            }
            return false;
        };
    }

    // ---- Lookup order ----

    @Test
    void lookup_directBlockMatchBeatsTagMatch() {
        EnchantingStatRegistry reg = new EnchantingStatRegistry();
        EnchantingStats direct = new EnchantingStats(30F, 2F, 0F, 0F, 0F, 0);
        EnchantingStats viaTag = new EnchantingStats(15F, 1F, 0F, 0F, 0F, 0);

        reg.registerBlock(BOOKSHELF, direct);
        reg.registerTag(CUSTOM_TAG, viaTag);

        EnchantingStats result = reg.resolveWith(BOOKSHELF, inTags(CUSTOM_TAG, BlockTags.ENCHANTMENT_POWER_PROVIDER));

        assertEquals(direct, result, "direct block registration should beat a tag match");
    }

    @Test
    void lookup_tagMatchBeatsVanillaFallback() {
        EnchantingStatRegistry reg = new EnchantingStatRegistry();
        EnchantingStats viaTag = new EnchantingStats(25F, 1.5F, 3F, 0F, 0F, 0);

        reg.registerTag(CUSTOM_TAG, viaTag);

        EnchantingStats result = reg.resolveWith(BOOKSHELF, inTags(CUSTOM_TAG, BlockTags.ENCHANTMENT_POWER_PROVIDER));

        assertEquals(viaTag, result, "explicit tag registration should beat the vanilla Java fallback");
    }

    @Test
    void lookup_vanillaFallbackFiresForPowerProviderOnly() {
        EnchantingStatRegistry reg = new EnchantingStatRegistry();

        EnchantingStats result = reg.resolveWith(BOOKSHELF, inTags(BlockTags.ENCHANTMENT_POWER_PROVIDER));

        assertEquals(EnchantingStatRegistry.VANILLA_FALLBACK, result,
                "blocks in BlockTags.ENCHANTMENT_POWER_PROVIDER with no datapack entry should return the Java fallback");
    }

    @Test
    void lookup_zeroWhenNoMatchesAnywhere() {
        EnchantingStatRegistry reg = new EnchantingStatRegistry();

        EnchantingStats result = reg.resolveWith(STONE, t -> false);

        assertEquals(EnchantingStats.ZERO, result);
    }

    @Test
    void lookup_firstMatchingTagWins() {
        EnchantingStatRegistry reg = new EnchantingStatRegistry();
        TagKey<Block> second =
                TagKey.create(Registries.BLOCK, ResourceLocation.parse("meridian:second"));
        EnchantingStats statsA = new EnchantingStats(10F, 1F, 0F, 0F, 0F, 0);
        EnchantingStats statsB = new EnchantingStats(20F, 2F, 0F, 0F, 0F, 0);

        reg.registerTag(CUSTOM_TAG, statsA);
        reg.registerTag(second, statsB);

        EnchantingStats result = reg.resolveWith(STONE, inTags(CUSTOM_TAG, second));

        assertEquals(statsA, result,
                "when a block matches multiple tags, the first registered match should win");
    }

    // ---- Reload clears state ----

    @Test
    void clear_removesAllRegistrations() {
        EnchantingStatRegistry reg = new EnchantingStatRegistry();
        reg.registerBlock(BOOKSHELF, new EnchantingStats(99F, 5F, 0F, 0F, 0F, 0));
        reg.registerTag(CUSTOM_TAG, new EnchantingStats(50F, 3F, 0F, 0F, 0F, 0));
        assertEquals(1, reg.blockEntryCount());
        assertEquals(1, reg.tagEntryCount());

        reg.clear();

        assertEquals(0, reg.blockEntryCount());
        assertEquals(0, reg.tagEntryCount());
        assertEquals(EnchantingStats.ZERO,
                reg.resolveWith(BOOKSHELF, inTags(CUSTOM_TAG)),
                "cleared registry should fall through to zero");
    }

    // ---- parseAndRegister integration ----

    @Test
    void parseAndRegister_withBlockEntry_registersByBlock() {
        EnchantingStatRegistry reg = new EnchantingStatRegistry();
        JsonElement json = JsonParser.parseString(
                "{\"block\":\"minecraft:bookshelf\",\"maxEterna\":15,\"eterna\":1}");

        reg.parseAndRegister(ResourceLocation.parse("meridian:vanilla_provider"), json);

        EnchantingStats result = reg.resolveWith(BOOKSHELF, t -> false);
        assertEquals(new EnchantingStats(15F, 1F, 0F, 0F, 0F, 0), result);
    }

    @Test
    void parseAndRegister_withTagEntry_registersByTag() {
        EnchantingStatRegistry reg = new EnchantingStatRegistry();
        JsonElement json = JsonParser.parseString(
                "{\"tag\":\"#minecraft:enchantment_power_provider\",\"maxEterna\":15,\"eterna\":1}");

        reg.parseAndRegister(ResourceLocation.parse("meridian:vanilla_provider"), json);

        assertEquals(0, reg.blockEntryCount());
        assertEquals(1, reg.tagEntryCount());
    }

    // ---- Codec validation ----

    @Test
    void codec_withOnlyBlock_parses() {
        JsonElement json = JsonParser.parseString(
                "{\"block\":\"minecraft:bookshelf\",\"maxEterna\":15,\"eterna\":1}");

        EnchantingStatRegistry.StatEntry entry =
                EnchantingStatRegistry.StatEntry.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();

        assertEquals(BOOKSHELF, entry.block().orElseThrow());
        assertTrue(entry.tag().isEmpty());
        assertEquals(15F, entry.stats().maxEterna());
        assertEquals(1F, entry.stats().eterna());
    }

    @Test
    void codec_withOnlyTag_parses_andStripsHashPrefix() {
        JsonElement json = JsonParser.parseString(
                "{\"tag\":\"#minecraft:enchantment_power_provider\",\"maxEterna\":15,\"eterna\":1}");

        EnchantingStatRegistry.StatEntry entry =
                EnchantingStatRegistry.StatEntry.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();

        assertTrue(entry.block().isEmpty());
        TagKey<Block> tag = entry.tag().orElseThrow();
        assertEquals(ResourceLocation.parse("minecraft:enchantment_power_provider"), tag.location());
        assertEquals(Registries.BLOCK, tag.registry());
    }

    @Test
    void codec_withBothBlockAndTag_failsWithClearMessage() {
        JsonElement json = JsonParser.parseString(
                "{\"block\":\"minecraft:bookshelf\"," +
                "\"tag\":\"#minecraft:enchantment_power_provider\"," +
                "\"maxEterna\":15}");

        DataResult<EnchantingStatRegistry.StatEntry> result =
                EnchantingStatRegistry.StatEntry.CODEC.parse(JsonOps.INSTANCE, json);

        assertTrue(result.error().isPresent(), "parse should fail when both block and tag are present");
        String message = result.error().get().message();
        assertTrue(message.toLowerCase().contains("not both"),
                "error message should mention 'not both', got: " + message);
    }

    @Test
    void codec_withNeitherBlockNorTag_fails() {
        JsonElement json = JsonParser.parseString("{\"maxEterna\":15,\"eterna\":1}");

        DataResult<EnchantingStatRegistry.StatEntry> result =
                EnchantingStatRegistry.StatEntry.CODEC.parse(JsonOps.INSTANCE, json);

        assertTrue(result.error().isPresent(), "parse should fail when neither block nor tag is present");
    }

    @Test
    void parseAndRegister_withBothBlockAndTag_throwsJsonParseException() {
        EnchantingStatRegistry reg = new EnchantingStatRegistry();
        JsonElement json = JsonParser.parseString(
                "{\"block\":\"minecraft:bookshelf\"," +
                "\"tag\":\"#minecraft:enchantment_power_provider\"," +
                "\"maxEterna\":15}");

        JsonParseException ex = assertThrows(JsonParseException.class,
                () -> reg.parseAndRegister(ResourceLocation.parse("meridian:bad"), json));
        assertTrue(ex.getMessage().contains("meridian:bad"),
                "exception should name the source file, got: " + ex.getMessage());
    }

    @Test
    void codec_missingStatFields_defaultToZero() {
        JsonElement json = JsonParser.parseString("{\"block\":\"minecraft:bookshelf\"}");

        EnchantingStatRegistry.StatEntry entry =
                EnchantingStatRegistry.StatEntry.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();

        assertEquals(EnchantingStats.ZERO, entry.stats());
    }

    @Test
    void vanillaProviderSeedApplies() throws Exception {
        EnchantingStatRegistry reg = new EnchantingStatRegistry();
        JsonElement json;
        try (InputStream in = EnchantingStatRegistryTest.class.getResourceAsStream(
                "/data/meridian/enchanting_stats/vanilla_provider.json")) {
            assertNotNull(in, "vanilla_provider.json must be on the classpath");
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                json = JsonParser.parseReader(reader);
            }
        }

        reg.parseAndRegister(
                ResourceLocation.parse("meridian:vanilla_provider"), json);

        EnchantingStats result = reg.resolveWith(
                BOOKSHELF, inTags(BlockTags.ENCHANTMENT_POWER_PROVIDER));

        assertEquals(new EnchantingStats(15F, 1F, 0F, 0F, 0F, 0), result,
                "shipped vanilla_provider.json should give vanilla bookshelves maxEterna=15, eterna=1");
    }
}
