package com.rfizzle.meridian.data;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.shelf.MeridianShelves;

import java.util.concurrent.CompletableFuture;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public class MeridianItemTagProvider extends FabricTagProvider.ItemTagProvider {

    private static final TagKey<Item> DEEPSLATE = TagKey.create(Registries.ITEM, Meridian.id("deepslate"));
    private static final TagKey<Item> ENCHANTABLE_DOG = TagKey.create(Registries.ITEM, Meridian.id("enchantable/dog"));
    private static final TagKey<Item> ENCHANTABLE_ELYTRA = TagKey.create(Registries.ITEM, Meridian.id("enchantable/elytra"));
    private static final TagKey<Item> ENCHANTABLE_HOES = TagKey.create(Registries.ITEM, Meridian.id("enchantable/hoes"));
    private static final TagKey<Item> ENCHANTABLE_MINING_AND_DAMAGE = TagKey.create(Registries.ITEM, Meridian.id("enchantable/mining_and_damage"));
    private static final TagKey<Item> ENCHANTABLE_MOUNTED = TagKey.create(Registries.ITEM, Meridian.id("enchantable/mounted"));
    private static final TagKey<Item> ENCHANTABLE_SHEARS = TagKey.create(Registries.ITEM, Meridian.id("enchantable/shears"));
    private static final TagKey<Item> ENCHANTABLE_SHIELD = TagKey.create(Registries.ITEM, Meridian.id("enchantable/shield"));
    private static final TagKey<Item> ENCHANTABLE_PICKAXES = TagKey.create(Registries.ITEM, Meridian.id("enchantable/pickaxes"));
    private static final TagKey<Item> ENCHANTABLE_RANGE = TagKey.create(Registries.ITEM, Meridian.id("enchantable/range"));
    private static final TagKey<Item> ENCHANTABLE_CHEST_AND_LEG = TagKey.create(Registries.ITEM, Meridian.id("enchantable/chest_and_leg_armor"));
    private static final TagKey<Item> ENCHANTABLE_LEG_AND_FOOT = TagKey.create(Registries.ITEM, Meridian.id("enchantable/leg_and_foot_armor"));
    private static final TagKey<Item> INFUSED_SHELVES = TagKey.create(Registries.ITEM, Meridian.id("infused_shelves"));

    public MeridianItemTagProvider(FabricDataOutput output,
                                   CompletableFuture<HolderLookup.Provider> registryLookup,
                                   BlockTagProvider blockTagProvider) {
        super(output, registryLookup, blockTagProvider);
    }

    @Override
    protected void addTags(HolderLookup.Provider wrapperLookup) {
        getOrCreateTagBuilder(DEEPSLATE)
                .add(Items.DEEPSLATE)
                .add(Items.COBBLED_DEEPSLATE)
                .add(Items.CHISELED_DEEPSLATE)
                .add(Items.POLISHED_DEEPSLATE)
                .add(Items.DEEPSLATE_BRICKS)
                .add(Items.CRACKED_DEEPSLATE_BRICKS)
                .add(Items.DEEPSLATE_TILES)
                .add(Items.CRACKED_DEEPSLATE_TILES);

        getOrCreateTagBuilder(ENCHANTABLE_DOG)
                .add(Items.WOLF_ARMOR);

        getOrCreateTagBuilder(ENCHANTABLE_ELYTRA)
                .add(Items.ELYTRA);

        getOrCreateTagBuilder(ENCHANTABLE_HOES)
                .addOptionalTag(ItemTags.HOES);

        getOrCreateTagBuilder(ENCHANTABLE_MINING_AND_DAMAGE)
                .addOptionalTag(ItemTags.MINING_ENCHANTABLE)
                .addOptionalTag(ItemTags.WEAPON_ENCHANTABLE);

        getOrCreateTagBuilder(ENCHANTABLE_MOUNTED)
                .add(Items.IRON_HORSE_ARMOR)
                .add(Items.GOLDEN_HORSE_ARMOR)
                .add(Items.DIAMOND_HORSE_ARMOR)
                .add(Items.LEATHER_HORSE_ARMOR);

        getOrCreateTagBuilder(ENCHANTABLE_SHEARS)
                .add(Items.SHEARS);

        getOrCreateTagBuilder(ENCHANTABLE_SHIELD)
                .add(Items.SHIELD);

        getOrCreateTagBuilder(ENCHANTABLE_PICKAXES)
                .addOptionalTag(ItemTags.PICKAXES);

        getOrCreateTagBuilder(ENCHANTABLE_RANGE)
                .add(Items.BOW)
                .add(Items.CROSSBOW);

        getOrCreateTagBuilder(ENCHANTABLE_CHEST_AND_LEG)
                .addOptionalTag(ItemTags.CHEST_ARMOR_ENCHANTABLE)
                .addOptionalTag(ItemTags.LEG_ARMOR_ENCHANTABLE);

        getOrCreateTagBuilder(ENCHANTABLE_LEG_AND_FOOT)
                .addOptionalTag(ItemTags.FOOT_ARMOR_ENCHANTABLE)
                .addOptionalTag(ItemTags.LEG_ARMOR_ENCHANTABLE);

        getOrCreateTagBuilder(INFUSED_SHELVES)
                .add(MeridianShelves.INFUSED_HELLSHELF.asItem())
                .add(MeridianShelves.INFUSED_SEASHELF.asItem())
                .add(MeridianShelves.DEEPSHELF.asItem());

        getOrCreateTagBuilder(ItemTags.WEAPON_ENCHANTABLE)
                .add(Items.SHIELD);
    }
}
