// Tier: 2 (fabric-loader-junit)
package com.rfizzle.meridian.net;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class NetworkingRegistryTest {

    @BeforeAll
    static void registerOnce() {
        // StatsPayload's static init pulls in ItemStack, which requires BuiltInRegistries to
        // have been bootstrapped — otherwise the class loads partially and poisons later tests.
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        MeridianNetworking.registerPayloads();
    }

    @Test
    void registerPayloads_statsTypeIsRegistered() {
        // Re-registering the same type throws — proving the first registration took effect.
        assertThrows(IllegalArgumentException.class, () ->
                PayloadTypeRegistry.playS2C().register(StatsPayload.TYPE, StatsPayload.CODEC));
    }

    @Test
    void registerPayloads_cluesTypeIsRegistered() {
        assertThrows(IllegalArgumentException.class, () ->
                PayloadTypeRegistry.playS2C().register(CluesPayload.TYPE, CluesPayload.CODEC));
    }
}
