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
        addMeridianObtainabilityTags();
    }

    private void addMeridianObtainabilityTags() {
        getOrCreateTagBuilder(EnchantmentTags.NON_TREASURE)
                .addOptional(Meridian.id("alacrity"))
                .addOptional(Meridian.id("animus"))
                .addOptional(Meridian.id("antidote"))
                .addOptional(Meridian.id("beckon"))
                .addOptional(Meridian.id("blight"))
                .addOptional(Meridian.id("bounty"))
                .addOptional(Meridian.id("bulwark"))
                .addOptional(Meridian.id("cinderwalk"))
                .addOptional(Meridian.id("clamber"))
                .addOptional(Meridian.id("cleave"))
                .addOptional(Meridian.id("decay"))
                .addOptional(Meridian.id("excavate"))
                .addOptional(Meridian.id("fortify"))
                .addOptional(Meridian.id("frostguard"))
                .addOptional(Meridian.id("furrow"))
                .addOptional(Meridian.id("gale_shot"))
                .addOptional(Meridian.id("gallop"))
                .addOptional(Meridian.id("glacial_lance"))
                .addOptional(Meridian.id("gravitas"))
                .addOptional(Meridian.id("impact_ward"))
                .addOptional(Meridian.id("insight"))
                .addOptional(Meridian.id("ironwing"))
                .addOptional(Meridian.id("keen_edge"))
                .addOptional(Meridian.id("luminance"))
                .addOptional(Meridian.id("masons_reach"))
                .addOptional(Meridian.id("nightfall"))
                .addOptional(Meridian.id("outreach"))
                .addOptional(Meridian.id("permafrost"))
                .addOptional(Meridian.id("premonition"))
                .addOptional(Meridian.id("prismatic"))
                .addOptional(Meridian.id("prospect"))
                .addOptional(Meridian.id("pummel"))
                .addOptional(Meridian.id("quell"))
                .addOptional(Meridian.id("renewal"))
                .addOptional(Meridian.id("repulse"))
                .addOptional(Meridian.id("resonance"))
                .addOptional(Meridian.id("ricochet"))
                .addOptional(Meridian.id("rift_strike"))
                .addOptional(Meridian.id("saddleguard"))
                .addOptional(Meridian.id("sanctify"))
                .addOptional(Meridian.id("seismic_slam"))
                .addOptional(Meridian.id("sentinel"))
                .addOptional(Meridian.id("shackle"))
                .addOptional(Meridian.id("siphon"))
                .addOptional(Meridian.id("skybound"))
                .addOptional(Meridian.id("slipstream"))
                .addOptional(Meridian.id("soul_tax"))
                .addOptional(Meridian.id("spellguard"))
                .addOptional(Meridian.id("steadfast"))
                .addOptional(Meridian.id("stormcall"))
                .addOptional(Meridian.id("tempest"))
                .addOptional(Meridian.id("tempo"))
                .addOptional(Meridian.id("terrasculpt"))
                .addOptional(Meridian.id("trample"))
                .addOptional(Meridian.id("true_flight"))
                .addOptional(Meridian.id("updraft"))
                .addOptional(Meridian.id("vault"))
                .addOptional(Meridian.id("vitality"))
                .addOptional(Meridian.id("voidbane"));

        getOrCreateTagBuilder(EnchantmentTags.TREASURE)
                .addOptional(Meridian.id("abyss_ward"))
                .addOptional(Meridian.id("aurify"))
                .addOptional(Meridian.id("bloodrage"))
                .addOptional(Meridian.id("colossus"))
                .addOptional(Meridian.id("detonation"))
                .addOptional(Meridian.id("diminish"))
                .addOptional(Meridian.id("final_gambit"))
                .addOptional(Meridian.id("plunder"))
                .addOptional(Meridian.id("rally"))
                .addOptional(Meridian.id("reckless"))
                .addOptional(Meridian.id("retribution"))
                .addOptional(Meridian.id("snare"))
                .addOptional(Meridian.id("tether"))
                .addOptional(Meridian.id("vital_mend"));

        getOrCreateTagBuilder(EnchantmentTags.CURSE)
                .addOptional(Meridian.id("curse_of_decay"))
                .addOptional(Meridian.id("curse_of_sealing"));

        getOrCreateTagBuilder(EnchantmentTags.TRADEABLE)
                .addOptional(Meridian.id("abyss_ward"))
                .addOptional(Meridian.id("aurify"))
                .addOptional(Meridian.id("bloodrage"))
                .addOptional(Meridian.id("colossus"))
                .addOptional(Meridian.id("detonation"))
                .addOptional(Meridian.id("diminish"))
                .addOptional(Meridian.id("final_gambit"))
                .addOptional(Meridian.id("plunder"))
                .addOptional(Meridian.id("rally"))
                .addOptional(Meridian.id("reckless"))
                .addOptional(Meridian.id("retribution"))
                .addOptional(Meridian.id("snare"))
                .addOptional(Meridian.id("tether"))
                .addOptional(Meridian.id("vital_mend"))
                .addOptional(Meridian.id("curse_of_decay"))
                .addOptional(Meridian.id("curse_of_sealing"));

        getOrCreateTagBuilder(EnchantmentTags.ON_RANDOM_LOOT)
                .addOptional(Meridian.id("abyss_ward"))
                .addOptional(Meridian.id("aurify"))
                .addOptional(Meridian.id("bloodrage"))
                .addOptional(Meridian.id("colossus"))
                .addOptional(Meridian.id("detonation"))
                .addOptional(Meridian.id("diminish"))
                .addOptional(Meridian.id("final_gambit"))
                .addOptional(Meridian.id("plunder"))
                .addOptional(Meridian.id("rally"))
                .addOptional(Meridian.id("reckless"))
                .addOptional(Meridian.id("retribution"))
                .addOptional(Meridian.id("snare"))
                .addOptional(Meridian.id("tether"))
                .addOptional(Meridian.id("vital_mend"))
                .addOptional(Meridian.id("curse_of_decay"))
                .addOptional(Meridian.id("curse_of_sealing"));

        getOrCreateTagBuilder(EnchantmentTags.DOUBLE_TRADE_PRICE)
                .addOptional(Meridian.id("curse_of_decay"))
                .addOptional(Meridian.id("curse_of_sealing"));
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
                .addOptional(Meridian.id("tempest"))
                .addOptional(Meridian.id("seismic_slam"))
                .addOptional(Meridian.id("updraft"));
    }

    private static ResourceLocation mc(String path) {
        return ResourceLocation.withDefaultNamespace(path);
    }
}
