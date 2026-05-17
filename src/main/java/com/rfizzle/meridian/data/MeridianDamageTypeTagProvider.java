package com.rfizzle.meridian.data;

import com.rfizzle.meridian.Meridian;

import java.util.concurrent.CompletableFuture;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;

public class MeridianDamageTypeTagProvider extends FabricTagProvider<DamageType> {

    private static final TagKey<DamageType> IS_MAGIC =
            TagKey.create(Registries.DAMAGE_TYPE, Meridian.id("is_magic"));

    public MeridianDamageTypeTagProvider(FabricDataOutput output,
                                         CompletableFuture<HolderLookup.Provider> registryLookup) {
        super(output, Registries.DAMAGE_TYPE, registryLookup);
    }

    @Override
    protected void addTags(HolderLookup.Provider wrapperLookup) {
        getOrCreateTagBuilder(IS_MAGIC)
                .addOptional(ResourceLocation.withDefaultNamespace("magic"))
                .addOptional(ResourceLocation.withDefaultNamespace("indirect_magic"))
                .addOptional(ResourceLocation.withDefaultNamespace("dragon_breath"))
                .addOptional(ResourceLocation.withDefaultNamespace("wither"))
                .addOptional(ResourceLocation.withDefaultNamespace("sonic_boom"));
    }
}
