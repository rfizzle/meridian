package com.rfizzle.meridian.library;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Tier: 2
class LibraryNbtTest {

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
    void roundTrip_preservesPointsAndMaxLevels() {
        TestLibraryBlockEntity saved = newLibrary();
        saved.getPoints().put(SHARPNESS, 4096);
        saved.getPoints().put(UNBREAKING, 64);
        saved.getMaxLevels().put(SHARPNESS, 5);
        saved.getMaxLevels().put(UNBREAKING, 3);

        CompoundTag tag = saved.saveCustomOnly(provider);

        TestLibraryBlockEntity reloaded = newLibrary();
        reloaded.loadCustomOnly(tag, provider);

        assertEquals(4096, reloaded.getPoints().getInt(SHARPNESS),
                "sharpness points survive the round-trip");
        assertEquals(64, reloaded.getPoints().getInt(UNBREAKING),
                "unbreaking points survive the round-trip");
        assertEquals(5, reloaded.getMaxLevels().getInt(SHARPNESS),
                "sharpness max-level survives the round-trip");
        assertEquals(3, reloaded.getMaxLevels().getInt(UNBREAKING),
                "unbreaking max-level survives the round-trip");
        assertEquals(2, reloaded.getPoints().size(), "no phantom entries added on reload");
        assertEquals(2, reloaded.getMaxLevels().size(), "no phantom entries added on reload");
    }

    @Test
    void save_writesExpectedSchemaShape() {
        TestLibraryBlockEntity be = newLibrary();
        be.getPoints().put(SHARPNESS, 128);
        be.getMaxLevels().put(SHARPNESS, 4);

        CompoundTag tag = be.saveCustomOnly(provider);

        assertTrue(tag.contains(EnchantmentLibraryBlockEntity.TAG_POINTS),
                "save writes the Points compound tag");
        assertTrue(tag.contains(EnchantmentLibraryBlockEntity.TAG_LEVELS),
                "save writes the Levels compound tag");
        CompoundTag points = tag.getCompound(EnchantmentLibraryBlockEntity.TAG_POINTS);
        assertEquals(128, points.getInt(SHARPNESS.location().toString()),
                "keys are serialized via ResourceLocation#toString()");
        CompoundTag levels = tag.getCompound(EnchantmentLibraryBlockEntity.TAG_LEVELS);
        assertEquals(4, levels.getInt(SHARPNESS.location().toString()),
                "max-level serialization uses the same key shape");
    }

    @Test
    void load_dropsUnresolvedEnchantmentsKeepingRemainderIntact() {
        CompoundTag tag = new CompoundTag();
        CompoundTag points = new CompoundTag();
        points.putInt(SHARPNESS.location().toString(), 2048);
        points.putInt("minecraft:never_registered", 999);
        tag.put(EnchantmentLibraryBlockEntity.TAG_POINTS, points);

        CompoundTag levels = new CompoundTag();
        levels.putInt(SHARPNESS.location().toString(), 5);
        levels.putInt("minecraft:never_registered", 9);
        tag.put(EnchantmentLibraryBlockEntity.TAG_LEVELS, levels);

        TestLibraryBlockEntity be = newLibrary();
        be.loadCustomOnly(tag, provider);

        assertEquals(2048, be.getPoints().getInt(SHARPNESS),
                "resolvable point entry survives the load");
        assertEquals(5, be.getMaxLevels().getInt(SHARPNESS),
                "resolvable max-level entry survives the load");
        assertEquals(1, be.getPoints().size(),
                "unresolved key dropped from points, only the surviving one remains");
        assertEquals(1, be.getMaxLevels().size(),
                "unresolved key dropped from maxLevels as well");
        for (ResourceKey<Enchantment> key : be.getPoints().keySet()) {
            assertFalse(key.location().getPath().equals("never_registered"),
                    "dropped key must not appear in the resolved map");
        }
    }

    @Test
    void load_dropsMalformedKeysWithoutCrashing() {
        CompoundTag tag = new CompoundTag();
        CompoundTag points = new CompoundTag();
        points.putInt(SHARPNESS.location().toString(), 12);
        points.putInt("not a valid id", 42);
        tag.put(EnchantmentLibraryBlockEntity.TAG_POINTS, points);
        tag.put(EnchantmentLibraryBlockEntity.TAG_LEVELS, new CompoundTag());

        TestLibraryBlockEntity be = newLibrary();
        be.loadCustomOnly(tag, provider);

        assertEquals(12, be.getPoints().getInt(SHARPNESS),
                "valid entry survives alongside a malformed sibling");
        assertEquals(1, be.getPoints().size(),
                "malformed id dropped, leaving the valid entry alone");
    }

    @Test
    void load_clearsPriorStateBeforeApplyingNbt() {
        TestLibraryBlockEntity be = newLibrary();
        be.getPoints().put(UNBREAKING, 77);
        be.getMaxLevels().put(UNBREAKING, 2);

        CompoundTag tag = new CompoundTag();
        CompoundTag points = new CompoundTag();
        points.putInt(SHARPNESS.location().toString(), 1);
        tag.put(EnchantmentLibraryBlockEntity.TAG_POINTS, points);
        tag.put(EnchantmentLibraryBlockEntity.TAG_LEVELS, new CompoundTag());

        be.loadCustomOnly(tag, provider);

        assertFalse(be.getPoints().containsKey(UNBREAKING),
                "pre-load state must be dropped — load is an overwrite, not a merge");
        assertEquals(1, be.getPoints().getInt(SHARPNESS),
                "loaded entry lands as written");
    }

    @Test
    void save_emptyMapsYieldEmptyTags() {
        TestLibraryBlockEntity be = newLibrary();
        CompoundTag tag = be.saveCustomOnly(provider);
        assertTrue(tag.getCompound(EnchantmentLibraryBlockEntity.TAG_POINTS).isEmpty(),
                "empty point map → empty Points compound tag");
        assertTrue(tag.getCompound(EnchantmentLibraryBlockEntity.TAG_LEVELS).isEmpty(),
                "empty max-level map → empty Levels compound tag");

        TestLibraryBlockEntity reloaded = newLibrary();
        reloaded.loadCustomOnly(tag, provider);
        assertTrue(reloaded.getPoints().isEmpty(),
                "empty tag loads into an empty point map, no crash");
        assertTrue(reloaded.getMaxLevels().isEmpty(),
                "empty tag loads into an empty max-level map");
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
