// Tier: 3 (Fabric Gametest)
package com.rfizzle.meridian.gametest.enchantments;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.event.ProjectileEnchantmentHandler;
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
 * Covers the {@code combat.seekerTargetsPlayers} toggle's enabled path (the disabled
 * default is asserted in {@code RangedEnchantmentGameTest}). Rewrites the shared
 * {@code config/meridian.json}, so it follows {@code ConfigDisableEnchantmentTest}'s
 * rule: a unique {@code batch} serializes it against the other config-mutating tests.
 */
public class SeekerConfigGameTest implements FabricGameTest {

    private static final Path CONFIG_FILE =
            FabricLoader.getInstance().getConfigDir().resolve("meridian.json");

    @GameTest(template = "meridian:empty_3x3", batch = "configMutation10")
    public void seekerTargetsPlayersWhenConfigEnabled(GameTestHelper helper) {
        byte[] original;
        try {
            original = Files.readAllBytes(CONFIG_FILE);
        } catch (IOException e) {
            helper.fail("Could not read config file: " + e.getMessage());
            return;
        }

        String enabledConfig = "{\n"
                + "  \"configVersion\": 1,\n"
                + "  \"combat\": { \"seekerTargetsPlayers\": true }\n"
                + "}";
        try {
            Files.writeString(CONFIG_FILE, enabledConfig);
            Meridian.reloadConfig(helper.getLevel().getServer());
        } catch (IOException e) {
            Meridian.LOGGER.error("Could not write config in seekerTargetsPlayersWhenConfigEnabled", e);
            try { Files.write(CONFIG_FILE, original); } catch (IOException ignored) {}
            helper.fail("Could not write config: " + e.getMessage());
            return;
        }

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        try {
            if (!ProjectileEnchantmentHandler.seekerTargetAllowed(player)) {
                helper.fail("Players should be eligible Seeker targets with combat.seekerTargetsPlayers=true");
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
