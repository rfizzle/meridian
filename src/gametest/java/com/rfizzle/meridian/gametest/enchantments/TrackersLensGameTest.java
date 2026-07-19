// Tier: 3 (Fabric Gametest)
package com.rfizzle.meridian.gametest.enchantments;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.enchanting.SpyglassEnchantMath;
import com.rfizzle.meridian.event.TrackersLensHandler;
import com.rfizzle.meridian.gametest.MockPlayers;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Behavior coverage for Tracker's Lens: the sighting threshold, the line-of-sight requirement,
 * the no-enchant baseline, and the mobs-only default. Drives
 * {@link TrackersLensHandler#tickScope} directly rather than faking item-use state, which is why
 * that method takes the spyglass explicitly.
 *
 * <p>The enabled path of {@code combat.trackersLensAffectsPlayers} lives in
 * {@code TrackersLensConfigGameTest}, which mutates the shared config file and therefore needs its
 * own batch.
 */
public class TrackersLensGameTest implements FabricGameTest {

    private Holder<Enchantment> lookup(GameTestHelper helper, String id) {
        Registry<Enchantment> reg = helper.getLevel().registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT);
        return reg.getHolder(Meridian.id(id)).orElse(null);
    }

    /** A mock player at {@code rel}, aimed at {@code target}'s eyes. */
    private ServerPlayer scopingPlayerAt(GameTestHelper helper, BlockPos rel, Vec3 target) {
        ServerPlayer player = MockPlayers.serverPlayerInLevel(helper);
        BlockPos abs = helper.absolutePos(rel);
        player.teleportTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5);
        player.lookAt(EntityAnchorArgument.Anchor.EYES, target);
        return player;
    }

    private ItemStack enchantedSpyglass(Holder<Enchantment> lens, int level) {
        ItemStack spyglass = new ItemStack(Items.SPYGLASS);
        spyglass.enchant(lens, level);
        return spyglass;
    }

    private void scopeFor(ServerPlayer player, ItemStack spyglass, int ticks) {
        for (int i = 0; i < ticks; i++) {
            TrackersLensHandler.tickScope(player, spyglass);
        }
    }

    // A creature held in the lens for the full sighting is marked and glows.
    @GameTest(template = "meridian:empty_5x5x5")
    public void marksMobAfterSustainedSighting(GameTestHelper helper) {
        TrackersLensHandler.reset();
        Holder<Enchantment> lens = lookup(helper, "trackers_lens");
        if (lens == null) { helper.fail("trackers_lens not in registry"); return; }

        Pig victim = helper.spawn(EntityType.PIG, new BlockPos(1, 1, 4));
        ServerPlayer player = scopingPlayerAt(helper, new BlockPos(1, 1, 0), victim.getEyePosition());

        try {
            scopeFor(player, enchantedSpyglass(lens, 1),
                    SpyglassEnchantMath.TRACKERS_LENS_SIGHTING_TICKS);
            if (!victim.hasEffect(MobEffects.GLOWING)) {
                helper.fail("Tracker's Lens should mark a mob held for the full sighting");
                return;
            }
            helper.succeed();
        } finally {
            player.discard();
        }
    }

    // One tick short of the threshold, nothing is marked — the sighting is a real cost.
    @GameTest(template = "meridian:empty_5x5x5")
    public void doesNotMarkBeforeTheSightingCompletes(GameTestHelper helper) {
        TrackersLensHandler.reset();
        Holder<Enchantment> lens = lookup(helper, "trackers_lens");
        if (lens == null) { helper.fail("trackers_lens not in registry"); return; }

        Pig victim = helper.spawn(EntityType.PIG, new BlockPos(1, 1, 4));
        ServerPlayer player = scopingPlayerAt(helper, new BlockPos(1, 1, 0), victim.getEyePosition());

        try {
            scopeFor(player, enchantedSpyglass(lens, 4),
                    SpyglassEnchantMath.TRACKERS_LENS_SIGHTING_TICKS - 1);
            if (victim.hasEffect(MobEffects.GLOWING)) {
                helper.fail("Tracker's Lens must not mark before the sighting completes");
                return;
            }
            helper.succeed();
        } finally {
            player.discard();
        }
    }

    // Level scales the glow, not the sighting: level IV holds far longer than level I.
    @GameTest(template = "meridian:empty_5x5x5")
    public void higherLevelGlowsLonger(GameTestHelper helper) {
        TrackersLensHandler.reset();
        Holder<Enchantment> lens = lookup(helper, "trackers_lens");
        if (lens == null) { helper.fail("trackers_lens not in registry"); return; }

        Pig victim = helper.spawn(EntityType.PIG, new BlockPos(1, 1, 4));
        ServerPlayer player = scopingPlayerAt(helper, new BlockPos(1, 1, 0), victim.getEyePosition());

        try {
            scopeFor(player, enchantedSpyglass(lens, 4),
                    SpyglassEnchantMath.TRACKERS_LENS_SIGHTING_TICKS);
            var effect = victim.getEffect(MobEffects.GLOWING);
            if (effect == null) {
                helper.fail("Tracker's Lens IV should mark a mob held for the full sighting");
                return;
            }
            if (effect.getDuration() <= SpyglassEnchantMath.trackersLensGlowTicks(1)) {
                helper.fail("Tracker's Lens IV must glow longer than level I, got "
                        + effect.getDuration() + " ticks");
                return;
            }
            helper.succeed();
        } finally {
            player.discard();
        }
    }

    // An unenchanted spyglass marks nothing, however long it is held.
    @GameTest(template = "meridian:empty_5x5x5")
    public void plainSpyglassMarksNothing(GameTestHelper helper) {
        TrackersLensHandler.reset();
        Pig victim = helper.spawn(EntityType.PIG, new BlockPos(1, 1, 4));
        ServerPlayer player = scopingPlayerAt(helper, new BlockPos(1, 1, 0), victim.getEyePosition());

        try {
            scopeFor(player, new ItemStack(Items.SPYGLASS),
                    SpyglassEnchantMath.TRACKERS_LENS_SIGHTING_TICKS * 2);
            if (victim.hasEffect(MobEffects.GLOWING)) {
                helper.fail("An unenchanted spyglass must never mark");
                return;
            }
            helper.succeed();
        } finally {
            player.discard();
        }
    }

    // The initial sighting needs line of sight: a wall between the two blocks acquisition.
    @GameTest(template = "meridian:empty_5x5x5")
    public void wallBlocksAcquisition(GameTestHelper helper) {
        TrackersLensHandler.reset();
        Holder<Enchantment> lens = lookup(helper, "trackers_lens");
        if (lens == null) { helper.fail("trackers_lens not in registry"); return; }

        Pig victim = helper.spawn(EntityType.PIG, new BlockPos(1, 1, 4));
        ServerPlayer player = scopingPlayerAt(helper, new BlockPos(1, 1, 0), victim.getEyePosition());

        // A full-height stone column across the sight line, after the aim was taken.
        helper.setBlock(new BlockPos(1, 1, 2), Blocks.STONE);
        helper.setBlock(new BlockPos(1, 2, 2), Blocks.STONE);

        try {
            scopeFor(player, enchantedSpyglass(lens, 4),
                    SpyglassEnchantMath.TRACKERS_LENS_SIGHTING_TICKS * 2);
            if (victim.hasEffect(MobEffects.GLOWING)) {
                helper.fail("Tracker's Lens must not acquire a target through a wall");
                return;
            }
            helper.succeed();
        } finally {
            player.discard();
        }
    }

    // With the default config, a player held in the lens is left unmarked.
    @GameTest(template = "meridian:empty_5x5x5")
    public void ignoresPlayersByDefault(GameTestHelper helper) {
        TrackersLensHandler.reset();
        Holder<Enchantment> lens = lookup(helper, "trackers_lens");
        if (lens == null) { helper.fail("trackers_lens not in registry"); return; }

        ServerPlayer victim = MockPlayers.serverPlayerInLevel(helper);
        BlockPos victimAbs = helper.absolutePos(new BlockPos(1, 1, 4));
        victim.teleportTo(victimAbs.getX() + 0.5, victimAbs.getY(), victimAbs.getZ() + 0.5);

        ServerPlayer player = scopingPlayerAt(helper, new BlockPos(1, 1, 0), victim.getEyePosition());

        try {
            scopeFor(player, enchantedSpyglass(lens, 4),
                    SpyglassEnchantMath.TRACKERS_LENS_SIGHTING_TICKS * 2);
            if (victim.hasEffect(MobEffects.GLOWING)) {
                helper.fail("Tracker's Lens must not mark a player while "
                        + "combat.trackersLensAffectsPlayers is false");
                return;
            }
            helper.succeed();
        } finally {
            victim.discard();
            player.discard();
        }
    }
}
