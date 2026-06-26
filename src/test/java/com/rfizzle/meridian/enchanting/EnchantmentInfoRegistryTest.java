// Tier: 2 (fabric-loader-junit — applyFromPayload takes ResourceKey<Enchantment>)
package com.rfizzle.meridian.enchanting;

import com.rfizzle.meridian.api.EnchantmentInfo;
import com.rfizzle.meridian.enchanting.PowerFunction;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnchantmentInfoRegistryTest {

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @AfterEach
    void resetRegistry() {
        EnchantmentInfoRegistry.clear();
    }

    private static Holder.Reference<Enchantment> sharpness() {
        return VanillaRegistries.createLookup()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.SHARPNESS);
    }

    // ── flag lifecycle ────────────────────────────────────────────────────────

    @Test
    void hasSyncBeenReceived_falseOnFreshRegistry() {
        assertFalse(EnchantmentInfoRegistry.hasSyncBeenReceived(),
                "Registry must start with syncReceived = false");
    }

    @Test
    void hasSyncBeenReceived_trueAfterApplyFromPayload() {
        Holder.Reference<Enchantment> sharp = sharpness();
        EnchantmentInfo info = EnchantmentInfo.fallback(sharp);
        EnchantmentInfoRegistry.applyFromPayload(Map.of(sharp.key(), info));

        assertTrue(EnchantmentInfoRegistry.hasSyncBeenReceived(),
                "hasSyncBeenReceived must return true after applyFromPayload");
    }

    @Test
    void hasSyncBeenReceived_falseAfterClear() {
        Holder.Reference<Enchantment> sharp = sharpness();
        EnchantmentInfo info = EnchantmentInfo.fallback(sharp);
        EnchantmentInfoRegistry.applyFromPayload(Map.of(sharp.key(), info));
        assertTrue(EnchantmentInfoRegistry.hasSyncBeenReceived());

        EnchantmentInfoRegistry.clear();

        assertFalse(EnchantmentInfoRegistry.hasSyncBeenReceived(),
                "hasSyncBeenReceived must return false after clear()");
    }

    // ── data coherence ────────────────────────────────────────────────────────

    @Test
    void getInfo_returnsAppliedDataAfterSync() {
        Holder.Reference<Enchantment> sharp = sharpness();
        EnchantmentInfo override = new EnchantmentInfo(
                sharp, 10, 5, -1,
                PowerFunction.DefaultMaxPowerFunction.INSTANCE,
                new PowerFunction.DefaultMinPowerFunction(sharp),
                true);
        EnchantmentInfoRegistry.applyFromPayload(Map.of(sharp.key(), override));

        EnchantmentInfo result = EnchantmentInfoRegistry.getInfo(sharp);
        assertNotNull(result);
        assertTrue(result.getMaxLevel() == 10,
                "getInfo must return the server-supplied max level after applyFromPayload");
    }

    @Test
    void getByInstance_returnsNullAfterClear() {
        Holder.Reference<Enchantment> sharp = sharpness();
        EnchantmentInfo info = EnchantmentInfo.fallback(sharp);
        EnchantmentInfoRegistry.applyFromPayload(Map.of(sharp.key(), info));

        EnchantmentInfoRegistry.clear();

        assertNull(EnchantmentInfoRegistry.getInfoByInstance(sharp.value()),
                "getInfoByInstance must return null after clear()");
    }

    @Test
    void getAll_emptyAfterClear() {
        Holder.Reference<Enchantment> sharp = sharpness();
        EnchantmentInfoRegistry.applyFromPayload(Map.of(sharp.key(), EnchantmentInfo.fallback(sharp)));
        assertFalse(EnchantmentInfoRegistry.getAll().isEmpty());

        EnchantmentInfoRegistry.clear();

        assertTrue(EnchantmentInfoRegistry.getAll().isEmpty(),
                "getAll() must return empty map after clear()");
    }
}
