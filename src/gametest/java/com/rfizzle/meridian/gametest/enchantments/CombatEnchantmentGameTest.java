package com.rfizzle.meridian.gametest.enchantments;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.enchanting.CombatEnchantMath;
import com.rfizzle.meridian.event.EnchantmentEffectHandler;
import com.rfizzle.meridian.mixin.LivingEntityCombatAccessor;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.GameType;

import java.util.List;

public class CombatEnchantmentGameTest implements FabricGameTest {

    private Holder<Enchantment> lookup(GameTestHelper helper, String id) {
        Registry<Enchantment> reg = helper.getLevel().registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT);
        return reg.getHolder(Meridian.id(id)).orElse(null);
    }

    private static void clearEquipment(Mob mob) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            mob.setItemSlot(slot, ItemStack.EMPTY);
        }
    }

    /**
     * Clears the victim's post-hit invulnerability window and {@code lastHurt} threshold.
     * Ambient chip damage in the shared test arena (spawn falls, neighboring-test splash)
     * otherwise leaves a live window that docks the next measured hit by the vanilla
     * partial-damage rule, making loss comparisons off by the chip amount.
     */
    private static void resetHurtState(Mob mob) {
        mob.invulnerableTime = 0;
        ((LivingEntityCombatAccessor) mob).meridian$setLastHurt(0.0f);
    }

    // --- Ambush: full-health opener out-damages an identical plain hit ---

    @GameTest(template = "meridian:empty_3x3")
    public void ambushDealsBonusDamageToFullHealthTarget(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "ambush");
        if (ench == null) { helper.fail("ambush not in registry"); return; }

        Mob attacker = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(0, 1, 1));
        clearEquipment(attacker);
        ItemStack ambushSword = new ItemStack(Items.DIAMOND_SWORD);
        ambushSword.enchant(ench, 4);
        attacker.setItemSlot(EquipmentSlot.MAINHAND, ambushSword);

        Mob controlAttacker = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(2, 1, 1));
        clearEquipment(controlAttacker);
        controlAttacker.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));

        // High-HP, knockback-immune targets: a low-HP victim could cap the measurable
        // bonus at its death overkill and make the margin depend on tick timing.
        Mob target = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new BlockPos(0, 1, 2));
        Mob controlTarget = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new BlockPos(2, 1, 2));
        float startHealth = target.getHealth();
        float controlStartHealth = controlTarget.getHealth();

        attacker.doHurtTarget(target);
        controlAttacker.doHurtTarget(controlTarget);

        helper.runAfterDelay(2, () -> {
            float ambushLoss = startHealth - target.getHealth();
            float plainLoss = controlStartHealth - controlTarget.getHealth();
            float expectedBonus = CombatEnchantMath.ambushBonusDamage(4, 1.0f);
            if (ambushLoss < plainLoss + expectedBonus - 1.0f) {
                helper.fail("Ambush IV vs a full-health target should add ~" + expectedBonus
                        + " damage over a plain hit. ambush=" + ambushLoss + ", plain=" + plainLoss);
                return;
            }
            helper.succeed();
        });
    }

    // --- Pinpoint: the crit hook applies exactly the flat bonus ---

    @GameTest(template = "meridian:empty_3x3")
    public void pinpointCritHookAppliesFlatBonus(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "pinpoint");
        if (ench == null) { helper.fail("pinpoint not in registry"); return; }

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.enchant(ench, 4);
        player.setItemSlot(EquipmentSlot.MAINHAND, sword);

        Mob target = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(1, 1, 1));
        float startHealth = target.getHealth();

        EnchantmentEffectHandler.handlePinpointCrit(player, target);

        float loss = startHealth - target.getHealth();
        float expected = CombatEnchantMath.pinpointBonusDamage(4);
        player.discard();
        if (Math.abs(loss - expected) > 0.01f) {
            helper.fail("Pinpoint IV crit hook should deal " + expected + " to an unarmored target, dealt " + loss);
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void pinpointHookIsInertWithoutEnchant(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));

        Mob target = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(1, 1, 1));
        float startHealth = target.getHealth();

        EnchantmentEffectHandler.handlePinpointCrit(player, target);

        float health = target.getHealth();
        player.discard();
        if (health != startHealth) {
            helper.fail("Pinpoint hook must not damage the target when the weapon lacks the enchant");
            return;
        }
        helper.succeed();
    }

    // --- Sunder: the strip removes exactly one piece and drops it, recoverable ---

    @GameTest(template = "meridian:empty_3x3")
    public void sunderStripRemovesAndDropsEquipment(GameTestHelper helper) {
        Mob victim = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 1, 1));
        clearEquipment(victim);
        victim.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));

        EquipmentSlot stripped = EnchantmentEffectHandler.sunderStrip(victim);

        if (stripped != EquipmentSlot.HEAD) {
            helper.fail("Sunder should strip the only occupied slot (HEAD), got " + stripped);
            return;
        }
        if (!victim.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
            helper.fail("Stripped slot should be empty on the victim");
            return;
        }
        List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                victim.getBoundingBox().inflate(4.0),
                item -> item.getItem().is(Items.IRON_HELMET));
        if (drops.isEmpty()) {
            helper.fail("Stripped equipment should drop as a recoverable item entity");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void sunderStripOnBareVictimDoesNothing(GameTestHelper helper) {
        Mob victim = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(1, 1, 1));
        clearEquipment(victim);
        if (EnchantmentEffectHandler.sunderStrip(victim) != null) {
            helper.fail("Sunder strip on an unequipped victim should return null");
            return;
        }
        helper.succeed();
    }

    // --- Sunder: mobs-only default (the enabled path is covered by SunderConfigGameTest) ---

    @GameTest(template = "meridian:empty_3x3")
    public void sunderIgnoresPlayersByDefault(GameTestHelper helper) {
        Mob mob = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 1, 1));
        if (!EnchantmentEffectHandler.sunderVictimAllowed(mob)) {
            helper.fail("Mobs must always be eligible Sunder victims");
            return;
        }
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        boolean allowed = EnchantmentEffectHandler.sunderVictimAllowed(player);
        player.discard();
        if (allowed) {
            helper.fail("Players must not be eligible Sunder victims with the default config");
            return;
        }
        helper.succeed();
    }

    // --- Trophy: per-victim head mapping ---

    @GameTest(template = "meridian:empty_3x3")
    public void trophyHeadMatchesVictimType(GameTestHelper helper) {
        Mob zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(0, 1, 0));
        Mob creeper = helper.spawnWithNoFreeWill(EntityType.CREEPER, new BlockPos(2, 1, 0));
        Mob cow = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(1, 1, 2));

        if (!EnchantmentEffectHandler.trophyHeadFor(zombie).is(Items.ZOMBIE_HEAD)) {
            helper.fail("Zombie trophy should be a zombie head");
            return;
        }
        if (!EnchantmentEffectHandler.trophyHeadFor(creeper).is(Items.CREEPER_HEAD)) {
            helper.fail("Creeper trophy should be a creeper head");
            return;
        }
        if (!EnchantmentEffectHandler.trophyHeadFor(cow).isEmpty()) {
            helper.fail("A mob without a head item must yield no trophy");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void trophyPlayerHeadCarriesOwnerProfile(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack head = EnchantmentEffectHandler.trophyHeadFor(player);
        boolean ok = head.is(Items.PLAYER_HEAD) && head.get(DataComponents.PROFILE) != null;
        player.discard();
        if (!ok) {
            helper.fail("Player trophy should be a player head carrying the victim's profile");
            return;
        }
        helper.succeed();
    }

    // --- Definitions: item eligibility follows the issue's item lists ---

    @GameTest(template = "meridian:empty_3x3")
    public void combatEnchantsApplyToTheirItemSets(GameTestHelper helper) {
        Holder<Enchantment> ambush = lookup(helper, "ambush");
        Holder<Enchantment> pinpoint = lookup(helper, "pinpoint");
        Holder<Enchantment> sunder = lookup(helper, "sunder");
        Holder<Enchantment> trophy = lookup(helper, "trophy");
        Holder<Enchantment> fortuity = lookup(helper, "fortuity");
        if (ambush == null || pinpoint == null || sunder == null || trophy == null || fortuity == null) {
            helper.fail("combat enchantments missing from registry");
            return;
        }

        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        ItemStack axe = new ItemStack(Items.DIAMOND_AXE);
        ItemStack mace = new ItemStack(Items.MACE);
        ItemStack shield = new ItemStack(Items.SHIELD);

        for (var entry : List.of(
                java.util.Map.entry("ambush", ambush),
                java.util.Map.entry("trophy", trophy),
                java.util.Map.entry("fortuity", fortuity))) {
            Enchantment e = entry.getValue().value();
            if (!e.canEnchant(sword) || !e.canEnchant(axe)) {
                helper.fail(entry.getKey() + " should apply to swords and axes");
                return;
            }
            if (e.canEnchant(mace)) {
                helper.fail(entry.getKey() + " should NOT apply to a mace");
                return;
            }
        }

        for (var entry : List.of(
                java.util.Map.entry("pinpoint", pinpoint),
                java.util.Map.entry("sunder", sunder))) {
            Enchantment e = entry.getValue().value();
            if (!e.canEnchant(sword) || !e.canEnchant(axe) || !e.canEnchant(mace)) {
                helper.fail(entry.getKey() + " should apply to swords, axes, and maces");
                return;
            }
            if (e.canEnchant(shield)) {
                helper.fail(entry.getKey() + " should NOT apply to a shield");
                return;
            }
        }
        helper.succeed();
    }

    // --- Exclusive sets: damage line, loot bonus, trophy pairs ---

    @GameTest(template = "meridian:empty_3x3")
    public void combatEnchantExclusiveSetsAreEnforced(GameTestHelper helper) {
        Holder<Enchantment> ambush = lookup(helper, "ambush");
        Holder<Enchantment> pinpoint = lookup(helper, "pinpoint");
        Holder<Enchantment> keenEdge = lookup(helper, "keen_edge");
        Holder<Enchantment> fortuity = lookup(helper, "fortuity");
        Holder<Enchantment> plunder = lookup(helper, "plunder");
        Holder<Enchantment> trophy = lookup(helper, "trophy");
        Holder<Enchantment> snare = lookup(helper, "snare");
        if (ambush == null || pinpoint == null || keenEdge == null || fortuity == null
                || plunder == null || trophy == null || snare == null) {
            helper.fail("enchantments missing from registry");
            return;
        }

        record Pair(String name, Holder<Enchantment> a, Holder<Enchantment> b) {}
        for (Pair pair : List.of(
                new Pair("ambush x keen_edge (damage)", ambush, keenEdge),
                new Pair("ambush x pinpoint (damage)", ambush, pinpoint),
                new Pair("fortuity x plunder (loot_bonus)", fortuity, plunder),
                new Pair("trophy x snare (trophy)", trophy, snare))) {
            if (Enchantment.areCompatible(pair.a(), pair.b())) {
                helper.fail("Exclusive pair should be incompatible: " + pair.name());
                return;
            }
        }

        if (!Enchantment.areCompatible(fortuity, trophy)) {
            helper.fail("fortuity and trophy are in different sets and should be compatible");
            return;
        }
        helper.succeed();
    }

    // --- Crescendo: consecutive hits on the same target ramp; switching resets ---

    @GameTest(template = "meridian:empty_3x3")
    public void crescendoSecondHitOutdamagesFirst(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "crescendo");
        if (ench == null) { helper.fail("crescendo not in registry"); return; }

        Mob attacker = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 1, 0));
        clearEquipment(attacker);
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.enchant(ench, 3);
        attacker.setItemSlot(EquipmentSlot.MAINHAND, sword);

        Mob target = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new BlockPos(1, 1, 2));

        // Delay the opening hit: the sword's attribute modifiers only apply once the
        // attacker has ticked, and the target's spawn fall damage leaves a vanilla
        // invulnerability window that would dock the first measured hit via lastHurt.
        helper.runAfterDelay(25, () -> {
            resetHurtState(target);
            float startHealth = target.getHealth();
            attacker.doHurtTarget(target);
            float firstLoss = startHealth - target.getHealth();

            // A later tick, well inside Crescendo's timeout.
            helper.runAfterDelay(15, () -> {
                resetHurtState(target);
                float beforeSecond = target.getHealth();
                attacker.doHurtTarget(target);
                float secondLoss = beforeSecond - target.getHealth();

                float expectedBonus = CombatEnchantMath.crescendoBonusDamage(3, 1);
                if (firstLoss <= 0) {
                    helper.fail("Opening hit should land damage, lost " + firstLoss);
                    return;
                }
                if (secondLoss < firstLoss + expectedBonus - 0.01f) {
                    helper.fail("Crescendo's second consecutive hit should add ~" + expectedBonus
                            + " over the opener. first=" + firstLoss + ", second=" + secondLoss);
                    return;
                }
                helper.succeed();
            });
        });
    }

    @GameTest(template = "meridian:empty_3x3")
    public void crescendoResetsOnTargetSwitch(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "crescendo");
        if (ench == null) { helper.fail("crescendo not in registry"); return; }

        Mob attacker = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 1, 0));
        clearEquipment(attacker);
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.enchant(ench, 3);
        attacker.setItemSlot(EquipmentSlot.MAINHAND, sword);

        Mob first = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new BlockPos(0, 1, 2));
        Mob second = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new BlockPos(2, 1, 2));

        // Delay the opening hit: the sword's attribute modifiers only apply once the
        // attacker has ticked, and the target's spawn fall damage leaves a vanilla
        // invulnerability window that would dock the first measured hit via lastHurt.
        helper.runAfterDelay(25, () -> {
            resetHurtState(first);
            float firstStart = first.getHealth();
            attacker.doHurtTarget(first);
            float firstLoss = firstStart - first.getHealth();

            helper.runAfterDelay(15, () -> {
                resetHurtState(second);
                float secondStart = second.getHealth();
                attacker.doHurtTarget(second);
                float switchedLoss = secondStart - second.getHealth();

                if (switchedLoss > firstLoss + 0.01f) {
                    helper.fail("Switching targets should restart the ramp: both hits are streak"
                            + " openers. first=" + firstLoss + ", switched=" + switchedLoss);
                    return;
                }
                helper.succeed();
            });
        });
    }

    @GameTest(template = "meridian:empty_3x3", timeoutTicks = 150)
    public void crescendoTimeoutResetsStreak(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "crescendo");
        if (ench == null) { helper.fail("crescendo not in registry"); return; }

        Mob attacker = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 1, 0));
        clearEquipment(attacker);
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.enchant(ench, 3);
        attacker.setItemSlot(EquipmentSlot.MAINHAND, sword);

        Mob target = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new BlockPos(1, 1, 2));

        helper.runAfterDelay(25, () -> {
            resetHurtState(target);
            float startHealth = target.getHealth();
            attacker.doHurtTarget(target);
            float firstLoss = startHealth - target.getHealth();

            // Pause past the streak timeout: the next hit must be a bonus-free opener again.
            helper.runAfterDelay(CombatEnchantMath.CRESCENDO_TIMEOUT_TICKS + 5, () -> {
                resetHurtState(target);
                float beforeSecond = target.getHealth();
                attacker.doHurtTarget(target);
                float secondLoss = beforeSecond - target.getHealth();

                if (secondLoss > firstLoss + 0.01f) {
                    helper.fail("A hit after the timeout should restart the ramp with no bonus."
                            + " first=" + firstLoss + ", afterTimeout=" + secondLoss);
                    return;
                }
                helper.succeed();
            });
        });
    }

    // --- Riposte: post-block window grants one bonus hit, then is consumed ---

    @GameTest(template = "meridian:empty_3x3")
    public void riposteRealShieldBlockArmsWindow(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "riposte");
        if (ench == null) { helper.fail("riposte not in registry"); return; }

        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.enchant(ench, 3);
        player.setItemSlot(EquipmentSlot.MAINHAND, sword);
        player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));

        // Face north toward the attacker — a shield only blocks hits from the front,
        // and the block check reads the HEAD rotation, which moveTo does not set.
        BlockPos playerPos = helper.absolutePos(new BlockPos(1, 1, 2));
        player.moveTo(playerPos.getX() + 0.5, playerPos.getY(), playerPos.getZ() + 0.5, 180.0f, 0.0f);
        player.setYHeadRot(180.0f);
        player.startUsingItem(InteractionHand.OFF_HAND);

        // A mock player is never ticked by a connection, so the shield's 5-tick raise
        // delay and the 60-tick join invulnerability would both hold forever; age both
        // by hand (neither field has an accessor).
        player.invulnerableTime = 0;
        try {
            java.lang.reflect.Field useItemRemaining =
                    net.minecraft.world.entity.LivingEntity.class.getDeclaredField("useItemRemaining");
            useItemRemaining.setAccessible(true);
            useItemRemaining.setInt(player, player.getUseItem().getUseDuration(player) - 10);
            java.lang.reflect.Field spawnInvulnerableTime =
                    ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");
            spawnInvulnerableTime.setAccessible(true);
            spawnInvulnerableTime.setInt(player, 0);
        } catch (ReflectiveOperationException e) {
            player.discard();
            helper.fail("useItemRemaining/spawnInvulnerableTime not found — mapping changed? " + e);
            return;
        }

        Mob attacker = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 1, 0));
        clearEquipment(attacker);

        if (EnchantmentEffectHandler.hasRiposteWindow(player)) {
            player.discard();
            helper.fail("The window must not be armed before any block");
            return;
        }
        if (!player.isBlocking()) {
            player.discard();
            helper.fail("test setup: the mock player never raised its shield");
            return;
        }
        attacker.doHurtTarget(player);
        boolean armed = EnchantmentEffectHandler.hasRiposteWindow(player);
        player.discard();
        if (!armed) {
            helper.fail("A real shield block must arm the Riposte window");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void riposteWindowGrantsBonusOnceThenIsConsumed(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "riposte");
        if (ench == null) { helper.fail("riposte not in registry"); return; }

        Mob attacker = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 1, 0));
        clearEquipment(attacker);
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.enchant(ench, 3);
        attacker.setItemSlot(EquipmentSlot.MAINHAND, sword);

        Mob target = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new BlockPos(1, 1, 2));

        // Delay the opening hit: the sword's attribute modifiers only apply once the
        // attacker has ticked, and the target's spawn fall damage leaves a vanilla
        // invulnerability window that would dock the first measured hit via lastHurt.
        helper.runAfterDelay(25, () -> {
            resetHurtState(target);
            float startHealth = target.getHealth();
            EnchantmentEffectHandler.recordRiposteBlock(attacker);
            attacker.doHurtTarget(target);
            float riposteLoss = startHealth - target.getHealth();

            helper.runAfterDelay(15, () -> {
                resetHurtState(target);
                float beforePlain = target.getHealth();
                attacker.doHurtTarget(target);
                float plainLoss = beforePlain - target.getHealth();

                float expectedBonus = CombatEnchantMath.riposteBonusDamage(3);
                if (riposteLoss < plainLoss + expectedBonus - 0.01f) {
                    helper.fail("The first hit inside the window should carry ~" + expectedBonus
                            + " bonus. riposte=" + riposteLoss + ", follow-up=" + plainLoss);
                    return;
                }
                helper.succeed();
            });
        });
    }

    @GameTest(template = "meridian:empty_3x3")
    public void riposteExpiredWindowGrantsNoBonus(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "riposte");
        if (ench == null) { helper.fail("riposte not in registry"); return; }

        Mob attacker = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 1, 0));
        clearEquipment(attacker);
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.enchant(ench, 3);
        attacker.setItemSlot(EquipmentSlot.MAINHAND, sword);

        Mob target = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new BlockPos(1, 1, 2));

        EnchantmentEffectHandler.recordRiposteBlock(attacker);

        helper.runAfterDelay(CombatEnchantMath.RIPOSTE_WINDOW_TICKS + 5, () -> {
            resetHurtState(target);
            float startHealth = target.getHealth();
            attacker.doHurtTarget(target);
            float expiredLoss = startHealth - target.getHealth();

            helper.runAfterDelay(15, () -> {
                resetHurtState(target);
                float baselineStart = target.getHealth();
                attacker.doHurtTarget(target);
                float baselineLoss = baselineStart - target.getHealth();

                if (expiredLoss > baselineLoss + 0.01f) {
                    helper.fail("A hit after the window expired must not carry the bonus."
                            + " expired=" + expiredLoss + ", baseline=" + baselineLoss);
                    return;
                }
                helper.succeed();
            });
        });
    }

    // --- Joust: bonus only while mounted and moving, scaled by mount speed ---

    @GameTest(template = "meridian:empty_3x3")
    public void joustScalesWithMountSpeedAndIsInertWhenStationary(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "joust");
        if (ench == null) { helper.fail("joust not in registry"); return; }

        Mob horse = helper.spawnWithNoFreeWill(EntityType.HORSE, new BlockPos(1, 1, 0));
        Mob rider = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 1, 0));
        clearEquipment(rider);
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.enchant(ench, 3);
        rider.setItemSlot(EquipmentSlot.MAINHAND, sword);
        if (!rider.startRiding(horse, true)) {
            helper.fail("rider failed to mount the horse");
            return;
        }

        Mob target = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new BlockPos(1, 1, 2));

        // Delay the opening hit: the sword's attribute modifiers only apply once the
        // attacker has ticked, and the target's spawn fall damage leaves a vanilla
        // invulnerability window that would dock the first measured hit via lastHurt.
        helper.runAfterDelay(25, () -> {
            resetHurtState(target);
            float startHealth = target.getHealth();

            // Stationary mount (an idle AI-less horse has zero per-tick displacement): no bonus.
            rider.doHurtTarget(target);
            float stationaryLoss = startHealth - target.getHealth();

            helper.runAfterDelay(15, () -> {
                // Charging mount: the handler reads the mount's horizontal displacement
                // this tick, so a position move — how a real player-ridden mount travels
                // server-side — is the honest way to simulate the charge.
                double speed = 0.5;
                resetHurtState(target);
                float beforeCharging = target.getHealth();
                horse.setPos(horse.getX() + speed, horse.getY(), horse.getZ());
                rider.doHurtTarget(target);
                float chargingLoss = beforeCharging - target.getHealth();

                float expectedBonus = CombatEnchantMath.joustBonusDamage(3, speed);
                if (expectedBonus <= 0) {
                    helper.fail("test setup: expected a positive Joust bonus at speed " + speed);
                    return;
                }
                if (chargingLoss < stationaryLoss + expectedBonus - 0.01f) {
                    helper.fail("Joust III at mount speed " + speed + " should add ~" + expectedBonus
                            + " over a stationary hit. stationary=" + stationaryLoss
                            + ", charging=" + chargingLoss);
                    return;
                }
                helper.succeed();
            });
        });
    }

    @GameTest(template = "meridian:empty_3x3")
    public void joustGrantsNothingWhileDismounted(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "joust");
        if (ench == null) { helper.fail("joust not in registry"); return; }

        Mob attacker = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(0, 1, 1));
        clearEquipment(attacker);
        ItemStack joustSword = new ItemStack(Items.DIAMOND_SWORD);
        joustSword.enchant(ench, 3);
        attacker.setItemSlot(EquipmentSlot.MAINHAND, joustSword);

        Mob controlAttacker = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(2, 1, 1));
        clearEquipment(controlAttacker);
        controlAttacker.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));

        Mob target = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new BlockPos(0, 1, 2));
        Mob controlTarget = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new BlockPos(2, 1, 2));

        helper.runAfterDelay(25, () -> {
            resetHurtState(target);
            resetHurtState(controlTarget);
            float startHealth = target.getHealth();
            float controlStartHealth = controlTarget.getHealth();

            attacker.doHurtTarget(target);
            controlAttacker.doHurtTarget(controlTarget);

            float joustLoss = startHealth - target.getHealth();
            float plainLoss = controlStartHealth - controlTarget.getHealth();
            if (joustLoss > plainLoss + 0.01f) {
                helper.fail("A dismounted Joust hit must match a plain hit. joust=" + joustLoss
                        + ", plain=" + plainLoss);
                return;
            }
            helper.succeed();
        });
    }

    // --- Definitions: the new enchants follow the issue's item lists ---

    @GameTest(template = "meridian:empty_3x3")
    public void newCombatEnchantsApplyToTheirItemSets(GameTestHelper helper) {
        Holder<Enchantment> crescendo = lookup(helper, "crescendo");
        Holder<Enchantment> riposte = lookup(helper, "riposte");
        Holder<Enchantment> joust = lookup(helper, "joust");
        if (crescendo == null || riposte == null || joust == null) {
            helper.fail("new combat enchantments missing from registry");
            return;
        }

        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        ItemStack axe = new ItemStack(Items.DIAMOND_AXE);
        ItemStack mace = new ItemStack(Items.MACE);

        Enchantment c = crescendo.value();
        if (!c.canEnchant(sword) || !c.canEnchant(mace) || c.canEnchant(axe)) {
            helper.fail("crescendo should apply to swords and maces, not axes");
            return;
        }
        Enchantment r = riposte.value();
        if (!r.canEnchant(sword) || r.canEnchant(axe) || r.canEnchant(mace)) {
            helper.fail("riposte should apply to swords only");
            return;
        }
        Enchantment j = joust.value();
        if (!j.canEnchant(sword) || !j.canEnchant(axe) || j.canEnchant(mace)) {
            helper.fail("joust should apply to swords and axes, not maces");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void newCombatEnchantExclusiveSetsAreEnforced(GameTestHelper helper) {
        Holder<Enchantment> crescendo = lookup(helper, "crescendo");
        Holder<Enchantment> riposte = lookup(helper, "riposte");
        Holder<Enchantment> joust = lookup(helper, "joust");
        Holder<Enchantment> keenEdge = lookup(helper, "keen_edge");
        Holder<Enchantment> ambush = lookup(helper, "ambush");
        if (crescendo == null || riposte == null || joust == null || keenEdge == null || ambush == null) {
            helper.fail("enchantments missing from registry");
            return;
        }

        if (Enchantment.areCompatible(crescendo, keenEdge)
                || Enchantment.areCompatible(crescendo, ambush)) {
            helper.fail("crescendo is in the damage set and must exclude keen_edge and ambush");
            return;
        }
        if (!Enchantment.areCompatible(riposte, crescendo)
                || !Enchantment.areCompatible(joust, keenEdge)
                || !Enchantment.areCompatible(riposte, joust)) {
            helper.fail("riposte and joust are set-free and must coexist with the damage line");
            return;
        }
        helper.succeed();
    }
}
