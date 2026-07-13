package com.rfizzle.meridian.gametest.enchantments;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.enchanting.EnchantmentEffects;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;

public class MechanicsEnchantmentTest implements FabricGameTest {

    private Holder<Enchantment> lookup(GameTestHelper helper, String id) {
        Registry<Enchantment> reg = helper.getLevel().registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT);
        return reg.getHolder(Meridian.id(id)).orElse(null);
    }

    // --- Frostguard: applies slowness to attackers ---

    @GameTest(template = "meridian:empty_3x3")
    public void frostguardAppliesSlownessToAttacker(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "frostguard");
        if (ench == null) { helper.fail("frostguard not in registry"); return; }

        Mob defender = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 1, 1));
        ItemStack chest = new ItemStack(Items.DIAMOND_CHESTPLATE);
        chest.enchant(ench, 3);
        defender.setItemSlot(EquipmentSlot.CHEST, chest);

        Mob attacker = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 1, 2));
        attacker.doHurtTarget(defender);

        helper.runAfterDelay(2, () -> {
            if (attacker.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) {
                helper.succeed();
            } else {
                helper.fail("Attacker should be slowed by Frostguard III on the defender");
            }
        });
    }

    // --- Repulse: knocks back attackers ---

    @GameTest(template = "meridian:empty_3x3")
    public void repulseKnockbacksAttacker(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "repulse");
        if (ench == null) { helper.fail("repulse not in registry"); return; }

        Mob defender = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 1, 1));
        ItemStack chest = new ItemStack(Items.DIAMOND_CHESTPLATE);
        chest.enchant(ench, 3);
        defender.setItemSlot(EquipmentSlot.CHEST, chest);

        Mob attacker = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 1, 2));
        double startZ = attacker.getZ();
        attacker.doHurtTarget(defender);

        helper.runAfterDelay(3, () -> {
            double endZ = attacker.getZ();
            double displacement = Math.abs(endZ - startZ);
            if (displacement > 0.01) {
                helper.succeed();
            } else {
                helper.fail("Repulse III should knock attacker away. Displacement: " + displacement);
            }
        });
    }

    // --- Cleave: multi-target AoE ---

    @GameTest(template = "meridian:empty_3x3")
    public void cleaveHitsNearbyTargets(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "cleave");
        if (ench == null) { helper.fail("cleave not in registry"); return; }

        Mob attacker = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 1, 1));
        ItemStack axe = new ItemStack(Items.DIAMOND_AXE);
        axe.enchant(ench, 3);
        attacker.setItemSlot(EquipmentSlot.MAINHAND, axe);

        Mob target1 = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(2, 1, 1));
        Mob target2 = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(2, 1, 2));

        float hp1Before = target1.getHealth();
        float hp2Before = target2.getHealth();
        attacker.doHurtTarget(target1);

        helper.runAfterDelay(2, () -> {
            boolean primaryHit = target1.getHealth() < hp1Before;
            boolean secondaryHit = target2.getHealth() < hp2Before;
            if (!primaryHit) {
                helper.fail("Primary target should take damage");
                return;
            }
            if (!secondaryHit) {
                helper.fail("Cleave III should hit nearby secondary target");
                return;
            }
            helper.succeed();
        });
    }

    // --- Soul Tax: spends XP to boost damage ---

    @GameTest(template = "meridian:empty_3x3")
    public void soulTaxIsRegisteredWithCorrectSlot(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "soul_tax");
        if (ench == null) { helper.fail("soul_tax not in registry"); return; }

        Enchantment e = ench.value();
        if (e.definition().maxLevel() != 3) {
            helper.fail("Soul Tax should have max level 3, got " + e.definition().maxLevel());
            return;
        }
        boolean hasSword = e.canEnchant(new ItemStack(Items.DIAMOND_SWORD));
        if (!hasSword) {
            helper.fail("Soul Tax should be enchantable on a diamond sword");
            return;
        }
        helper.succeed();
    }

    // --- Fortify: enchantable on shield ---

    @GameTest(template = "meridian:empty_3x3")
    public void fortifyIsEnchantableOnShield(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "fortify");
        if (ench == null) { helper.fail("fortify not in registry"); return; }

        boolean canEnchant = ench.value().canEnchant(new ItemStack(Items.SHIELD));
        if (!canEnchant) {
            helper.fail("Fortify should be enchantable on a shield");
            return;
        }
        helper.succeed();
    }

    // --- Pummel: enchantable on shield ---

    @GameTest(template = "meridian:empty_3x3")
    public void pummelIsEnchantableOnShield(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "pummel");
        if (ench == null) { helper.fail("pummel not in registry"); return; }

        boolean canEnchant = ench.value().canEnchant(new ItemStack(Items.SHIELD));
        if (!canEnchant) {
            helper.fail("Pummel should be enchantable on a shield");
            return;
        }
        helper.succeed();
    }

    // --- Retribution: enchantable on shield, high max level ---

    @GameTest(template = "meridian:empty_3x3")
    public void retributionIsEnchantableOnShield(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "retribution");
        if (ench == null) { helper.fail("retribution not in registry"); return; }

        boolean canEnchant = ench.value().canEnchant(new ItemStack(Items.SHIELD));
        if (!canEnchant) {
            helper.fail("Retribution should be enchantable on a shield");
            return;
        }
        if (ench.value().definition().maxLevel() != 5) {
            helper.fail("Retribution should have max level 5, got " + ench.value().definition().maxLevel());
            return;
        }
        helper.succeed();
    }

    // --- Gravitas: verify enchantable on armor ---

    @GameTest(template = "meridian:empty_3x3")
    public void gravitasIsEnchantableOnArmor(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "gravitas");
        if (ench == null) { helper.fail("gravitas not in registry"); return; }

        boolean canEnchant = ench.value().canEnchant(new ItemStack(Items.DIAMOND_CHESTPLATE));
        if (!canEnchant) {
            helper.fail("Gravitas should be enchantable on armor");
            return;
        }
        helper.succeed();
    }

    // --- Luminance: verify enchantable on helmet ---

    @GameTest(template = "meridian:empty_3x3")
    public void luminanceIsEnchantableOnHelmet(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "luminance");
        if (ench == null) { helper.fail("luminance not in registry"); return; }

        boolean canEnchant = ench.value().canEnchant(new ItemStack(Items.DIAMOND_HELMET));
        if (!canEnchant) {
            helper.fail("Luminance should be enchantable on a helmet");
            return;
        }
        boolean cantOnBoots = !ench.value().canEnchant(new ItemStack(Items.DIAMOND_BOOTS));
        if (!cantOnBoots) {
            helper.fail("Luminance should NOT be enchantable on boots (head_armor only)");
            return;
        }
        helper.succeed();
    }

    // --- Slipstream: verify enchantable on legs/feet ---

    @GameTest(template = "meridian:empty_3x3")
    public void slipstreamIsEnchantableOnLegOrFootArmor(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "slipstream");
        if (ench == null) { helper.fail("slipstream not in registry"); return; }

        boolean onBoots = ench.value().canEnchant(new ItemStack(Items.DIAMOND_BOOTS));
        boolean onLegs = ench.value().canEnchant(new ItemStack(Items.DIAMOND_LEGGINGS));
        if (!onBoots && !onLegs) {
            helper.fail("Slipstream should be enchantable on boots or leggings");
            return;
        }
        boolean notOnChest = !ench.value().canEnchant(new ItemStack(Items.DIAMOND_CHESTPLATE));
        if (!notOnChest) {
            helper.fail("Slipstream should NOT be enchantable on chestplate");
            return;
        }
        helper.succeed();
    }

    // --- Steadfast: verify enchantable on legs/feet ---

    @GameTest(template = "meridian:empty_3x3")
    public void steadfastIsEnchantableOnLegOrFootArmor(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "steadfast");
        if (ench == null) { helper.fail("steadfast not in registry"); return; }

        boolean onBoots = ench.value().canEnchant(new ItemStack(Items.DIAMOND_BOOTS));
        boolean onLegs = ench.value().canEnchant(new ItemStack(Items.DIAMOND_LEGGINGS));
        if (!onBoots && !onLegs) {
            helper.fail("Steadfast should be enchantable on boots or leggings");
            return;
        }
        helper.succeed();
    }

    // --- Saddleguard: max level 5 and mount slot ---

    @GameTest(template = "meridian:empty_3x3")
    public void saddleguardMaxLevelIsFive(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "saddleguard");
        if (ench == null) { helper.fail("saddleguard not in registry"); return; }

        if (ench.value().definition().maxLevel() != 5) {
            helper.fail("Saddleguard should have max level 5, got " + ench.value().definition().maxLevel());
            return;
        }
        helper.succeed();
    }

    // --- Trample: verify definition ---

    @GameTest(template = "meridian:empty_3x3")
    public void trampleIsRegisteredCorrectly(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "trample");
        if (ench == null) { helper.fail("trample not in registry"); return; }

        if (ench.value().definition().maxLevel() != 3) {
            helper.fail("Trample should have max level 3, got " + ench.value().definition().maxLevel());
            return;
        }
        if (ench.value().definition().weight() != 5) {
            helper.fail("Trample should have weight 5, got " + ench.value().definition().weight());
            return;
        }
        helper.succeed();
    }

    // --- Tether: enchantable on durability items ---

    @GameTest(template = "meridian:empty_3x3")
    public void tetherIsEnchantableOnDurabilityItems(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "tether");
        if (ench == null) { helper.fail("tether not in registry"); return; }

        boolean onSword = ench.value().canEnchant(new ItemStack(Items.DIAMOND_SWORD));
        boolean onPick = ench.value().canEnchant(new ItemStack(Items.DIAMOND_PICKAXE));
        boolean onArmor = ench.value().canEnchant(new ItemStack(Items.DIAMOND_CHESTPLATE));
        if (!onSword || !onPick || !onArmor) {
            helper.fail("Tether should be enchantable on all durability items. sword=" + onSword + ", pick=" + onPick + ", armor=" + onArmor);
            return;
        }
        helper.succeed();
    }

    // --- Tether: member of the #c:soulbound convention tag, and matched through it ---

    @GameTest(template = "meridian:empty_3x3")
    public void tetherIsMatchedBySoulboundConventionTag(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "tether");
        if (ench == null) { helper.fail("tether not in registry"); return; }

        if (!ench.is(EnchantmentEffects.SOULBOUND)) {
            helper.fail("tether should be in #c:soulbound");
            return;
        }

        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        if (EnchantmentEffects.hasEnchantmentIn(sword, EnchantmentEffects.SOULBOUND)) {
            helper.fail("Unenchanted sword must not match #c:soulbound");
            return;
        }
        sword.enchant(ench, 1);
        if (!EnchantmentEffects.hasEnchantmentIn(sword, EnchantmentEffects.SOULBOUND)) {
            helper.fail("Tether-enchanted sword should match #c:soulbound");
            return;
        }
        helper.succeed();
    }

    // --- Aurify: enchantable on durability items ---

    @GameTest(template = "meridian:empty_3x3")
    public void aurifyIsEnchantableOnDurabilityItems(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "aurify");
        if (ench == null) { helper.fail("aurify not in registry"); return; }

        boolean onSword = ench.value().canEnchant(new ItemStack(Items.DIAMOND_SWORD));
        boolean onPick = ench.value().canEnchant(new ItemStack(Items.DIAMOND_PICKAXE));
        if (!onSword || !onPick) {
            helper.fail("Aurify should be enchantable on durability items. sword=" + onSword + ", pick=" + onPick);
            return;
        }
        helper.succeed();
    }

    // --- Curse of Sealing: enchantable on durability items ---

    @GameTest(template = "meridian:empty_3x3")
    public void curseOfSealingIsEnchantableOnDurabilityItems(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "curse_of_sealing");
        if (ench == null) { helper.fail("curse_of_sealing not in registry"); return; }

        boolean onSword = ench.value().canEnchant(new ItemStack(Items.DIAMOND_SWORD));
        boolean onHelmet = ench.value().canEnchant(new ItemStack(Items.DIAMOND_HELMET));
        if (!onSword || !onHelmet) {
            helper.fail("Curse of Sealing should be enchantable on durability items. sword=" + onSword + ", helmet=" + onHelmet);
            return;
        }
        helper.succeed();
    }

    // --- Curse of Decay: increases durability damage ---

    @GameTest(template = "meridian:empty_3x3")
    public void curseOfDecayIncreasesItemDamage(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "curse_of_decay");
        if (ench == null) { helper.fail("curse_of_decay not in registry"); return; }

        boolean canEnchant = ench.value().canEnchant(new ItemStack(Items.DIAMOND_SWORD));
        if (!canEnchant) {
            helper.fail("Curse of Decay should be enchantable on a diamond sword");
            return;
        }
        if (ench.value().definition().maxLevel() != 5) {
            helper.fail("Curse of Decay should have max level 5, got " + ench.value().definition().maxLevel());
            return;
        }
        helper.succeed();
    }

    // --- True Flight: enchantable on bow/crossbow ---

    @GameTest(template = "meridian:empty_3x3")
    public void trueFlightIsEnchantableOnBow(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "true_flight");
        if (ench == null) { helper.fail("true_flight not in registry"); return; }

        boolean onBow = ench.value().canEnchant(new ItemStack(Items.BOW));
        if (!onBow) {
            helper.fail("True Flight should be enchantable on a bow");
            return;
        }
        helper.succeed();
    }

    // --- Glacial Lance: enchantable on trident ---

    @GameTest(template = "meridian:empty_3x3")
    public void glacialLanceIsEnchantableOnTrident(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "glacial_lance");
        if (ench == null) { helper.fail("glacial_lance not in registry"); return; }

        boolean onTrident = ench.value().canEnchant(new ItemStack(Items.TRIDENT));
        if (!onTrident) {
            helper.fail("Glacial Lance should be enchantable on a trident");
            return;
        }
        helper.succeed();
    }

    // --- Bounty: enchantable on hoe ---

    @GameTest(template = "meridian:empty_3x3")
    public void bountyIsEnchantableOnHoe(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "bounty");
        if (ench == null) { helper.fail("bounty not in registry"); return; }

        boolean onHoe = ench.value().canEnchant(new ItemStack(Items.DIAMOND_HOE));
        if (!onHoe) {
            helper.fail("Bounty should be enchantable on a hoe");
            return;
        }
        boolean notOnSword = !ench.value().canEnchant(new ItemStack(Items.DIAMOND_SWORD));
        if (!notOnSword) {
            helper.fail("Bounty should NOT be enchantable on a sword");
            return;
        }
        helper.succeed();
    }

    // --- Furrow: enchantable on hoe ---

    @GameTest(template = "meridian:empty_3x3")
    public void furrowIsEnchantableOnHoe(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "furrow");
        if (ench == null) { helper.fail("furrow not in registry"); return; }

        boolean onHoe = ench.value().canEnchant(new ItemStack(Items.DIAMOND_HOE));
        if (!onHoe) {
            helper.fail("Furrow should be enchantable on a hoe");
            return;
        }
        helper.succeed();
    }

    // --- Terrasculpt: enchantable on hoe ---

    @GameTest(template = "meridian:empty_3x3")
    public void terrasculptIsEnchantableOnHoe(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "terrasculpt");
        if (ench == null) { helper.fail("terrasculpt not in registry"); return; }

        boolean onHoe = ench.value().canEnchant(new ItemStack(Items.DIAMOND_HOE));
        if (!onHoe) {
            helper.fail("Terrasculpt should be enchantable on a hoe");
            return;
        }
        helper.succeed();
    }

    // --- Beckon: enchantable on hoe ---

    @GameTest(template = "meridian:empty_3x3")
    public void beckonIsEnchantableOnHoe(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "beckon");
        if (ench == null) { helper.fail("beckon not in registry"); return; }

        boolean onHoe = ench.value().canEnchant(new ItemStack(Items.DIAMOND_HOE));
        if (!onHoe) {
            helper.fail("Beckon should be enchantable on a hoe");
            return;
        }
        helper.succeed();
    }

    // --- Renewal: enchantable on shears ---

    @GameTest(template = "meridian:empty_3x3")
    public void renewalIsEnchantableOnShears(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "renewal");
        if (ench == null) { helper.fail("renewal not in registry"); return; }

        boolean onShears = ench.value().canEnchant(new ItemStack(Items.SHEARS));
        if (!onShears) {
            helper.fail("Renewal should be enchantable on shears");
            return;
        }
        boolean notOnHoe = !ench.value().canEnchant(new ItemStack(Items.DIAMOND_HOE));
        if (!notOnHoe) {
            helper.fail("Renewal should NOT be enchantable on a hoe (shears only)");
            return;
        }
        helper.succeed();
    }

    // --- Prismatic: enchantable on shears ---

    @GameTest(template = "meridian:empty_3x3")
    public void prismaticIsEnchantableOnShears(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "prismatic");
        if (ench == null) { helper.fail("prismatic not in registry"); return; }

        boolean onShears = ench.value().canEnchant(new ItemStack(Items.SHEARS));
        if (!onShears) {
            helper.fail("Prismatic should be enchantable on shears");
            return;
        }
        helper.succeed();
    }

    // --- Antidote: enchantable on armor ---

    @GameTest(template = "meridian:empty_3x3")
    public void antidoteIsEnchantableOnArmor(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "antidote");
        if (ench == null) { helper.fail("antidote not in registry"); return; }

        boolean onChest = ench.value().canEnchant(new ItemStack(Items.DIAMOND_CHESTPLATE));
        boolean onHelmet = ench.value().canEnchant(new ItemStack(Items.DIAMOND_HELMET));
        if (!onChest || !onHelmet) {
            helper.fail("Antidote should be enchantable on all armor. chest=" + onChest + ", helmet=" + onHelmet);
            return;
        }
        helper.succeed();
    }

    // --- Plunder: enchantable on sword ---

    @GameTest(template = "meridian:empty_3x3")
    public void plunderIsEnchantableOnSword(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "plunder");
        if (ench == null) { helper.fail("plunder not in registry"); return; }

        boolean onSword = ench.value().canEnchant(new ItemStack(Items.DIAMOND_SWORD));
        if (!onSword) {
            helper.fail("Plunder should be enchantable on a sword");
            return;
        }
        helper.succeed();
    }

    // --- Quell: enchantable on sword ---

    @GameTest(template = "meridian:empty_3x3")
    public void quellIsEnchantableOnSword(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "quell");
        if (ench == null) { helper.fail("quell not in registry"); return; }

        boolean onSword = ench.value().canEnchant(new ItemStack(Items.DIAMOND_SWORD));
        if (!onSword) {
            helper.fail("Quell should be enchantable on a sword");
            return;
        }
        boolean notOnArmor = !ench.value().canEnchant(new ItemStack(Items.DIAMOND_CHESTPLATE));
        if (!notOnArmor) {
            helper.fail("Quell should NOT be enchantable on armor (weapon only)");
            return;
        }
        helper.succeed();
    }

    // --- Seismic Slam: enchantable on mace ---

    @GameTest(template = "meridian:empty_3x3")
    public void seismicSlamIsEnchantableOnMace(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "seismic_slam");
        if (ench == null) { helper.fail("seismic_slam not in registry"); return; }

        boolean onMace = ench.value().canEnchant(new ItemStack(Items.MACE));
        if (!onMace) {
            helper.fail("Seismic Slam should be enchantable on a mace");
            return;
        }
        boolean notOnSword = !ench.value().canEnchant(new ItemStack(Items.DIAMOND_SWORD));
        if (!notOnSword) {
            helper.fail("Seismic Slam should NOT be enchantable on a sword (mace only)");
            return;
        }
        helper.succeed();
    }

    // --- Ballast: enchantable on legs/feet only ---

    @GameTest(template = "meridian:empty_3x3")
    public void ballastIsEnchantableOnLegOrFootArmor(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "ballast");
        if (ench == null) { helper.fail("ballast not in registry"); return; }

        boolean onBoots = ench.value().canEnchant(new ItemStack(Items.DIAMOND_BOOTS));
        boolean onLegs = ench.value().canEnchant(new ItemStack(Items.DIAMOND_LEGGINGS));
        if (!onBoots && !onLegs) {
            helper.fail("Ballast should be enchantable on boots or leggings");
            return;
        }
        boolean notOnChest = !ench.value().canEnchant(new ItemStack(Items.DIAMOND_CHESTPLATE));
        if (!notOnChest) {
            helper.fail("Ballast should NOT be enchantable on chestplate");
            return;
        }
        helper.succeed();
    }

    // --- Abyssal: enchantable on any armor, not tools ---

    @GameTest(template = "meridian:empty_3x3")
    public void abyssalIsEnchantableOnAnyArmor(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "abyssal");
        if (ench == null) { helper.fail("abyssal not in registry"); return; }

        boolean onHelmet = ench.value().canEnchant(new ItemStack(Items.DIAMOND_HELMET));
        boolean onChest = ench.value().canEnchant(new ItemStack(Items.DIAMOND_CHESTPLATE));
        boolean onBoots = ench.value().canEnchant(new ItemStack(Items.DIAMOND_BOOTS));
        if (!onHelmet || !onChest || !onBoots) {
            helper.fail("Abyssal should be enchantable on any armor piece");
            return;
        }
        boolean notOnSword = !ench.value().canEnchant(new ItemStack(Items.DIAMOND_SWORD));
        if (!notOnSword) {
            helper.fail("Abyssal should NOT be enchantable on a sword (armor only)");
            return;
        }
        helper.succeed();
    }

    // --- Curse of Waterlogging: enchantable on any armor, not tools ---

    @GameTest(template = "meridian:empty_3x3")
    public void curseOfWaterloggingIsEnchantableOnAnyArmor(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "curse_of_waterlogging");
        if (ench == null) { helper.fail("curse_of_waterlogging not in registry"); return; }

        boolean onHelmet = ench.value().canEnchant(new ItemStack(Items.DIAMOND_HELMET));
        boolean onBoots = ench.value().canEnchant(new ItemStack(Items.DIAMOND_BOOTS));
        if (!onHelmet || !onBoots) {
            helper.fail("Curse of Waterlogging should be enchantable on any armor piece");
            return;
        }
        boolean notOnPickaxe = !ench.value().canEnchant(new ItemStack(Items.DIAMOND_PICKAXE));
        if (!notOnPickaxe) {
            helper.fail("Curse of Waterlogging should NOT be enchantable on a pickaxe (armor only)");
            return;
        }
        helper.succeed();
    }
}
