package com.rfizzle.meridian.gametest.enchantments;

import com.rfizzle.meridian.Meridian;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.gameevent.BlockPositionSource;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.gameevent.GameEventListenerRegistry;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.phys.Vec3;

/**
 * End-to-end coverage for {@code EchoesVibrationMixin} — the Curse of Echoes redirect that widens
 * the range at which a vibration listener still accepts a wearer's movement game events.
 *
 * <p>The math is unit-tested in {@code EchoesVibrationMathTest}; this drives the redirect through
 * the real dispatch path ({@code ServerLevel.gameEvent} → {@code GameEventDispatcher.post} →
 * {@code EuclideanGameEventListenerRegistry.visitInRangeListeners} → the mixin's redirect of
 * {@code getPostableListenerPosition}). Rather than a live sculk sensor — whose downstream
 * {@code VibrationSystem} delivery is tick-delayed, distance-scaled, and occlusion-checked (the
 * flakiness that kept this deferred) — the listener under test is a {@link RecordingListener} with
 * a sculk sensor's native radius (8) and the default {@code UNSPECIFIED} delivery mode, so its
 * {@code handleGameEvent} fires <em>synchronously</em> inside the {@code gameEvent} call. Every
 * assertion is made the instant that call returns, with no ticks.
 *
 * <p>The wearer (a frozen zombie) sits at the listener so its chunk section is always the
 * structure's force-loaded one; the movement event's position is the independent variable, emitted
 * at a fixed offset. The mixin keys on the source entity's enchantment and on the event position
 * relative to the listener — exactly the two axes exercised here.
 */
public class EchoesVibrationGameTest implements FabricGameTest {

    /** A sculk sensor's native listener radius, mirrored by the listener under test. */
    private static final int LISTENER_RADIUS = 8;
    /** Beyond the native radius (8), within the level-1 Echoes-widened radius (8 + 8 = 16). */
    private static final int BEYOND_NATIVE = 10;
    /** Within the native radius — vanilla-audible with or without the curse. */
    private static final int WITHIN_NATIVE = 5;

    private Holder<Enchantment> lookup(GameTestHelper helper, String id) {
        Registry<Enchantment> reg = helper.getLevel().registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT);
        return reg.getHolder(Meridian.id(id)).orElse(null);
    }

    /**
     * Registers a fresh recording listener at the structure origin, emits {@code event} from a
     * frozen zombie wearing {@code legs} at {@code dist} blocks away, and reports whether the
     * listener was delivered the event. The listener is registered only after the zombie is
     * equipped, so incidental spawn/equip events cannot be mistaken for the movement event.
     */
    private boolean fireAndHear(GameTestHelper helper, int dist, ItemStack legs,
                                Holder<GameEvent> event) {
        ServerLevel level = helper.getLevel();
        BlockPos listenerPos = helper.absolutePos(new BlockPos(1, 2, 1));
        BlockPos eventPos = listenerPos.offset(dist, 0, 0);

        Zombie source = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 2, 1));
        source.setItemSlot(EquipmentSlot.LEGS, legs);
        source.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);

        RecordingListener listener = new RecordingListener(listenerPos);
        GameEventListenerRegistry registry = level.getChunkAt(listenerPos)
                .getListenerRegistry(SectionPos.blockToSectionCoord(listenerPos.getY()));
        registry.register(listener);
        try {
            level.gameEvent(source, event, Vec3.atCenterOf(eventPos));
            return listener.heard;
        } finally {
            registry.unregister(listener);
            source.discard();
        }
    }

    private ItemStack curseLeggings(GameTestHelper helper, Holder<Enchantment> curse) {
        ItemStack leggings = new ItemStack(Items.DIAMOND_LEGGINGS);
        leggings.enchant(curse, 1);
        return leggings;
    }

    /** The core proof: an Echoes wearer's STEP beyond native range still reaches the listener. */
    @GameTest(template = "meridian:empty_3x3")
    public void echoesWidensRangeForWearer(GameTestHelper helper) {
        Holder<Enchantment> curse = lookup(helper, "curse_of_echoes");
        if (curse == null) {
            helper.fail("curse_of_echoes not in registry");
            return;
        }
        boolean heard = fireAndHear(helper, BEYOND_NATIVE, curseLeggings(helper, curse), GameEvent.STEP);
        if (!heard) {
            helper.fail("An Echoes wearer's STEP " + BEYOND_NATIVE
                    + " blocks out (native " + LISTENER_RADIUS + ") should reach the listener");
            return;
        }
        helper.succeed();
    }

    /**
     * The gate accepts more than STEP: an Echoes wearer's SWIM beyond native range also reaches
     * the listener, guarding against a regression that drops an event from the movement set.
     */
    @GameTest(template = "meridian:empty_3x3")
    public void echoesWidensRangeForSwimEvent(GameTestHelper helper) {
        Holder<Enchantment> curse = lookup(helper, "curse_of_echoes");
        if (curse == null) {
            helper.fail("curse_of_echoes not in registry");
            return;
        }
        boolean heard = fireAndHear(helper, BEYOND_NATIVE, curseLeggings(helper, curse), GameEvent.SWIM);
        if (!heard) {
            helper.fail("An Echoes wearer's SWIM " + BEYOND_NATIVE
                    + " blocks out (native " + LISTENER_RADIUS + ") should reach the listener");
            return;
        }
        helper.succeed();
    }

    /** Negative control: an unenchanted mover beyond native range is not widened in. */
    @GameTest(template = "meridian:empty_3x3")
    public void noCurseRejectedBeyondNativeRange(GameTestHelper helper) {
        boolean heard = fireAndHear(helper, BEYOND_NATIVE, ItemStack.EMPTY, GameEvent.STEP);
        if (heard) {
            helper.fail("An unenchanted mover " + BEYOND_NATIVE
                    + " blocks out must not reach the listener — Echoes must not widen for everyone");
            return;
        }
        helper.succeed();
    }

    /** The widening is gated to movement events: a wearer's BLOCK_PLACE stays at native range. */
    @GameTest(template = "meridian:empty_3x3")
    public void nonMovementEventNotWidened(GameTestHelper helper) {
        Holder<Enchantment> curse = lookup(helper, "curse_of_echoes");
        if (curse == null) {
            helper.fail("curse_of_echoes not in registry");
            return;
        }
        boolean heard = fireAndHear(helper, BEYOND_NATIVE, curseLeggings(helper, curse), GameEvent.BLOCK_PLACE);
        if (heard) {
            helper.fail("Echoes must widen only movement events — BLOCK_PLACE beyond native range "
                    + "must not reach the listener");
            return;
        }
        helper.succeed();
    }

    /**
     * Proves the harness truly delivers in-range (so the rejections above are not false negatives
     * from broken wiring) and that the mixin's vanilla in-range early-return is intact.
     */
    @GameTest(template = "meridian:empty_3x3")
    public void nativeRangeStillDeliversWithoutCurse(GameTestHelper helper) {
        boolean heard = fireAndHear(helper, WITHIN_NATIVE, ItemStack.EMPTY, GameEvent.STEP);
        if (!heard) {
            helper.fail("A listener must still hear an unenchanted mover " + WITHIN_NATIVE
                    + " blocks out, within its native " + LISTENER_RADIUS + "-block radius");
            return;
        }
        helper.succeed();
    }

    /**
     * A minimal vibration listener that records whether the dispatcher delivered it an event. Its
     * default {@code UNSPECIFIED} delivery mode makes {@code handleGameEvent} run synchronously
     * inside {@code ServerLevel.gameEvent}, so no ticks are needed to observe the outcome.
     */
    private static final class RecordingListener implements GameEventListener {
        private final PositionSource source;
        private boolean heard;

        RecordingListener(BlockPos pos) {
            this.source = new BlockPositionSource(pos);
        }

        @Override
        public PositionSource getListenerSource() {
            return this.source;
        }

        @Override
        public int getListenerRadius() {
            return LISTENER_RADIUS;
        }

        @Override
        public boolean handleGameEvent(ServerLevel level, Holder<GameEvent> event,
                                       GameEvent.Context context, Vec3 pos) {
            this.heard = true;
            return true;
        }
    }
}
