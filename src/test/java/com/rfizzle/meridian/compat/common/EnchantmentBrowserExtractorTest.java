// Tier: 2 (fabric-loader-junit)
package com.rfizzle.meridian.compat.common;

import com.rfizzle.meridian.api.EnchantmentInfo;
import com.rfizzle.meridian.enchanting.EnchantmentInfoRegistry;
import com.rfizzle.meridian.enchanting.PowerFunction;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class EnchantmentBrowserExtractorTest {

    private static HolderLookup.Provider lookup;

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        // Enchantments are data-driven in 1.21.1 (dynamic registry, not BuiltInRegistries); the
        // vanilla data-bootstrap populates them so SHARPNESS and friends resolve below.
        lookup = VanillaRegistries.createLookup();
    }

    @AfterEach
    void resetOverrides() {
        EnchantmentInfoRegistry.clear();
    }

    private static Holder.Reference<Enchantment> sharpness() {
        return lookup.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS);
    }

    /** Simulates a full vanilla sync so that {@code hasSyncBeenReceived()} returns {@code true}. */
    private static void simulateVanillaSync() {
        Map<ResourceKey<Enchantment>, EnchantmentInfo> allVanilla =
                lookup.lookupOrThrow(Registries.ENCHANTMENT).listElements()
                        .collect(Collectors.toMap(Holder.Reference::key, EnchantmentInfo::fallback));
        EnchantmentInfoRegistry.applyFromPayload(allVanilla);
    }

    @Test
    void extract_returnsEmptyBeforeSyncReceived() {
        // Registry was just cleared by @AfterEach / @BeforeAll — syncReceived is false.
        List<EnchantmentBrowserRecord> records = EnchantmentBrowserExtractor.extract(lookup);

        assertTrue(records.isEmpty(),
                "extract() must return an empty list when hasSyncBeenReceived() is false");
    }

    @Test
    void extract_surfacesAllEnchantments() {
        simulateVanillaSync();
        List<EnchantmentBrowserRecord> records = EnchantmentBrowserExtractor.extract(lookup);

        long registrySize = lookup.lookupOrThrow(Registries.ENCHANTMENT).listElements().count();
        assertFalse(records.isEmpty());
        assertEquals(registrySize, records.size());
    }

    @Test
    void extract_detectsConfigOverride() {
        Holder.Reference<Enchantment> sharp = sharpness();

        // Mock a config override: max level raised from vanilla's 5 to 10.
        EnchantmentInfo override = new EnchantmentInfo(
                sharp, 10, 5, -1,
                PowerFunction.DefaultMaxPowerFunction.INSTANCE,
                new PowerFunction.DefaultMinPowerFunction(sharp),
                true);

        EnchantmentInfoRegistry.applyFromPayload(Map.of(sharp.key(), override));

        List<EnchantmentBrowserRecord> records = EnchantmentBrowserExtractor.extract(lookup);
        EnchantmentBrowserRecord sharpRecord = findRecord(records, sharp);

        assertTrue(sharpRecord.isConfigOverridden());
        assertEquals(10, sharpRecord.maxLevel());
    }

    @Test
    void extract_noOverrideWhenStatsMatchVanilla() {
        Holder.Reference<Enchantment> sharp = sharpness();
        simulateVanillaSync();

        List<EnchantmentBrowserRecord> records = EnchantmentBrowserExtractor.extract(lookup);
        EnchantmentBrowserRecord sharpRecord = findRecord(records, sharp);

        assertFalse(sharpRecord.isConfigOverridden());
    }

    @Test
    void extract_calculatesPowerWindows() {
        Holder.Reference<Enchantment> sharp = sharpness();
        simulateVanillaSync();

        List<EnchantmentBrowserRecord> records = EnchantmentBrowserExtractor.extract(lookup);
        EnchantmentBrowserRecord sharpRecord = findRecord(records, sharp);

        assertEquals(sharp.value().getMaxLevel(), sharpRecord.powerWindows().size());
        for (int i = 0; i < sharpRecord.powerWindows().size(); i++) {
            int level = i + 1;
            int[] window = sharpRecord.powerWindows().get(i);
            assertEquals(sharp.value().getMinCost(level), window[0]);
            assertEquals(200, window[1]); // Default max power is 200
        }
    }

    private static EnchantmentBrowserRecord findRecord(List<EnchantmentBrowserRecord> records,
                                                       Holder.Reference<Enchantment> ench) {
        return records.stream()
                .filter(r -> r.ench().is(ench.key()))
                .findFirst()
                .orElseThrow();
    }
}
