package com.rfizzle.meridian.gametest.enchantments;

import com.rfizzle.meridian.Meridian;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.ArrayList;
import java.util.List;

public class ExclusiveSetEnforcementTest implements FabricGameTest {

    private Holder<Enchantment> lookup(GameTestHelper helper, String id) {
        Registry<Enchantment> reg = helper.getLevel().registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT);
        return reg.getHolder(Meridian.id(id)).orElse(null);
    }

    private boolean areCompatible(Holder<Enchantment> a, Holder<Enchantment> b) {
        return Enchantment.areCompatible(a, b);
    }

    @GameTest(template = "meridian:empty_3x3")
    public void aspectSetMembersAreExclusive(GameTestHelper helper) {
        List<String> members = List.of("blight", "decay", "shackle", "nightfall");
        assertAllMutuallyExclusive(helper, members, "aspect");
    }

    @GameTest(template = "meridian:empty_3x3")
    public void damageSetMembersAreExclusive(GameTestHelper helper) {
        List<String> members = List.of("voidbane", "sanctify", "sentinel", "rift_strike", "keen_edge");
        assertAllMutuallyExclusive(helper, members, "damage");
    }

    @GameTest(template = "meridian:empty_3x3")
    public void arrowImpactSetMembersAreExclusive(GameTestHelper helper) {
        List<String> members = List.of("gale_shot", "resonance", "permafrost", "detonation", "stormcall");
        assertAllMutuallyExclusive(helper, members, "arrow_impact");
    }

    @GameTest(template = "meridian:empty_3x3")
    public void maceSetMembersAreExclusive(GameTestHelper helper) {
        List<String> members = List.of("tempest", "seismic_slam", "updraft");
        assertAllMutuallyExclusive(helper, members, "mace");
    }

    @GameTest(template = "meridian:empty_3x3")
    public void sizeSetMembersAreExclusive(GameTestHelper helper) {
        Holder<Enchantment> diminish = lookup(helper, "diminish");
        Holder<Enchantment> colossus = lookup(helper, "colossus");
        if (diminish == null || colossus == null) {
            helper.fail("diminish or colossus not in registry");
            return;
        }
        if (areCompatible(diminish, colossus)) {
            helper.fail("Diminish and Colossus should be mutually exclusive (size set)");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void miningSetMembersAreExclusive(GameTestHelper helper) {
        Holder<Enchantment> excavate = lookup(helper, "excavate");
        Holder<Enchantment> prospect = lookup(helper, "prospect");
        if (excavate == null || prospect == null) {
            helper.fail("excavate or prospect not in registry");
            return;
        }
        if (areCompatible(excavate, prospect)) {
            helper.fail("Excavate and Prospect should be mutually exclusive (mining set)");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void glassCannonSetMembersAreExclusive(GameTestHelper helper) {
        Holder<Enchantment> bloodrage = lookup(helper, "bloodrage");
        Holder<Enchantment> reckless = lookup(helper, "reckless");
        if (bloodrage == null || reckless == null) {
            helper.fail("bloodrage or reckless not in registry");
            return;
        }
        if (areCompatible(bloodrage, reckless)) {
            helper.fail("Bloodrage and Reckless should be mutually exclusive (glass_cannon set)");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void mendingSetMembersAreExclusive(GameTestHelper helper) {
        Holder<Enchantment> vitalMend = lookup(helper, "vital_mend");
        if (vitalMend == null) { helper.fail("vital_mend not in registry"); return; }

        Registry<Enchantment> reg = helper.getLevel().registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> mending = reg.getHolderOrThrow(
                net.minecraft.world.item.enchantment.Enchantments.MENDING);

        if (areCompatible(vitalMend, mending)) {
            helper.fail("Vital Mend and Mending should be mutually exclusive (mending set)");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void unrelatedEnchantmentsAreCompatible(GameTestHelper helper) {
        Holder<Enchantment> tempo = lookup(helper, "tempo");
        Holder<Enchantment> siphon = lookup(helper, "siphon");
        Holder<Enchantment> outreach = lookup(helper, "outreach");
        if (tempo == null || siphon == null || outreach == null) {
            helper.fail("tempo, siphon, or outreach not in registry");
            return;
        }
        if (!areCompatible(tempo, siphon)) {
            helper.fail("Tempo and Siphon should be compatible (different categories)");
            return;
        }
        if (!areCompatible(tempo, outreach)) {
            helper.fail("Tempo and Outreach should be compatible (both sword utility)");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void blightCannotCombineWithDecayOnAnvil(GameTestHelper helper) {
        Holder<Enchantment> blight = lookup(helper, "blight");
        Holder<Enchantment> decay = lookup(helper, "decay");
        if (blight == null || decay == null) {
            helper.fail("blight or decay not in registry");
            return;
        }

        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.enchant(blight, 2);

        if (decay.value().canEnchant(sword)) {
            if (!areCompatible(blight, decay)) {
                helper.succeed();
                return;
            }
            helper.fail("Blight and Decay should not be combinable (aspect set)");
            return;
        }
        helper.succeed();
    }

    private void assertAllMutuallyExclusive(GameTestHelper helper, List<String> ids, String setName) {
        List<Holder<Enchantment>> holders = new ArrayList<>();
        for (String id : ids) {
            Holder<Enchantment> h = lookup(helper, id);
            if (h == null) {
                helper.fail(id + " not in registry");
                return;
            }
            holders.add(h);
        }

        for (int i = 0; i < holders.size(); i++) {
            for (int j = i + 1; j < holders.size(); j++) {
                if (areCompatible(holders.get(i), holders.get(j))) {
                    helper.fail(ids.get(i) + " and " + ids.get(j)
                            + " should be mutually exclusive (" + setName + " set)");
                    return;
                }
            }
        }
        helper.succeed();
    }
}
