// Tier: 3 (Fabric Gametest)
package com.rfizzle.meridian.event;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.enchanting.DefenseEnchantMath;
import com.rfizzle.meridian.enchanting.TraversalEnchantMath;
import com.rfizzle.meridian.gametest.MockPlayers;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Field;

public class ArmorTickHandlerGameTest implements FabricGameTest {

    // Cinderwalk reverts obsidian -> lava only in the dimension that created it.
    // Regression guard for cross-dimension corruption (#7): a block tracked under one
    // dimension must never be reverted at the same coordinates in another dimension.
    @GameTest(template = "meridian:empty_3x3")
    public void cinderwalkRevertsOnlyInTrackedDimension(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));

        // Anchor tracking one tick past the revert threshold relative to the canonical clock
        // (server.overworld().getGameTime(), which revertCinderwalkBlocks compares against), so
        // the entries are always due no matter how many ticks elapsed before the test ran.
        long pastTick = server.overworld().getGameTime() - (ArmorTickHandler.CINDERWALK_REVERT_TICKS + 1L);

        // The reset is in a finally so the shared static cinderwalkBlocks map is cleaned up even when
        // a helper.fail() short-circuits the test — otherwise a failing run could leak tracked entries
        // into the next test's view of the map before the tick handler self-heals.
        try {
            // Positive case: tracked in THIS dimension past the threshold, so the obsidian reverts.
            ArmorTickHandler.cinderwalkResetForTest();
            level.setBlockAndUpdate(pos, Blocks.OBSIDIAN.defaultBlockState());
            ArmorTickHandler.cinderwalkTrackForTest(level.dimension(), pos, pastTick);
            ArmorTickHandler.revertCinderwalkBlocks(server);
            if (!level.getBlockState(pos).is(Blocks.LAVA)) {
                helper.fail("Cinderwalk block tracked in this dimension should revert to lava, got "
                        + level.getBlockState(pos));
                return;
            }

            // Isolation case: same coordinates, but tracked under a DIFFERENT dimension key.
            // The revert targets the other dimension, so this dimension's block is untouched.
            ResourceKey<Level> otherDimension =
                    level.dimension().equals(Level.OVERWORLD) ? Level.NETHER : Level.OVERWORLD;
            ArmorTickHandler.cinderwalkResetForTest();
            level.setBlockAndUpdate(pos, Blocks.OBSIDIAN.defaultBlockState());
            ArmorTickHandler.cinderwalkTrackForTest(otherDimension, pos, pastTick);
            ArmorTickHandler.revertCinderwalkBlocks(server);
            if (!level.getBlockState(pos).is(Blocks.OBSIDIAN)) {
                helper.fail("Cinderwalk block tracked in another dimension must not revert here, got "
                        + level.getBlockState(pos));
                return;
            }

            helper.succeed();
        } finally {
            ArmorTickHandler.cinderwalkResetForTest();
        }
    }

    private Holder<Enchantment> curse(GameTestHelper helper, String id) {
        Registry<Enchantment> reg = helper.getLevel().registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT);
        return reg.getHolder(Meridian.id(id)).orElse(null);
    }

    // Curse of Hunger: wearing it adds food exhaustion each tick, scaled by level; not wearing it
    // adds none. Exhaustion has no public getter, so read it reflectively off the live FoodData.
    @GameTest(template = "meridian:empty_3x3")
    public void curseOfHungerAddsExhaustionWhileWorn(GameTestHelper helper) throws Exception {
        Holder<Enchantment> ench = curse(helper, "curse_of_hunger");
        if (ench == null) { helper.fail("curse_of_hunger not in registry"); return; }

        ServerPlayer player = MockPlayers.serverPlayerInLevel(helper);
        try {
            // causeFoodExhaustion is a no-op for an invulnerable player; force it off so the
            // survival exhaustion path runs.
            player.getAbilities().invulnerable = false;
            player.onUpdateAbilities();

            Field exhaustionField = FoodData.class.getDeclaredField("exhaustionLevel");
            exhaustionField.setAccessible(true);

            // Not worn: exhaustion must not move.
            float baseline = exhaustionField.getFloat(player.getFoodData());
            ArmorTickHandler.handleCurseOfHunger(player);
            if (exhaustionField.getFloat(player.getFoodData()) != baseline) {
                helper.fail("Curse of Hunger must not add exhaustion when it is not worn");
                return;
            }

            // Worn at level 3: exhaustion grows by rate * level per call.
            ItemStack boots = new ItemStack(Items.DIAMOND_BOOTS);
            boots.enchant(ench, 3);
            player.setItemSlot(EquipmentSlot.FEET, boots);

            float before = exhaustionField.getFloat(player.getFoodData());
            ArmorTickHandler.handleCurseOfHunger(player);
            float after = exhaustionField.getFloat(player.getFoodData());

            float expectedDelta = ArmorTickHandler.CURSE_OF_HUNGER_EXHAUSTION_PER_LEVEL * 3;
            if (after - before < expectedDelta - 1.0e-4f) {
                helper.fail("Curse of Hunger III should add " + expectedDelta
                        + " exhaustion, got delta " + (after - before));
                return;
            }
            helper.succeed();
        } finally {
            player.discard();
        }
    }

    // Curse of Attraction: a nearby hostile with no target is pulled onto the wearer; without the
    // curse it is left alone.
    @GameTest(template = "meridian:empty_3x3")
    public void curseOfAttractionPullsHostilesOntoWearer(GameTestHelper helper) {
        Holder<Enchantment> ench = curse(helper, "curse_of_attraction");
        if (ench == null) { helper.fail("curse_of_attraction not in registry"); return; }

        Mob zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 1, 1));
        ServerPlayer player = MockPlayers.serverPlayerInLevel(helper);
        try {
            player.getAbilities().invulnerable = false;
            player.onUpdateAbilities();
            BlockPos abs = helper.absolutePos(new BlockPos(1, 1, 1));
            player.teleportTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5);

            if (zombie.getTarget() != null) {
                helper.fail("precondition: a freshly spawned zombie should have no target");
                return;
            }

            // Not worn: no forced target.
            ArmorTickHandler.handleCurseOfAttraction(player);
            if (zombie.getTarget() != null) {
                helper.fail("Curse of Attraction must not pull hostiles when it is not worn");
                return;
            }

            // Worn: the nearby zombie is pulled onto the wearer.
            ItemStack boots = new ItemStack(Items.DIAMOND_BOOTS);
            boots.enchant(ench, 1);
            player.setItemSlot(EquipmentSlot.FEET, boots);
            ArmorTickHandler.handleCurseOfAttraction(player);

            if (zombie.getTarget() != player) {
                helper.fail("Curse of Attraction should set the wearer as the zombie's target, got "
                        + zombie.getTarget());
                return;
            }
            helper.succeed();
        } finally {
            player.discard();
        }
    }

    // Curse of Molting: while gliding, the elytra sheds an extra burst of durability; standing
    // still it sheds none.
    @GameTest(template = "meridian:empty_3x3")
    public void curseOfMoltingShedsElytraDurabilityWhileGliding(GameTestHelper helper) {
        Holder<Enchantment> ench = curse(helper, "curse_of_molting");
        if (ench == null) { helper.fail("curse_of_molting not in registry"); return; }

        ServerPlayer player = MockPlayers.serverPlayerInLevel(helper);
        try {
            player.getAbilities().invulnerable = false;
            player.getAbilities().instabuild = false;
            player.onUpdateAbilities();

            ItemStack elytra = new ItemStack(Items.ELYTRA);
            elytra.enchant(ench, 1);
            elytra.setDamageValue(0);
            player.setItemSlot(EquipmentSlot.CHEST, elytra);

            // Not gliding: no shed.
            ArmorTickHandler.handleCurseOfMolting(player);
            if (elytra.getDamageValue() != 0) {
                helper.fail("Curse of Molting must not shed durability when the wearer is not gliding");
                return;
            }

            // Gliding: a burst of durability is shed.
            player.startFallFlying();
            ArmorTickHandler.handleCurseOfMolting(player);
            if (elytra.getDamageValue() < TraversalEnchantMath.MOLTING_SHED_DURABILITY) {
                helper.fail("Curse of Molting should shed at least " + TraversalEnchantMath.MOLTING_SHED_DURABILITY
                        + " durability while gliding, got " + elytra.getDamageValue());
                return;
            }
            helper.succeed();
        } finally {
            player.stopFallFlying();
            player.discard();
        }
    }

    // Bullrush: sprinting into a mob with an enchanted shield knocks it back, dazes it with
    // Slowness, and spends shield durability. Standing still (not sprinting) does nothing.
    @GameTest(template = "meridian:empty_3x3")
    public void bullrushBashKnocksBackAndDazesNearbyMob(GameTestHelper helper) {
        Holder<Enchantment> ench = curse(helper, "bullrush");
        if (ench == null) { helper.fail("bullrush not in registry"); return; }

        ServerPlayer player = MockPlayers.serverPlayerInLevel(helper);
        Mob zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 1, 1));
        try {
            player.getAbilities().instabuild = false;
            player.onUpdateAbilities();

            BlockPos abs = helper.absolutePos(new BlockPos(1, 1, 1));
            player.teleportTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5);
            // Half a block north of the player: inside the bash reach, and offset so the knockback
            // has a direction to push along (a mob dead-centre would get a zero-length impulse).
            zombie.moveTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 1.0, 0.0f, 0.0f);
            zombie.setDeltaMovement(Vec3.ZERO);

            ItemStack shield = new ItemStack(Items.SHIELD);
            shield.enchant(ench, 2);
            shield.setDamageValue(0);
            player.setItemSlot(EquipmentSlot.OFFHAND, shield);

            // Not sprinting: the charge never fires.
            player.setSprinting(false);
            ArmorTickHandler.handleBullrush(player);
            if (zombie.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) || shield.getDamageValue() != 0) {
                helper.fail("Bullrush must not bash while the wearer is not sprinting");
                return;
            }

            // Sprinting: the mob is dazed, shoved north (+z), and the shield takes durability.
            player.setSprinting(true);
            ArmorTickHandler.handleBullrush(player);

            if (!zombie.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) {
                helper.fail("Bullrush should daze the bashed mob with Slowness");
                return;
            }
            if (zombie.getDeltaMovement().z <= 0.0) {
                helper.fail("Bullrush should knock the mob away from the charge, got dz "
                        + zombie.getDeltaMovement().z);
                return;
            }
            if (shield.getDamageValue() < DefenseEnchantMath.BULLRUSH_DURABILITY_COST) {
                helper.fail("Bullrush should spend at least " + DefenseEnchantMath.BULLRUSH_DURABILITY_COST
                        + " shield durability per bash, got " + shield.getDamageValue());
                return;
            }
            helper.succeed();
        } finally {
            player.discard();
        }
    }

    // Regression (#221): with a Bullrush shield in each hand, only the resolved offhand shield is
    // charged — level and durability come from that one stack, never a cross-hand mix.
    @GameTest(template = "meridian:empty_3x3")
    public void bullrushChargesTheResolvedShieldNotBothHands(GameTestHelper helper) {
        Holder<Enchantment> ench = curse(helper, "bullrush");
        if (ench == null) { helper.fail("bullrush not in registry"); return; }

        ServerPlayer player = MockPlayers.serverPlayerInLevel(helper);
        Mob zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 1, 1));
        try {
            player.getAbilities().instabuild = false;
            player.onUpdateAbilities();

            BlockPos abs = helper.absolutePos(new BlockPos(1, 1, 1));
            player.teleportTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5);
            zombie.moveTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 1.0, 0.0f, 0.0f);
            zombie.setDeltaMovement(Vec3.ZERO);

            ItemStack offhandShield = new ItemStack(Items.SHIELD);
            offhandShield.enchant(ench, 2);
            offhandShield.setDamageValue(0);
            player.setItemSlot(EquipmentSlot.OFFHAND, offhandShield);

            ItemStack mainhandShield = new ItemStack(Items.SHIELD);
            mainhandShield.enchant(ench, 1);
            mainhandShield.setDamageValue(0);
            player.setItemSlot(EquipmentSlot.MAINHAND, mainhandShield);

            player.setSprinting(true);
            ArmorTickHandler.handleBullrush(player);

            if (offhandShield.getDamageValue() <= 0) {
                helper.fail("Bullrush should spend the resolved offhand shield's durability");
                return;
            }
            if (mainhandShield.getDamageValue() != 0) {
                helper.fail("Bullrush must not also charge the other hand's shield, got mainhand damage "
                        + mainhandShield.getDamageValue());
                return;
            }
            helper.succeed();
        } finally {
            player.discard();
        }
    }

    // Falconstrike: gliding into a mob damages it by the kinetic amount and the glider keeps flying,
    // bleeding only a slice of horizontal momentum. Not gliding does nothing.
    @GameTest(template = "meridian:empty_3x3")
    public void falconstrikeGlideStrikeDamagesMobAndPreservesGlide(GameTestHelper helper) {
        Holder<Enchantment> ench = curse(helper, "falconstrike");
        if (ench == null) { helper.fail("falconstrike not in registry"); return; }

        ServerPlayer player = MockPlayers.serverPlayerInLevel(helper);
        Mob zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 1, 1));
        try {
            player.getAbilities().invulnerable = false;
            player.onUpdateAbilities();

            BlockPos abs = helper.absolutePos(new BlockPos(1, 1, 1));
            player.teleportTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5);
            // Overlap the player so the strike AABB certainly catches the mob.
            zombie.moveTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5, 0.0f, 0.0f);
            float fullHealth = zombie.getMaxHealth();
            zombie.setHealth(fullHealth);
            zombie.invulnerableTime = 0;

            ItemStack elytra = new ItemStack(Items.ELYTRA);
            elytra.enchant(ench, 2);
            player.setItemSlot(EquipmentSlot.CHEST, elytra);

            // A cruising glide speed of 1.0 blocks/tick along +z, above the drift threshold.
            player.setDeltaMovement(0.0, -0.1, 1.0);

            // Not gliding: no strike.
            ArmorTickHandler.handleFalconstrike(player);
            if (zombie.getHealth() < fullHealth) {
                helper.fail("Falconstrike must not strike while the wearer is not gliding");
                return;
            }

            // Gliding: the mob takes kinetic damage and the glider keeps most of its momentum.
            player.startFallFlying();
            player.setDeltaMovement(0.0, -0.1, 1.0);
            ArmorTickHandler.handleFalconstrike(player);

            if (zombie.getHealth() >= fullHealth) {
                helper.fail("Falconstrike should damage the struck mob while gliding");
                return;
            }
            if (!player.isFallFlying()) {
                helper.fail("Falconstrike should preserve the glide, not halt it");
                return;
            }
            double retainedZ = player.getDeltaMovement().z;
            if (retainedZ <= 0.5 || retainedZ >= 1.0) {
                helper.fail("Falconstrike should bleed only a slice of horizontal momentum, got dz "
                        + retainedZ);
                return;
            }
            helper.succeed();
        } finally {
            player.stopFallFlying();
            player.discard();
        }
    }
}
