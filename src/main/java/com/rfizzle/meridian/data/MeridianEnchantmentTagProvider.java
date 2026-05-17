package com.rfizzle.meridian.data;

import com.rfizzle.meridian.Meridian;

import java.util.concurrent.CompletableFuture;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.enchantment.Enchantment;

public class MeridianEnchantmentTagProvider extends FabricTagProvider.EnchantmentTagProvider {

    private static final TagKey<Enchantment> MACE_EXCLUSIVE = TagKey.create(
            Registries.ENCHANTMENT, ResourceLocation.withDefaultNamespace("exclusive_set/mace"));

    public MeridianEnchantmentTagProvider(FabricDataOutput output,
                                          CompletableFuture<HolderLookup.Provider> registryLookup) {
        super(output, registryLookup);
    }

    private static final TagKey<Enchantment> ASPECT_EXCLUSIVE = TagKey.create(
            Registries.ENCHANTMENT, Meridian.id("exclusive_set/aspect"));
    private static final TagKey<Enchantment> ARROW_IMPACT_EXCLUSIVE = TagKey.create(
            Registries.ENCHANTMENT, Meridian.id("exclusive_set/arrow_impact"));
    private static final TagKey<Enchantment> SIZE_EXCLUSIVE = TagKey.create(
            Registries.ENCHANTMENT, Meridian.id("exclusive_set/size"));
    private static final TagKey<Enchantment> MINING_EXCLUSIVE = TagKey.create(
            Registries.ENCHANTMENT, Meridian.id("exclusive_set/mining"));
    private static final TagKey<Enchantment> GLASS_CANNON_EXCLUSIVE = TagKey.create(
            Registries.ENCHANTMENT, Meridian.id("exclusive_set/glass_cannon"));
    private static final TagKey<Enchantment> MENDING_EXCLUSIVE = TagKey.create(
            Registries.ENCHANTMENT, Meridian.id("exclusive_set/mending"));

    @Override
    protected void addTags(HolderLookup.Provider wrapperLookup) {
        appendVanillaTags();
        addMeridianExclusiveSets();
    }

    private void addMeridianExclusiveSets() {
        getOrCreateTagBuilder(ASPECT_EXCLUSIVE)
                .addOptional(Meridian.id("blight"))
                .addOptional(Meridian.id("decay"))
                .addOptional(Meridian.id("shackle"))
                .addOptional(Meridian.id("nightfall"))
                .addOptional(mc("fire_aspect"));

        getOrCreateTagBuilder(ARROW_IMPACT_EXCLUSIVE)
                .addOptional(Meridian.id("gale_shot"))
                .addOptional(Meridian.id("resonance"))
                .addOptional(Meridian.id("permafrost"))
                .addOptional(Meridian.id("detonation"))
                .addOptional(Meridian.id("stormcall"));

        getOrCreateTagBuilder(SIZE_EXCLUSIVE)
                .addOptional(Meridian.id("diminish"))
                .addOptional(Meridian.id("colossus"));

        getOrCreateTagBuilder(MINING_EXCLUSIVE)
                .addOptional(Meridian.id("excavate"))
                .addOptional(Meridian.id("prospect"));

        getOrCreateTagBuilder(GLASS_CANNON_EXCLUSIVE)
                .addOptional(Meridian.id("bloodrage"))
                .addOptional(Meridian.id("reckless"));

        getOrCreateTagBuilder(MENDING_EXCLUSIVE)
                .addOptional(Meridian.id("vital_mend"))
                .addOptional(mc("mending"));
    }

    private void appendVanillaTags() {
        getOrCreateTagBuilder(EnchantmentTags.DAMAGE_EXCLUSIVE)
                .addOptional(Meridian.id("voidbane"))
                .addOptional(Meridian.id("sanctify"))
                .addOptional(Meridian.id("sentinel"))
                .addOptional(Meridian.id("rift_strike"))
                .addOptional(Meridian.id("keen_edge"));

        getOrCreateTagBuilder(EnchantmentTags.ARMOR_EXCLUSIVE)
                .addOptional(Meridian.id("spellguard"));

        getOrCreateTagBuilder(EnchantmentTags.BOOTS_EXCLUSIVE)
                .addOptional(Meridian.id("cinderwalk"));

        getOrCreateTagBuilder(MACE_EXCLUSIVE)
                .addOptional(mc("density"))
                .addOptional(mc("breach"))
                .addOptional(mc("wind_burst"))
                .addOptional(Meridian.id("tempest"))
                .addOptional(Meridian.id("seismic_slam"))
                .addOptional(Meridian.id("updraft"));
    }

    private static ResourceLocation mc(String path) {
        return ResourceLocation.withDefaultNamespace(path);
    }
}
