package com.rfizzle.meridian.gametest.enchantments;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.gametest.MockPlayers;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * Ironclasp's drop-key half, driven through {@link ServerPlayer#drop(boolean)} — the server
 * handler for the Q / Ctrl+Q drop key. A connected mock player is required so the
 * client-resync packet the mixin sends has a live connection to absorb it.
 */
public class IronclaspGameTest implements FabricGameTest {

    private Holder<Enchantment> lookup(GameTestHelper helper, String id) {
        Registry<Enchantment> reg = helper.getLevel().registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT);
        return reg.getHolder(Meridian.id(id)).orElse(null);
    }

    @GameTest(template = "meridian:empty_3x3")
    public void ironclaspBlocksDropKey(GameTestHelper helper) {
        Holder<Enchantment> ironclasp = lookup(helper, "ironclasp");
        if (ironclasp == null) { helper.fail("ironclasp not in registry"); return; }

        ServerPlayer player = MockPlayers.serverPlayerInLevel(helper);
        BlockPos abs = helper.absolutePos(new BlockPos(1, 2, 1));
        player.teleportTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5);

        // Ironclasp item in the selected hotbar slot.
        ItemStack clasped = new ItemStack(Items.DIAMOND_SWORD);
        clasped.enchant(ironclasp, 1);
        player.getInventory().selected = 0;
        player.getInventory().setItem(0, clasped);

        boolean claspedDropped = player.drop(false);
        boolean claspedStillHeld = !player.getInventory().getSelected().isEmpty();

        // Control: a plain item in the next slot still drops normally.
        ItemStack plain = new ItemStack(Items.DIAMOND_PICKAXE);
        player.getInventory().selected = 1;
        player.getInventory().setItem(1, plain);

        boolean plainDropped = player.drop(false);
        boolean plainSlotEmpty = player.getInventory().getItem(1).isEmpty();

        player.discard();

        if (claspedDropped) { helper.fail("The drop key must report no drop for an Ironclasp item"); return; }
        if (!claspedStillHeld) { helper.fail("An Ironclasp item must stay in the selected slot after the drop key"); return; }
        if (!plainDropped) { helper.fail("A normal item should still drop with the drop key"); return; }
        if (!plainSlotEmpty) { helper.fail("The normal item's slot should be empty after it drops"); return; }
        helper.succeed();
    }
}
