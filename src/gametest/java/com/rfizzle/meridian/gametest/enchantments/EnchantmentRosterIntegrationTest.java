package com.rfizzle.meridian.gametest.enchantments;

import com.rfizzle.meridian.Meridian;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.ArrayList;
import java.util.List;

public class EnchantmentRosterIntegrationTest implements FabricGameTest {

    private static final List<String> ALL_IDS = List.of(
            "abyss_ward", "adamant", "alacrity", "ambush", "animus",
            "antidote", "attunement", "aurify", "bastion", "beckon", "blight", "blink",
            "bloodrage", "bounty", "bullrush", "bulwark", "cinderwalk", "clamber",
            "cleave", "colossus", "crescendo", "curse_of_decay", "curse_of_sealing",
            "curse_of_echoes", "curse_of_hunger", "curse_of_attraction", "curse_of_leaden",
            "curse_of_blunting", "curse_of_fumbling", "curse_of_wavering", "curse_of_timidity",
            "curse_of_molting", "curse_of_skittishness", "curse_of_obscurity",
            "decay", "decoy", "detonation", "diminish", "dowse", "emberward", "endurance", "everbloom", "excavate",
            "falconstrike", "final_gambit", "fortify", "fortuity", "frostguard", "furrow",
            "gale_shot", "gallop", "glacial_lance", "grapnel", "gravitas", "grind",
            "harpoon", "hush", "impact_ward", "inexorable", "insight", "ironclasp", "ironwing",
            "joust", "keen_edge", "kiln", "loft", "longshot", "luminance",
            "mark", "masons_reach", "meticulous", "nightfall", "outreach", "permafrost", "pin", "pinpoint",
            "plunder", "premonition", "prismatic", "prospect", "pummel",
            "quell", "rally", "reap", "reckless", "reclaim", "renewal",
            "reprieve", "repulse", "resonance", "retribution", "ricochet",
            "rift_strike", "riposte", "saddleguard", "sanctify", "seeker",
            "seismic_slam", "sentinel", "shackle", "siphon", "skybound", "skyfall",
            "slipstream", "snare", "soul_tax", "spellguard", "stagger", "steadfast",
            "stormcall", "stormward", "sunder", "tailwind", "tempest", "tempo", "terrasculpt",
            "tether", "thermal", "timberfell", "torrent", "trailblaze", "trample", "trophy",
            "true_flight", "twin_hook",
            "umbral", "undertow", "updraft",
            "vault", "vital_mend", "vitality", "voidbane", "volley", "wavestride", "winterward");

    @GameTest(template = "meridian:empty_3x3")
    public void allRosterEnchantmentsResolveInRegistry(GameTestHelper helper) {
        Registry<Enchantment> reg = helper.getLevel().registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT);
        List<String> missing = new ArrayList<>();
        for (String id : ALL_IDS) {
            ResourceLocation loc = Meridian.id(id);
            if (!reg.containsKey(loc)) {
                missing.add(id);
            }
        }
        if (!missing.isEmpty()) {
            helper.fail("Enchantments missing from registry (" + missing.size() + "): " + missing);
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void enchantmentCountMatchesExpected(GameTestHelper helper) {
        Registry<Enchantment> reg = helper.getLevel().registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT);
        long meridianCount = reg.holders()
                .filter(h -> h.key().location().getNamespace().equals("meridian"))
                .count();
        if (meridianCount != 140) {
            helper.fail("Expected 140 meridian enchantments in registry, found " + meridianCount);
            return;
        }
        helper.succeed();
    }
}
