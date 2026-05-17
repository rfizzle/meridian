package com.rfizzle.meridian.data;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

public class MeridianDataGenerator implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator generator) {
        FabricDataGenerator.Pack pack = generator.createPack();
        pack.addProvider(MeridianModelProvider::new);
        pack.addProvider(MeridianBlockLootTableProvider::new);
        pack.addProvider(MeridianRecipeProvider::new);
        MeridianBlockTagProvider blockTags = pack.addProvider(MeridianBlockTagProvider::new);
        pack.addProvider((output, registries) -> new MeridianItemTagProvider(output, registries, blockTags));
        pack.addProvider(MeridianEnchantmentTagProvider::new);
        pack.addProvider(MeridianEntityTypeTagProvider::new);
        pack.addProvider(MeridianDamageTypeTagProvider::new);
    }
}
