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
            "bloodrage", "bounty", "bulwark", "cinderwalk", "clamber",
            "cleave", "colossus", "crescendo", "curse_of_decay", "curse_of_sealing",
            "curse_of_echoes", "curse_of_hunger", "curse_of_attraction", "curse_of_leaden",
            "decay", "decoy", "detonation", "diminish", "emberward", "everbloom", "excavate",
            "final_gambit", "fortify", "fortuity", "frostguard", "furrow",
            "gale_shot", "gallop", "glacial_lance", "grapnel", "gravitas", "grind",
            "harpoon", "hush", "impact_ward", "inexorable", "insight", "ironwing",
            "joust", "keen_edge", "kiln", "loft", "longshot", "luminance",
            "mark", "masons_reach", "meticulous", "nightfall", "outreach", "permafrost", "pinpoint",
            "plunder", "premonition", "prismatic", "prospect", "pummel",
            "quell", "rally", "reap", "reckless", "reclaim", "renewal",
            "reprieve", "repulse", "resonance", "retribution", "ricochet",
            "rift_strike", "riposte", "saddleguard", "sanctify", "seeker",
            "seismic_slam", "sentinel", "shackle", "siphon", "skybound",
            "slipstream", "snare", "soul_tax", "spellguard", "steadfast",
            "stormcall", "sunder", "tempest", "tempo", "terrasculpt",
            "tether", "thermal", "timberfell", "trample", "trophy", "true_flight",
            "umbral", "updraft",
            "vault", "vital_mend", "vitality", "voidbane", "volley", "winterward");

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
        if (meridianCount != 113) {
            helper.fail("Expected 113 meridian enchantments in registry, found " + meridianCount);
            return;
        }
        helper.succeed();
    }
}
