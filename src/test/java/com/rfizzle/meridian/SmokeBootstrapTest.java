// Tier: 2 (fabric-loader-junit)
package com.rfizzle.meridian;

import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SmokeBootstrapTest {

    @BeforeAll
    static void bootstrapVanillaRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void vanillaRegistriesAreAvailable() {
        assertNotNull(Items.DIAMOND_SWORD);
        assertTrue(BuiltInRegistries.ITEM.containsKey(BuiltInRegistries.ITEM.getKey(Items.DIAMOND_SWORD)));
    }
}
