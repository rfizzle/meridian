package com.rfizzle.meridian.gametest.enchantments;

import com.rfizzle.meridian.Meridian;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DefinitionVerificationTest implements FabricGameTest {

    private Holder<Enchantment> lookup(GameTestHelper helper, String id) {
        Registry<Enchantment> reg = helper.getLevel().registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT);
        return reg.getHolder(Meridian.id(id)).orElse(null);
    }

    private void assertDefinition(GameTestHelper helper, String id, int expectedMaxLevel, int expectedWeight) {
        Holder<Enchantment> ench = lookup(helper, id);
        if (ench == null) {
            helper.fail(id + " not in registry");
            return;
        }
        Enchantment e = ench.value();
        if (e.definition().maxLevel() != expectedMaxLevel) {
            helper.fail(id + " maxLevel: expected " + expectedMaxLevel + ", got " + e.definition().maxLevel());
            return;
        }
        if (e.definition().weight() != expectedWeight) {
            helper.fail(id + " weight: expected " + expectedWeight + ", got " + e.definition().weight());
        }
    }

    @GameTest(template = "meridian:empty_3x3")
    public void animusDefinitionIsCorrect(GameTestHelper helper) {
        assertDefinition(helper, "animus", 3, 5);
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void antidoteDefinitionIsCorrect(GameTestHelper helper) {
        assertDefinition(helper, "antidote", 1, 5);
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void aurifyDefinitionIsCorrect(GameTestHelper helper) {
        assertDefinition(helper, "aurify", 1, 1);
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void beckonDefinitionIsCorrect(GameTestHelper helper) {
        assertDefinition(helper, "beckon", 1, 10);
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void bountyDefinitionIsCorrect(GameTestHelper helper) {
        assertDefinition(helper, "bounty", 3, 5);
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void cleaveDefinitionIsCorrect(GameTestHelper helper) {
        assertDefinition(helper, "cleave", 3, 2);
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void curseOfDecayDefinitionIsCorrect(GameTestHelper helper) {
        assertDefinition(helper, "curse_of_decay", 5, 2);
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void curseOfSealingDefinitionIsCorrect(GameTestHelper helper) {
        assertDefinition(helper, "curse_of_sealing", 1, 1);
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void fortifyDefinitionIsCorrect(GameTestHelper helper) {
        assertDefinition(helper, "fortify", 3, 5);
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void frostguardDefinitionIsCorrect(GameTestHelper helper) {
        assertDefinition(helper, "frostguard", 3, 2);
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void furrowDefinitionIsCorrect(GameTestHelper helper) {
        assertDefinition(helper, "furrow", 3, 5);
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void gallopDefinitionIsCorrect(GameTestHelper helper) {
        assertDefinition(helper, "gallop", 4, 5);
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void glacialLanceDefinitionIsCorrect(GameTestHelper helper) {
        assertDefinition(helper, "glacial_lance", 3, 2);
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void gravitasDefinitionIsCorrect(GameTestHelper helper) {
        assertDefinition(helper, "gravitas", 3, 5);
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void impactWardDefinitionIsCorrect(GameTestHelper helper) {
        assertDefinition(helper, "impact_ward", 5, 5);
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void insightDefinitionIsCorrect(GameTestHelper helper) {
        assertDefinition(helper, "insight", 3, 10);
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void ironwingDefinitionIsCorrect(GameTestHelper helper) {
        assertDefinition(helper, "ironwing", 4, 5);
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void luminanceDefinitionIsCorrect(GameTestHelper helper) {
        assertDefinition(helper, "luminance", 1, 5);
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void masonsReachDefinitionIsCorrect(GameTestHelper helper) {
        assertDefinition(helper, "masons_reach", 3, 5);
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void plunderDefinitionIsCorrect(GameTestHelper helper) {
        assertDefinition(helper, "plunder", 3, 1);
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void prismaticDefinitionIsCorrect(GameTestHelper helper) {
        assertDefinition(helper, "prismatic", 1, 5);
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void pummelDefinitionIsCorrect(GameTestHelper helper) {
        assertDefinition(helper, "pummel", 4, 2);
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void quellDefinitionIsCorrect(GameTestHelper helper) {
        assertDefinition(helper, "quell", 1, 5);
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void renewalDefinitionIsCorrect(GameTestHelper helper) {
        assertDefinition(helper, "renewal", 1, 2);
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void repulseDefinitionIsCorrect(GameTestHelper helper) {
        assertDefinition(helper, "repulse", 3, 2);
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void retributionDefinitionIsCorrect(GameTestHelper helper) {
        assertDefinition(helper, "retribution", 5, 1);
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void saddleguardDefinitionIsCorrect(GameTestHelper helper) {
        assertDefinition(helper, "saddleguard", 5, 5);
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void seismicSlamDefinitionIsCorrect(GameTestHelper helper) {
        assertDefinition(helper, "seismic_slam", 1, 2);
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void skyboundDefinitionIsCorrect(GameTestHelper helper) {
        assertDefinition(helper, "skybound", 7, 2);
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void slipstreamDefinitionIsCorrect(GameTestHelper helper) {
        assertDefinition(helper, "slipstream", 1, 2);
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void soulTaxDefinitionIsCorrect(GameTestHelper helper) {
        assertDefinition(helper, "soul_tax", 3, 2);
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void spellguardDefinitionIsCorrect(GameTestHelper helper) {
        assertDefinition(helper, "spellguard", 4, 5);
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void steadfastDefinitionIsCorrect(GameTestHelper helper) {
        assertDefinition(helper, "steadfast", 1, 2);
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void terrasculptDefinitionIsCorrect(GameTestHelper helper) {
        assertDefinition(helper, "terrasculpt", 1, 2);
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void tetherDefinitionIsCorrect(GameTestHelper helper) {
        assertDefinition(helper, "tether", 1, 1);
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void trampleDefinitionIsCorrect(GameTestHelper helper) {
        assertDefinition(helper, "trample", 3, 5);
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void trueFlightDefinitionIsCorrect(GameTestHelper helper) {
        assertDefinition(helper, "true_flight", 1, 2);
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void vaultDefinitionIsCorrect(GameTestHelper helper) {
        assertDefinition(helper, "vault", 3, 5);
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void allUncoveredEnchantmentsHaveCorrectDefinitions(GameTestHelper helper) {
        Map<String, int[]> expected = Map.ofEntries(
                Map.entry("animus", new int[]{3, 5}),
                Map.entry("antidote", new int[]{1, 5}),
                Map.entry("aurify", new int[]{1, 1}),
                Map.entry("beckon", new int[]{1, 10}),
                Map.entry("bounty", new int[]{3, 5}),
                Map.entry("cleave", new int[]{3, 2}),
                Map.entry("curse_of_decay", new int[]{5, 2}),
                Map.entry("curse_of_sealing", new int[]{1, 1}),
                Map.entry("fortify", new int[]{3, 5}),
                Map.entry("frostguard", new int[]{3, 2}),
                Map.entry("furrow", new int[]{3, 5}),
                Map.entry("gallop", new int[]{4, 5}),
                Map.entry("glacial_lance", new int[]{3, 2}),
                Map.entry("gravitas", new int[]{3, 5}),
                Map.entry("impact_ward", new int[]{5, 5}),
                Map.entry("insight", new int[]{3, 10}),
                Map.entry("ironwing", new int[]{4, 5}),
                Map.entry("luminance", new int[]{1, 5}),
                Map.entry("masons_reach", new int[]{3, 5}),
                Map.entry("plunder", new int[]{3, 1}),
                Map.entry("prismatic", new int[]{1, 5}),
                Map.entry("pummel", new int[]{4, 2}),
                Map.entry("quell", new int[]{1, 5}),
                Map.entry("renewal", new int[]{1, 2}),
                Map.entry("repulse", new int[]{3, 2}),
                Map.entry("retribution", new int[]{5, 1}),
                Map.entry("saddleguard", new int[]{5, 5})
        );

        Map<String, int[]> expected2 = Map.ofEntries(
                Map.entry("seismic_slam", new int[]{1, 2}),
                Map.entry("skybound", new int[]{7, 2}),
                Map.entry("slipstream", new int[]{1, 2}),
                Map.entry("soul_tax", new int[]{3, 2}),
                Map.entry("spellguard", new int[]{4, 5}),
                Map.entry("steadfast", new int[]{1, 2}),
                Map.entry("terrasculpt", new int[]{1, 2}),
                Map.entry("tether", new int[]{1, 1}),
                Map.entry("trample", new int[]{3, 5}),
                Map.entry("true_flight", new int[]{1, 2}),
                Map.entry("vault", new int[]{3, 5})
        );

        List<String> failures = new ArrayList<>();
        checkAll(helper, expected, failures);
        checkAll(helper, expected2, failures);

        if (!failures.isEmpty()) {
            helper.fail("Definition mismatches (" + failures.size() + "): " + String.join("; ", failures));
            return;
        }
        helper.succeed();
    }

    private void checkAll(GameTestHelper helper, Map<String, int[]> expected, List<String> failures) {
        for (var entry : expected.entrySet()) {
            String id = entry.getKey();
            int[] vals = entry.getValue();
            Holder<Enchantment> ench = lookup(helper, id);
            if (ench == null) {
                failures.add(id + " missing from registry");
                continue;
            }
            Enchantment e = ench.value();
            if (e.definition().maxLevel() != vals[0]) {
                failures.add(id + " maxLevel=" + e.definition().maxLevel() + " expected=" + vals[0]);
            }
            if (e.definition().weight() != vals[1]) {
                failures.add(id + " weight=" + e.definition().weight() + " expected=" + vals[1]);
            }
        }
    }
}
