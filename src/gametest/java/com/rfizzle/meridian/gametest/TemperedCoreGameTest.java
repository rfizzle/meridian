// Tier: 3 (Fabric Gametest)
package com.rfizzle.meridian.gametest;

import com.rfizzle.meridian.MeridianRegistry;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

public class TemperedCoreGameTest implements FabricGameTest {

    private static final BlockPos ANVIL_POS = new BlockPos(1, 1, 1);

    private AnvilMenu openAnvil(GameTestHelper helper) {
        helper.setBlock(ANVIL_POS, Blocks.ANVIL.defaultBlockState());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.experienceLevel = 30;
        return new AnvilMenu(
                1, player.getInventory(),
                ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(ANVIL_POS)));
    }

    @GameTest(template = "meridian:empty_3x3")
    public void damagedEnchantedSwordPlusCoreProducesUnbreakableOutput(GameTestHelper helper) {
        AnvilMenu menu = openAnvil(helper);

        Registry<Enchantment> reg = helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.enchant(reg.getHolderOrThrow(Enchantments.SHARPNESS), 5);
        sword.setDamageValue(700);

        menu.getSlot(0).set(sword);
        menu.getSlot(1).set(new ItemStack(MeridianRegistry.TEMPERED_CORE));

        ItemStack output = menu.getSlot(2).getItem();
        if (output.isEmpty()) {
            helper.fail("Output slot should contain the unbreakable sword, but is empty");
            return;
        }
        if (!output.has(DataComponents.UNBREAKABLE)) {
            helper.fail("Output must carry minecraft:unbreakable");
            return;
        }
        if (output.getDamageValue() != 0) {
            helper.fail("Output damage should be healed to 0, got " + output.getDamageValue());
            return;
        }
        ItemEnchantments outputEnchants = EnchantmentHelper.getEnchantmentsForCrafting(output);
        if (outputEnchants.getLevel(reg.getHolderOrThrow(Enchantments.SHARPNESS)) != 5) {
            helper.fail("Sharpness 5 should be preserved on the unbreakable output");
            return;
        }
        if (menu.getCost() != 10) {
            helper.fail("Application should cost 10 levels, got " + menu.getCost());
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void alreadyUnbreakableInputDeclined(GameTestHelper helper) {
        AnvilMenu menu = openAnvil(helper);

        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.set(DataComponents.UNBREAKABLE, new Unbreakable(true));

        menu.getSlot(0).set(sword);
        menu.getSlot(1).set(new ItemStack(MeridianRegistry.TEMPERED_CORE));

        if (!menu.getSlot(2).getItem().isEmpty()) {
            helper.fail("Already-unbreakable input + core must produce no output (one core per item)");
            return;
        }
        helper.succeed();
    }
}
