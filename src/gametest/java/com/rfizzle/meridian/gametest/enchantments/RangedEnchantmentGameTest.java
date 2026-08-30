// Tier: 3 (Fabric Gametest)
package com.rfizzle.meridian.gametest.enchantments;

import com.rfizzle.meridian.enchanting.RangedEnchantMath;
import com.rfizzle.meridian.event.ProjectileEnchantmentHandler;
import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.gametest.util.MockPlayers;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.SpectralArrow;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Behavior coverage for the ranged/thrown trio: Longshot's distance-scaled damage
 * (grace window, linear ramp, hard cap), Seeker's fire-time lock and bounded per-tick
 * curve (including no re-acquisition and the players-excluded default), and Harpoon's
 * pull-toward-thrower with boss-tag immunity.
 */
public class RangedEnchantmentGameTest implements FabricGameTest {

    private Holder<Enchantment> lookup(GameTestHelper helper, String id) {
        Registry<Enchantment> reg = helper.getLevel().registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT);
        return reg.getHolder(Meridian.id(id)).orElse(null);
    }

    private Arrow arrowAt(GameTestHelper helper, BlockPos rel, ItemStack weapon) {
        ServerLevel level = helper.getLevel();
        BlockPos abs = helper.absolutePos(rel);
        return new Arrow(level, abs.getX() + 0.5, abs.getY() + 1, abs.getZ() + 0.5,
                new ItemStack(Items.ARROW), weapon);
    }

    // Longshot: no bonus inside the grace distance, capped bonus far beyond it, and an
    // unenchanted arrow is left alone.
    @GameTest(template = "meridian:empty_3x3")
    public void longshotScalesDamageWithDistanceAndCaps(GameTestHelper helper) {
        Holder<Enchantment> longshot = lookup(helper, "longshot");
        if (longshot == null) { helper.fail("longshot not in registry"); return; }

        ItemStack bow = new ItemStack(Items.BOW);
        bow.enchant(longshot, 3);
        Arrow arrow = arrowAt(helper, new BlockPos(1, 1, 1), bow);
        double initial = arrow.getBaseDamage();

        ProjectileEnchantmentHandler.handleTick(arrow);
        if (Math.abs(arrow.getBaseDamage() - initial) > 1.0e-6) {
            helper.fail("Longshot must grant no bonus at point blank, damage went from "
                    + initial + " to " + arrow.getBaseDamage());
            return;
        }

        arrow.setPos(arrow.getX() + 100.0, arrow.getY(), arrow.getZ());
        ProjectileEnchantmentHandler.handleTick(arrow);
        double expected = initial * (1.0 + RangedEnchantMath.LONGSHOT_BONUS_PER_LEVEL * 3);
        if (Math.abs(arrow.getBaseDamage() - expected) > 1.0e-6) {
            helper.fail("Longshot III at 100 blocks should cap damage at " + expected
                    + ", got " + arrow.getBaseDamage());
            return;
        }

        Arrow control = arrowAt(helper, new BlockPos(1, 1, 1), new ItemStack(Items.BOW));
        double controlInitial = control.getBaseDamage();
        ProjectileEnchantmentHandler.handleTick(control);
        control.setPos(control.getX() + 100.0, control.getY(), control.getZ());
        ProjectileEnchantmentHandler.handleTick(control);
        if (Math.abs(control.getBaseDamage() - controlInitial) > 1.0e-6) {
            helper.fail("Arrow without Longshot must keep its base damage");
            return;
        }

        helper.succeed();
    }

    // Seeker: the first server tick locks the creature under the shooter's crosshair and
    // each tick bends the bolt toward it without changing its speed.
    @GameTest(template = "meridian:empty_3x3")
    public void seekerCurvesTowardCrosshairTarget(GameTestHelper helper) {
        Holder<Enchantment> seeker = lookup(helper, "seeker");
        if (seeker == null) { helper.fail("seeker not in registry"); return; }

        Pig pig = helper.spawn(EntityType.PIG, new BlockPos(1, 1, 2));
        Player shooter = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos shooterAbs = helper.absolutePos(new BlockPos(1, 1, 0));
        shooter.moveTo(shooterAbs.getX() + 0.5, shooterAbs.getY(), shooterAbs.getZ() + 0.5);
        shooter.lookAt(EntityAnchorArgument.Anchor.EYES, pig.getBoundingBox().getCenter());

        ItemStack crossbow = new ItemStack(Items.CROSSBOW);
        crossbow.enchant(seeker, 2);
        Vec3 eye = shooter.getEyePosition();
        Arrow bolt = new Arrow(helper.getLevel(), eye.x, eye.y, eye.z,
                new ItemStack(Items.ARROW), crossbow);
        bolt.setOwner(shooter);

        // Fire deliberately off-target: the aim direction plus a sideways component.
        Vec3 aim = shooter.getViewVector(1.0f);
        Vec3 sideways = aim.cross(new Vec3(0.0, 1.0, 0.0)).normalize();
        Vec3 velocity = aim.scale(3.0).add(sideways.scale(0.6));
        bolt.setDeltaMovement(velocity);

        Vec3 toTarget = pig.getBoundingBox().getCenter().subtract(bolt.position()).normalize();
        double alignmentBefore = velocity.normalize().dot(toTarget);
        double speedBefore = velocity.length();

        ProjectileEnchantmentHandler.handleTick(bolt);

        Vec3 after = bolt.getDeltaMovement();
        double alignmentAfter = after.normalize().dot(toTarget);
        if (alignmentAfter <= alignmentBefore + 1.0e-4) {
            helper.fail("Seeker bolt should bend toward the locked target: alignment "
                    + alignmentBefore + " -> " + alignmentAfter);
            shooter.discard();
            return;
        }
        if (Math.abs(after.length() - speedBefore) > 1.0e-4) {
            helper.fail("Seeker must redirect, not accelerate: speed "
                    + speedBefore + " -> " + after.length());
            shooter.discard();
            return;
        }

        shooter.discard();
        helper.succeed();
    }

    // Seeker: the lock happens at fire time only — once the target is gone the bolt
    // flies straight, it never picks a new one.
    @GameTest(template = "meridian:empty_3x3")
    public void seekerNeverReacquiresAfterTargetGone(GameTestHelper helper) {
        Holder<Enchantment> seeker = lookup(helper, "seeker");
        if (seeker == null) { helper.fail("seeker not in registry"); return; }

        Pig locked = helper.spawn(EntityType.PIG, new BlockPos(1, 1, 2));
        Player shooter = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos shooterAbs = helper.absolutePos(new BlockPos(1, 1, 0));
        shooter.moveTo(shooterAbs.getX() + 0.5, shooterAbs.getY(), shooterAbs.getZ() + 0.5);
        shooter.lookAt(EntityAnchorArgument.Anchor.EYES, locked.getBoundingBox().getCenter());

        ItemStack crossbow = new ItemStack(Items.CROSSBOW);
        crossbow.enchant(seeker, 2);
        Vec3 eye = shooter.getEyePosition();
        Arrow bolt = new Arrow(helper.getLevel(), eye.x, eye.y, eye.z,
                new ItemStack(Items.ARROW), crossbow);
        bolt.setOwner(shooter);
        bolt.setDeltaMovement(shooter.getViewVector(1.0f).scale(3.0));
        ProjectileEnchantmentHandler.handleTick(bolt);

        // The locked target disappears; a fresh bystander is standing right there.
        locked.discard();
        helper.spawn(EntityType.PIG, new BlockPos(1, 1, 2));

        Vec3 straight = new Vec3(0.4, 0.0, 3.0);
        bolt.setDeltaMovement(straight);
        ProjectileEnchantmentHandler.handleTick(bolt);
        if (bolt.getDeltaMovement().subtract(straight).length() > 1.0e-6) {
            helper.fail("Seeker must not re-acquire after its locked target is gone, velocity changed to "
                    + bolt.getDeltaMovement());
            shooter.discard();
            return;
        }

        shooter.discard();
        helper.succeed();
    }

    // Seeker: with the default config, a player under the crosshair is never locked.
    @GameTest(template = "meridian:empty_3x3")
    public void seekerIgnoresPlayersByDefault(GameTestHelper helper) {
        Holder<Enchantment> seeker = lookup(helper, "seeker");
        if (seeker == null) { helper.fail("seeker not in registry"); return; }

        var target = MockPlayers.serverPlayerInLevel(helper);
        BlockPos targetAbs = helper.absolutePos(new BlockPos(1, 1, 2));
        target.teleportTo(targetAbs.getX() + 0.5, targetAbs.getY(), targetAbs.getZ() + 0.5);

        Player shooter = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos shooterAbs = helper.absolutePos(new BlockPos(1, 1, 0));
        shooter.moveTo(shooterAbs.getX() + 0.5, shooterAbs.getY(), shooterAbs.getZ() + 0.5);
        shooter.lookAt(EntityAnchorArgument.Anchor.EYES, target.getBoundingBox().getCenter());

        ItemStack crossbow = new ItemStack(Items.CROSSBOW);
        crossbow.enchant(seeker, 2);
        Vec3 eye = shooter.getEyePosition();
        Arrow bolt = new Arrow(helper.getLevel(), eye.x, eye.y, eye.z,
                new ItemStack(Items.ARROW), crossbow);
        bolt.setOwner(shooter);

        Vec3 aim = shooter.getViewVector(1.0f);
        Vec3 sideways = aim.cross(new Vec3(0.0, 1.0, 0.0)).normalize();
        Vec3 velocity = aim.scale(3.0).add(sideways.scale(0.6));
        bolt.setDeltaMovement(velocity);
        ProjectileEnchantmentHandler.handleTick(bolt);

        boolean unchanged = bolt.getDeltaMovement().subtract(velocity).length() < 1.0e-6;
        shooter.discard();
        MockPlayers.retire(target);
        if (!unchanged) {
            helper.fail("Seeker must not lock onto players while combat.seekerTargetsPlayers is false");
            return;
        }
        helper.succeed();
    }

    // Seeker: a creature behind a wall is not under the crosshair — the sight line is
    // clipped against blocks, so nothing is locked and the bolt flies straight.
    @GameTest(template = "meridian:empty_3x3")
    public void seekerDoesNotLockThroughWalls(GameTestHelper helper) {
        Holder<Enchantment> seeker = lookup(helper, "seeker");
        if (seeker == null) { helper.fail("seeker not in registry"); return; }

        Pig pig = helper.spawn(EntityType.PIG, new BlockPos(1, 1, 2));
        helper.setBlock(new BlockPos(1, 1, 1), Blocks.STONE);
        helper.setBlock(new BlockPos(1, 2, 1), Blocks.STONE);

        Player shooter = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos shooterAbs = helper.absolutePos(new BlockPos(1, 1, 0));
        shooter.moveTo(shooterAbs.getX() + 0.5, shooterAbs.getY(), shooterAbs.getZ() + 0.5);
        shooter.lookAt(EntityAnchorArgument.Anchor.EYES, pig.getBoundingBox().getCenter());

        ItemStack crossbow = new ItemStack(Items.CROSSBOW);
        crossbow.enchant(seeker, 2);
        Vec3 eye = shooter.getEyePosition();
        Arrow bolt = new Arrow(helper.getLevel(), eye.x, eye.y, eye.z,
                new ItemStack(Items.ARROW), crossbow);
        bolt.setOwner(shooter);

        Vec3 aim = shooter.getViewVector(1.0f);
        Vec3 sideways = aim.cross(new Vec3(0.0, 1.0, 0.0)).normalize();
        Vec3 velocity = aim.scale(3.0).add(sideways.scale(0.6));
        bolt.setDeltaMovement(velocity);
        ProjectileEnchantmentHandler.handleTick(bolt);

        boolean unchanged = bolt.getDeltaMovement().subtract(velocity).length() < 1.0e-6;
        shooter.discard();
        if (!unchanged) {
            helper.fail("Seeker must not lock a creature behind a wall");
            return;
        }
        helper.succeed();
    }

    // Harpoon: with the default config, a player victim is never dragged.
    @GameTest(template = "meridian:empty_3x3")
    public void harpoonIgnoresPlayersByDefault(GameTestHelper helper) {
        Holder<Enchantment> harpoon = lookup(helper, "harpoon");
        if (harpoon == null) { helper.fail("harpoon not in registry"); return; }

        Player thrower = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos throwerAbs = helper.absolutePos(new BlockPos(1, 1, 0));
        thrower.moveTo(throwerAbs.getX() + 0.5, throwerAbs.getY(), throwerAbs.getZ() + 0.5);

        var victim = MockPlayers.serverPlayerInLevel(helper);
        BlockPos victimAbs = helper.absolutePos(new BlockPos(1, 1, 2));
        victim.teleportTo(victimAbs.getX() + 0.5, victimAbs.getY(), victimAbs.getZ() + 0.5);
        victim.setDeltaMovement(Vec3.ZERO);

        ItemStack trident = new ItemStack(Items.TRIDENT);
        trident.enchant(harpoon, 2);
        ThrownTrident thrown = new ThrownTrident(helper.getLevel(), thrower, trident);
        thrown.setPos(victim.getX(), victim.getY(), victim.getZ());

        ProjectileEnchantmentHandler.handleEntityImpact(thrown, new EntityHitResult(victim));

        Vec3 delta = victim.getDeltaMovement();
        thrower.discard();
        MockPlayers.retire(victim);
        if (delta.length() > 1.0e-6) {
            helper.fail("Harpoon must not move a player while combat.harpoonAffectsPlayers is false, got "
                    + delta);
            return;
        }
        helper.succeed();
    }

    // Harpoon: an enchanted trident hit pulls the victim toward the thrower (with lift).
    @GameTest(template = "meridian:empty_3x3")
    public void harpoonPullsVictimTowardThrower(GameTestHelper helper) {
        Holder<Enchantment> harpoon = lookup(helper, "harpoon");
        if (harpoon == null) { helper.fail("harpoon not in registry"); return; }

        Player thrower = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos throwerAbs = helper.absolutePos(new BlockPos(1, 1, 0));
        thrower.moveTo(throwerAbs.getX() + 0.5, throwerAbs.getY(), throwerAbs.getZ() + 0.5);

        Pig victim = helper.spawn(EntityType.PIG, new BlockPos(1, 1, 2));
        victim.setDeltaMovement(Vec3.ZERO);

        ItemStack trident = new ItemStack(Items.TRIDENT);
        trident.enchant(harpoon, 2);
        ThrownTrident thrown = new ThrownTrident(helper.getLevel(), thrower, trident);
        thrown.setPos(victim.getX(), victim.getY(), victim.getZ());

        ProjectileEnchantmentHandler.handleEntityImpact(thrown, new EntityHitResult(victim));

        Vec3 delta = victim.getDeltaMovement();
        Vec3 toThrower = thrower.position().subtract(victim.position());
        double pullAlignment = delta.x * toThrower.x + delta.z * toThrower.z;
        boolean pulled = pullAlignment > 1.0e-4 && delta.y > 0.0;
        thrower.discard();
        if (!pulled) {
            helper.fail("Harpoon should drag the victim toward the thrower with lift, got " + delta);
            return;
        }
        helper.succeed();
    }

    // Harpoon: entities in #meridian:harpoon_immune (bosses) are unaffected.
    @GameTest(template = "meridian:empty_3x3")
    public void harpoonLeavesBossTagEntitiesAlone(GameTestHelper helper) {
        Holder<Enchantment> harpoon = lookup(helper, "harpoon");
        if (harpoon == null) { helper.fail("harpoon not in registry"); return; }

        Player thrower = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos throwerAbs = helper.absolutePos(new BlockPos(1, 1, 0));
        thrower.moveTo(throwerAbs.getX() + 0.5, throwerAbs.getY(), throwerAbs.getZ() + 0.5);

        ElderGuardian boss = helper.spawn(EntityType.ELDER_GUARDIAN, new BlockPos(1, 1, 2));
        boss.setDeltaMovement(Vec3.ZERO);

        ItemStack trident = new ItemStack(Items.TRIDENT);
        trident.enchant(harpoon, 2);
        ThrownTrident thrown = new ThrownTrident(helper.getLevel(), thrower, trident);
        thrown.setPos(boss.getX(), boss.getY(), boss.getZ());

        ProjectileEnchantmentHandler.handleEntityImpact(thrown, new EntityHitResult(boss));

        Vec3 delta = boss.getDeltaMovement();
        thrower.discard();
        if (delta.length() > 1.0e-6) {
            helper.fail("Harpoon must not move a #meridian:harpoon_immune entity, got " + delta);
            return;
        }
        helper.succeed();
    }

    // Undertow: on impact, a nearby bystander is dragged toward the impact point (crowd-gather).
    @GameTest(template = "meridian:empty_3x3")
    public void undertowGathersCreaturesTowardImpact(GameTestHelper helper) {
        Holder<Enchantment> undertow = lookup(helper, "undertow");
        if (undertow == null) { helper.fail("undertow not in registry"); return; }

        Player thrower = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos throwerAbs = helper.absolutePos(new BlockPos(1, 1, 0));
        thrower.moveTo(throwerAbs.getX() + 0.5, throwerAbs.getY(), throwerAbs.getZ() + 0.5);

        // The struck creature sits at the impact point; a bystander stands one block off it.
        Pig struck = helper.spawn(EntityType.PIG, new BlockPos(1, 1, 2));
        struck.setDeltaMovement(Vec3.ZERO);
        Pig bystander = helper.spawn(EntityType.PIG, new BlockPos(1, 1, 1));
        bystander.setDeltaMovement(Vec3.ZERO);

        ItemStack trident = new ItemStack(Items.TRIDENT);
        trident.enchant(undertow, 2);
        ThrownTrident thrown = new ThrownTrident(helper.getLevel(), thrower, trident);
        thrown.setPos(struck.getX(), struck.getY(), struck.getZ());

        ProjectileEnchantmentHandler.handleEntityImpact(thrown, new EntityHitResult(struck));

        Vec3 delta = bystander.getDeltaMovement();
        Vec3 toImpact = struck.position().subtract(bystander.position());
        double alignment = delta.x * toImpact.x + delta.z * toImpact.z;
        boolean gathered = alignment > 1.0e-4 && delta.y > 0.0;
        thrower.discard();
        if (!gathered) {
            helper.fail("Undertow should drag a nearby creature toward the impact point with lift, got " + delta);
            return;
        }
        helper.succeed();
    }

    // Undertow: with the default config, a player bystander is never gathered.
    @GameTest(template = "meridian:empty_3x3")
    public void undertowIgnoresPlayersByDefault(GameTestHelper helper) {
        Holder<Enchantment> undertow = lookup(helper, "undertow");
        if (undertow == null) { helper.fail("undertow not in registry"); return; }

        Player thrower = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos throwerAbs = helper.absolutePos(new BlockPos(1, 1, 0));
        thrower.moveTo(throwerAbs.getX() + 0.5, throwerAbs.getY(), throwerAbs.getZ() + 0.5);

        Pig struck = helper.spawn(EntityType.PIG, new BlockPos(1, 1, 2));
        struck.setDeltaMovement(Vec3.ZERO);
        var bystander = MockPlayers.serverPlayerInLevel(helper);
        BlockPos byAbs = helper.absolutePos(new BlockPos(1, 1, 1));
        bystander.teleportTo(byAbs.getX() + 0.5, byAbs.getY(), byAbs.getZ() + 0.5);
        bystander.setDeltaMovement(Vec3.ZERO);

        ItemStack trident = new ItemStack(Items.TRIDENT);
        trident.enchant(undertow, 2);
        ThrownTrident thrown = new ThrownTrident(helper.getLevel(), thrower, trident);
        thrown.setPos(struck.getX(), struck.getY(), struck.getZ());

        ProjectileEnchantmentHandler.handleEntityImpact(thrown, new EntityHitResult(struck));

        Vec3 delta = bystander.getDeltaMovement();
        thrower.discard();
        MockPlayers.retire(bystander);
        if (delta.length() > 1.0e-6) {
            helper.fail("Undertow must not move a player while combat.undertowAffectsPlayers is false, got "
                    + delta);
            return;
        }
        helper.succeed();
    }

    // Undertow: entities in #meridian:harpoon_immune (bosses) are unaffected.
    @GameTest(template = "meridian:empty_3x3")
    public void undertowLeavesBossTagEntitiesAlone(GameTestHelper helper) {
        Holder<Enchantment> undertow = lookup(helper, "undertow");
        if (undertow == null) { helper.fail("undertow not in registry"); return; }

        Player thrower = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos throwerAbs = helper.absolutePos(new BlockPos(1, 1, 0));
        thrower.moveTo(throwerAbs.getX() + 0.5, throwerAbs.getY(), throwerAbs.getZ() + 0.5);

        Pig struck = helper.spawn(EntityType.PIG, new BlockPos(1, 1, 2));
        struck.setDeltaMovement(Vec3.ZERO);
        ElderGuardian boss = helper.spawn(EntityType.ELDER_GUARDIAN, new BlockPos(1, 1, 1));
        boss.setDeltaMovement(Vec3.ZERO);

        ItemStack trident = new ItemStack(Items.TRIDENT);
        trident.enchant(undertow, 2);
        ThrownTrident thrown = new ThrownTrident(helper.getLevel(), thrower, trident);
        thrown.setPos(struck.getX(), struck.getY(), struck.getZ());

        ProjectileEnchantmentHandler.handleEntityImpact(thrown, new EntityHitResult(struck));

        Vec3 delta = boss.getDeltaMovement();
        thrower.discard();
        if (delta.length() > 1.0e-6) {
            helper.fail("Undertow must not move a #meridian:harpoon_immune entity, got " + delta);
            return;
        }
        helper.succeed();
    }

    // Mark: an enchanted arrow hitting a mob makes it glow through walls.
    @GameTest(template = "meridian:empty_3x3")
    public void markGlowsStruckMob(GameTestHelper helper) {
        Holder<Enchantment> mark = lookup(helper, "mark");
        if (mark == null) { helper.fail("mark not in registry"); return; }

        Pig victim = helper.spawn(EntityType.PIG, new BlockPos(1, 1, 2));

        ItemStack bow = new ItemStack(Items.BOW);
        bow.enchant(mark, 1);
        Arrow arrow = arrowAt(helper, new BlockPos(1, 1, 1), bow);

        ProjectileEnchantmentHandler.handleEntityImpact(arrow, new EntityHitResult(victim));

        if (!victim.hasEffect(MobEffects.GLOWING)) {
            helper.fail("Mark should apply Glowing to a struck mob");
            return;
        }
        helper.succeed();
    }

    // Mark: with the default config, a struck player is left unmarked.
    @GameTest(template = "meridian:empty_3x3")
    public void markIgnoresPlayersByDefault(GameTestHelper helper) {
        Holder<Enchantment> mark = lookup(helper, "mark");
        if (mark == null) { helper.fail("mark not in registry"); return; }

        var victim = MockPlayers.serverPlayerInLevel(helper);
        BlockPos victimAbs = helper.absolutePos(new BlockPos(1, 1, 2));
        victim.teleportTo(victimAbs.getX() + 0.5, victimAbs.getY(), victimAbs.getZ() + 0.5);

        ItemStack bow = new ItemStack(Items.BOW);
        bow.enchant(mark, 1);
        Arrow arrow = arrowAt(helper, new BlockPos(1, 1, 1), bow);

        ProjectileEnchantmentHandler.handleEntityImpact(arrow, new EntityHitResult(victim));

        boolean glowing = victim.hasEffect(MobEffects.GLOWING);
        MockPlayers.retire(victim);
        if (glowing) {
            helper.fail("Mark must not glow a player while combat.markAffectsPlayers is false");
            return;
        }
        helper.succeed();
    }

    // Pin: a crossbow bolt roots a struck mob standing against the ground.
    @GameTest(template = "meridian:empty_3x3")
    public void pinRootsGroundedMob(GameTestHelper helper) {
        Holder<Enchantment> pin = lookup(helper, "pin");
        if (pin == null) { helper.fail("pin not in registry"); return; }

        Player shooter = helper.makeMockPlayer(GameType.SURVIVAL);
        Pig victim = helper.spawn(EntityType.PIG, new BlockPos(1, 1, 2));
        victim.setOnGround(true);

        ItemStack crossbow = new ItemStack(Items.CROSSBOW);
        crossbow.enchant(pin, 2);
        Arrow bolt = arrowAt(helper, new BlockPos(1, 1, 1), crossbow);
        bolt.setOwner(shooter);

        ProjectileEnchantmentHandler.handleEntityImpact(bolt, new EntityHitResult(victim));

        var effect = victim.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
        shooter.discard();
        if (effect == null) {
            helper.fail("Pin should root a grounded mob with Slowness");
            return;
        }
        if (effect.getAmplifier() != RangedEnchantMath.PIN_ROOT_SLOWNESS_AMPLIFIER) {
            helper.fail("Pin's root should use amplifier " + RangedEnchantMath.PIN_ROOT_SLOWNESS_AMPLIFIER
                    + ", got " + effect.getAmplifier());
            return;
        }
        helper.succeed();
    }

    // Pin: a creature loosed in open air, away from any surface, is left unrooted.
    @GameTest(template = "meridian:empty_3x3")
    public void pinLeavesAirborneMobAlone(GameTestHelper helper) {
        Holder<Enchantment> pin = lookup(helper, "pin");
        if (pin == null) { helper.fail("pin not in registry"); return; }

        Player shooter = helper.makeMockPlayer(GameType.SURVIVAL);
        Pig victim = helper.spawn(EntityType.PIG, new BlockPos(1, 1, 2));
        // Lift the pig high into open air, clear of the floor and any template wall, so the
        // only thing under test is Pin's surface gate — not the 3x3 structure's geometry.
        BlockPos abs = helper.absolutePos(new BlockPos(1, 1, 2));
        victim.moveTo(abs.getX() + 0.5, abs.getY() + 20.0, abs.getZ() + 0.5);
        victim.setOnGround(false);

        ItemStack crossbow = new ItemStack(Items.CROSSBOW);
        crossbow.enchant(pin, 2);
        Arrow bolt = arrowAt(helper, new BlockPos(1, 1, 1), crossbow);
        bolt.setOwner(shooter);

        ProjectileEnchantmentHandler.handleEntityImpact(bolt, new EntityHitResult(victim));

        boolean rooted = victim.hasEffect(MobEffects.MOVEMENT_SLOWDOWN);
        shooter.discard();
        if (rooted) {
            helper.fail("Pin must not root a creature that is not near a wall or the ground");
            return;
        }
        helper.succeed();
    }

    // Pin: an airborne creature pressed against a wall is still rooted — the surface gate
    // catches a wall, not just the ground.
    @GameTest(template = "meridian:empty_3x3")
    public void pinRootsAirborneMobAgainstWall(GameTestHelper helper) {
        Holder<Enchantment> pin = lookup(helper, "pin");
        if (pin == null) { helper.fail("pin not in registry"); return; }

        Player shooter = helper.makeMockPlayer(GameType.SURVIVAL);
        Pig victim = helper.spawn(EntityType.PIG, new BlockPos(1, 1, 1));
        victim.setOnGround(false);
        // A solid block flush against the pig's east side — the only surface in reach.
        helper.setBlock(new BlockPos(2, 1, 1), Blocks.STONE);

        ItemStack crossbow = new ItemStack(Items.CROSSBOW);
        crossbow.enchant(pin, 2);
        Arrow bolt = arrowAt(helper, new BlockPos(1, 1, 0), crossbow);
        bolt.setOwner(shooter);

        ProjectileEnchantmentHandler.handleEntityImpact(bolt, new EntityHitResult(victim));

        boolean rooted = victim.hasEffect(MobEffects.MOVEMENT_SLOWDOWN);
        shooter.discard();
        if (!rooted) {
            helper.fail("Pin should root an airborne creature that is pressed against a wall");
            return;
        }
        helper.succeed();
    }

    // Pin: with the default config, a struck player is not rooted.
    @GameTest(template = "meridian:empty_3x3")
    public void pinIgnoresPlayersByDefault(GameTestHelper helper) {
        Holder<Enchantment> pin = lookup(helper, "pin");
        if (pin == null) { helper.fail("pin not in registry"); return; }

        Player shooter = helper.makeMockPlayer(GameType.SURVIVAL);
        var victim = MockPlayers.serverPlayerInLevel(helper);
        BlockPos victimAbs = helper.absolutePos(new BlockPos(1, 1, 2));
        victim.teleportTo(victimAbs.getX() + 0.5, victimAbs.getY(), victimAbs.getZ() + 0.5);
        victim.setOnGround(true);

        ItemStack crossbow = new ItemStack(Items.CROSSBOW);
        crossbow.enchant(pin, 2);
        Arrow bolt = arrowAt(helper, new BlockPos(1, 1, 1), crossbow);
        bolt.setOwner(shooter);

        ProjectileEnchantmentHandler.handleEntityImpact(bolt, new EntityHitResult(victim));

        boolean rooted = victim.hasEffect(MobEffects.MOVEMENT_SLOWDOWN);
        shooter.discard();
        MockPlayers.retire(victim);
        if (rooted) {
            helper.fail("Pin must not root a player while combat.pinAffectsPlayers is false");
            return;
        }
        helper.succeed();
    }

    // Pin: a #meridian:harpoon_immune boss is exempt, honouring the boss-immunity boundary.
    @GameTest(template = "meridian:empty_3x3")
    public void pinLeavesBossTagEntitiesAlone(GameTestHelper helper) {
        Holder<Enchantment> pin = lookup(helper, "pin");
        if (pin == null) { helper.fail("pin not in registry"); return; }

        Player shooter = helper.makeMockPlayer(GameType.SURVIVAL);
        ElderGuardian boss = helper.spawn(EntityType.ELDER_GUARDIAN, new BlockPos(1, 1, 2));
        boss.setOnGround(true);

        ItemStack crossbow = new ItemStack(Items.CROSSBOW);
        crossbow.enchant(pin, 2);
        Arrow bolt = arrowAt(helper, new BlockPos(1, 1, 1), crossbow);
        bolt.setOwner(shooter);

        ProjectileEnchantmentHandler.handleEntityImpact(bolt, new EntityHitResult(boss));

        boolean rooted = boss.hasEffect(MobEffects.MOVEMENT_SLOWDOWN);
        shooter.discard();
        if (rooted) {
            helper.fail("Pin must not root a #meridian:harpoon_immune entity");
            return;
        }
        helper.succeed();
    }

    // Skyfall: an arrow loosed while the shooter is airborne carries a per-level damage bonus,
    // applied once; a grounded shot is left untouched.
    @GameTest(template = "meridian:empty_3x3")
    public void skyfallBoostsAirborneShotOnce(GameTestHelper helper) {
        Holder<Enchantment> skyfall = lookup(helper, "skyfall");
        if (skyfall == null) { helper.fail("skyfall not in registry"); return; }

        ItemStack bow = new ItemStack(Items.BOW);
        bow.enchant(skyfall, 3);

        Player airborne = helper.makeMockPlayer(GameType.SURVIVAL);
        airborne.setOnGround(false);
        Arrow boosted = arrowAt(helper, new BlockPos(1, 1, 1), bow);
        boosted.setOwner(airborne);
        double base = boosted.getBaseDamage();
        double expected = base * RangedEnchantMath.skyfallMultiplier(3);

        ProjectileEnchantmentHandler.handleTick(boosted);
        if (Math.abs(boosted.getBaseDamage() - expected) > 1.0e-6) {
            helper.fail("Skyfall III airborne should scale base damage to " + expected
                    + ", got " + boosted.getBaseDamage());
            airborne.discard();
            ProjectileEnchantmentHandler.clearForTest();
            return;
        }

        // A second tick must not re-apply the bonus — the snapshot is once-only.
        ProjectileEnchantmentHandler.handleTick(boosted);
        if (Math.abs(boosted.getBaseDamage() - expected) > 1.0e-6) {
            helper.fail("Skyfall must apply its bonus once, not compound each tick: "
                    + boosted.getBaseDamage());
            airborne.discard();
            ProjectileEnchantmentHandler.clearForTest();
            return;
        }

        Player grounded = helper.makeMockPlayer(GameType.SURVIVAL);
        grounded.setOnGround(true);
        Arrow flat = arrowAt(helper, new BlockPos(1, 1, 1), bow);
        flat.setOwner(grounded);
        double flatBase = flat.getBaseDamage();
        ProjectileEnchantmentHandler.handleTick(flat);
        boolean groundedUnchanged = Math.abs(flat.getBaseDamage() - flatBase) <= 1.0e-6;

        airborne.discard();
        grounded.discard();
        ProjectileEnchantmentHandler.clearForTest();
        if (!groundedUnchanged) {
            helper.fail("Skyfall must not boost a grounded shot, damage changed to " + flat.getBaseDamage());
            return;
        }
        helper.succeed();
    }

    // Volley: firing looses the extra arrows — a reduced-damage, unrecoverable fan.
    @GameTest(template = "meridian:empty_3x3")
    public void volleyLoosesReducedUnrecoverableFan(GameTestHelper helper) {
        Holder<Enchantment> volley = lookup(helper, "volley");
        if (volley == null) { helper.fail("volley not in registry"); return; }

        ServerLevel level = helper.getLevel();
        Player shooter = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos shooterAbs = helper.absolutePos(new BlockPos(1, 1, 1));
        shooter.moveTo(shooterAbs.getX() + 0.5, shooterAbs.getY(), shooterAbs.getZ() + 0.5);

        ItemStack bow = new ItemStack(Items.BOW);
        bow.enchant(volley, 2); // level II: five in the fan, four beyond the one primary shot

        // Reference primary arrow (built the vanilla way) for the base-damage comparison.
        AbstractArrow reference = ((ArrowItem) Items.ARROW).createArrow(
                level, new ItemStack(Items.ARROW), shooter, bow);
        double fullBaseDamage = reference.getBaseDamage();
        reference.discard();

        AABB area = new AABB(shooterAbs).inflate(48);
        List<Arrow> before = level.getEntitiesOfClass(Arrow.class, area);
        ProjectileEnchantmentHandler.handleVolley(
                level, shooter, bow, new ItemStack(Items.ARROW), 3.0f, 1.0f, false, 1);
        List<Arrow> extras = new ArrayList<>();
        for (Arrow a : level.getEntitiesOfClass(Arrow.class, area)) {
            if (!before.contains(a)) extras.add(a);
        }

        int expectedExtra = RangedEnchantMath.volleyExtraCount(2, 1);
        shooter.discard();
        if (extras.size() != expectedExtra) {
            helper.fail("Volley II should loose " + expectedExtra + " extra arrows, loosed " + extras.size());
            return;
        }
        double expectedDamage = fullBaseDamage * RangedEnchantMath.VOLLEY_DAMAGE_MULTIPLIER;
        for (Arrow extra : extras) {
            if (Math.abs(extra.getBaseDamage() - expectedDamage) > 1.0e-4) {
                helper.fail("Volley extra arrows should deal reduced damage " + expectedDamage
                        + ", got " + extra.getBaseDamage());
                return;
            }
            if (extra.pickup != AbstractArrow.Pickup.DISALLOWED) {
                helper.fail("Volley extra arrows must not be recoverable, pickup was " + extra.pickup);
                return;
            }
        }
        helper.succeed();
    }

    // Volley: the extra arrows match the fired ammo type — spectral ammo yields spectral extras.
    @GameTest(template = "meridian:empty_3x3")
    public void volleyExtrasMatchFiredAmmoType(GameTestHelper helper) {
        Holder<Enchantment> volley = lookup(helper, "volley");
        if (volley == null) { helper.fail("volley not in registry"); return; }

        ServerLevel level = helper.getLevel();
        Player shooter = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos shooterAbs = helper.absolutePos(new BlockPos(1, 1, 1));
        shooter.moveTo(shooterAbs.getX() + 0.5, shooterAbs.getY(), shooterAbs.getZ() + 0.5);

        ItemStack bow = new ItemStack(Items.BOW);
        bow.enchant(volley, 1); // level I: three in the fan, two extras

        AABB area = new AABB(shooterAbs).inflate(48);
        List<AbstractArrow> before = level.getEntitiesOfClass(AbstractArrow.class, area);
        ProjectileEnchantmentHandler.handleVolley(
                level, shooter, bow, new ItemStack(Items.SPECTRAL_ARROW), 3.0f, 1.0f, false, 1);

        List<SpectralArrow> spectral = new ArrayList<>();
        for (AbstractArrow a : level.getEntitiesOfClass(AbstractArrow.class, area)) {
            if (!before.contains(a) && a instanceof SpectralArrow sa) spectral.add(sa);
        }

        int expectedExtra = RangedEnchantMath.volleyExtraCount(1, 1);
        shooter.discard();
        if (spectral.size() != expectedExtra) {
            helper.fail("Volley extras should match the fired ammo type: expected " + expectedExtra
                    + " spectral arrows, got " + spectral.size());
            return;
        }
        helper.succeed();
    }

    // Curse of Wavering: a cursed bow fires with visibly wider scatter than a clean one. Fires real
    // shots through ProjectileWeaponItem.shoot so the inaccuracy @ModifyVariable is exercised end to
    // end — guarding the ordinal-based capture against a future mapping bump silently breaking it.
    @GameTest(template = "meridian:empty_3x3")
    public void waveringWidensArrowScatter(GameTestHelper helper) {
        Holder<Enchantment> wavering = lookup(helper, "curse_of_wavering");
        if (wavering == null) { helper.fail("curse_of_wavering not in registry"); return; }

        ItemStack cleanBow = new ItemStack(Items.BOW);
        ItemStack waveringBow = new ItemStack(Items.BOW);
        waveringBow.enchant(wavering, 2);

        int shots = 48;
        double cleanMaxDev = maxArrowDeviation(helper, cleanBow, shots);
        double waveringMaxDev = maxArrowDeviation(helper, waveringBow, shots);

        // Wavering II fires at inaccuracy 5.0 vs the bow's base 1.0 — a fivefold scatter. Over 48
        // shots the cursed maximum deviation clears the clean one with an overwhelming margin, so
        // this compares aggregates rather than one flaky roll.
        if (!(waveringMaxDev > cleanMaxDev)) {
            helper.fail("Curse of Wavering should widen arrow scatter (cleanMaxDev=" + cleanMaxDev
                    + ", waveringMaxDev=" + waveringMaxDev + ")");
            return;
        }
        helper.succeed();
    }

    // Fires `shots` arrows from `bow` through the real ProjectileWeaponItem.shoot path (base
    // inaccuracy 1.0) with the shooter aimed down +Z, and returns the largest deviation of any
    // arrow's launch velocity from that aim, as 1 - cos(angle) so wider scatter reads larger.
    private double maxArrowDeviation(GameTestHelper helper, ItemStack bow, int shots) {
        ServerLevel level = helper.getLevel();
        ServerPlayer shooter = MockPlayers.serverPlayerInLevel(helper);
        shooter.setGameMode(GameType.SURVIVAL);
        BlockPos abs = helper.absolutePos(new BlockPos(1, 3, 1));
        shooter.moveTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5, 0.0f, 0.0f);
        shooter.setYHeadRot(0.0f);
        Vec3 aim = new Vec3(0.0, 0.0, 1.0);
        ProjectileWeaponItem weapon = (ProjectileWeaponItem) bow.getItem();

        double maxDeviation = 0.0;
        try {
            for (int i = 0; i < shots; i++) {
                weapon.shoot(level, shooter, InteractionHand.MAIN_HAND, bow,
                        List.of(new ItemStack(Items.ARROW)), 3.0f, 1.0f, false, null);
                AABB box = shooter.getBoundingBox().inflate(6.0);
                for (Arrow arrow : level.getEntitiesOfClass(Arrow.class, box)) {
                    Vec3 dir = arrow.getDeltaMovement().normalize();
                    double deviation = 1.0 - dir.dot(aim);
                    if (deviation > maxDeviation) maxDeviation = deviation;
                    arrow.discard();
                }
            }
        } finally {
            MockPlayers.retire(shooter);
        }
        return maxDeviation;
    }
}
