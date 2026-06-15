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
        Holder<Enchantment> curseOfDecay = reg.getHolderOrThrow(reg.getResourceKey(reg.get(Meridian.id("curse_of_decay"))).get());

        if (!curseOfDecay.is(EnchantmentTags.CURSE)) {
            helper.fail("meridian:curse_of_decay should be in minecraft:curse");
        }

        if (!curseOfDecay.is(EnchantmentTags.TRADEABLE)) {
            helper.fail("meridian:curse_of_decay should be in minecraft:tradeable");
        }

        if (!curseOfDecay.is(EnchantmentTags.DOUBLE_TRADE_PRICE)) {
            helper.fail("meridian:curse_of_decay should be in minecraft:double_trade_price");
        }

        if (curseOfDecay.is(EnchantmentTags.NON_TREASURE)) {
            helper.fail("meridian:curse_of_decay should NOT be in minecraft:non_treasure");
        }

        helper.succeed();
    }
}
