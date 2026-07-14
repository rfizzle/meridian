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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Thrift (elytra): a firework boost used while gliding sometimes leaves the rocket unspent, scaling
 * per level, and a plain elytra always spends it. Both are exercised against the real
 * {@code FireworkRocketItem.use} consume path — the exact call the {@link FireworkRocketItemMixin}
 * wraps — by handing a fall-flying mock player a single rocket and checking whether it survives the
 * use. The boost rocket is spawned before the consume regardless of the refund, so the boost itself
 * is untouched; this test asserts that spawn still happens.
 */
public class ThriftGameTest implements FabricGameTest {

    private static final int TRIALS = 128;

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
        player.moveTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5, 0.0f, 0.0f);
        player.setDeltaMovement(Vec3.ZERO);
        player.startFallFlying();
        return player;
    }

    /** Fires {@link #TRIALS} single rockets from a gliding player, counting how many survive the use. */
    private static int countRefunds(ServerLevel level, ServerPlayer glider) {
        int refunds = 0;
        for (int i = 0; i < TRIALS; i++) {
            glider.startFallFlying();
            glider.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.FIREWORK_ROCKET, 1));
            Items.FIREWORK_ROCKET.use(level, glider, InteractionHand.MAIN_HAND);
            if (!glider.getItemInHand(InteractionHand.MAIN_HAND).isEmpty()) {
                refunds++;
            }
        }
        // The use path spawns a boost rocket every trial; clear them so they don't leak between checks.
        discardFireworks(level);
        return refunds;
    }

    /**
     * Discards every firework rocket in the level. Collects first, then discards — discarding while
     * iterating the live entity view corrupts its iterator.
     */
    private static void discardFireworks(ServerLevel level) {
        List<FireworkRocketEntity> rockets = new ArrayList<>();
        for (Entity e : level.getAllEntities()) {
            if (e instanceof FireworkRocketEntity rocket) rockets.add(rocket);
        }
        for (FireworkRocketEntity rocket : rockets) {
            rocket.discard();
        }
    }

    // --- Thrift sometimes leaves the rocket unspent; a plain elytra always spends it ---

    @GameTest(template = "meridian:empty_3x3")
    public void thriftSometimesRefundsFireworkBoost(GameTestHelper helper) {
        Holder<Enchantment> thrift = lookup(helper, "thrift");
        if (thrift == null) { helper.fail("thrift not in registry"); return; }
        ServerLevel level = helper.getLevel();

        ItemStack thriftElytra = new ItemStack(Items.ELYTRA);
        thriftElytra.enchant(thrift, 2);
        ServerPlayer thrifty = glidingPlayer(helper, new BlockPos(1, 2, 1), thriftElytra);
        ServerPlayer plain = glidingPlayer(helper, new BlockPos(2, 2, 1), new ItemStack(Items.ELYTRA));

        int thriftyRefunds = countRefunds(level, thrifty);
        int plainRefunds = countRefunds(level, plain);
        thrifty.discard();
        plain.discard();

        // A plain elytra always spends the rocket; Thrift II (~50%) leaves some but not all. Over 128
        // trials the odds of zero or all-128 refunds are negligible, so the bounds are safe, not flaky.
        if (plainRefunds != 0) {
            helper.fail("A plain elytra must always spend the firework, got " + plainRefunds
                    + "/" + TRIALS + " refunded");
            return;
        }
        if (thriftyRefunds <= 0 || thriftyRefunds >= TRIALS) {
            helper.fail("Thrift should sometimes (but not always) refund the firework, got "
                    + thriftyRefunds + "/" + TRIALS);
            return;
        }
        helper.succeed();
    }

    // --- The boost rocket is still spawned even when Thrift refunds the firework ---

    @GameTest(template = "meridian:empty_3x3")
    public void thriftRefundStillSpawnsBoost(GameTestHelper helper) {
        Holder<Enchantment> thrift = lookup(helper, "thrift");
        if (thrift == null) { helper.fail("thrift not in registry"); return; }
        ServerLevel level = helper.getLevel();

        // Thrift II gives a high enough refund chance (~50%) that a refunded use shows up well within
        // the trial budget; any spawned rocket on that use proves the boost fires when the item is not
        // consumed.
        ItemStack thriftElytra = new ItemStack(Items.ELYTRA);
        thriftElytra.enchant(thrift, 2);
        ServerPlayer glider = glidingPlayer(helper, new BlockPos(1, 2, 1), thriftElytra);

        boolean sawRefundedBoost = false;
        for (int i = 0; i < TRIALS && !sawRefundedBoost; i++) {
            glider.startFallFlying();
            glider.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.FIREWORK_ROCKET, 1));
            Items.FIREWORK_ROCKET.use(level, glider, InteractionHand.MAIN_HAND);
            boolean refunded = !glider.getItemInHand(InteractionHand.MAIN_HAND).isEmpty();
            boolean boostSpawned = hasFirework(level);
            discardFireworks(level);
            if (refunded && boostSpawned) sawRefundedBoost = true;
        }
        glider.discard();

        if (!sawRefundedBoost) {
            helper.fail("Thrift should still spawn the boost rocket on a refunded firework use");
            return;
        }
        helper.succeed();
    }

    private static boolean hasFirework(ServerLevel level) {
        for (Entity e : level.getAllEntities()) {
            if (e instanceof FireworkRocketEntity) return true;
        }
        return false;
    }
}
