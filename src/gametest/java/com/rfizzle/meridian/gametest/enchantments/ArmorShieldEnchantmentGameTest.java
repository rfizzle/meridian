package com.rfizzle.meridian.gametest.enchantments;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.enchanting.DefenseEnchantMath;
import com.rfizzle.meridian.event.DecoyManager;
import com.rfizzle.meridian.gametest.util.MockPlayers;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Runtime coverage for the armor/shield bundle whose behavior lives in code rather than in the
 * enchantment definition: Everbloom's beneficial-duration extension (the mixin) and Decoy's
 * summoned body (the manager). Bastion's block detection rides the same {@code AFTER_DAMAGE}
 * {@code blocked} path proven by the Riposte real-block test, and Hush's injection is validated
 * by ServerLevel loading the mixin; their formulas are unit-tested in {@code DefenseEnchantMathTest}.
 */
public class ArmorShieldEnchantmentGameTest implements FabricGameTest {

    private Holder<Enchantment> lookup(GameTestHelper helper, String id) {
        Registry<Enchantment> reg = helper.getLevel().registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT);
        return reg.getHolder(Meridian.id(id)).orElse(null);
    }

    // --- Everbloom: extends beneficial durations, leaves harmful ones alone ---

    @GameTest(template = "meridian:empty_3x3")
    public void everbloomExtendsOnlyBeneficialDurations(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "everbloom");
        if (ench == null) { helper.fail("everbloom not in registry"); return; }

        // Everbloom rides EverbloomMixin on LivingEntity.addEffect and only reads the equipped
        // chest enchantment — no connection, player-list, or proximity — so the lighter non-placed
        // Player stub is sufficient (see mc-testing-mock).
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        ItemStack chest = new ItemStack(Items.DIAMOND_CHESTPLATE);
        chest.enchant(ench, 3);
        player.setItemSlot(EquipmentSlot.CHEST, chest);

        int base = 600;
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, base, 0, true, true, true));
        MobEffectInstance regen = player.getEffect(MobEffects.REGENERATION);
        int expected = DefenseEnchantMath.everbloomExtendedDuration(base, 3);
        if (regen == null || regen.getDuration() != expected) {
            helper.fail("Everbloom III should extend a " + base + "-tick beneficial effect to "
                    + expected + ", found " + (regen == null ? "none" : regen.getDuration()));
            player.discard();
            return;
        }

        // A harmful effect is untouched — Everbloom is the beneficial-only mirror of Antidote.
        player.addEffect(new MobEffectInstance(MobEffects.POISON, base, 0, true, true, true));
        MobEffectInstance poison = player.getEffect(MobEffects.POISON);
        if (poison == null || poison.getDuration() != base) {
            helper.fail("Everbloom must not touch harmful durations; poison expected " + base
                    + ", found " + (poison == null ? "none" : poison.getDuration()));
            player.discard();
            return;
        }

        player.discard();
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void everbloomWithoutEnchantLeavesDurationUnchanged(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        int base = 600;
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, base, 0, true, true, true));
        MobEffectInstance regen = player.getEffect(MobEffects.REGENERATION);
        if (regen == null || regen.getDuration() != base) {
            helper.fail("A bare chest should not extend durations; expected " + base
                    + ", found " + (regen == null ? "none" : regen.getDuration()));
            player.discard();
            return;
        }
        player.discard();
        helper.succeed();
    }

    // --- Decoy: the manager spawns a tracked body and taunts a nearby hostile ---

    @GameTest(template = "meridian:empty_3x3")
    public void decoyDeploysBodyAndExpires(GameTestHelper helper) {
        DecoyManager.clearForTest();
        ServerPlayer owner = MockPlayers.serverPlayerInLevel(helper);

        DecoyManager.deploy(helper.getLevel(), owner);

        if (DecoyManager.activeDecoyCountForTest() != 1) {
            helper.fail("Decoy deployment should track exactly one active decoy, found "
                    + DecoyManager.activeDecoyCountForTest());
            DecoyManager.clearForTest();
            MockPlayers.retire(owner);
            return;
        }

        AABB around = owner.getBoundingBox().inflate(4.0);
        List<ArmorStand> stands = helper.getLevel().getEntitiesOfClass(ArmorStand.class, around);
        if (stands.isEmpty()) {
            helper.fail("Decoy deployment should have spawned an armor-stand body near the owner");
            DecoyManager.clearForTest();
            MockPlayers.retire(owner);
            return;
        }

        // Clearing (as SERVER_STOPPED does) discards every tracked decoy.
        DecoyManager.clearForTest();
        if (DecoyManager.activeDecoyCountForTest() != 0) {
            helper.fail("Clearing decoys should leave none tracked, found "
                    + DecoyManager.activeDecoyCountForTest());
            MockPlayers.retire(owner);
            return;
        }

        MockPlayers.retire(owner);
        helper.succeed();
    }
}
