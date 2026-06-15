package com.rfizzle.meridian.gametest.enchantments;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.api.EnchantmentInfo;
import com.rfizzle.meridian.enchanting.EnchantmentInfoRegistry;
import com.rfizzle.meridian.enchanting.RealEnchantmentHelper;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/**
 * Each test below rewrites the shared {@code config/meridian.json} and reloads the global
 * config singleton, so no two of them may run in the same gametest batch: batches run their
 * tests interleaved on the server thread, and a concurrent test would capture (and later
 * restore) another test's mutated config instead of the pristine one. A unique {@code batch}
 * per config-mutating test serializes them — vanilla runs batches strictly one at a time.
 */
public class ConfigDisableEnchantmentTest implements FabricGameTest {

    private static final Path CONFIG_FILE =
            FabricLoader.getInstance().getConfigDir().resolve("meridian.json");

    private Holder<Enchantment> lookup(GameTestHelper helper, String id) {
        Registry<Enchantment> reg = helper.getLevel().registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT);
        return reg.getHolder(Meridian.id(id)).orElse(null);
    }

    @GameTest(template = "meridian:empty_3x3", batch = "configMutation1")
    public void disabledEnchantmentSuppressesItemGlint(GameTestHelper helper) {
        Holder<Enchantment> bulwark = lookup(helper, "bulwark");
        if (bulwark == null) { helper.fail("bulwark not in registry"); return; }

        ItemStack chest = new ItemStack(Items.DIAMOND_CHESTPLATE);
        chest.enchant(bulwark, 3);
        if (!chest.hasFoil()) {
            helper.fail("Sanity: an item with an enabled enchantment should have a glint");
            return;
        }

        byte[] original = saveAndDisable(helper, "meridian:bulwark");
        if (original == null) return;

        try {
            // Disabling does not strip the enchantment from gear that already carries it, but it
            // does mark the item inert: with every enchantment disabled, the glint is suppressed.
            if (chest.hasFoil()) {
                helper.fail("Disabling the only enchantment on an item should suppress its glint");
                return;
            }
            helper.succeed();
        } finally {
            restoreConfig(original, helper.getLevel().getServer());
        }
    }

    @GameTest(template = "meridian:empty_3x3", batch = "configMutation2")
    public void disabledEnchantmentNotInTablePool(GameTestHelper helper) {
        byte[] original = saveAndDisable(helper, "meridian:shackle");
        if (original == null) return;

        try {
            Registry<Enchantment> reg = helper.getLevel().registryAccess()
                    .registryOrThrow(Registries.ENCHANTMENT);
            ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
            List<EnchantmentInstance> results = RealEnchantmentHelper.getAvailableEnchantmentResults(
                    30, sword, reg, false, Set.of());

            ResourceKey<Enchantment> shackleKey = ResourceKey.create(
                    Registries.ENCHANTMENT, Meridian.id("shackle"));
            for (EnchantmentInstance inst : results) {
                if (inst.enchantment.is(shackleKey)) {
                    helper.fail("Disabled enchantment 'shackle' should not appear in enchanting table pool");
                    return;
                }
            }
            helper.succeed();
        } finally {
            restoreConfig(original, helper.getLevel().getServer());
        }
    }

    @GameTest(template = "meridian:empty_3x3", batch = "configMutation3")
    public void glintRetainedWhenAnotherEnchantmentStillEnabled(GameTestHelper helper) {
        Holder<Enchantment> bulwark = lookup(helper, "bulwark");
        Holder<Enchantment> alacrity = lookup(helper, "alacrity");
        if (bulwark == null || alacrity == null) {
            helper.fail("bulwark/alacrity not in registry");
            return;
        }

        ItemStack chest = new ItemStack(Items.DIAMOND_CHESTPLATE);
        chest.enchant(bulwark, 3);
        chest.enchant(alacrity, 1);

        byte[] original = saveAndDisable(helper, "meridian:bulwark");
        if (original == null) return;

        try {
            // Glint is suppressed only when every enchantment on the item is disabled. With
            // alacrity still enabled, disabling bulwark alone must leave the glint intact.
            if (!chest.hasFoil()) {
                helper.fail("Glint should remain while another enabled enchantment is present");
                return;
            }
            helper.succeed();
        } finally {
            restoreConfig(original, helper.getLevel().getServer());
        }
    }

    @GameTest(template = "meridian:empty_3x3", batch = "configMutation4")
    public void reEnablingEnchantmentRestoresEffect(GameTestHelper helper) {
        Holder<Enchantment> bulwark = lookup(helper, "bulwark");
        if (bulwark == null) { helper.fail("bulwark not in registry"); return; }

        byte[] original = saveAndDisable(helper, "meridian:bulwark");
        if (original == null) return;

        // The inline restore below is the re-enable under test; the finally is only a safety net
        // for the paths that fail before reaching it, so it must not restore a second time.
        boolean restored = false;
        try {
            EnchantmentInfo infoDisabled = EnchantmentInfoRegistry.getInfo(bulwark);
            if (infoDisabled.enabled()) {
                helper.fail("Bulwark should be disabled");
                return;
            }

            restoreConfig(original, helper.getLevel().getServer());
            restored = true;
            EnchantmentInfo infoRestored = EnchantmentInfoRegistry.getInfo(bulwark);
            if (!infoRestored.enabled()) {
                helper.fail("Bulwark should be re-enabled after restoring config");
                return;
            }
            helper.succeed();
        } finally {
            if (!restored) {
                restoreConfig(original, helper.getLevel().getServer());
            }
        }
    }

    private byte[] saveAndDisable(GameTestHelper helper, String enchantmentId) {
        byte[] original;
        try {
            original = Files.readAllBytes(CONFIG_FILE);
        } catch (IOException e) {
            helper.fail("Could not read config file: " + e.getMessage());
            return null;
        }

        String disabledConfig = "{\n"
                + "  \"configVersion\": 1,\n"
                + "  \"enchantmentOverrides\": {\n"
                + "    \"" + enchantmentId + "\": { \"enabled\": false }\n"
                + "  }\n"
                + "}";
        try {
            Files.writeString(CONFIG_FILE, disabledConfig);
            Meridian.reloadConfig(helper.getLevel().getServer());
        } catch (IOException e) {
            Meridian.LOGGER.error("Could not write config in saveAndDisable", e);
            try { Files.write(CONFIG_FILE, original); } catch (IOException ignored) {}
            helper.fail("Could not write config: " + e.getMessage());
            return null;
        }
        return original;
    }

    private void restoreConfig(byte[] original, net.minecraft.server.MinecraftServer server) {
        try {
            Files.write(CONFIG_FILE, original);
        } catch (IOException e) {
            Meridian.LOGGER.error("Failed to restore config", e);
        }
        Meridian.reloadConfig(server);
    }
}
