// Tier: 3 (Fabric Gametest)
package com.rfizzle.meridian.gametest;

import com.rfizzle.meridian.Meridian;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.List;

public class EnchantmentTagGameTest implements FabricGameTest {

    @GameTest(template = "meridian:empty_3x3")
    public void meridianEnchantmentsInNonTreasureTag(GameTestHelper helper) {
        Registry<Enchantment> reg = helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> alacrity = reg.getHolderOrThrow(reg.getResourceKey(reg.get(Meridian.id("alacrity"))).get());

        if (!alacrity.is(EnchantmentTags.NON_TREASURE)) {
            helper.fail("meridian:alacrity should be in minecraft:non_treasure");
        }

        // Also check if it propagates to IN_ENCHANTING_TABLE
        if (!alacrity.is(EnchantmentTags.IN_ENCHANTING_TABLE)) {
            helper.fail("meridian:alacrity should be in minecraft:in_enchanting_table (via non_treasure)");
        }

        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void meridianTreasureEnchantmentsInTreasureTag(GameTestHelper helper) {
        Registry<Enchantment> reg = helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> abyssWard = reg.getHolderOrThrow(reg.getResourceKey(reg.get(Meridian.id("abyss_ward"))).get());

        if (!abyssWard.is(EnchantmentTags.TREASURE)) {
            helper.fail("meridian:abyss_ward should be in minecraft:treasure");
        }

        if (!abyssWard.is(EnchantmentTags.TRADEABLE)) {
            helper.fail("meridian:abyss_ward should be in minecraft:tradeable");
        }

        if (abyssWard.is(EnchantmentTags.IN_ENCHANTING_TABLE)) {
            helper.fail("meridian:abyss_ward (treasure) should NOT be in minecraft:in_enchanting_table");
        }

        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void meridianCursesInCurseTag(GameTestHelper helper) {
        Registry<Enchantment> reg = helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT);

        // Every Meridian curse must carry identical tag membership so the cleansing/removal paths
        // and the trade/loot pool treat them all like Curse of Decay.
        for (String id : List.of("curse_of_decay", "curse_of_sealing", "curse_of_echoes",
                "curse_of_hunger", "curse_of_attraction", "curse_of_leaden",
                "curse_of_blunting", "curse_of_fumbling", "curse_of_wavering", "curse_of_timidity",
                "curse_of_molting", "curse_of_skittishness", "curse_of_obscurity")) {
            Holder<Enchantment> curse =
                    reg.getHolderOrThrow(reg.getResourceKey(reg.get(Meridian.id(id))).get());

            if (!curse.is(EnchantmentTags.CURSE)) {
                helper.fail("meridian:" + id + " should be in minecraft:curse");
            }
            if (!curse.is(EnchantmentTags.TRADEABLE)) {
                helper.fail("meridian:" + id + " should be in minecraft:tradeable");
            }
            if (!curse.is(EnchantmentTags.DOUBLE_TRADE_PRICE)) {
                helper.fail("meridian:" + id + " should be in minecraft:double_trade_price");
            }
            if (curse.is(EnchantmentTags.NON_TREASURE)) {
                helper.fail("meridian:" + id + " should NOT be in minecraft:non_treasure");
            }
        }

        helper.succeed();
    }
}
