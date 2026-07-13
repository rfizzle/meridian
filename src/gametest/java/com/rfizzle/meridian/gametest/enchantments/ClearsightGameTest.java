package com.rfizzle.meridian.gametest.enchantments;

import com.rfizzle.meridian.Meridian;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * Clearsight — verifies the {@code addEffect} gate blocks blindness and darkness for a wearer,
 * leaves other effects untouched, and answers Nightfall's darkness end-to-end.
 */
public class ClearsightGameTest implements FabricGameTest {

    private Holder<Enchantment> lookup(GameTestHelper helper, String id) {
        Registry<Enchantment> reg = helper.getLevel().registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT);
        return reg.getHolder(Meridian.id(id)).orElse(null);
    }

    private Mob clearsightWearer(GameTestHelper helper, BlockPos pos) {
        Holder<Enchantment> clearsight = lookup(helper, "clearsight");
        if (clearsight == null) {
            helper.fail("clearsight not in registry");
            return null;
        }
        ItemStack helmet = new ItemStack(Items.DIAMOND_HELMET);
        helmet.enchant(clearsight, 1);
        // A cow (not an undead mob) is the clean baseline: it is immune to none of the effects
        // under test, so any absence is attributable to Clearsight, not innate mob immunity.
        Mob wearer = helper.spawnWithNoFreeWill(EntityType.COW, pos);
        wearer.setItemSlot(EquipmentSlot.HEAD, helmet);
        return wearer;
    }

    @GameTest(template = "meridian:empty_3x3")
    public void clearsightBlocksBlindness(GameTestHelper helper) {
        Mob wearer = clearsightWearer(helper, new BlockPos(1, 1, 1));
        if (wearer == null) return;

        wearer.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 200, 0));
        if (wearer.hasEffect(MobEffects.BLINDNESS)) {
            helper.fail("Clearsight wearer should not receive Blindness");
        } else {
            helper.succeed();
        }
    }

    @GameTest(template = "meridian:empty_3x3")
    public void clearsightBlocksDarkness(GameTestHelper helper) {
        Mob wearer = clearsightWearer(helper, new BlockPos(1, 1, 1));
        if (wearer == null) return;

        wearer.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 200, 0));
        if (wearer.hasEffect(MobEffects.DARKNESS)) {
            helper.fail("Clearsight wearer should not receive Darkness");
        } else {
            helper.succeed();
        }
    }

    @GameTest(template = "meridian:empty_3x3")
    public void clearsightLeavesOtherEffectsAlone(GameTestHelper helper) {
        Mob wearer = clearsightWearer(helper, new BlockPos(1, 1, 1));
        if (wearer == null) return;

        // Poison is not a vision effect — Clearsight must not over-block.
        wearer.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 0));
        if (wearer.hasEffect(MobEffects.POISON)) {
            helper.succeed();
        } else {
            helper.fail("Clearsight must not block unrelated effects like Poison");
        }
    }

    @GameTest(template = "meridian:empty_3x3")
    public void withoutClearsightBlindnessApplies(GameTestHelper helper) {
        // Control: a plain-helmet mob is still blinded, proving the block is enchantment-gated.
        Mob victim = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(1, 1, 1));
        victim.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));

        victim.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 200, 0));
        if (victim.hasEffect(MobEffects.BLINDNESS)) {
            helper.succeed();
        } else {
            helper.fail("A mob without Clearsight should still be blinded");
        }
    }

    @GameTest(template = "meridian:empty_3x3")
    public void clearsightCountersNightfall(GameTestHelper helper) {
        Holder<Enchantment> nightfall = lookup(helper, "nightfall");
        if (nightfall == null) { helper.fail("nightfall not in registry"); return; }

        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.enchant(nightfall, 1);
        Mob attacker = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 1, 1));
        attacker.setItemSlot(EquipmentSlot.MAINHAND, sword);

        Mob victim = clearsightWearer(helper, new BlockPos(1, 1, 2));
        if (victim == null) return;
        attacker.doHurtTarget(victim);

        helper.runAfterDelay(2, () -> {
            if (victim.hasEffect(MobEffects.DARKNESS)) {
                helper.fail("Clearsight victim should not receive Darkness from Nightfall");
            } else {
                helper.succeed();
            }
        });
    }
}
