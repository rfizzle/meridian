package com.rfizzle.meridian.gametest.enchantments;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.enchanting.TraversalEnchantMath;
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

import java.util.UUID;

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

    // --- Curse of Molting: a share of firework boosts fizzle (the rocket is discarded on its first
    // gliding tick before any push); a plain elytra never fizzles. Exercises the real
    // FireworkRocketMixin tick path, not just the fizzle-decision constant. ---

    @GameTest(template = "meridian:empty_3x3")
    public void curseOfMoltingFizzlesFireworkBoost(GameTestHelper helper) {
        Holder<Enchantment> molting = lookup(helper, "curse_of_molting");
        if (molting == null) { helper.fail("curse_of_molting not in registry"); return; }
        ServerLevel level = helper.getLevel();

        ItemStack moltingElytra = new ItemStack(Items.ELYTRA);
        moltingElytra.enchant(molting, 1);
        ServerPlayer cursed = glidingPlayer(helper, new BlockPos(1, 2, 1), moltingElytra);
        ServerPlayer plain = glidingPlayer(helper, new BlockPos(2, 2, 1), new ItemStack(Items.ELYTRA));

        int trials = 64;
        int cursedFizzled = countFizzledBoosts(level, cursed, trials);
        int plainFizzled = countFizzledBoosts(level, plain, trials);
        cursed.discard();
        plain.discard();

        // A plain elytra never fizzles; the curse fizzles roughly a quarter of boosts. Over 64 trials
        // the odds of zero cursed fizzles are ~1e-8, so ">0" is a safe assertion, not a flaky one.
        if (plainFizzled != 0) {
            helper.fail("A plain elytra must never fizzle a firework boost, got " + plainFizzled);
            return;
        }
        if (cursedFizzled <= 0) {
            helper.fail("Curse of Molting should fizzle some firework boosts, got "
                    + cursedFizzled + "/" + trials);
            return;
        }
        helper.succeed();
    }

    // --- Curse of Molting: the fizzle verdict is a pure function of the rocket's UUID, which both
    // sides hold from the spawn packet. Asserting the in-world outcome tracks the derived verdict is
    // the headless proxy for "the client predicts what the server does" — a gametest server has no
    // client replica to compare against directly. ---

    @GameTest(template = "meridian:empty_3x3")
    public void curseOfMoltingFizzleFollowsRocketUuid(GameTestHelper helper) {
        Holder<Enchantment> molting = lookup(helper, "curse_of_molting");
        if (molting == null) { helper.fail("curse_of_molting not in registry"); return; }
        ServerLevel level = helper.getLevel();

        ItemStack moltingElytra = new ItemStack(Items.ELYTRA);
        moltingElytra.enchant(molting, 1);
        ServerPlayer cursed = glidingPlayer(helper, new BlockPos(1, 2, 1), moltingElytra);

        for (int i = 0; i < 64; i++) {
            FireworkRocketEntity rocket = new FireworkRocketEntity(
                    level, new ItemStack(Items.FIREWORK_ROCKET), cursed);
            level.addFreshEntity(rocket);
            UUID id = rocket.getUUID();
            boolean predicted = TraversalEnchantMath.moltingFizzles(
                    id.getMostSignificantBits(), id.getLeastSignificantBits());
            rocket.tick();
            boolean fizzled = rocket.isRemoved();
            if (!rocket.isRemoved()) rocket.discard();
            if (fizzled != predicted) {
                cursed.discard();
                helper.fail("Molting fizzle must follow the rocket UUID: rocket " + id
                        + " predicted " + predicted + " but observed " + fizzled);
                return;
            }
        }
        cursed.discard();
        helper.succeed();
    }

    // Constructs `trials` boost rockets on the gliding player, ticks each once, and returns how many
    // fizzled — were discarded by the Molting mixin before applying any boost. A non-fizzled rocket
    // survives its first tick (vanilla removes it only past its lifetime), so it is discarded here.
    private static int countFizzledBoosts(ServerLevel level, ServerPlayer glider, int trials) {
        int fizzled = 0;
        for (int i = 0; i < trials; i++) {
            FireworkRocketEntity rocket = new FireworkRocketEntity(
                    level, new ItemStack(Items.FIREWORK_ROCKET), glider);
            level.addFreshEntity(rocket);
            rocket.tick();
            if (rocket.isRemoved()) {
                fizzled++;
            } else {
                rocket.discard();
            }
        }
        return fizzled;
    }
}
