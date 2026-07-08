// Tier: 3 (Fabric Gametest)
package com.rfizzle.meridian.library;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.gametest.MockPlayers;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Tier-3 coverage for the library feedback added in the #156 pass. Exercises the two paths that
 * need a live {@code ServerLevel} and so cannot be reached from Tier-1: the deposit path, whose
 * positional sound + {@code sendParticles} confirmation runs against the real level (a bad cast or
 * particle call would throw here, not in a pure test), and the server-side rejection path, whose
 * actionbar message is dispatched through a real {@code ServerPlayer} connection.
 */
public class EnchantmentLibraryFeedbackGameTest implements FabricGameTest {

    private static final BlockPos LIB_POS = new BlockPos(2, 1, 2);

    /**
     * Depositing an enchanted book into slot 0 absorbs its enchantment into the pool and fires the
     * block-local confirmation without a player present. Asserts the pool updated (proving the
     * deposit ran) and that {@code playDepositFeedback} completed against the live server level.
     */
    @GameTest(template = "meridian:empty_5x5x5", timeoutTicks = 100, batch = "libraryFeedbackDeposit")
    public void depositAbsorbsBookAndFiresFeedback(GameTestHelper helper) {
        EnchantmentLibraryBlockEntity tile = placeLibrary(helper);
        if (tile == null) return;

        Registry<Enchantment> enchReg = helper.getLevel().registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> sharpness = enchReg.getHolderOrThrow(Enchantments.SHARPNESS);
        ResourceKey<Enchantment> sharpKey = Enchantments.SHARPNESS;

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        EnchantmentLibraryMenu menu = new EnchantmentLibraryMenu(1, player.getInventory(), tile);

        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        mutable.set(sharpness, 1);
        book.set(DataComponents.STORED_ENCHANTMENTS, mutable.toImmutable());

        // Slot.set fires setChanged → absorbDepositSlot → depositBookSilent + playDepositFeedback.
        menu.getSlot(EnchantmentLibraryMenu.DEPOSIT_SLOT).set(book);

        if (tile.getPoints().getInt(sharpKey) <= 0) {
            helper.fail("Deposit did not absorb the Sharpness book into the pool");
            return;
        }
        if (!menu.ioInv.getItem(EnchantmentLibraryMenu.DEPOSIT_SLOT).isEmpty()) {
            helper.fail("Deposit slot should be cleared after absorption");
            return;
        }
        helper.succeed();
    }

    /**
     * Clicking a row the pool cannot satisfy returns false, delivers no book to the extract slot,
     * and drives the actionbar rejection message through a real connection without throwing.
     */
    @GameTest(template = "meridian:empty_5x5x5", timeoutTicks = 100, batch = "libraryFeedbackReject")
    public void unaffordableClickIsRejectedWithoutDelivery(GameTestHelper helper) {
        EnchantmentLibraryBlockEntity tile = placeLibrary(helper);
        if (tile == null) return;

        Registry<Enchantment> enchReg = helper.getLevel().registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT);
        Enchantment sharpness = enchReg.getOrThrow(Enchantments.SHARPNESS);
        int index = enchReg.getId(sharpness);

        ServerPlayer player = MockPlayers.serverPlayerInLevel(helper);
        EnchantmentLibraryMenu menu = new EnchantmentLibraryMenu(1, player.getInventory(), tile);

        // Empty pool → canExtract is false → clickMenuButton must reject and message the player.
        boolean clicked = menu.clickMenuButton(player, index);
        if (clicked) {
            helper.fail("Click on an empty pool should be rejected (returned true)");
            player.discard();
            return;
        }
        if (!menu.ioInv.getItem(EnchantmentLibraryMenu.EXTRACT_SLOT).isEmpty()) {
            helper.fail("Rejected extraction must not place a book in the extract slot");
            player.discard();
            return;
        }
        player.discard();
        helper.succeed();
    }

    private EnchantmentLibraryBlockEntity placeLibrary(GameTestHelper helper) {
        Block libraryBlock = BuiltInRegistries.BLOCK.get(Meridian.id("library"));
        if (libraryBlock == Blocks.AIR) {
            helper.fail("library block not found in registry");
            return null;
        }
        helper.setBlock(LIB_POS, libraryBlock.defaultBlockState());
        BlockEntity be = helper.getLevel().getBlockEntity(helper.absolutePos(LIB_POS));
        if (!(be instanceof EnchantmentLibraryBlockEntity tile)) {
            helper.fail("library block entity missing after placement");
            return null;
        }
        return tile;
    }
}
