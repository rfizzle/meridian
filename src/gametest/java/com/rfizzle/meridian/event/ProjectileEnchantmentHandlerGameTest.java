// Tier: 3 (Fabric Gametest)
package com.rfizzle.meridian.event;

import com.rfizzle.meridian.Meridian;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class ProjectileEnchantmentHandlerGameTest implements FabricGameTest {

    private Holder<Enchantment> lookup(GameTestHelper helper, String id) {
        Registry<Enchantment> reg = helper.getLevel().registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT);
        return reg.getHolder(Meridian.id(id)).orElse(null);
    }

    private Arrow arrowFiredFrom(GameTestHelper helper, ItemStack weapon) {
        ServerLevel level = helper.getLevel();
        BlockPos abs = helper.absolutePos(new BlockPos(1, 1, 1));
        return new Arrow(level, abs.getX() + 0.5, abs.getY() + 1, abs.getZ() + 0.5,
                new ItemStack(Items.ARROW), weapon);
    }

    // True Flight: a tick on an arrow fired from a True Flight weapon cancels its gravity; an
    // ordinary arrow is left alone.
    @GameTest(template = "meridian:empty_3x3")
    public void trueFlightCancelsArrowGravity(GameTestHelper helper) {
        Holder<Enchantment> trueFlight = lookup(helper, "true_flight");
        if (trueFlight == null) { helper.fail("true_flight not in registry"); return; }

        ItemStack bow = new ItemStack(Items.BOW);
        bow.enchant(trueFlight, 1);
        Arrow enchanted = arrowFiredFrom(helper, bow);
        ProjectileEnchantmentHandler.handleTick(enchanted);
        if (!enchanted.isNoGravity()) {
            helper.fail("True Flight arrow should have gravity disabled after a tick");
            return;
        }

        Arrow control = arrowFiredFrom(helper, new ItemStack(Items.BOW));
        ProjectileEnchantmentHandler.handleTick(control);
        if (control.isNoGravity()) {
            helper.fail("Arrow without True Flight must keep gravity");
            return;
        }

        helper.succeed();
    }

    // Ricochet: hitting a block reflects the arrow's velocity (and consumes the impact) instead of
    // letting it stick.
    @GameTest(template = "meridian:empty_3x3")
    public void ricochetReflectsArrowOffBlock(GameTestHelper helper) {
        Holder<Enchantment> ricochet = lookup(helper, "ricochet");
        if (ricochet == null) { helper.fail("ricochet not in registry"); return; }

        ItemStack bow = new ItemStack(Items.BOW);
        bow.enchant(ricochet, 2);
        Arrow arrow = arrowFiredFrom(helper, bow);
        arrow.setDeltaMovement(new Vec3(0.0, 0.0, 1.0));

        BlockHitResult hit = new BlockHitResult(arrow.position(), Direction.NORTH,
                BlockPos.containing(arrow.position()), false);
        boolean consumed = ProjectileEnchantmentHandler.handleBlockImpact(arrow, hit);

        if (!consumed) {
            helper.fail("Ricochet should consume the block impact (return true)");
            return;
        }
        if (arrow.getDeltaMovement().z >= 0.0) {
            helper.fail("Ricochet should reflect +Z velocity to negative, got "
                    + arrow.getDeltaMovement());
            return;
        }

        helper.succeed();
    }

    // Lifecycle hygiene: the per-projectile trackers (bounces, Longshot launches, Seeker locks)
    // populated during flight are all emptied by the SERVER_STOPPED reset, exercised here through
    // clearForTest() (the same reset path the lifecycle listener runs).
    @GameTest(template = "meridian:empty_3x3")
    public void resetClearsPerProjectileTrackers(GameTestHelper helper) {
        Holder<Enchantment> ricochet = lookup(helper, "ricochet");
        Holder<Enchantment> longshot = lookup(helper, "longshot");
        Holder<Enchantment> seeker = lookup(helper, "seeker");
        if (ricochet == null || longshot == null || seeker == null) {
            helper.fail("ricochet/longshot/seeker not in registry");
            return;
        }

        // Start from a known-empty baseline — earlier tests may have left weakly-held entries.
        ProjectileEnchantmentHandler.clearForTest();

        ItemStack ricochetBow = new ItemStack(Items.BOW);
        ricochetBow.enchant(ricochet, 2);
        Arrow bouncer = arrowFiredFrom(helper, ricochetBow);
        bouncer.setDeltaMovement(new Vec3(0.0, 0.0, 1.0));
        BlockHitResult hit = new BlockHitResult(bouncer.position(), Direction.NORTH,
                BlockPos.containing(bouncer.position()), false);
        ProjectileEnchantmentHandler.handleBlockImpact(bouncer, hit);

        ItemStack longshotBow = new ItemStack(Items.BOW);
        longshotBow.enchant(longshot, 1);
        Arrow longshotArrow = arrowFiredFrom(helper, longshotBow);
        ProjectileEnchantmentHandler.handleTick(longshotArrow);

        ItemStack seekerBow = new ItemStack(Items.BOW);
        seekerBow.enchant(seeker, 1);
        Arrow seekerArrow = arrowFiredFrom(helper, seekerBow);
        ProjectileEnchantmentHandler.handleTick(seekerArrow);

        if (ProjectileEnchantmentHandler.bouncesRemainingSizeForTest() < 1
                || ProjectileEnchantmentHandler.longshotLaunchesSizeForTest() < 1
                || ProjectileEnchantmentHandler.seekerLocksSizeForTest() < 1) {
            helper.fail("Trackers should be populated after flight events: bounces="
                    + ProjectileEnchantmentHandler.bouncesRemainingSizeForTest()
                    + " longshot=" + ProjectileEnchantmentHandler.longshotLaunchesSizeForTest()
                    + " seeker=" + ProjectileEnchantmentHandler.seekerLocksSizeForTest());
            return;
        }

        ProjectileEnchantmentHandler.clearForTest();

        if (ProjectileEnchantmentHandler.bouncesRemainingSizeForTest() != 0
                || ProjectileEnchantmentHandler.longshotLaunchesSizeForTest() != 0
                || ProjectileEnchantmentHandler.seekerLocksSizeForTest() != 0) {
            helper.fail("Reset should clear every per-projectile tracker: bounces="
                    + ProjectileEnchantmentHandler.bouncesRemainingSizeForTest()
                    + " longshot=" + ProjectileEnchantmentHandler.longshotLaunchesSizeForTest()
                    + " seeker=" + ProjectileEnchantmentHandler.seekerLocksSizeForTest());
            return;
        }

        helper.succeed();
    }
}
