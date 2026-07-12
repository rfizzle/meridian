package com.rfizzle.meridian.client;

import com.rfizzle.meridian.MeridianRegistry;
import com.rfizzle.meridian.client.config.ClientMeridianConfig;
import com.rfizzle.meridian.client.net.ClientPayloadHandlers;
import com.rfizzle.meridian.client.net.LoftClientHandler;
import com.rfizzle.meridian.client.screen.EnchantmentLibraryScreen;
import com.rfizzle.meridian.client.screen.MeridianEnchantmentScreen;
import com.rfizzle.meridian.client.tooltip.InlineEnchDescTooltipHandler;
import com.rfizzle.meridian.client.tooltip.ObscurityTooltipHandler;
import com.rfizzle.meridian.client.tooltip.OverLeveledTooltipHandler;
import com.rfizzle.meridian.enchanting.EnchantingStatRegistry;
import com.rfizzle.meridian.enchanting.EnchantmentInfoRegistry;
import com.rfizzle.meridian.particle.ModParticles;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.particle.FlyTowardsPositionParticle;

public class MeridianClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPayloadHandlers.register();
        LoftClientHandler.register();
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            EnchantmentInfoRegistry.clear();
            EnchantingStatRegistry.getInstance().clearClientState();
            ClientMeridianConfig.clear();
        });
        MenuScreens.register(MeridianRegistry.ENCHANTING_TABLE_MENU, MeridianEnchantmentScreen::new);
        MenuScreens.register(MeridianRegistry.LIBRARY_MENU, EnchantmentLibraryScreen::new);
        // Obscurity strips hidden enchantment lines before any handler that keys additions off
        // those names (over-leveled warnings, inline descriptions) can attach to them.
        ObscurityTooltipHandler.register();
        OverLeveledTooltipHandler.register();
        InlineEnchDescTooltipHandler.register();
        registerParticleProviders();
    }

    private static void registerParticleProviders() {
        ParticleFactoryRegistry r = ParticleFactoryRegistry.getInstance();
        r.register(ModParticles.ENCHANT_FIRE, FlyTowardsPositionParticle.EnchantProvider::new);
        r.register(ModParticles.ENCHANT_WATER, FlyTowardsPositionParticle.EnchantProvider::new);
        r.register(ModParticles.ENCHANT_SCULK, FlyTowardsPositionParticle.EnchantProvider::new);
        r.register(ModParticles.ENCHANT_END, FlyTowardsPositionParticle.EnchantProvider::new);
    }
}
