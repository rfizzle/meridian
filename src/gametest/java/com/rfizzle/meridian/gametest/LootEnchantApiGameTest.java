// Tier: 3 (Fabric Gametest)
package com.rfizzle.meridian.gametest;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.api.MeridianAPI;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * End-to-end coverage for {@link MeridianAPI#rollLootEnchantments} and the loot-table
 * {@code maxLootLevel} enforcement that the {@code EnchantmentHelper} mixin applies.
 *
 * <p>Several tests rewrite the shared {@code config/meridian.json} and reload the global config
 * singleton, so each config-mutating test gets a unique {@code batch}: vanilla runs batches
 * strictly sequentially, which keeps concurrent tests from observing — or saving and restoring —
 * another test's mutated config (see {@code ConfigDisableEnchantmentTest}).
 */
public class LootEnchantApiGameTest implements FabricGameTest {

    private static final Path CONFIG_FILE =
            FabricLoader.getInstance().getConfigDir().resolve("meridian.json");

    @GameTest(template = "meridian:empty_3x3")
    public void rollIsDeterministicForAGivenSeed(GameTestHelper helper) {
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        List<EnchantmentInstance> first = MeridianAPI.rollLootEnchantments(
                helper.getLevel(), RandomSource.create(42L), sword, 40, false);
        List<EnchantmentInstance> second = MeridianAPI.rollLootEnchantments(
                helper.getLevel(), RandomSource.create(42L), sword, 40, false);

        if (!sameRoll(first, second)) {
            helper.fail("Same seed must produce the same roll: " + describe(first)
                    + " vs " + describe(second));
            return;
        }
        if (first.isEmpty()) {
            helper.fail("A diamond sword at power 40 should roll at least one enchantment");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3", batch = "lootApiMutation1")
    public void maxLootLevelClampsRolledLevels(GameTestHelper helper) {
        byte[] original = applyOverrides(helper,
                "\"minecraft:sharpness\": { \"maxLootLevel\": 1 }");
        if (original == null) return;
        try {
            ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
            boolean sawSharpness = false;
            for (long seed = 0L; seed < 400L; seed++) {
                List<EnchantmentInstance> rolled = MeridianAPI.rollLootEnchantments(
                        helper.getLevel(), RandomSource.create(seed), sword, 50, false);
                for (EnchantmentInstance inst : rolled) {
                    if (inst.enchantment.is(Enchantments.SHARPNESS)) {
                        sawSharpness = true;
                        if (inst.level > 1) {
                            helper.fail("Sharpness rolled at level " + inst.level
                                    + " despite maxLootLevel=1 (seed " + seed + ")");
                            return;
                        }
                    }
                }
            }
            if (!sawSharpness) {
                helper.fail("Sharpness never rolled across 400 seeds; cap assertion is vacuous");
                return;
            }
            helper.succeed();
        } finally {
            restore(original, helper);
        }
    }

    @GameTest(template = "meridian:empty_3x3", batch = "lootApiMutation2")
    public void disabledEnchantmentNeverRolls(GameTestHelper helper) {
        byte[] original = applyOverrides(helper,
                "\"minecraft:sharpness\": { \"enabled\": false }");
        if (original == null) return;
        try {
            ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
            for (long seed = 0L; seed < 400L; seed++) {
                List<EnchantmentInstance> rolled = MeridianAPI.rollLootEnchantments(
                        helper.getLevel(), RandomSource.create(seed), sword, 50, false);
                for (EnchantmentInstance inst : rolled) {
                    if (inst.enchantment.is(Enchantments.SHARPNESS)) {
                        helper.fail("Disabled Sharpness rolled at seed " + seed);
                        return;
                    }
                }
            }
            helper.succeed();
        } finally {
            restore(original, helper);
        }
    }

    @GameTest(template = "meridian:empty_3x3")
    public void treasureEnchantsExcludedWhenDisallowed(GameTestHelper helper) {
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        for (long seed = 0L; seed < 400L; seed++) {
            List<EnchantmentInstance> rolled = MeridianAPI.rollLootEnchantments(
                    helper.getLevel(), RandomSource.create(seed), sword, 50, false);
            for (EnchantmentInstance inst : rolled) {
                if (inst.enchantment.is(EnchantmentTags.TREASURE)) {
                    helper.fail("Treasure enchantment " + inst.enchantment.unwrapKey().orElseThrow()
                            + " rolled with treasureAllowed=false (seed " + seed + ")");
                    return;
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3", batch = "lootApiMutation3")
    public void lootTableRollIsClampedByMixin(GameTestHelper helper) {
        byte[] original = applyOverrides(helper,
                "\"minecraft:sharpness\": { \"maxLootLevel\": 1 }");
        if (original == null) return;
        try {
            Registry<Enchantment> registry = enchantmentRegistry(helper);
            Holder<Enchantment> sharpness = registry.getHolderOrThrow(Enchantments.SHARPNESS);
            ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);

            boolean sawSharpness = false;
            for (long seed = 0L; seed < 400L; seed++) {
                RandomSource random = RandomSource.create(seed);
                // Vanilla loot path: this is the mixed-in EnchantmentHelper.selectEnchantment.
                List<EnchantmentInstance> rolled = EnchantmentHelper.selectEnchantment(
                        random, sword, 50, java.util.stream.Stream.of(sharpness));
                for (EnchantmentInstance inst : rolled) {
                    if (inst.enchantment.is(Enchantments.SHARPNESS)) {
                        sawSharpness = true;
                        if (inst.level > 1) {
                            helper.fail("Loot-table Sharpness at level " + inst.level
                                    + " escaped the maxLootLevel=1 clamp (seed " + seed + ")");
                            return;
                        }
                    }
                }
            }
            if (!sawSharpness) {
                helper.fail("Sharpness never selected by the loot path; clamp assertion is vacuous");
                return;
            }
            helper.succeed();
        } finally {
            restore(original, helper);
        }
    }

    private static Registry<Enchantment> enchantmentRegistry(GameTestHelper helper) {
        return helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
    }

    private static boolean sameRoll(List<EnchantmentInstance> a, List<EnchantmentInstance> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            ResourceKey<Enchantment> ka = a.get(i).enchantment.unwrapKey().orElseThrow();
            ResourceKey<Enchantment> kb = b.get(i).enchantment.unwrapKey().orElseThrow();
            if (!ka.equals(kb) || a.get(i).level != b.get(i).level) return false;
        }
        return true;
    }

    private static String describe(List<EnchantmentInstance> list) {
        StringBuilder sb = new StringBuilder("[");
        for (EnchantmentInstance inst : list) {
            sb.append(inst.enchantment.unwrapKey().map(ResourceKey::location).orElse(null))
                    .append('@').append(inst.level).append(' ');
        }
        return sb.append(']').toString();
    }

    private byte[] applyOverrides(GameTestHelper helper, String overrideEntries) {
        byte[] original;
        try {
            original = Files.readAllBytes(CONFIG_FILE);
        } catch (IOException e) {
            helper.fail("Could not read config file: " + e.getMessage());
            return null;
        }
        String json = "{\n"
                + "  \"configVersion\": 1,\n"
                + "  \"enchantmentOverrides\": {\n"
                + "    " + overrideEntries + "\n"
                + "  }\n"
                + "}";
        try {
            Files.writeString(CONFIG_FILE, json);
            Meridian.reloadConfig(helper.getLevel().getServer());
        } catch (IOException e) {
            Meridian.LOGGER.error("Could not write config in applyOverrides", e);
            try { Files.write(CONFIG_FILE, original); } catch (IOException ignored) {}
            helper.fail("Could not write config: " + e.getMessage());
            return null;
        }
        return original;
    }

    private void restore(byte[] original, GameTestHelper helper) {
        try {
            Files.write(CONFIG_FILE, original);
        } catch (IOException e) {
            Meridian.LOGGER.error("Failed to restore config", e);
        }
        Meridian.reloadConfig(helper.getLevel().getServer());
    }
}
