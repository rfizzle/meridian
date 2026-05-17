// Tier: 2 (fabric-loader-junit)
package com.rfizzle.meridian.net;

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

import java.util.List;
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
    void cluesPayload_threeEntryList_roundTrips() {
        CluesPayload original = new CluesPayload(
                1,
                List.of(
                        new EnchantmentClue(enchantKey("minecraft:sharpness"), 5),
                        new EnchantmentClue(enchantKey("minecraft:mending"), 1),
                        new EnchantmentClue(enchantKey("minecraft:unbreaking"), 3)),
                false);

        RegistryFriendlyByteBuf buf = newBuf();
        CluesPayload.CODEC.encode(buf, original);
        CluesPayload decoded = CluesPayload.CODEC.decode(buf);

        assertEquals(original, decoded);
        assertEquals(3, decoded.clues().size());
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
}
