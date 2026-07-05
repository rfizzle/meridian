// Tier: 2 (fabric-loader-junit)
package com.rfizzle.meridian.net;

import com.rfizzle.meridian.config.MeridianConfig;
import com.rfizzle.meridian.enchanting.EnchantingStats;
import io.netty.buffer.Unpooled;
import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PayloadCodecTest {

    private static RegistryAccess.Frozen REGISTRIES;

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        REGISTRIES = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    }

    private static RegistryFriendlyByteBuf newBuf() {
        return new RegistryFriendlyByteBuf(Unpooled.buffer(), REGISTRIES);
    }

    private static ResourceKey<Enchantment> enchantKey(String path) {
        return ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.parse(path));
    }

    // ---- StatsPayload ------------------------------------------------------

    @Test
    void statsPayload_zeroStat_roundTrips() {
        StatsPayload original = new StatsPayload(
                0F, 0F, 0F, 0F, 0, 0F, List.of(), false, Optional.empty());

        RegistryFriendlyByteBuf buf = newBuf();
        StatsPayload.CODEC.encode(buf, original);
        StatsPayload decoded = StatsPayload.CODEC.decode(buf);

        assertEquals(original, decoded);
        assertEquals(0, buf.readableBytes(), "codec should consume every byte it wrote");
    }

    @Test
    void statsPayload_midStat_roundTrips() {
        StatsPayload original = new StatsPayload(
                7.5F, 3F, 2F, 1F, 2, 22.5F,
                List.of(enchantKey("minecraft:sharpness"), enchantKey("minecraft:mending")),
                false,
                Optional.empty());

        RegistryFriendlyByteBuf buf = newBuf();
        StatsPayload.CODEC.encode(buf, original);
        StatsPayload decoded = StatsPayload.CODEC.decode(buf);

        assertEquals(original, decoded);
        assertEquals(List.of(enchantKey("minecraft:sharpness"), enchantKey("minecraft:mending")),
                decoded.blacklist(), "blacklist ordering must be preserved through round-trip");
        assertEquals(0, buf.readableBytes());
    }

    @Test
    void statsPayload_saturated_roundTrips() {
        CraftingResultEntry result = new CraftingResultEntry(
                new ItemStack(Items.ENCHANTED_BOOK),
                30,
                ResourceLocation.fromNamespaceAndPath("meridian", "test_recipe"));
        StatsPayload original = new StatsPayload(
                50F, 15F, 15F, 20F, 3, 50F,
                List.of(enchantKey("minecraft:fortune"),
                        enchantKey("minecraft:efficiency"),
                        enchantKey("minecraft:unbreaking")),
                true,
                Optional.of(result));

        RegistryFriendlyByteBuf buf = newBuf();
        StatsPayload.CODEC.encode(buf, original);
        StatsPayload decoded = StatsPayload.CODEC.decode(buf);

        assertEquals(original.eterna(), decoded.eterna());
        assertEquals(original.quanta(), decoded.quanta());
        assertEquals(original.arcana(), decoded.arcana());
        assertEquals(original.rectification(), decoded.rectification());
        assertEquals(original.clues(), decoded.clues());
        assertEquals(original.maxEterna(), decoded.maxEterna());
        assertEquals(original.blacklist(), decoded.blacklist());
        assertEquals(original.treasure(), decoded.treasure());
        assertTrue(decoded.craftingResult().isPresent(),
                "saturated payload must carry craftingResult through round-trip");
        CraftingResultEntry decodedResult = decoded.craftingResult().orElseThrow();
        assertTrue(ItemStack.matches(result.result(), decodedResult.result()),
                "crafting result ItemStack must survive round-trip");
        assertEquals(result.xpCost(), decodedResult.xpCost());
        assertEquals(result.recipeId(), decodedResult.recipeId());
        assertEquals(0, buf.readableBytes());
    }

    @Test
    void statsPayload_fiveClues_roundTrips() {
        StatsPayload original = new StatsPayload(
                0F, 0F, 0F, 0F, 5, 0F, List.of(), false, Optional.empty());

        RegistryFriendlyByteBuf buf = newBuf();
        StatsPayload.CODEC.encode(buf, original);
        StatsPayload decoded = StatsPayload.CODEC.decode(buf);

        assertEquals(5, decoded.clues());
        assertEquals(0, buf.readableBytes());
    }

    @Test
    void statsPayload_typeId_isNamespaced() {
        assertEquals(ResourceLocation.fromNamespaceAndPath("meridian", "stats"),
                StatsPayload.TYPE.id());
    }

    // ---- CluesPayload ------------------------------------------------------

    @Test
    void cluesPayload_emptyList_roundTrips() {
        CluesPayload original = new CluesPayload(0, List.of(), false);

        RegistryFriendlyByteBuf buf = newBuf();
        CluesPayload.CODEC.encode(buf, original);
        CluesPayload decoded = CluesPayload.CODEC.decode(buf);

        assertEquals(original, decoded);
        assertEquals(0, buf.readableBytes());
    }

    @Test
    void cluesPayload_fiveEntryList_roundTrips() {
        CluesPayload original = new CluesPayload(
                1,
                List.of(
                        new EnchantmentClue(enchantKey("minecraft:sharpness"), 5),
                        new EnchantmentClue(enchantKey("minecraft:mending"), 1),
                        new EnchantmentClue(enchantKey("minecraft:unbreaking"), 3),
                        new EnchantmentClue(enchantKey("minecraft:fortune"), 2),
                        new EnchantmentClue(enchantKey("minecraft:efficiency"), 4)),
                false);

        RegistryFriendlyByteBuf buf = newBuf();
        CluesPayload.CODEC.encode(buf, original);
        CluesPayload decoded = CluesPayload.CODEC.decode(buf);

        assertEquals(original, decoded);
        assertEquals(5, decoded.clues().size());
        assertEquals(0, buf.readableBytes());
    }

    @Test
    void cluesPayload_exhaustedFlag_roundTrips() {
        CluesPayload original = new CluesPayload(
                2,
                List.of(new EnchantmentClue(enchantKey("minecraft:fortune"), 3)),
                true);

        RegistryFriendlyByteBuf buf = newBuf();
        CluesPayload.CODEC.encode(buf, original);
        CluesPayload decoded = CluesPayload.CODEC.decode(buf);

        assertEquals(original, decoded);
        assertTrue(decoded.exhaustedList(),
                "exhaustedList=true must survive round-trip");
        assertEquals(0, buf.readableBytes());
    }

    @Test
    void cluesPayload_typeId_isNamespaced() {
        assertEquals(ResourceLocation.fromNamespaceAndPath("meridian", "clues"),
                CluesPayload.TYPE.id());
    }

    // ---- EnchantingStats.STREAM_CODEC --------------------------------------

    @Test
    void enchantingStats_streamCodec_roundTrips() {
        EnchantingStats original = new EnchantingStats(12.5F, 3F, 2F, 1.5F, 0.25F, 4);

        RegistryFriendlyByteBuf buf = newBuf();
        EnchantingStats.STREAM_CODEC.encode(buf, original);
        EnchantingStats decoded = EnchantingStats.STREAM_CODEC.decode(buf);

        assertEquals(original, decoded);
        assertEquals(0, buf.readableBytes(), "codec should consume every byte it wrote");
    }

    // ---- EnchantingStatSyncPayload -----------------------------------------

    @Test
    void enchantingStatSyncPayload_empty_roundTrips() {
        EnchantingStatSyncPayload original = new EnchantingStatSyncPayload(Map.of(), List.of());

        RegistryFriendlyByteBuf buf = newBuf();
        EnchantingStatSyncPayload.CODEC.encode(buf, original);
        EnchantingStatSyncPayload decoded = EnchantingStatSyncPayload.CODEC.decode(buf);

        assertEquals(original, decoded);
        assertEquals(0, buf.readableBytes());
    }

    @Test
    void enchantingStatSyncPayload_populated_roundTrips() {
        Map<ResourceLocation, EnchantingStats> blocks = new LinkedHashMap<>();
        blocks.put(ResourceLocation.parse("meridian:oak_shelf"),
                new EnchantingStats(15F, 4F, 2F, 1F, 0.5F, 2));
        blocks.put(ResourceLocation.parse("minecraft:bookshelf"), EnchantingStats.ZERO);
        List<EnchantingStatSyncPayload.TagEntry> tags = List.of(
                new EnchantingStatSyncPayload.TagEntry(
                        ResourceLocation.parse("meridian:shelves"),
                        new EnchantingStats(1F, 1F, 1F, 1F, 1F, 1)));
        EnchantingStatSyncPayload original = new EnchantingStatSyncPayload(blocks, tags);

        RegistryFriendlyByteBuf buf = newBuf();
        EnchantingStatSyncPayload.CODEC.encode(buf, original);
        EnchantingStatSyncPayload decoded = EnchantingStatSyncPayload.CODEC.decode(buf);

        assertEquals(original, decoded);
        assertEquals(blocks, decoded.blocks(), "block stats must survive round-trip");
        assertEquals(tags, decoded.tags(), "tag entries must survive round-trip");
        assertEquals(0, buf.readableBytes());
    }

    @Test
    void enchantingStatSyncPayload_typeId_isNamespaced() {
        assertEquals(ResourceLocation.fromNamespaceAndPath("meridian", "enchanting_stat_sync"),
                EnchantingStatSyncPayload.TYPE.id());
    }

    // ---- ConfigSyncPayload -------------------------------------------------

    @Test
    void configSyncPayload_gameplayValues_roundTrip() {
        MeridianConfig server = new MeridianConfig();
        server.enchantingTable.maxEterna = 42;
        server.anvil.prismaticWebLevelCost = 99;
        server.tomes.extractionTomeRepairPercent = 0.5;
        server.combat.sunderAffectsPlayers = true;
        MeridianConfig.EnchantmentOverride override = new MeridianConfig.EnchantmentOverride();
        override.levelCap = 7;
        server.enchantmentOverrides.put("minecraft:sharpness", override);

        ConfigSyncPayload payload = new ConfigSyncPayload(server.toSyncJson());
        RegistryFriendlyByteBuf buf = newBuf();
        ConfigSyncPayload.CODEC.encode(buf, payload);
        ConfigSyncPayload decodedPayload = ConfigSyncPayload.CODEC.decode(buf);
        assertEquals(0, buf.readableBytes(), "codec should consume every byte it wrote");

        MeridianConfig decoded = MeridianConfig.fromJson(decodedPayload.configJson());
        assertEquals(42, decoded.enchantingTable.maxEterna);
        assertEquals(99, decoded.anvil.prismaticWebLevelCost);
        assertEquals(0.5, decoded.tomes.extractionTomeRepairPercent);
        assertTrue(decoded.combat.sunderAffectsPlayers, "gameplay toggle must survive round-trip");
        assertTrue(decoded.enchantmentOverrides.containsKey("minecraft:sharpness"),
                "enchantment overrides must survive round-trip");
        assertEquals(7, decoded.enchantmentOverrides.get("minecraft:sharpness").levelCap);
    }

    @Test
    void configSyncPayload_excludesClientDisplayFields() {
        MeridianConfig server = new MeridianConfig();
        server.display.overLeveledColor = "#123456";
        server.display.showBookTooltips = false;

        String syncJson = server.toSyncJson();
        assertTrue(!syncJson.contains("display"),
                "synced view must omit the client-only display block");

        MeridianConfig decoded = MeridianConfig.fromJson(syncJson);
        // The client keeps its own display prefs; the synced copy carries defaults, never the
        // server operator's cosmetic choices.
        assertEquals("#FF6600", decoded.display.overLeveledColor);
        assertTrue(decoded.display.showBookTooltips);
    }

    @Test
    void configSyncPayload_typeId_isNamespaced() {
        assertEquals(ResourceLocation.fromNamespaceAndPath("meridian", "config_sync"),
                ConfigSyncPayload.TYPE.id());
    }
}
