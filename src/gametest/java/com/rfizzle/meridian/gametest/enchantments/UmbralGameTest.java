package com.rfizzle.meridian.gametest.enchantments;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.enchanting.EnchantmentEffects;
import com.rfizzle.meridian.enchanting.UmbralStealthMath;
import com.rfizzle.meridian.gametest.util.MockPlayers;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Blocks;

/**
 * Umbral cuts the visibility factor a hostile mob uses to acquire a new target — but only while the
 * wearer sneaks in darkness. The reduction is applied at {@code LivingEntity#getVisibilityPercent},
 * the exact factor {@code TargetingConditions#test} multiplies into its acquisition range, so a
 * smaller factor shrinks the spot distance; target retention reads raw distance and never calls this
 * method, so an already-acquired target is unaffected by construction.
 *
 * <p>The stealth decision (light threshold + per-level scaling) is a pure function proven
 * exhaustively in {@code UmbralStealthMathTest}; the gametest world cannot be driven below light
 * level 7, so these gametests instead pin down the in-world wiring that unit tests can't reach — that
 * the mixin is hooked into {@code getVisibilityPercent} and correctly reads the sneak state and the
 * light level (staying inert when either gate fails), and that Umbral coexists with Hush.
 */
public class UmbralGameTest implements FabricGameTest {

    /** Vanilla's own sneak (isDiscrete) factor in getVisibilityPercent — the baseline Umbral scales. */
    private static final double VANILLA_SNEAK_FACTOR = 0.8;
    private static final double EPSILON = 1.0e-4;

    private Holder<Enchantment> lookup(GameTestHelper helper, String id) {
        Registry<Enchantment> reg = helper.getLevel().registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT);
        return reg.getHolder(Meridian.id(id)).orElse(null);
    }

    private ItemStack umbralHelmet(Holder<Enchantment> ench, int level) {
        ItemStack helmet = new ItemStack(Items.DIAMOND_HELMET);
        helmet.enchant(ench, level);
        return helmet;
    }

    @GameTest(template = "meridian:empty_3x3")
    public void umbralInertWhenNotSneaking(GameTestHelper helper) {
        Holder<Enchantment> umbral = lookup(helper, "umbral");
        if (umbral == null) { helper.fail("umbral not in registry"); return; }

        ServerPlayer wearer = MockPlayers.serverPlayerInLevel(helper);
        BlockPos abs = helper.absolutePos(new BlockPos(1, 1, 1));
        wearer.teleportTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5);
        wearer.setItemSlot(EquipmentSlot.HEAD, umbralHelmet(umbral, 3));
        wearer.setShiftKeyDown(false);

        Zombie hostile = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 1, 1));
        double visibility = wearer.getVisibilityPercent(hostile);
        MockPlayers.retire(wearer);
        hostile.discard();

        // Standing (not discrete): the mixin returns before touching the factor, so it stays vanilla's 1.0.
        if (Math.abs(visibility - 1.0) > EPSILON) {
            helper.fail("Umbral must not reduce visibility while standing, got " + visibility);
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void umbralInertInBrightLight(GameTestHelper helper) {
        Holder<Enchantment> umbral = lookup(helper, "umbral");
        if (umbral == null) { helper.fail("umbral not in registry"); return; }

        // Guarantee a bright spot regardless of ambient light.
        helper.setBlock(new BlockPos(0, 1, 1), Blocks.GLOWSTONE);

        ServerPlayer wearer = MockPlayers.serverPlayerInLevel(helper);
        BlockPos abs = helper.absolutePos(new BlockPos(1, 1, 1));
        wearer.teleportTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5);
        wearer.setItemSlot(EquipmentSlot.HEAD, umbralHelmet(umbral, 3));
        wearer.setShiftKeyDown(true);

        Zombie hostile = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 1, 1));

        helper.runAfterDelay(5, () -> {
            int brightness = helper.getLevel().getMaxLocalRawBrightness(wearer.blockPosition());
            double visibility = wearer.getVisibilityPercent(hostile);
            MockPlayers.retire(wearer);
            hostile.discard();

            if (brightness <= UmbralStealthMath.MAX_LIGHT_LEVEL) {
                helper.fail("precondition: spot should be brightly lit, brightness=" + brightness);
                return;
            }
            // Sneaking but lit: only vanilla's sneak factor applies, Umbral stays inert.
            if (Math.abs(visibility - VANILLA_SNEAK_FACTOR) > EPSILON) {
                helper.fail("Umbral must not reduce visibility in bright light, got " + visibility);
                return;
            }
            helper.succeed();
        });
    }

    @GameTest(template = "meridian:empty_3x3")
    public void umbralAndHushCoexistOnSeparateSlots(GameTestHelper helper) {
        Holder<Enchantment> umbral = lookup(helper, "umbral");
        Holder<Enchantment> hush = lookup(helper, "hush");
        if (umbral == null) { helper.fail("umbral not in registry"); return; }
        if (hush == null) { helper.fail("hush not in registry"); return; }

        ServerPlayer wearer = MockPlayers.serverPlayerInLevel(helper);
        BlockPos abs = helper.absolutePos(new BlockPos(1, 1, 1));
        wearer.teleportTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5);
        wearer.setItemSlot(EquipmentSlot.HEAD, umbralHelmet(umbral, 2));
        ItemStack hushBoots = new ItemStack(Items.DIAMOND_BOOTS);
        hushBoots.enchant(hush, 1);
        wearer.setItemSlot(EquipmentSlot.FEET, hushBoots);

        // The two hook disjoint systems (head-slot sight vs feet-slot sculk vibrations); worn together,
        // each is independently detected on its own slot with no exclusivity clash.
        int umbralLevel = EnchantmentEffects.getEquippedLevel(wearer, EnchantmentEffects.UMBRAL, EquipmentSlot.HEAD);
        int hushLevel = EnchantmentEffects.getEquippedLevel(wearer, EnchantmentEffects.HUSH, EquipmentSlot.FEET);
        MockPlayers.retire(wearer);

        if (umbralLevel != 2) { helper.fail("Umbral should read level 2 on the head, got " + umbralLevel); return; }
        if (hushLevel != 1) { helper.fail("Hush should read level 1 on the feet, got " + hushLevel); return; }
        helper.succeed();
    }
}
