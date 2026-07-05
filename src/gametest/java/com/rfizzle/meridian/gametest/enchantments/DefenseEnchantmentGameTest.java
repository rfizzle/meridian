package com.rfizzle.meridian.gametest.enchantments;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.attachment.MeridianAttachments;
import com.rfizzle.meridian.enchanting.DefenseEnchantMath;
import com.rfizzle.meridian.event.LoftHandler;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Blocks;

public class DefenseEnchantmentGameTest implements FabricGameTest {

    private Holder<Enchantment> lookup(GameTestHelper helper, String id) {
        Registry<Enchantment> reg = helper.getLevel().registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT);
        return reg.getHolder(Meridian.id(id)).orElse(null);
    }

    private Zombie spawnWearing(GameTestHelper helper, EquipmentSlot slot, ItemStack gear) {
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 2, 1));
        zombie.setItemSlot(slot, gear);
        return zombie;
    }

    // --- Blink: fatal-hit rescue, once per game day, totem priority ---

    @GameTest(template = "meridian:empty_3x3")
    public void blinkCancelsFatalHitThenLocksOut(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "blink");
        if (ench == null) { helper.fail("blink not in registry"); return; }

        ItemStack chest = new ItemStack(Items.DIAMOND_CHESTPLATE);
        chest.enchant(ench, 1);
        Zombie wearer = spawnWearing(helper, EquipmentSlot.CHEST, chest);

        wearer.hurt(wearer.damageSources().generic(), 1000.0f);

        if (!wearer.isAlive()) {
            helper.fail("Blink should cancel an otherwise-fatal hit");
            return;
        }
        if (Math.abs(wearer.getHealth() - DefenseEnchantMath.BLINK_SURVIVAL_HEALTH) > 1e-4f) {
            helper.fail("Blink survivor should be left at " + DefenseEnchantMath.BLINK_SURVIVAL_HEALTH
                    + " health, found " + wearer.getHealth());
            return;
        }
        if (!wearer.hasEffect(MobEffects.WEAKNESS)) {
            helper.fail("Blink should apply Weakness to the survivor");
            return;
        }

        // Within the game-day lockout the next fatal hit goes through.
        wearer.invulnerableTime = 0;
        wearer.hurt(wearer.damageSources().generic(), 1000.0f);
        boolean lockedOut = !wearer.isAlive();
        wearer.discard();
        if (!lockedOut) {
            helper.fail("Blink must not fire twice inside its game-day cooldown");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void blinkDefersToHeldTotem(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "blink");
        if (ench == null) { helper.fail("blink not in registry"); return; }

        ItemStack chest = new ItemStack(Items.DIAMOND_CHESTPLATE);
        chest.enchant(ench, 1);
        Zombie wearer = spawnWearing(helper, EquipmentSlot.CHEST, chest);
        wearer.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.TOTEM_OF_UNDYING));

        wearer.hurt(wearer.damageSources().generic(), 1000.0f);

        boolean alive = wearer.isAlive();
        boolean totemConsumed = wearer.getOffhandItem().isEmpty();
        long blinkStamp = wearer.getAttachedOrElse(MeridianAttachments.BLINK_LAST_USED,
                DefenseEnchantMath.BLINK_NEVER_USED);
        wearer.discard();

        if (!alive) { helper.fail("Totem should have saved the wearer"); return; }
        if (!totemConsumed) { helper.fail("Vanilla totem should be the one to fire (and be consumed)"); return; }
        if (blinkStamp != DefenseEnchantMath.BLINK_NEVER_USED) {
            helper.fail("Blink must not fire on the same death event as a totem");
            return;
        }
        helper.succeed();
    }

    // --- Inexorable: slow-effect immunity, other effects untouched ---

    @GameTest(template = "meridian:empty_3x3")
    public void inexorableBlocksSlownessAndFatigueOnly(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "inexorable");
        if (ench == null) { helper.fail("inexorable not in registry"); return; }

        ItemStack boots = new ItemStack(Items.DIAMOND_BOOTS);
        boots.enchant(ench, 1);
        Zombie wearer = spawnWearing(helper, EquipmentSlot.FEET, boots);

        wearer.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 0));
        wearer.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 0));
        // Weakness as the control — harmful, and (unlike Poison) it affects undead.
        wearer.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));

        boolean slowed = wearer.hasEffect(MobEffects.MOVEMENT_SLOWDOWN);
        boolean fatigued = wearer.hasEffect(MobEffects.DIG_SLOWDOWN);
        boolean weakened = wearer.hasEffect(MobEffects.WEAKNESS);
        wearer.discard();

        if (slowed) { helper.fail("Inexorable should block Slowness"); return; }
        if (fatigued) { helper.fail("Inexorable should block Mining Fatigue"); return; }
        if (!weakened) { helper.fail("Inexorable must not block unrelated effects like Weakness"); return; }
        helper.succeed();
    }

    // --- Emberward: reactive Fire Resistance after fire/lava damage ---

    @GameTest(template = "meridian:empty_3x3")
    public void emberwardReactsToFireDamageOnly(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "emberward");
        if (ench == null) { helper.fail("emberward not in registry"); return; }

        ItemStack leggings = new ItemStack(Items.DIAMOND_LEGGINGS);
        leggings.enchant(ench, 1);
        Zombie wearer = spawnWearing(helper, EquipmentSlot.LEGS, leggings);

        wearer.hurt(wearer.damageSources().generic(), 1.0f);
        boolean fromGeneric = wearer.hasEffect(MobEffects.FIRE_RESISTANCE);

        wearer.invulnerableTime = 0;
        wearer.hurt(wearer.damageSources().inFire(), 1.0f);
        MobEffectInstance resistance = wearer.getEffect(MobEffects.FIRE_RESISTANCE);
        wearer.discard();

        if (fromGeneric) { helper.fail("Emberward must only react to fire/lava damage"); return; }
        if (resistance == null) { helper.fail("Emberward should grant Fire Resistance after fire damage"); return; }
        if (resistance.getDuration() > DefenseEnchantMath.EMBERWARD_FIRE_RES_TICKS) {
            helper.fail("Emberward's Fire Resistance should be a short burst, found "
                    + resistance.getDuration() + " ticks");
            return;
        }
        helper.succeed();
    }

    // --- Reprieve: extended post-hit invulnerability window ---

    @GameTest(template = "meridian:empty_3x3")
    public void reprieveExtendsInvulnerabilityWindow(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "reprieve");
        if (ench == null) { helper.fail("reprieve not in registry"); return; }

        Zombie control = spawnWearing(helper, EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
        control.hurt(control.damageSources().generic(), 1.0f);
        int vanillaWindow = control.invulnerableTime;
        control.discard();

        ItemStack helmet = new ItemStack(Items.DIAMOND_HELMET);
        helmet.enchant(ench, 2);
        Zombie wearer = spawnWearing(helper, EquipmentSlot.HEAD, helmet);
        wearer.hurt(wearer.damageSources().generic(), 1.0f);
        int reprieveWindow = wearer.invulnerableTime;
        wearer.discard();

        if (vanillaWindow != DefenseEnchantMath.VANILLA_HURT_INVULNERABILITY_TICKS) {
            helper.fail("Control zombie should have the vanilla 20-tick window, found " + vanillaWindow);
            return;
        }
        if (reprieveWindow != DefenseEnchantMath.reprieveInvulnerabilityTicks(2)) {
            helper.fail("Reprieve II should set a " + DefenseEnchantMath.reprieveInvulnerabilityTicks(2)
                    + "-tick window, found " + reprieveWindow);
            return;
        }
        helper.succeed();
    }

    // --- Loft: one mid-air jump per airtime, re-armed on landing ---

    @GameTest(template = "meridian:empty_3x3")
    public void loftGrantsExactlyOneAirJumpPerAirtime(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "loft");
        if (ench == null) { helper.fail("loft not in registry"); return; }

        // The re-arm check derives groundedness from real block collision, not from the
        // client-reported onGround flag — so give the player a real floor to land on,
        // placed through the helper so block and teleport targets share one transform
        // (the template's own floor does not line up with absolutePos in the test world).
        helper.setBlock(new BlockPos(1, 1, 1), Blocks.STONE);
        BlockPos onFloor = helper.absolutePos(new BlockPos(1, 2, 1));
        BlockPos inAir = helper.absolutePos(new BlockPos(1, 3, 1));

        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.teleportTo(inAir.getX() + 0.5, inAir.getY(), inAir.getZ() + 0.5);
        player.setOnGround(false);

        // Without the enchant, the request is rejected.
        if (LoftHandler.tryAirJump(player)) {
            helper.fail("Air jump must require Loft on the boots");
            player.discard();
            return;
        }

        ItemStack boots = new ItemStack(Items.DIAMOND_BOOTS);
        boots.enchant(ench, 1);
        player.setItemSlot(EquipmentSlot.FEET, boots);

        if (!LoftHandler.tryAirJump(player)) {
            helper.fail("Airborne Loft wearer should get a mid-air jump");
            player.discard();
            return;
        }
        if (Math.abs(player.getDeltaMovement().y - DefenseEnchantMath.LOFT_JUMP_VELOCITY) > 1e-6) {
            helper.fail("Air jump should set upward velocity " + DefenseEnchantMath.LOFT_JUMP_VELOCITY
                    + ", found " + player.getDeltaMovement().y);
            player.discard();
            return;
        }
        if (LoftHandler.tryAirJump(player)) {
            helper.fail("Loft grants exactly one mid-air jump per airtime");
            player.discard();
            return;
        }

        // Landing on the real floor re-arms the jump; while grounded no jump fires. The
        // second jump also has to wait out the anti-spam interval.
        player.teleportTo(onFloor.getX() + 0.5, onFloor.getY(), onFloor.getZ() + 0.5);
        LoftHandler.resetOnGround(player);
        if (LoftHandler.isAirJumpSpentForTest(player.getUUID())) {
            helper.fail("Landing on real ground should clear the air-jump budget (grounded="
                    + LoftHandler.isGroundedForTest(player) + ")");
            player.discard();
            return;
        }
        if (LoftHandler.tryAirJump(player)) {
            helper.fail("A collision-grounded player must not air jump");
            player.discard();
            return;
        }
        player.teleportTo(inAir.getX() + 0.5, inAir.getY(), inAir.getZ() + 0.5);
        player.setOnGround(false);

        helper.runAfterDelay(DefenseEnchantMath.LOFT_AIR_JUMP_MIN_INTERVAL_TICKS * 2L, () -> {
            boolean rearmed = LoftHandler.tryAirJump(player);
            player.discard();
            if (!rearmed) {
                helper.fail("Touching ground should re-arm the mid-air jump");
                return;
            }
            helper.succeed();
        });
    }

    @GameTest(template = "meridian:empty_3x3")
    public void loftRaisesSafeFallHeight(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "loft");
        if (ench == null) { helper.fail("loft not in registry"); return; }

        // 5-block fall: vanilla forgives 3 blocks -> 2 damage. Loft II forgives 3 more -> none.
        Zombie control = spawnWearing(helper, EquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS));
        float controlBefore = control.getHealth();
        control.causeFallDamage(5.0f, 1.0f, control.damageSources().fall());
        float controlDamage = controlBefore - control.getHealth();
        control.discard();

        ItemStack boots = new ItemStack(Items.DIAMOND_BOOTS);
        boots.enchant(ench, 2);
        Zombie wearer = spawnWearing(helper, EquipmentSlot.FEET, boots);
        float wearerBefore = wearer.getHealth();
        wearer.causeFallDamage(5.0f, 1.0f, wearer.damageSources().fall());
        float wearerDamage = wearerBefore - wearer.getHealth();
        wearer.discard();

        if (controlDamage <= 0.0f) {
            helper.fail("Control zombie should take damage from a 5-block fall");
            return;
        }
        if (wearerDamage != 0.0f) {
            helper.fail("Loft II should absorb a 5-block fall entirely, took " + wearerDamage);
            return;
        }
        helper.succeed();
    }
}
