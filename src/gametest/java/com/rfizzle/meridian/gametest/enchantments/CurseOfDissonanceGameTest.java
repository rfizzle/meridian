// Tier: 3 (Fabric Gametest)
package com.rfizzle.meridian.gametest.enchantments;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.api.StatCollection;
import com.rfizzle.meridian.enchanting.DissonanceMath;
import com.rfizzle.meridian.enchanting.MeridianEnchantmentMenu;
import com.rfizzle.meridian.gametest.util.MockPlayers;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EnchantingTableBlock;

/**
 * End-to-end coverage for Curse of Dissonance's table sabotage. The reduction math is unit-tested
 * in {@code DissonanceMathTest}; this drives the real menu path — open a table, gather stats, then
 * equip and remove a cursed item and confirm the wearer's own session Eterna and Clues drop and
 * restore. The {@code broadcastChanges} tick hook is what makes the equip/unequip take effect
 * without touching the enchant slots, so it is exercised directly here.
 */
public class CurseOfDissonanceGameTest implements FabricGameTest {

    private static final BlockPos TABLE_POS = new BlockPos(4, 1, 4);

    private Holder<Enchantment> curse(GameTestHelper helper, String id) {
        Registry<Enchantment> reg = helper.getLevel().registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT);
        return reg.getHolder(Meridian.id(id)).orElse(null);
    }

    @GameTest(template = "meridian:shelf_scan_9x4x9")
    public void equippingReducesStatsAndRemovingRestores(GameTestHelper helper) {
        Holder<Enchantment> ench = curse(helper, "curse_of_dissonance");
        if (ench == null) { helper.fail("curse_of_dissonance not in registry"); return; }

        helper.setBlock(TABLE_POS, Blocks.ENCHANTING_TABLE.defaultBlockState());
        for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
            helper.setBlock(TABLE_POS.offset(offset), Blocks.BOOKSHELF.defaultBlockState());
        }

        ServerPlayer player = MockPlayers.serverPlayerInLevel(helper);
        try {
            BlockPos absTable = helper.absolutePos(TABLE_POS);
            MeridianEnchantmentMenu menu = new MeridianEnchantmentMenu(
                    1, player.getInventory(),
                    ContainerLevelAccess.create(helper.getLevel(), absTable));

            // Place an item so the recompute path runs and stores baseline session stats.
            menu.getSlot(0).set(new ItemStack(Items.DIAMOND_SWORD));
            StatCollection base = menu.getLastStats();
            if (base.eterna() <= DissonanceMath.ETERNA_REDUCTION_PER_LEVEL || base.clues() < 1) {
                helper.fail("precondition: baseline needs eterna > "
                        + DissonanceMath.ETERNA_REDUCTION_PER_LEVEL + " and clues >= 1, got eterna "
                        + base.eterna() + " clues " + base.clues());
                return;
            }
            float baseEterna = base.eterna();
            int baseClues = base.clues();

            // Equip a Dissonance-cursed pickaxe; the tick hook (broadcastChanges) must notice and
            // re-gather with the reduction applied, even though no enchant slot changed.
            ItemStack pick = new ItemStack(Items.DIAMOND_PICKAXE);
            pick.enchant(ench, 1);
            player.setItemSlot(EquipmentSlot.MAINHAND, pick);
            menu.broadcastChanges();

            StatCollection cursed = menu.getLastStats();
            float expectedEterna = baseEterna - DissonanceMath.ETERNA_REDUCTION_PER_LEVEL;
            int expectedClues = baseClues - DissonanceMath.CLUES_REDUCTION_PER_LEVEL;
            if (Math.abs(cursed.eterna() - expectedEterna) > 1e-4f) {
                helper.fail("Equipped Dissonance should drop eterna to " + expectedEterna
                        + ", got " + cursed.eterna());
                return;
            }
            if (cursed.clues() != expectedClues) {
                helper.fail("Equipped Dissonance should drop clues to " + expectedClues
                        + ", got " + cursed.clues());
                return;
            }

            // Remove the cursed item; the hook must restore the full baseline stats immediately.
            player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            menu.broadcastChanges();

            StatCollection restored = menu.getLastStats();
            if (Math.abs(restored.eterna() - baseEterna) > 1e-4f || restored.clues() != baseClues) {
                helper.fail("Removing Dissonance should restore eterna " + baseEterna + " / clues "
                        + baseClues + ", got eterna " + restored.eterna() + " clues " + restored.clues());
                return;
            }
            helper.succeed();
        } finally {
            MockPlayers.retire(player);
        }
    }
}
