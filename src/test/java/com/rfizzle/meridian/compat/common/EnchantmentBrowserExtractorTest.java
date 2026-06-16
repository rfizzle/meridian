// Tier: 2 (fabric-loader-junit)
package com.rfizzle.meridian.compat.common;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.api.EnchantmentInfo;
import com.rfizzle.meridian.enchanting.EnchantmentInfoRegistry;
import com.rfizzle.meridian.enchanting.PowerFunction;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EnchantmentBrowserExtractorTest {

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void extract_surfacesAllEnchantments() {
        RegistryAccess ra = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        Registry<Enchantment> registry = ra.registryOrThrow(Registries.ENCHANTMENT);

        List<EnchantmentBrowserRecord> records = EnchantmentBrowserExtractor.extract(ra);

        // Should have at least some vanilla enchantments if bootstrapped
        assertFalse(records.isEmpty());
        assertEquals(registry.size(), records.size());
    }

    @Test
    void extract_detectsConfigOverride() {
        RegistryAccess ra = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        Holder.Reference<Enchantment> sharp = ra.registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.SHARPNESS);

        // Mock a config override
        EnchantmentInfo override = new EnchantmentInfo(
                sharp, 10, 5, -1,
                PowerFunction.DefaultMaxPowerFunction.INSTANCE,
                new PowerFunction.DefaultMinPowerFunction(sharp),
                true);

        EnchantmentInfoRegistry.applyFromPayload(Map.of(sharp.key(), override));

        List<EnchantmentBrowserRecord> records = EnchantmentBrowserExtractor.extract(ra);
        EnchantmentBrowserRecord sharpRecord = records.stream()
                .filter(r -> r.ench().equals(sharp))
                .findFirst()
                .orElseThrow();

        assertTrue(sharpRecord.isConfigOverridden());
        assertEquals(10, sharpRecord.maxLevel());

        // Cleanup
        EnchantmentInfoRegistry.clear();
    }

    @Test
    void extract_calculatesPowerWindows() {
        RegistryAccess ra = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        Holder.Reference<Enchantment> sharp = ra.registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.SHARPNESS);

        List<EnchantmentBrowserRecord> records = EnchantmentBrowserExtractor.extract(ra);
        EnchantmentBrowserRecord sharpRecord = records.stream()
                .filter(r -> r.ench().equals(sharp))
                .findFirst()
                .orElseThrow();

        assertEquals(sharp.value().getMaxLevel(), sharpRecord.powerWindows().size());
        for (int i = 0; i < sharpRecord.powerWindows().size(); i++) {
            int level = i + 1;
            int[] window = sharpRecord.powerWindows().get(i);
            assertEquals(sharp.value().getMinCost(level), window[0]);
            assertEquals(200, window[1]); // Default max is 200
        }
    }
}
