package com.rfizzle.meridian.data;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.enchanting.EnchantmentEffects;
import com.rfizzle.meridian.enchanting.RealEnchantmentHelper;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

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
    private static final TagKey<Enchantment> AXE_EXCLUSIVE = TagKey.create(
            Registries.ENCHANTMENT, Meridian.id("exclusive_set/axe"));
    private static final TagKey<Enchantment> GLASS_CANNON_EXCLUSIVE = TagKey.create(
            Registries.ENCHANTMENT, Meridian.id("exclusive_set/glass_cannon"));
    private static final TagKey<Enchantment> MENDING_EXCLUSIVE = TagKey.create(
            Registries.ENCHANTMENT, Meridian.id("exclusive_set/mending"));
    private static final TagKey<Enchantment> LOOT_BONUS_EXCLUSIVE = TagKey.create(
            Registries.ENCHANTMENT, Meridian.id("exclusive_set/loot_bonus"));
    private static final TagKey<Enchantment> TROPHY_EXCLUSIVE = TagKey.create(
            Registries.ENCHANTMENT, Meridian.id("exclusive_set/trophy"));
    private static final TagKey<Enchantment> MOBILITY_EXCLUSIVE = TagKey.create(
            Registries.ENCHANTMENT, Meridian.id("exclusive_set/mobility"));

    /**
     * Curated subset of Meridian enchants that are balanced to appear on hostile-mob equipment.
     * This is a provider contract for sibling mods (e.g. Tribulation scaling mob gear) — Meridian
     * itself never reads it. Consumers hardcode the {@code meridian:mob_equipment} id and roll from
     * this pool, with vanilla item/exclusivity rules filtering what actually applies to a given mob.
     */
    public static final TagKey<Enchantment> MOB_EQUIPMENT = TagKey.create(
            Registries.ENCHANTMENT, Meridian.id("mob_equipment"));

    /**
     * Published rarity classification of the non-curse enchantment catalog — a provider contract
     * for sibling mods and datapacks (Prosperity's tag-sourced loot injection is the driving
     * consumer). Indexed by {@link RealEnchantmentHelper#rarityBucket(int)}: common, uncommon,
     * rare, very_rare. Each non-curse enchantment lands in exactly one tag, derived from its
     * registered {@code weight}, so the tags and the enchanting table's Arcana buckets can never
     * disagree. Consumers reference them as e.g. {@code #meridian:rarity/rare} in a vanilla
     * {@code minecraft:enchant_randomly} loot function.
     */
    public static final List<TagKey<Enchantment>> RARITY_TAGS = List.of(
            TagKey.create(Registries.ENCHANTMENT, Meridian.id("rarity/common")),
            TagKey.create(Registries.ENCHANTMENT, Meridian.id("rarity/uncommon")),
            TagKey.create(Registries.ENCHANTMENT, Meridian.id("rarity/rare")),
            TagKey.create(Registries.ENCHANTMENT, Meridian.id("rarity/very_rare")));

    /**
     * Meridian's curses. Single source of truth for curse-ness in datagen: members go into
     * {@code #minecraft:curse} and are excluded from every {@link #RARITY_TAGS} tag.
     */
    private static final List<ResourceLocation> CURSES = List.of(
            Meridian.id("curse_of_decay"),
            Meridian.id("curse_of_sealing"),
            Meridian.id("curse_of_echoes"),
            Meridian.id("curse_of_hunger"),
            Meridian.id("curse_of_attraction"),
            Meridian.id("curse_of_leaden"),
            Meridian.id("curse_of_blunting"),
            Meridian.id("curse_of_fumbling"),
            Meridian.id("curse_of_wavering"),
            Meridian.id("curse_of_timidity"),
            Meridian.id("curse_of_molting"),
            Meridian.id("curse_of_skittishness"),
            Meridian.id("curse_of_obscurity"));

    @Override
    protected void addTags(HolderLookup.Provider wrapperLookup) {
        appendVanillaTags();
        addConventionTags();
        addMeridianExclusiveSets();
        addMeridianObtainabilityTags();
        addMobEquipmentTag();
        addRarityTags(wrapperLookup);
    }

    /**
     * Populates {@link #RARITY_TAGS} by enumerating every {@code meridian:} enchantment definition
     * and bucketing it by its registered weight — a newly added enchantment lands in the correct
     * tag on the next datagen run with no tag edit here. The definitions are read straight from
     * {@code data/meridian/enchantment/*.json} on the classpath: the datagen registry lookup only
     * contains vanilla and {@code buildRegistry}-generated dynamic entries, not a mod's
     * hand-written data files.
     */
    private void addRarityTags(HolderLookup.Provider wrapperLookup) {
        var byRarity = loadEnchantmentWeights().entrySet().stream()
                .filter(entry -> !CURSES.contains(entry.getKey()))
                .collect(Collectors.groupingBy(
                        entry -> RealEnchantmentHelper.rarityBucket(entry.getValue()),
                        LinkedHashMap::new,
                        Collectors.mapping(Map.Entry::getKey, Collectors.toList())));
        for (int bucket = 0; bucket < RARITY_TAGS.size(); bucket++) {
            var builder = getOrCreateTagBuilder(RARITY_TAGS.get(bucket));
            byRarity.getOrDefault(bucket, List.of()).forEach(builder::addOptional);
        }
    }

    /**
     * Reads each enchantment definition's {@code weight} from {@code data/meridian/enchantment/}
     * on the classpath, keyed by enchantment id in sorted order (deterministic tag output).
     */
    private static Map<ResourceLocation, Integer> loadEnchantmentWeights() {
        URL url = MeridianEnchantmentTagProvider.class.getClassLoader()
                .getResource("data/" + Meridian.MOD_ID + "/enchantment");
        if (url == null || !"file".equals(url.getProtocol())) {
            throw new IllegalStateException(
                    "enchantment definitions not found as a directory on the datagen classpath: " + url);
        }
        try (var files = Files.list(Path.of(url.toURI()))) {
            Map<ResourceLocation, Integer> weights = new LinkedHashMap<>();
            for (Path file : files.filter(f -> f.getFileName().toString().endsWith(".json"))
                    .sorted(Comparator.comparing(f -> f.getFileName().toString())).toList()) {
                String name = file.getFileName().toString();
                JsonObject json = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
                weights.put(Meridian.id(name.substring(0, name.length() - ".json".length())),
                        json.getAsJsonPrimitive("weight").getAsInt());
            }
            return weights;
        } catch (IOException | URISyntaxException e) {
            throw new IllegalStateException("failed to read enchantment definitions", e);
        }
    }

    /**
     * Meridian's contribution to the {@code c:} convention namespace. {@code #c:soulbound} is the
     * keep-on-death contract shared with sibling mods (Tribulation adds its own
     * {@code tribulation:soulbound} to the same tag); each mod contributes only its own enchant,
     * and Fabric merges the memberships when both are installed.
     */
    private void addConventionTags() {
        getOrCreateTagBuilder(EnchantmentEffects.SOULBOUND)
                .addOptional(Meridian.id("tether"));
    }

    /**
     * Populates {@link #MOB_EQUIPMENT}. Inclusion criteria: melee combat (bonus damage, on-hit
     * debuffs, reach/speed), armor protection (damage reduction, defensive retaliation, bonus
     * health), and ranged combat meaningful on a mob that shoots. Everything else is excluded:
     * <ul>
     *   <li>mobility/mount — {@code alacrity, clamber, loft, slipstream, skybound, true_flight,
     *       updraft, vault, gallop, trample, saddleguard, wavestride, endurance}, plus
     *       {@code joust} (mount-gated, inert on an unmounted mob)</li>
     *   <li>mining/terrain/farming — {@code excavate, prospect, grind, adamant, reclaim,
     *       terrasculpt, masons_reach, steadfast, furrow, beckon, bounty, prismatic, renewal,
     *       cinderwalk}</li>
     *   <li>no meaningful mob behavior — {@code impact_ward}, {@code ironwing} & {@code tailwind}
     *       (elytra-only), {@code animus, insight, soul_tax} (XP), {@code seismic_slam, tempest}
     *       (player crouch-slam input), {@code pinpoint} (player crit input), {@code riposte} &
     *       {@code stagger} (mobs never shield-block, so the block never triggers — and Stagger's
     *       player-attacker path is config-gated off by default besides), {@code quell, gravitas,
     *       luminance, premonition, snare, tether, aurify}, {@code sunder} (its player-victim
     *       path is config-gated off by default, so it would be inert on mob gear),
     *       {@code seeker} (its player-target path is config-gated off by default, and
     *       aim-assist on mob bolts is pure frustration besides)</li>
     *   <li>pure utility, not combat/protection — {@code fortify} (shield durability),
     *       {@code antidote, inexorable, ricochet, permafrost, glacial_lance}, plus
     *       {@code harpoon, undertow} (a drowned dragging players to it, or gathering a
     *       crowd, is pure frustration, not challenge)</li>
     *   <li>treasure-tier swings — {@code bloodrage, colossus, reckless, retribution, final_gambit,
     *       detonation, diminish, rally, plunder, abyss_ward, vital_mend}, plus {@code blink}
     *       (a mob that cheats death and teleports away is pure frustration, not challenge)
     *       (kept out of the baseline pool; a future {@code mob_equipment/elite} sub-tag could
     *       carry them)</li>
     *   <li>loot/trophy payoff, meaningless when a mob holds it — {@code trophy, fortuity}</li>
     *   <li>curses — {@code curse_of_decay, curse_of_sealing, curse_of_echoes, curse_of_hunger,
     *       curse_of_attraction, curse_of_leaden, curse_of_blunting, curse_of_fumbling,
     *       curse_of_wavering, curse_of_timidity, curse_of_molting, curse_of_skittishness,
     *       curse_of_obscurity}</li>
     * </ul>
     * All entries use {@code addOptional} so the tag loads cleanly regardless of which enchants a
     * given world-state has registered.
     */
    private void addMobEquipmentTag() {
        getOrCreateTagBuilder(MOB_EQUIPMENT)
                // Melee combat — bonus damage, on-hit debuffs, reach & speed
                .addOptional(Meridian.id("ambush"))
                .addOptional(Meridian.id("blight"))
                .addOptional(Meridian.id("cleave"))
                .addOptional(Meridian.id("crescendo"))
                .addOptional(Meridian.id("decay"))
                .addOptional(Meridian.id("keen_edge"))
                .addOptional(Meridian.id("nightfall"))
                .addOptional(Meridian.id("outreach"))
                .addOptional(Meridian.id("pummel"))
                .addOptional(Meridian.id("reap"))
                .addOptional(Meridian.id("rift_strike"))
                .addOptional(Meridian.id("sanctify"))
                .addOptional(Meridian.id("sentinel"))
                .addOptional(Meridian.id("shackle"))
                .addOptional(Meridian.id("siphon"))
                .addOptional(Meridian.id("tempo"))
                .addOptional(Meridian.id("torrent"))
                .addOptional(Meridian.id("voidbane"))
                // Armor — damage reduction, defensive retaliation, bonus health
                .addOptional(Meridian.id("bulwark"))
                .addOptional(Meridian.id("emberward"))
                .addOptional(Meridian.id("frostguard"))
                .addOptional(Meridian.id("reprieve"))
                .addOptional(Meridian.id("repulse"))
                .addOptional(Meridian.id("spellguard"))
                .addOptional(Meridian.id("vitality"))
                // Ranged combat — meaningful on mobs that shoot
                .addOptional(Meridian.id("gale_shot"))
                .addOptional(Meridian.id("longshot"))
                .addOptional(Meridian.id("resonance"))
                .addOptional(Meridian.id("stormcall"));
    }

    private void addMeridianObtainabilityTags() {
        getOrCreateTagBuilder(EnchantmentTags.NON_TREASURE)
                .addOptional(Meridian.id("adamant"))
                .addOptional(Meridian.id("alacrity"))
                .addOptional(Meridian.id("ambush"))
                .addOptional(Meridian.id("animus"))
                .addOptional(Meridian.id("antidote"))
                .addOptional(Meridian.id("bastion"))
                .addOptional(Meridian.id("beckon"))
                .addOptional(Meridian.id("blight"))
                .addOptional(Meridian.id("blink"))
                .addOptional(Meridian.id("bounty"))
                .addOptional(Meridian.id("bulwark"))
                .addOptional(Meridian.id("cinderwalk"))
                .addOptional(Meridian.id("clamber"))
                .addOptional(Meridian.id("cleave"))
                .addOptional(Meridian.id("crescendo"))
                .addOptional(Meridian.id("decay"))
                .addOptional(Meridian.id("decoy"))
                .addOptional(Meridian.id("emberward"))
                .addOptional(Meridian.id("endurance"))
                .addOptional(Meridian.id("everbloom"))
                .addOptional(Meridian.id("excavate"))
                .addOptional(Meridian.id("fortify"))
                .addOptional(Meridian.id("fortuity"))
                .addOptional(Meridian.id("frostguard"))
                .addOptional(Meridian.id("furrow"))
                .addOptional(Meridian.id("gale_shot"))
                .addOptional(Meridian.id("gallop"))
                .addOptional(Meridian.id("glacial_lance"))
                .addOptional(Meridian.id("grapnel"))
                .addOptional(Meridian.id("gravitas"))
                .addOptional(Meridian.id("grind"))
                .addOptional(Meridian.id("harpoon"))
                .addOptional(Meridian.id("hush"))
                .addOptional(Meridian.id("impact_ward"))
                .addOptional(Meridian.id("inexorable"))
                .addOptional(Meridian.id("insight"))
                .addOptional(Meridian.id("ironwing"))
                .addOptional(Meridian.id("joust"))
                .addOptional(Meridian.id("keen_edge"))
                .addOptional(Meridian.id("kiln"))
                .addOptional(Meridian.id("loft"))
                .addOptional(Meridian.id("longshot"))
                .addOptional(Meridian.id("luminance"))
                .addOptional(Meridian.id("mark"))
                .addOptional(Meridian.id("masons_reach"))
                .addOptional(Meridian.id("meticulous"))
                .addOptional(Meridian.id("nightfall"))
                .addOptional(Meridian.id("outreach"))
                .addOptional(Meridian.id("permafrost"))
                .addOptional(Meridian.id("pinpoint"))
                .addOptional(Meridian.id("premonition"))
                .addOptional(Meridian.id("prismatic"))
                .addOptional(Meridian.id("prospect"))
                .addOptional(Meridian.id("pummel"))
                .addOptional(Meridian.id("quell"))
                .addOptional(Meridian.id("reap"))
                .addOptional(Meridian.id("reclaim"))
                .addOptional(Meridian.id("renewal"))
                .addOptional(Meridian.id("reprieve"))
                .addOptional(Meridian.id("repulse"))
                .addOptional(Meridian.id("resonance"))
                .addOptional(Meridian.id("ricochet"))
                .addOptional(Meridian.id("rift_strike"))
                .addOptional(Meridian.id("riposte"))
                .addOptional(Meridian.id("saddleguard"))
                .addOptional(Meridian.id("sanctify"))
                .addOptional(Meridian.id("seeker"))
                .addOptional(Meridian.id("seismic_slam"))
                .addOptional(Meridian.id("sentinel"))
                .addOptional(Meridian.id("shackle"))
                .addOptional(Meridian.id("siphon"))
                .addOptional(Meridian.id("skybound"))
                .addOptional(Meridian.id("slipstream"))
                .addOptional(Meridian.id("soul_tax"))
                .addOptional(Meridian.id("spellguard"))
                .addOptional(Meridian.id("stagger"))
                .addOptional(Meridian.id("steadfast"))
                .addOptional(Meridian.id("stormcall"))
                .addOptional(Meridian.id("sunder"))
                .addOptional(Meridian.id("tailwind"))
                .addOptional(Meridian.id("tempest"))
                .addOptional(Meridian.id("tempo"))
                .addOptional(Meridian.id("terrasculpt"))
                .addOptional(Meridian.id("thermal"))
                .addOptional(Meridian.id("timberfell"))
                .addOptional(Meridian.id("torrent"))
                .addOptional(Meridian.id("trample"))
                .addOptional(Meridian.id("trophy"))
                .addOptional(Meridian.id("true_flight"))
                .addOptional(Meridian.id("umbral"))
                .addOptional(Meridian.id("undertow"))
                .addOptional(Meridian.id("updraft"))
                .addOptional(Meridian.id("vault"))
                .addOptional(Meridian.id("vitality"))
                .addOptional(Meridian.id("voidbane"))
                .addOptional(Meridian.id("volley"))
                .addOptional(Meridian.id("wavestride"))
                .addOptional(Meridian.id("winterward"));

        getOrCreateTagBuilder(EnchantmentTags.TREASURE)
                .addOptional(Meridian.id("abyss_ward"))
                .addOptional(Meridian.id("attunement"))
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

        var curseBuilder = getOrCreateTagBuilder(EnchantmentTags.CURSE);
        CURSES.forEach(curseBuilder::addOptional);

        getOrCreateTagBuilder(EnchantmentTags.TRADEABLE)
                .addOptional(Meridian.id("abyss_ward"))
                .addOptional(Meridian.id("attunement"))
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
                .addOptional(Meridian.id("curse_of_sealing"))
                .addOptional(Meridian.id("curse_of_echoes"))
                .addOptional(Meridian.id("curse_of_hunger"))
                .addOptional(Meridian.id("curse_of_attraction"))
                .addOptional(Meridian.id("curse_of_leaden"))
                .addOptional(Meridian.id("curse_of_blunting"))
                .addOptional(Meridian.id("curse_of_fumbling"))
                .addOptional(Meridian.id("curse_of_wavering"))
                .addOptional(Meridian.id("curse_of_timidity"))
                .addOptional(Meridian.id("curse_of_molting"))
                .addOptional(Meridian.id("curse_of_skittishness"))
                .addOptional(Meridian.id("curse_of_obscurity"));

        getOrCreateTagBuilder(EnchantmentTags.ON_RANDOM_LOOT)
                .addOptional(Meridian.id("abyss_ward"))
                .addOptional(Meridian.id("attunement"))
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
                .addOptional(Meridian.id("curse_of_sealing"))
                .addOptional(Meridian.id("curse_of_echoes"))
                .addOptional(Meridian.id("curse_of_hunger"))
                .addOptional(Meridian.id("curse_of_attraction"))
                .addOptional(Meridian.id("curse_of_leaden"))
                .addOptional(Meridian.id("curse_of_blunting"))
                .addOptional(Meridian.id("curse_of_fumbling"))
                .addOptional(Meridian.id("curse_of_wavering"))
                .addOptional(Meridian.id("curse_of_timidity"))
                .addOptional(Meridian.id("curse_of_molting"))
                .addOptional(Meridian.id("curse_of_skittishness"))
                .addOptional(Meridian.id("curse_of_obscurity"));

        getOrCreateTagBuilder(EnchantmentTags.DOUBLE_TRADE_PRICE)
                .addOptional(Meridian.id("curse_of_decay"))
                .addOptional(Meridian.id("curse_of_sealing"))
                .addOptional(Meridian.id("curse_of_echoes"))
                .addOptional(Meridian.id("curse_of_hunger"))
                .addOptional(Meridian.id("curse_of_attraction"))
                .addOptional(Meridian.id("curse_of_leaden"))
                .addOptional(Meridian.id("curse_of_blunting"))
                .addOptional(Meridian.id("curse_of_fumbling"))
                .addOptional(Meridian.id("curse_of_wavering"))
                .addOptional(Meridian.id("curse_of_timidity"))
                .addOptional(Meridian.id("curse_of_molting"))
                .addOptional(Meridian.id("curse_of_skittishness"))
                .addOptional(Meridian.id("curse_of_obscurity"));
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
                .addOptional(Meridian.id("grind"))
                .addOptional(Meridian.id("prospect"));

        // Axe-only mass-harvest set. Timberfell is the sole member today; the dedicated set
        // (rather than reusing the pickaxe-oriented mining set) keeps room for future axe
        // enchants that should conflict with whole-tree felling.
        getOrCreateTagBuilder(AXE_EXCLUSIVE)
                .addOptional(Meridian.id("timberfell"));

        getOrCreateTagBuilder(GLASS_CANNON_EXCLUSIVE)
                .addOptional(Meridian.id("bloodrage"))
                .addOptional(Meridian.id("reckless"))
                .addOptional(Meridian.id("reprieve"));

        getOrCreateTagBuilder(MENDING_EXCLUSIVE)
                .addOptional(Meridian.id("attunement"))
                .addOptional(Meridian.id("vital_mend"))
                .addOptional(mc("mending"));

        // "Better loot" (Fortuity) vs "more loot" (Plunder) — pick one.
        getOrCreateTagBuilder(LOOT_BONUS_EXCLUSIVE)
                .addOptional(Meridian.id("fortuity"))
                .addOptional(Meridian.id("plunder"));

        // Kill trophies: heads (Trophy) vs spawn eggs (Snare) — pick one.
        getOrCreateTagBuilder(TROPHY_EXCLUSIVE)
                .addOptional(Meridian.id("trophy"))
                .addOptional(Meridian.id("snare"));

        // One boots mobility pick: extra jump (Loft) vs jump height (Vault). Updraft is
        // deliberately absent — it's a mace enchant, so it can never share an item with
        // these, and it must keep #minecraft:exclusive_set/mace against Wind Burst.
        getOrCreateTagBuilder(MOBILITY_EXCLUSIVE)
                .addOptional(Meridian.id("loft"))
                .addOptional(Meridian.id("vault"));
    }

    private void appendVanillaTags() {
        getOrCreateTagBuilder(EnchantmentTags.DAMAGE_EXCLUSIVE)
                .addOptional(Meridian.id("voidbane"))
                .addOptional(Meridian.id("sanctify"))
                .addOptional(Meridian.id("sentinel"))
                .addOptional(Meridian.id("rift_strike"))
                .addOptional(Meridian.id("keen_edge"))
                .addOptional(Meridian.id("ambush"))
                .addOptional(Meridian.id("reap"))
                .addOptional(Meridian.id("pinpoint"))
                .addOptional(Meridian.id("longshot"))
                .addOptional(Meridian.id("crescendo"))
                .addOptional(Meridian.id("torrent"));

        getOrCreateTagBuilder(EnchantmentTags.ARMOR_EXCLUSIVE)
                .addOptional(Meridian.id("spellguard"));

        getOrCreateTagBuilder(EnchantmentTags.BOOTS_EXCLUSIVE)
                .addOptional(Meridian.id("cinderwalk"));

        // Vanilla mining exclusive set — Silk Touch and Fortune. Kiln joins it (and declares
        // it in its own definition) so a tool can carry only one drop-altering mining enchant.
        getOrCreateTagBuilder(EnchantmentTags.MINING_EXCLUSIVE)
                .addOptional(Meridian.id("kiln"));

        getOrCreateTagBuilder(MACE_EXCLUSIVE)
                .addOptional(Meridian.id("tempest"))
                .addOptional(Meridian.id("seismic_slam"))
                .addOptional(Meridian.id("updraft"));
    }

    private static ResourceLocation mc(String path) {
        return ResourceLocation.withDefaultNamespace(path);
    }
}
