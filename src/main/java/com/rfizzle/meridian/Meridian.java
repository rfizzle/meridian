package com.rfizzle.meridian;

import com.rfizzle.meridian.anvil.MeridianAnvilHandlers;
import com.rfizzle.meridian.command.MeridianCommand;
import com.rfizzle.meridian.config.MeridianConfig;
import com.rfizzle.meridian.advancement.ModTriggers;
import com.rfizzle.meridian.enchanting.EnchantingStatRegistry;
import com.rfizzle.meridian.enchanting.EnchantmentInfoRegistry;
import com.rfizzle.meridian.enchanting.recipe.EnchantingRecipeRegistry;
import com.rfizzle.meridian.event.ArmorTickHandler;
import com.rfizzle.meridian.event.AurifyHandler;
import com.rfizzle.meridian.event.EnchantmentEffectHandler;
import com.rfizzle.meridian.event.MountedEnchantmentHandler;
import com.rfizzle.meridian.event.TetherHandler;
import com.rfizzle.meridian.event.ToolEnchantmentHandler;
import com.rfizzle.meridian.event.WardenLootHandler;
import com.rfizzle.meridian.net.EnchantmentInfoPayload;
import com.rfizzle.meridian.net.MeridianNetworking;
import com.rfizzle.meridian.particle.ModParticles;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.enchantment.Enchantment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Meridian implements ModInitializer {
    public static final String MOD_ID = "meridian";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static volatile MeridianConfig config;

    @Override
    public void onInitialize() {
        LOGGER.info("Meridian initialized");
        config = MeridianConfig.load();
        ModParticles.register();
        ModTriggers.register();
        EnchantingStatRegistry.bootstrap();
        MeridianRegistry.register();
        MeridianRegistry.registerApiLookups();
        EnchantingRecipeRegistry.register();
        MeridianAnvilHandlers.register();
        WardenLootHandler.register();
        EnchantmentEffectHandler.register();
        ArmorTickHandler.register();
        ToolEnchantmentHandler.register();
        MountedEnchantmentHandler.register();
        TetherHandler.register();
        AurifyHandler.register();
        MeridianNetworking.registerPayloads();
        registerLifecycleEvents();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                MeridianCommand.register(dispatcher));
    }

    private void registerLifecycleEvents() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> rebuildEnchantmentInfo(server));

        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> {
            if (success) {
                rebuildEnchantmentInfo(server);
                syncEnchantmentInfoToAll(server);
            }
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            EnchantmentInfoPayload payload = EnchantmentInfoRegistry.buildPayload();
            ServerPlayNetworking.send(handler.player, payload);
        });
    }

    private static void rebuildEnchantmentInfo(MinecraftServer server) {
        Registry<Enchantment> reg = server.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        EnchantmentInfoRegistry.rebuild(reg, config);
    }

    private static void syncEnchantmentInfoToAll(MinecraftServer server) {
        EnchantmentInfoPayload payload = EnchantmentInfoRegistry.buildPayload();
        for (ServerPlayer sp : server.getPlayerList().getPlayers()) {
            ServerPlayNetworking.send(sp, payload);
        }
    }

    public static MeridianConfig getConfig() {
        return config;
    }

    public static void reloadConfig() {
        config = MeridianConfig.load();
    }

    public static void reloadConfig(MinecraftServer server) {
        config = MeridianConfig.load();
        rebuildEnchantmentInfo(server);
        syncEnchantmentInfoToAll(server);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
