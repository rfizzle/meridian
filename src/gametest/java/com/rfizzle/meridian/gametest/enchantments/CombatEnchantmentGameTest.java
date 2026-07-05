package com.rfizzle.meridian.gametest.enchantments;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.enchanting.CombatEnchantMath;
import com.rfizzle.meridian.event.EnchantmentEffectHandler;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
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
}
