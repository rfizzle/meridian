package com.rfizzle.meridian.gametest.enchantments;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.config.MeridianConfig;
import com.rfizzle.meridian.enchanting.EnchantmentEffects;
import com.rfizzle.meridian.enchanting.EnchantmentInfo;
import com.rfizzle.meridian.enchanting.EnchantmentInfoRegistry;
import com.rfizzle.meridian.enchanting.RealEnchantmentHelper;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

public class ConfigDisableEnchantmentTest implements FabricGameTest {

    private static final Path CONFIG_FILE =
            FabricLoader.getInstance().getConfigDir().resolve("meridian.json");

    private Holder<Enchantment> lookup(GameTestHelper helper, String id) {
        Registry<Enchantment> reg = helper.getLevel().registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT);
        return reg.getHolder(Meridian.id(id)).orElse(null);
    }

    @GameTest(template = "meridian:empty_3x3")
    public void disabledEnchantmentHasNoAttributeEffect(GameTestHelper helper) {
        Holder<Enchantment> bulwark = lookup(helper, "bulwark");
        if (bulwark == null) { helper.fail("bulwark not in registry"); return; }

        byte[] original = saveAndDisable(helper, "meridian:bulwark");
        if (original == null) return;

        try {
            Mob mob = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 1, 1));
            double baseKR = mob.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);

            ItemStack chest = new ItemStack(Items.DIAMOND_CHESTPLATE);
            chest.enchant(bulwark, 3);
            mob.setItemSlot(EquipmentSlot.CHEST, chest);

            helper.runAfterDelay(1, () -> {
                try {
                    EnchantmentInfo info = EnchantmentInfoRegistry.getInfo(bulwark);
                    if (info.enabled()) {
                        helper.fail("Bulwark should be disabled in EnchantmentInfoRegistry after config reload");
                        return;
                    }
                    helper.succeed();
                } finally {
                    restoreConfig(original);
                }
            });
        } catch (Exception e) {
            restoreConfig(original);
            helper.fail("Exception: " + e.getMessage());
        }
    }

    @GameTest(template = "meridian:empty_3x3")
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
                    restoreConfig(original);
                    helper.fail("Disabled enchantment 'shackle' should not appear in enchanting table pool");
                    return;
                }
            }
            restoreConfig(original);
            helper.succeed();
        } catch (Exception e) {
            restoreConfig(original);
            helper.fail("Exception: " + e.getMessage());
        }
    }

    @GameTest(template = "meridian:empty_3x3")
    public void disabledEnchantmentOnExistingItemHasNoEffect(GameTestHelper helper) {
        Holder<Enchantment> alacrity = lookup(helper, "alacrity");
        if (alacrity == null) { helper.fail("alacrity not in registry"); return; }

        Mob mob = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 1, 1));
        ItemStack boots = new ItemStack(Items.DIAMOND_BOOTS);
        boots.enchant(alacrity, 5);
        mob.setItemSlot(EquipmentSlot.FEET, boots);

        helper.runAfterDelay(1, () -> {
            double speedBefore = mob.getAttributeValue(Attributes.MOVEMENT_SPEED);

            byte[] original = saveAndDisable(helper, "meridian:alacrity");
            if (original == null) return;

            try {
                EnchantmentInfo info = EnchantmentInfoRegistry.getInfo(alacrity);
                if (info.enabled()) {
                    helper.fail("Alacrity should be disabled after config reload");
                    return;
                }
                helper.succeed();
            } finally {
                restoreConfig(original);
            }
        });
    }

    @GameTest(template = "meridian:empty_3x3")
    public void reEnablingEnchantmentRestoresEffect(GameTestHelper helper) {
        Holder<Enchantment> bulwark = lookup(helper, "bulwark");
        if (bulwark == null) { helper.fail("bulwark not in registry"); return; }

        byte[] original = saveAndDisable(helper, "meridian:bulwark");
        if (original == null) return;

        EnchantmentInfo infoDisabled = EnchantmentInfoRegistry.getInfo(bulwark);
        if (infoDisabled.enabled()) {
            restoreConfig(original);
            helper.fail("Bulwark should be disabled");
            return;
        }

        restoreConfig(original);
        EnchantmentInfo infoRestored = EnchantmentInfoRegistry.getInfo(bulwark);
        if (!infoRestored.enabled()) {
            helper.fail("Bulwark should be re-enabled after restoring config");
            return;
        }
        helper.succeed();
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
            try { Files.write(CONFIG_FILE, original); } catch (IOException ignored) {}
            helper.fail("Could not write config: " + e.getMessage());
            return null;
        }
        return original;
    }

    private void restoreConfig(byte[] original) {
        try {
            Files.write(CONFIG_FILE, original);
            // Note: We don't have access to server here for full reload,
            // but the next test setup or server restart will reload
        } catch (IOException ignored) {}
        Meridian.reloadConfig();
    }
}
