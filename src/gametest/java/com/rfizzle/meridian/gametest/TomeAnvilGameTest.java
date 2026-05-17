// Tier: 3 (Fabric Gametest)
package com.rfizzle.meridian.gametest;

import com.rfizzle.meridian.MeridianRegistry;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
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
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

public class TomeAnvilGameTest implements FabricGameTest {

    private static final BlockPos ANVIL_POS = new BlockPos(1, 1, 1);

    // --- TEST-5.2-T3a: ScrapTomeHandler end-to-end ---

    @GameTest(template = "meridian:empty_3x3")
    public void scrapTomeProducesBookWithOneEnchantment(GameTestHelper helper) {
        AnvilMenu menu = setupAnvil(helper);
        Registry<Enchantment> reg = helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT);

        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.enchant(reg.getHolderOrThrow(Enchantments.SHARPNESS), 5);
        sword.enchant(reg.getHolderOrThrow(Enchantments.UNBREAKING), 3);
        sword.enchant(reg.getHolderOrThrow(Enchantments.FIRE_ASPECT), 2);

        menu.getSlot(0).set(sword);
        menu.getSlot(1).set(new ItemStack(MeridianRegistry.SCRAP_TOME));

        ItemStack output = menu.getSlot(2).getItem();
        if (output.isEmpty()) {
            helper.fail("ScrapTome + enchanted sword should produce output");
            return;
        }
        if (!output.is(Items.ENCHANTED_BOOK)) {
            helper.fail("Output should be an enchanted book, got " + output.getItem());
            return;
        }
        ItemEnchantments stored = output.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (stored.size() != 1) {
            helper.fail("Scrap tome should produce exactly 1 enchantment on output, got " + stored.size());
            return;
        }
        helper.succeed();
    }

    // --- TEST-5.2-T3b: ImprovedScrapTomeHandler end-to-end ---

    @GameTest(template = "meridian:empty_3x3")
    public void improvedScrapTomeTransfersAllEnchantments(GameTestHelper helper) {
        AnvilMenu menu = setupAnvil(helper);
        Registry<Enchantment> reg = helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT);

        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.enchant(reg.getHolderOrThrow(Enchantments.SHARPNESS), 5);
        sword.enchant(reg.getHolderOrThrow(Enchantments.UNBREAKING), 3);
        sword.enchant(reg.getHolderOrThrow(Enchantments.FIRE_ASPECT), 2);

        menu.getSlot(0).set(sword);
        menu.getSlot(1).set(new ItemStack(MeridianRegistry.IMPROVED_SCRAP_TOME));

        ItemStack output = menu.getSlot(2).getItem();
        if (output.isEmpty()) {
            helper.fail("ImprovedScrapTome + enchanted sword should produce output");
            return;
        }
        if (!output.is(Items.ENCHANTED_BOOK)) {
            helper.fail("Output should be an enchanted book");
            return;
        }
        ItemEnchantments stored = output.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (stored.size() != 3) {
            helper.fail("Improved scrap tome should transfer all 3 enchantments, got " + stored.size());
            return;
        }
        if (stored.getLevel(reg.getHolderOrThrow(Enchantments.SHARPNESS)) != 5) {
            helper.fail("Sharpness 5 should be preserved");
            return;
        }
        helper.succeed();
    }

    // --- TEST-5.2-T3c: ExtractionTomeHandler end-to-end ---

    @GameTest(template = "meridian:empty_3x3")
    public void extractionTomeProducesBookAndPreservesItem(GameTestHelper helper) {
        AnvilMenu menu = setupAnvil(helper);
        Registry<Enchantment> reg = helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT);

        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.enchant(reg.getHolderOrThrow(Enchantments.SHARPNESS), 5);
        sword.enchant(reg.getHolderOrThrow(Enchantments.UNBREAKING), 3);

        menu.getSlot(0).set(sword);
        menu.getSlot(1).set(new ItemStack(MeridianRegistry.EXTRACTION_TOME));

        ItemStack output = menu.getSlot(2).getItem();
        if (output.isEmpty()) {
            helper.fail("ExtractionTome + enchanted sword should produce output");
            return;
        }
        if (!output.is(Items.ENCHANTED_BOOK)) {
            helper.fail("Output should be an enchanted book");
            return;
        }
        ItemEnchantments stored = output.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (stored.size() != 2) {
            helper.fail("Extraction tome should transfer all enchantments to book, got " + stored.size());
            return;
        }
        helper.succeed();
    }

    // --- TEST-5.2-T3d: ExtractionTomeFuelSlotRepairHandler ---

    @GameTest(template = "meridian:empty_3x3")
    public void extractionTomeRepairsDamagedUnenchantedSword(GameTestHelper helper) {
        AnvilMenu menu = setupAnvil(helper);

        ItemStack damagedSword = new ItemStack(Items.DIAMOND_SWORD);
        damagedSword.setDamageValue(500);

        menu.getSlot(0).set(damagedSword);
        menu.getSlot(1).set(new ItemStack(MeridianRegistry.EXTRACTION_TOME));

        ItemStack output = menu.getSlot(2).getItem();
        if (output.isEmpty()) {
            helper.fail("ExtractionTome + damaged unenchanted sword should produce repaired output");
            return;
        }
        if (!output.is(Items.DIAMOND_SWORD)) {
            helper.fail("Fuel-slot repair output should be a diamond sword, got " + output.getItem());
            return;
        }
        int outputDamage = output.getDamageValue();
        if (outputDamage >= 500) {
            helper.fail("Repaired sword should have less damage than 500, got " + outputDamage);
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void unenchantedUndamagedSwordWithTomeDeclines(GameTestHelper helper) {
        AnvilMenu menu = setupAnvil(helper);

        menu.getSlot(0).set(new ItemStack(Items.DIAMOND_SWORD));
        menu.getSlot(1).set(new ItemStack(MeridianRegistry.EXTRACTION_TOME));

        ItemStack output = menu.getSlot(2).getItem();
        if (!output.isEmpty()) {
            if (output.is(Items.DIAMOND_SWORD)) {
                helper.fail("Pristine unenchanted sword + extraction tome should decline");
                return;
            }
        }
        helper.succeed();
    }

    // --- Helpers ---

    private AnvilMenu setupAnvil(GameTestHelper helper) {
        helper.setBlock(ANVIL_POS, Blocks.ANVIL.defaultBlockState());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.experienceLevel = 30;
        BlockPos absAnvil = helper.absolutePos(ANVIL_POS);
        return new AnvilMenu(
                1, player.getInventory(),
                ContainerLevelAccess.create(helper.getLevel(), absAnvil));
    }
}
