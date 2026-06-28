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

    /**
     * Curated subset of Meridian enchants that are balanced to appear on hostile-mob equipment.
     * This is a provider contract for sibling mods (e.g. Tribulation scaling mob gear) — Meridian
     * itself never reads it. Consumers hardcode the {@code meridian:mob_equipment} id and roll from
     * this pool, with vanilla item/exclusivity rules filtering what actually applies to a given mob.
     */
    public static final TagKey<Enchantment> MOB_EQUIPMENT = TagKey.create(
            Registries.ENCHANTMENT, Meridian.id("mob_equipment"));

    @Override
    protected void addTags(HolderLookup.Provider wrapperLookup) {
        appendVanillaTags();
        addMeridianExclusiveSets();
        addMeridianObtainabilityTags();
        addMobEquipmentTag();
    }

    /**
     * Populates {@link #MOB_EQUIPMENT}. Inclusion criteria: melee combat (bonus damage, on-hit
     * debuffs, reach/speed), armor protection (damage reduction, defensive retaliation, bonus
     * health), and ranged combat meaningful on a mob that shoots. Everything else is excluded:
     * <ul>
     *   <li>mobility/mount — {@code alacrity, clamber, slipstream, skybound, true_flight, updraft,
     *       vault, gallop, trample, saddleguard}</li>
     *   <li>mining/terrain/farming — {@code excavate, prospect, terrasculpt, masons_reach,
     *       steadfast, furrow, beckon, bounty, prismatic, renewal, cinderwalk}</li>
     *   <li>no meaningful mob behavior — {@code impact_ward} & {@code ironwing} (elytra-only),
     *       {@code animus, insight, soul_tax} (XP), {@code seismic_slam, tempest} (player
     *       crouch-slam input), {@code quell, gravitas, luminance, premonition, snare, tether,
     *       aurify}</li>
     *   <li>pure utility, not combat/protection — {@code fortify} (shield durability),
     *       {@code antidote, ricochet, permafrost, glacial_lance}</li>
     *   <li>treasure-tier swings — {@code bloodrage, colossus, reckless, retribution, final_gambit,
     *       detonation, diminish, rally, plunder, abyss_ward, vital_mend} (kept out of the
     *       baseline pool; a future {@code mob_equipment/elite} sub-tag could carry them)</li>
     *   <li>curses — {@code curse_of_decay, curse_of_sealing}</li>
     * </ul>
     * All entries use {@code addOptional} so the tag loads cleanly regardless of which enchants a
     * given world-state has registered.
     */
    private void addMobEquipmentTag() {
        getOrCreateTagBuilder(MOB_EQUIPMENT)
                // Melee combat — bonus damage, on-hit debuffs, reach & speed
                .addOptional(Meridian.id("blight"))
                .addOptional(Meridian.id("cleave"))
                .addOptional(Meridian.id("decay"))
                .addOptional(Meridian.id("keen_edge"))
                .addOptional(Meridian.id("nightfall"))
                .addOptional(Meridian.id("outreach"))
                .addOptional(Meridian.id("pummel"))
                .addOptional(Meridian.id("rift_strike"))
                .addOptional(Meridian.id("sanctify"))
                .addOptional(Meridian.id("sentinel"))
                .addOptional(Meridian.id("shackle"))
                .addOptional(Meridian.id("siphon"))
                .addOptional(Meridian.id("tempo"))
                .addOptional(Meridian.id("voidbane"))
                // Armor — damage reduction, defensive retaliation, bonus health
                .addOptional(Meridian.id("bulwark"))
                .addOptional(Meridian.id("frostguard"))
                .addOptional(Meridian.id("repulse"))
                .addOptional(Meridian.id("spellguard"))
                .addOptional(Meridian.id("vitality"))
                // Ranged combat — meaningful on mobs that shoot
                .addOptional(Meridian.id("gale_shot"))
                .addOptional(Meridian.id("resonance"))
                .addOptional(Meridian.id("stormcall"));
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
