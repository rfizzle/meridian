package com.rfizzle.meridian.event;

import com.rfizzle.meridian.config.MeridianConfig;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * T-5.4.4 — verifies that {@link WardenPoolCondition} reads {@code config.warden.*} at roll
 * time rather than caching at MODIFY time. Mutating the config in memory must shift subsequent
 * roll expectations without any restart or datapack reload.
 *
 * <p>Uses the pure-function {@code resolveChance} helper (no supplier) and the {@code roll}
 * helper (goes through the supplier, simulating the reload pathway). The full
 * {@link WardenPoolCondition#test(net.minecraft.world.level.storage.loot.LootContext)} path
 * needs a {@code LootContext} — covered indirectly by the structural tests in
 * {@link WardenLootHandlerTest}.
 */
class WardenPoolConditionTest {

    @AfterEach
    void tearDown() {
        WardenPoolCondition.resetConfigSupplier();
    }

    @Test
    void dropChance_resolvesFromConfig() {
        MeridianConfig config = new MeridianConfig();
        config.warden.tendrilDropChance = 0.25;

        WardenPoolCondition condition = new WardenPoolCondition(WardenPoolCondition.Kind.DROP_CHANCE);
        assertEquals(0.25F, condition.resolveChance(0, config), 1.0e-6F,
                "DROP_CHANCE must mirror config.warden.tendrilDropChance verbatim — no "
                        + "attacker lookup, no looting scaling");
    }

    @Test
    void dropChance_mutationPropagates() {
        // Simulates `/meridian reload` — the condition is constructed once, the config
        // is mutated, and the subsequent roll must reflect the new value. With MODIFY-time
        // baking this test would always see the initial value.
        MeridianConfig config = new MeridianConfig();
        config.warden.tendrilDropChance = 1.0;

        WardenPoolCondition condition = new WardenPoolCondition(WardenPoolCondition.Kind.DROP_CHANCE);
        assertEquals(1.0F, condition.resolveChance(0, config), 1.0e-6F,
                "initial value must propagate");

        config.warden.tendrilDropChance = 0.0;
        assertEquals(0.0F, condition.resolveChance(0, config), 1.0e-6F,
                "mutated value must propagate on the next call — no caching between rolls");
    }

    @Test
    void dropChance_clampsAboveOne() {
        // Defensive clamp — if a racy mid-reload state briefly carries an out-of-range value,
        // the condition must still produce a legal probability for RandomSource#nextFloat.
        MeridianConfig config = new MeridianConfig();
        config.warden.tendrilDropChance = 2.5;

        WardenPoolCondition condition = new WardenPoolCondition(WardenPoolCondition.Kind.DROP_CHANCE);
        assertEquals(1.0F, condition.resolveChance(0, config), 1.0e-6F,
                "chance must clamp to 1.0 — a > 1.0 value would skew the random draw");
    }

    @Test
    void dropChance_clampsBelowZero() {
        MeridianConfig config = new MeridianConfig();
        config.warden.tendrilDropChance = -0.4;

        WardenPoolCondition condition = new WardenPoolCondition(WardenPoolCondition.Kind.DROP_CHANCE);
        assertEquals(0.0F, condition.resolveChance(0, config), 1.0e-6F,
                "chance must clamp to 0.0 — a negative value would let strange sign bugs "
                        + "through nextFloat() < chance");
    }

    @Test
    void lootingBonus_scalesByLevel() {
        MeridianConfig config = new MeridianConfig();
        config.warden.tendrilLootingBonus = 0.10;

        WardenPoolCondition condition = new WardenPoolCondition(WardenPoolCondition.Kind.LOOTING_BONUS);
        assertEquals(0.0F, condition.resolveChance(0, config), 1.0e-6F,
                "looting 0 collapses to 0 chance — matches vanilla "
                        + "LootItemRandomChanceWithEnchantedBonusCondition(0, linear) contract");
        assertEquals(0.10F, condition.resolveChance(1, config), 1.0e-6F,
                "looting 1 yields exactly the per-level bonus");
        assertEquals(0.30F, condition.resolveChance(3, config), 1.0e-6F,
                "looting 3 yields 3 * bonus — matches the 'dropChance=0, looting=3' "
                        + "acceptance bound from T-5.4.3");
    }

    @Test
    void lootingBonus_mutationPropagates() {
        MeridianConfig config = new MeridianConfig();
        config.warden.tendrilLootingBonus = 0.10;

        WardenPoolCondition condition = new WardenPoolCondition(WardenPoolCondition.Kind.LOOTING_BONUS);
        assertEquals(0.20F, condition.resolveChance(2, config), 1.0e-6F);

        config.warden.tendrilLootingBonus = 0.25;
        assertEquals(0.50F, condition.resolveChance(2, config), 1.0e-6F,
                "raising the bonus mid-flight must reflect in the next roll — this is the "
                        + "core /meridian reload guarantee");
    }

    @Test
    void lootingBonus_clampsAboveOne() {
        MeridianConfig config = new MeridianConfig();
        config.warden.tendrilLootingBonus = 0.40;

        WardenPoolCondition condition = new WardenPoolCondition(WardenPoolCondition.Kind.LOOTING_BONUS);
        assertEquals(1.0F, condition.resolveChance(5, config), 1.0e-6F,
                "looting-5 * 0.40 = 2.0 must clamp to 1.0 — the roll is still a single d-[0,1)");
    }

    @Test
    void roll_readsSupplierLive() {
        // Emulates the `/meridian reload` flow through the supplier — the condition
        // is constructed once, but each call to roll() must re-read the supplier. Swapping
        // the supplier's backing config must change roll outcomes without touching the
        // condition instance.
        AtomicReference<MeridianConfig> holder = new AtomicReference<>();
        MeridianConfig initial = new MeridianConfig();
        initial.warden.tendrilDropChance = 1.0;
        holder.set(initial);
        WardenPoolCondition.overrideConfigSupplier(holder::get);

        WardenPoolCondition condition = new WardenPoolCondition(WardenPoolCondition.Kind.DROP_CHANCE);

        // chance=1.0 → nextFloat() (∈ [0,1)) is always strictly less — always fires.
        RandomSource random = RandomSource.create(0xFEEDCAFEL);
        for (int i = 0; i < 20; i++) {
            assertTrue(condition.roll(random, 0),
                    "chance=1.0 must make every roll fire — iteration " + i);
        }

        // Simulate an operator lowering the drop chance and running /meridian reload
        // (which flips the singleton). The condition instance is unchanged.
        MeridianConfig reloaded = new MeridianConfig();
        reloaded.warden.tendrilDropChance = 0.0;
        holder.set(reloaded);

        for (int i = 0; i < 20; i++) {
            assertFalse(condition.roll(random, 0),
                    "chance=0.0 after reload must make every roll decline — iteration " + i);
        }
    }

    @Test
    void roll_nullConfig_declines() {
        // Guards the startup race where onInitialize hasn't set the singleton yet. Better to
        // drop the roll than NPE in the middle of a loot dispatch.
        WardenPoolCondition.overrideConfigSupplier(() -> null);
        WardenPoolCondition condition = new WardenPoolCondition(WardenPoolCondition.Kind.DROP_CHANCE);
        assertFalse(condition.roll(RandomSource.create(0L), 0),
                "null config must decline rather than NPE");
    }

    @Test
    void codec_roundTripsBothKinds() {
        // Pin the codec so an accidental rename of Kind's serialized name breaks loudly —
        // the serialized form appears in every LootTableEvents.MODIFY-generated table that a
        // world saves, so drifting the name quietly would orphan rolls on existing saves.
        MapCodec<WardenPoolCondition> codec = WardenPoolCondition.CODEC;
        for (WardenPoolCondition.Kind kind : WardenPoolCondition.Kind.values()) {
            WardenPoolCondition original = new WardenPoolCondition(kind);
            var json = codec.codec().encodeStart(JsonOps.INSTANCE, original).result().orElseThrow();
            WardenPoolCondition decoded = codec.codec().parse(JsonOps.INSTANCE, json).result().orElseThrow();
            assertEquals(original, decoded, "round-trip failed for " + kind);
        }
    }
}
