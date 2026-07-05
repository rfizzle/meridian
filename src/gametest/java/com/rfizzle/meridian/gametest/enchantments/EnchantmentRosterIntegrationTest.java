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
            "antidote", "aurify", "beckon", "blight", "blink",
            "bloodrage", "bounty", "bulwark", "cinderwalk", "clamber",
            "cleave", "colossus", "curse_of_decay", "curse_of_sealing", "decay",
            "detonation", "diminish", "emberward", "excavate", "final_gambit",
            "fortify", "fortuity", "frostguard", "furrow", "gale_shot",
            "gallop", "glacial_lance", "gravitas", "grind", "harpoon",
            "impact_ward", "inexorable", "insight", "ironwing", "keen_edge",
            "loft", "longshot", "luminance", "masons_reach", "nightfall",
            "outreach", "permafrost", "pinpoint", "plunder", "premonition",
            "prismatic", "prospect", "pummel", "quell", "rally",
            "reckless", "reclaim", "renewal", "reprieve", "repulse",
            "resonance", "retribution", "ricochet", "rift_strike", "saddleguard",
            "sanctify", "seeker", "seismic_slam", "sentinel", "shackle",
            "siphon", "skybound", "slipstream", "snare", "soul_tax",
            "spellguard", "steadfast", "stormcall", "sunder", "tempest",
            "tempo", "terrasculpt", "tether", "trample", "trophy",
            "true_flight", "updraft", "vault", "vital_mend", "vitality",
            "voidbane");

    @GameTest(template = "meridian:empty_3x3")
    public void all91EnchantmentsResolveInRegistry(GameTestHelper helper) {
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
        if (meridianCount != 91) {
            helper.fail("Expected 91 meridian enchantments in registry, found " + meridianCount);
            return;
        }
        helper.succeed();
    }
}
