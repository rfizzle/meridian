package com.rfizzle.meridian.data;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.MeridianRegistry;
import com.rfizzle.meridian.shelf.MeridianShelves;

import java.util.concurrent.CompletableFuture;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class MeridianBlockTagProvider extends FabricTagProvider.BlockTagProvider {

    private static final TagKey<Block> EXCAVATE_BLACKLIST = TagKey.create(Registries.BLOCK, Meridian.id("excavate_blacklist"));
    private static final TagKey<Block> NON_SOLID = TagKey.create(Registries.BLOCK, Meridian.id("non-solid"));
    private static final TagKey<Block> PROSPECT_ORES = TagKey.create(Registries.BLOCK, Meridian.id("prospect_ores"));

    public MeridianBlockTagProvider(FabricDataOutput output,
                                    CompletableFuture<HolderLookup.Provider> registryLookup) {
        super(output, registryLookup);
    }

    @Override
    protected void addTags(HolderLookup.Provider wrapperLookup) {
        addExcavateBlacklist();
        addNonSolid();
        addProspectOres();
        addMineableAxe();
        addMineablePickaxe();
    }

    private void addExcavateBlacklist() {
        getOrCreateTagBuilder(EXCAVATE_BLACKLIST)
                .addOptionalTag(BlockTags.CROPS)
                .addOptionalTag(BlockTags.FLOWERS)
                .addOptionalTag(BlockTags.SAPLINGS)
                .addOptionalTag(BlockTags.BANNERS)
                .addOptionalTag(BlockTags.BEDS)
                .addOptionalTag(BlockTags.CORALS)
                .addOptionalTag(BlockTags.DOORS)
                .addOptionalTag(BlockTags.BUTTONS)
                .addOptionalTag(BlockTags.SIGNS)
                .addOptionalTag(BlockTags.FENCES)
                .addOptionalTag(BlockTags.FENCE_GATES)
                .addOptionalTag(BlockTags.PRESSURE_PLATES)
                .addOptionalTag(BlockTags.WOOL_CARPETS)
                .addOptionalTag(BlockTags.SMALL_FLOWERS)
                .addOptionalTag(BlockTags.CANDLES)
                .addOptionalTag(BlockTags.REPLACEABLE)
                .addOptionalTag(BlockTags.DRAGON_IMMUNE)
                .add(Blocks.SPAWNER)
                .add(Blocks.TRIAL_SPAWNER)
                .add(Blocks.VAULT)
                .add(Blocks.POWDER_SNOW)
                .add(Blocks.STONECUTTER)
                .add(Blocks.LECTERN)
                .add(Blocks.BEEHIVE)
                .add(Blocks.BEE_NEST)
                .add(Blocks.COMPOSTER)
                .add(Blocks.CONDUIT)
                .add(Blocks.ENDER_CHEST)
                .add(Blocks.BARREL)
                .add(Blocks.CHEST)
                .addOptionalTag(BlockTags.SHULKER_BOXES)
                .add(Blocks.CRAFTING_TABLE)
                .add(Blocks.FURNACE)
                .add(Blocks.BLAST_FURNACE)
                .add(Blocks.SMITHING_TABLE)
                .add(Blocks.SMOKER)
                .add(Blocks.BREWING_STAND)
                .add(Blocks.ENCHANTING_TABLE)
                .add(Blocks.ANVIL)
                .add(Blocks.GRINDSTONE)
                .add(Blocks.NOTE_BLOCK)
                .add(Blocks.JUKEBOX)
                .add(Blocks.LODESTONE)
                .add(Blocks.BELL)
                .add(Blocks.BEACON)
                .add(Blocks.FLOWER_POT)
                .add(Blocks.LOOM)
                .add(Blocks.CARTOGRAPHY_TABLE)
                .add(Blocks.LANTERN)
                .add(Blocks.SOUL_LANTERN)
                .add(Blocks.SOUL_TORCH)
                .add(Blocks.TORCH)
                .add(Blocks.WALL_TORCH)
                .add(Blocks.REDSTONE_TORCH)
                .add(Blocks.CAMPFIRE)
                .add(Blocks.SOUL_CAMPFIRE)
                .add(Blocks.REDSTONE_WIRE)
                .add(Blocks.TRIPWIRE)
                .add(Blocks.TRIPWIRE_HOOK)
                .add(Blocks.LEVER);
    }

    private void addNonSolid() {
        getOrCreateTagBuilder(NON_SOLID)
                .addOptionalTag(BlockTags.REPLACEABLE);
    }

    private void addProspectOres() {
        getOrCreateTagBuilder(PROSPECT_ORES)
                .add(Blocks.GOLD_ORE)
                .add(Blocks.IRON_ORE)
                .add(Blocks.COPPER_ORE)
                .add(Blocks.COAL_ORE)
                .add(Blocks.LAPIS_ORE)
                .add(Blocks.DIAMOND_ORE)
                .add(Blocks.REDSTONE_ORE)
                .add(Blocks.EMERALD_ORE)
                .add(Blocks.NETHER_QUARTZ_ORE)
                .add(Blocks.NETHER_GOLD_ORE)
                .add(Blocks.DEEPSLATE_GOLD_ORE)
                .add(Blocks.DEEPSLATE_IRON_ORE)
                .add(Blocks.DEEPSLATE_COPPER_ORE)
                .add(Blocks.DEEPSLATE_COAL_ORE)
                .add(Blocks.DEEPSLATE_LAPIS_ORE)
                .add(Blocks.DEEPSLATE_DIAMOND_ORE)
                .add(Blocks.DEEPSLATE_REDSTONE_ORE)
                .add(Blocks.DEEPSLATE_EMERALD_ORE)
                .add(Blocks.ANCIENT_DEBRIS);
    }

    private void addMineableAxe() {
        getOrCreateTagBuilder(BlockTags.MINEABLE_WITH_AXE)
                .add(MeridianShelves.BEESHELF)
                .add(MeridianShelves.MELONSHELF)
                .add(MeridianRegistry.FILTERING_SHELF);
    }

    private void addMineablePickaxe() {
        getOrCreateTagBuilder(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(MeridianShelves.STONESHELF)
                .add(MeridianShelves.HELLSHELF)
                .add(MeridianShelves.BLAZING_HELLSHELF)
                .add(MeridianShelves.GLOWING_HELLSHELF)
                .add(MeridianShelves.INFUSED_HELLSHELF)
                .add(MeridianShelves.SEASHELF)
                .add(MeridianShelves.HEART_SEASHELF)
                .add(MeridianShelves.CRYSTAL_SEASHELF)
                .add(MeridianShelves.INFUSED_SEASHELF)
                .add(MeridianShelves.ENDSHELF)
                .add(MeridianShelves.PEARL_ENDSHELF)
                .add(MeridianShelves.DRACONIC_ENDSHELF)
                .add(MeridianShelves.DEEPSHELF)
                .add(MeridianShelves.DORMANT_DEEPSHELF)
                .add(MeridianShelves.ECHOING_DEEPSHELF)
                .add(MeridianShelves.SOUL_TOUCHED_DEEPSHELF)
                .add(MeridianShelves.ECHOING_SCULKSHELF)
                .add(MeridianShelves.SOUL_TOUCHED_SCULKSHELF)
                .add(MeridianShelves.SIGHTSHELF)
                .add(MeridianShelves.SIGHTSHELF_T2)
                .add(MeridianShelves.RECTIFIER)
                .add(MeridianShelves.RECTIFIER_T2)
                .add(MeridianShelves.RECTIFIER_T3)
                .add(MeridianRegistry.TREASURE_SHELF)
                .add(MeridianRegistry.BASIC_LIBRARY)
                .add(MeridianRegistry.ENDER_LIBRARY);
    }
}
