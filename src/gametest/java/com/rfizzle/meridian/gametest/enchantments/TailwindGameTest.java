package com.rfizzle.meridian.gametest.enchantments;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.gametest.MockPlayers;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/**
 * Tailwind (elytra): a firework rocket used mid-glide burns longer and pushes harder, scaling per
 * level. Both properties are exercised against a real {@link FireworkRocketEntity} attached to a
 * fall-flying mock player. A mock player is never ticked by a connection, so the fall-flying flag
 * we set by hand never clears — the firework's own {@code tick()} (driven directly) is all the
 * boost path needs.
 */
public class TailwindGameTest implements FabricGameTest {

    private Holder<Enchantment> lookup(GameTestHelper helper, String id) {
        Registry<Enchantment> reg = helper.getLevel().registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT);
        return reg.getHolder(Meridian.id(id)).orElse(null);
    }

    private static ServerPlayer glidingPlayer(GameTestHelper helper, BlockPos rel, ItemStack chest) {
        ServerPlayer player = MockPlayers.serverPlayerInLevel(helper);
        player.setGameMode(GameType.SURVIVAL);
        player.setItemSlot(EquipmentSlot.CHEST, chest);
        BlockPos abs = helper.absolutePos(rel);
        // Yaw/pitch 0 → a purely horizontal look, so the boost registers as horizontal speed.
        player.moveTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5, 0.0f, 0.0f);
        player.setYHeadRot(0.0f);
        player.setDeltaMovement(Vec3.ZERO);
        player.startFallFlying();
        return player;
    }

    // --- Tailwind: the mid-glide firework boost is measurably stronger with the enchantment ---

    @GameTest(template = "meridian:empty_3x3")
    public void tailwindBoostIsStrongerWhileGliding(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "tailwind");
        if (ench == null) { helper.fail("tailwind not in registry"); return; }
        ServerLevel level = helper.getLevel();

        ItemStack plainElytra = new ItemStack(Items.ELYTRA);
        ItemStack tailwindElytra = new ItemStack(Items.ELYTRA);
        tailwindElytra.enchant(ench, 3);

        ServerPlayer plain = glidingPlayer(helper, new BlockPos(0, 2, 1), plainElytra);
        ServerPlayer tailwind = glidingPlayer(helper, new BlockPos(2, 2, 1), tailwindElytra);

        FireworkRocketEntity plainRocket = new FireworkRocketEntity(
                level, new ItemStack(Items.FIREWORK_ROCKET), plain);
        FireworkRocketEntity tailwindRocket = new FireworkRocketEntity(
                level, new ItemStack(Items.FIREWORK_ROCKET), tailwind);
        level.addFreshEntity(plainRocket);
        level.addFreshEntity(tailwindRocket);

        // Drive a handful of boost ticks on each rocket. Both players glide with an identical look,
        // so vanilla's boost is the same for both — only Tailwind's extra push separates them.
        for (int i = 0; i < 5; i++) {
            if (!plainRocket.isRemoved()) plainRocket.tick();
            if (!tailwindRocket.isRemoved()) tailwindRocket.tick();
        }

        double plainSpeed = plain.getDeltaMovement().horizontalDistanceSqr();
        double tailwindSpeed = tailwind.getDeltaMovement().horizontalDistanceSqr();
        plain.discard();
        tailwind.discard();

        if (!(tailwindSpeed > plainSpeed + 0.05)) {
            helper.fail("Tailwind boost must be stronger than a plain firework boost "
                    + "(plainSpeedSqr=" + plainSpeed + ", tailwindSpeedSqr=" + tailwindSpeed + ")");
            return;
        }
        helper.succeed();
    }

    // --- Tailwind: the boost firework burns longer (extended lifetime) ---

    @GameTest(template = "meridian:empty_3x3")
    public void tailwindBoostBurnsLongerWhileGliding(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "tailwind");
        if (ench == null) { helper.fail("tailwind not in registry"); return; }
        ServerLevel level = helper.getLevel();

        ItemStack plainElytra = new ItemStack(Items.ELYTRA);
        ItemStack tailwindElytra = new ItemStack(Items.ELYTRA);
        tailwindElytra.enchant(ench, 3);

        ServerPlayer plain = glidingPlayer(helper, new BlockPos(0, 2, 1), plainElytra);
        ServerPlayer tailwind = glidingPlayer(helper, new BlockPos(2, 2, 1), tailwindElytra);

        FireworkRocketEntity plainRocket = new FireworkRocketEntity(
                level, new ItemStack(Items.FIREWORK_ROCKET), plain);
        FireworkRocketEntity tailwindRocket = new FireworkRocketEntity(
                level, new ItemStack(Items.FIREWORK_ROCKET), tailwind);

        int plainLifetime;
        int tailwindLifetime;
        try {
            java.lang.reflect.Field lifetime =
                    FireworkRocketEntity.class.getDeclaredField("lifetime");
            lifetime.setAccessible(true);
            plainLifetime = lifetime.getInt(plainRocket);
            tailwindLifetime = lifetime.getInt(tailwindRocket);
        } catch (ReflectiveOperationException e) {
            plain.discard();
            tailwind.discard();
            helper.fail("FireworkRocketEntity.lifetime not found — mapping changed? " + e);
            return;
        }
        plain.discard();
        tailwind.discard();

        // Vanilla's random lifetime spread is at most 11 ticks (nextInt(6) + nextInt(7)); Tailwind III
        // adds 30, so the enchanted rocket's lifetime always clears the plain one's, roll or no roll.
        if (tailwindLifetime <= plainLifetime) {
            helper.fail("Tailwind must extend the boost firework's lifetime "
                    + "(plain=" + plainLifetime + ", tailwind=" + tailwindLifetime + ")");
            return;
        }
        helper.succeed();
    }
}
