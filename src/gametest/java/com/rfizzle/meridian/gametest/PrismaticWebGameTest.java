// Tier: 3 (Fabric Gametest)
package com.rfizzle.meridian.gametest;

import com.rfizzle.meridian.MeridianRegistry;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

public class PrismaticWebGameTest implements FabricGameTest {

    private static final BlockPos ANVIL_POS = new BlockPos(1, 1, 1);

    @GameTest(template = "meridian:empty_3x3")
    public void cursedSwordPlusWebProducesUncursedOutput(GameTestHelper helper) {
        helper.setBlock(ANVIL_POS, Blocks.ANVIL.defaultBlockState());

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.experienceLevel = 30;
        BlockPos absAnvil = helper.absolutePos(ANVIL_POS);

        AnvilMenu menu = new AnvilMenu(
                1, player.getInventory(),
                ContainerLevelAccess.create(helper.getLevel(), absAnvil));

        Registry<Enchantment> reg = helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        ItemStack cursedSword = new ItemStack(Items.DIAMOND_SWORD);
        cursedSword.enchant(reg.getHolderOrThrow(Enchantments.VANISHING_CURSE), 1);
        cursedSword.enchant(reg.getHolderOrThrow(Enchantments.SHARPNESS), 5);

        ItemStack web = new ItemStack(MeridianRegistry.PRISMATIC_WEB);

        menu.getSlot(0).set(cursedSword);
        menu.getSlot(1).set(web);

        ItemStack output = menu.getSlot(2).getItem();
        if (output.isEmpty()) {
            helper.fail("Output slot should contain uncursed sword, but is empty");
            return;
        }

        ItemEnchantments outputEnchants = EnchantmentHelper.getEnchantmentsForCrafting(output);
        for (var entry : outputEnchants.entrySet()) {
            if (entry.getKey().is(EnchantmentTags.CURSE)) {
                helper.fail("Output should have no curses, found " + entry.getKey());
                return;
            }
        }

        Holder<Enchantment> sharpness = reg.getHolderOrThrow(Enchantments.SHARPNESS);
        if (outputEnchants.getLevel(sharpness) != 5) {
            helper.fail("Sharpness 5 should be preserved, got level " + outputEnchants.getLevel(sharpness));
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void nonCursedSwordDeclined(GameTestHelper helper) {
        helper.setBlock(ANVIL_POS, Blocks.ANVIL.defaultBlockState());

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.experienceLevel = 30;
        BlockPos absAnvil = helper.absolutePos(ANVIL_POS);

        AnvilMenu menu = new AnvilMenu(
                1, player.getInventory(),
                ContainerLevelAccess.create(helper.getLevel(), absAnvil));

        Registry<Enchantment> reg = helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.enchant(reg.getHolderOrThrow(Enchantments.SHARPNESS), 5);

        menu.getSlot(0).set(sword);
        menu.getSlot(1).set(new ItemStack(MeridianRegistry.PRISMATIC_WEB));

        ItemStack output = menu.getSlot(2).getItem();
        if (!output.isEmpty()) {
            ItemEnchantments outEnch = EnchantmentHelper.getEnchantmentsForCrafting(output);
            boolean hasSharp = outEnch.getLevel(reg.getHolderOrThrow(Enchantments.SHARPNESS)) == 5;
            boolean sameSize = outEnch.size() == 1;
            if (hasSharp && sameSize) {
                helper.fail("Non-cursed sword + web should decline (output should be empty or vanilla result, not stripped)");
                return;
            }
        }
        helper.succeed();
    }
}
