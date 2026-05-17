package com.rfizzle.meridian.library;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Tier: 2
class LibraryClientSyncTest {

    private static final ResourceKey<Enchantment> SHARPNESS = Enchantments.SHARPNESS;
    private static final ResourceKey<Enchantment> UNBREAKING = Enchantments.UNBREAKING;

    private static HolderLookup.Provider provider;
    private static BlockState state;

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        provider = VanillaRegistries.createLookup();
        state = Blocks.CHEST.defaultBlockState();
    }

    @Test
    void getUpdatePacket_overriddenOnLibraryBlockEntity() throws Exception {
        Method overridden = EnchantmentLibraryBlockEntity.class.getDeclaredMethod("getUpdatePacket");
        assertSame(EnchantmentLibraryBlockEntity.class, overridden.getDeclaringClass(),
                "library BE must override getUpdatePacket — default returns null and would skip sync");
        assertTrue(ClientboundBlockEntityDataPacket.class.isAssignableFrom(
                        overridden.getReturnType()) || overridden.getReturnType().getName().contains("Packet"),
                "override returns the vanilla packet family used by the BE-update pipeline");
    }

    @Test
    void getUpdateTag_includesMutationsLandedOnServer() {
        TestLibraryBlockEntity server = newLibrary();
        server.getPoints().put(SHARPNESS, 4096);
        server.getMaxLevels().put(SHARPNESS, 5);
        server.getPoints().put(UNBREAKING, 64);
        server.getMaxLevels().put(UNBREAKING, 3);

        CompoundTag updateTag = server.getUpdateTag(provider);

        assertTrue(updateTag.contains(EnchantmentLibraryBlockEntity.TAG_POINTS),
                "update tag carries the Points compound");
        assertTrue(updateTag.contains(EnchantmentLibraryBlockEntity.TAG_LEVELS),
                "update tag carries the Levels compound");
        CompoundTag points = updateTag.getCompound(EnchantmentLibraryBlockEntity.TAG_POINTS);
        assertEquals(4096, points.getInt(SHARPNESS.location().toString()),
                "post-mutation Sharpness points appear in the wire payload");
        assertEquals(64, points.getInt(UNBREAKING.location().toString()),
                "post-mutation Unbreaking points appear in the wire payload");
        CompoundTag levels = updateTag.getCompound(EnchantmentLibraryBlockEntity.TAG_LEVELS);
        assertEquals(5, levels.getInt(SHARPNESS.location().toString()));
        assertEquals(3, levels.getInt(UNBREAKING.location().toString()));
    }

    @Test
    void clientReconstructedFromUpdateTag_equalsServerState() {
        TestLibraryBlockEntity server = newLibrary();
        server.getPoints().put(SHARPNESS, 8192);
        server.getPoints().put(UNBREAKING, 128);
        server.getMaxLevels().put(SHARPNESS, 6);
        server.getMaxLevels().put(UNBREAKING, 4);

        CompoundTag wire = server.getUpdateTag(provider);

        TestLibraryBlockEntity client = newLibrary();
        client.loadCustomOnly(wire, provider);

        assertEquals(server.getPoints(), client.getPoints(),
                "client point map matches server byte-for-byte after applying update tag");
        assertEquals(server.getMaxLevels(), client.getMaxLevels(),
                "client max-level map matches server byte-for-byte after applying update tag");
    }

    @Test
    void getUpdateTag_reflectsSubsequentMutationsForFullResend() {
        TestLibraryBlockEntity server = newLibrary();
        server.getPoints().put(SHARPNESS, 100);
        server.getMaxLevels().put(SHARPNESS, 3);
        CompoundTag firstTag = server.getUpdateTag(provider);

        server.getPoints().put(SHARPNESS, 200);
        server.getMaxLevels().put(SHARPNESS, 4);
        server.getPoints().put(UNBREAKING, 16);
        CompoundTag secondTag = server.getUpdateTag(provider);

        TestLibraryBlockEntity client = newLibrary();
        client.loadCustomOnly(firstTag, provider);
        assertEquals(100, client.getPoints().getInt(SHARPNESS),
                "client picks up the first mutation");

        client.loadCustomOnly(secondTag, provider);
        assertEquals(200, client.getPoints().getInt(SHARPNESS),
                "second mutation overwrites the first — full resend, not delta merge");
        assertEquals(4, client.getMaxLevels().getInt(SHARPNESS));
        assertEquals(16, client.getPoints().getInt(UNBREAKING),
                "fresh entries from the second resend land on the client");
        assertEquals(2, client.getPoints().size(),
                "only the latest map state survives — no orphan entries from the first tag");
    }

    private static TestLibraryBlockEntity newLibrary() {
        return new TestLibraryBlockEntity(BlockEntityType.CHEST, BlockPos.ZERO, state, 16);
    }

    private static final class TestLibraryBlockEntity extends EnchantmentLibraryBlockEntity {
        TestLibraryBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int maxLevel) {
            super(type, pos, state, maxLevel);
        }
    }
}
