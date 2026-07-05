package com.rfizzle.meridian.gametest.enchantments;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.event.EnchantmentEffectHandler;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Covers the {@code combat.sunderAffectsPlayers} toggle's enabled path (the disabled
 * default is asserted in {@code CombatEnchantmentGameTest}). Rewrites the shared
 * {@code config/meridian.json}, so it follows {@code ConfigDisableEnchantmentTest}'s
 * rule: a unique {@code batch} serializes it against the other config-mutating tests.
 */
public class SunderConfigGameTest implements FabricGameTest {

    private static final Path CONFIG_FILE =
            FabricLoader.getInstance().getConfigDir().resolve("meridian.json");

    @GameTest(template = "meridian:empty_3x3", batch = "configMutation9")
    public void sunderAffectsPlayersWhenConfigEnabled(GameTestHelper helper) {
        byte[] original;
        try {
            original = Files.readAllBytes(CONFIG_FILE);
        } catch (IOException e) {
            helper.fail("Could not read config file: " + e.getMessage());
            return;
        }

        String enabledConfig = "{\n"
                + "  \"configVersion\": 1,\n"
                + "  \"combat\": { \"sunderAffectsPlayers\": true }\n"
                + "}";
        try {
            Files.writeString(CONFIG_FILE, enabledConfig);
            Meridian.reloadConfig(helper.getLevel().getServer());
        } catch (IOException e) {
            Meridian.LOGGER.error("Could not write config in sunderAffectsPlayersWhenConfigEnabled", e);
            try { Files.write(CONFIG_FILE, original); } catch (IOException ignored) {}
            helper.fail("Could not write config: " + e.getMessage());
            return;
        }

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        try {
            if (!EnchantmentEffectHandler.sunderVictimAllowed(player)) {
                helper.fail("Players should be eligible Sunder victims with combat.sunderAffectsPlayers=true");
                return;
            }
            helper.succeed();
        } finally {
            player.discard();
            try {
                Files.write(CONFIG_FILE, original);
            } catch (IOException e) {
                Meridian.LOGGER.error("Failed to restore config", e);
            }
            Meridian.reloadConfig(helper.getLevel().getServer());
        }
    }
}
