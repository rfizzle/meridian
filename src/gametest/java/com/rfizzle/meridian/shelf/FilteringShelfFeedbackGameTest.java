// Tier: 3 (Fabric Gametest)
package com.rfizzle.meridian.shelf;

import com.rfizzle.meridian.Meridian;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Tier-3 coverage for the filtering-shelf rejection feedback (#156): right-clicking the shelf with
 * a multi-enchant book must explain the refusal on the actionbar while leaving the book in hand.
 * The book-not-consumed half is asserted directly; the message half is exercised through a real
 * {@code ServerPlayer} connection (its content is verified by the manual/lang keys, not here).
 */
public class FilteringShelfFeedbackGameTest implements FabricGameTest {

    private static final BlockPos SHELF_POS = new BlockPos(2, 1, 2);

    @GameTest(template = "meridian:empty_5x5x5", timeoutTicks = 100, batch = "filteringShelfFeedback")
    public void multiEnchantBookRejectedButKept(GameTestHelper helper) {
        Block shelfBlock = BuiltInRegistries.BLOCK.get(Meridian.id("filtering_shelf"));
        if (shelfBlock == Blocks.AIR) {
            helper.fail("filtering_shelf block not found in registry");
            return;
        }
        helper.setBlock(SHELF_POS, shelfBlock.defaultBlockState());

        Registry<Enchantment> enchReg = helper.getLevel().registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> sharpness = enchReg.getHolderOrThrow(Enchantments.SHARPNESS);
        Holder<Enchantment> unbreaking = enchReg.getHolderOrThrow(Enchantments.UNBREAKING);

        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        mutable.set(sharpness, 1);
        mutable.set(unbreaking, 1);
        book.set(DataComponents.STORED_ENCHANTMENTS, mutable.toImmutable());

        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, book);

        // Default-facing (NORTH) shelf: horizontal = 1 - dx = 0.5 → column 1, dy 0.7 → top row → an
        // empty slot, so the interaction reaches the canInsert rejection branch.
        BlockPos abs = helper.absolutePos(SHELF_POS);
        Vec3 hitVec = new Vec3(abs.getX() + 0.5D, abs.getY() + 0.7D, abs.getZ());
        BlockHitResult hit = new BlockHitResult(hitVec, net.minecraft.core.Direction.NORTH, abs, false);

        player.gameMode.useItemOn(player, helper.getLevel(), book, InteractionHand.MAIN_HAND, hit);

        ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (held.isEmpty() || !held.is(Items.ENCHANTED_BOOK) || held.getCount() != 1) {
            helper.fail("Rejected book must stay in hand unchanged, held=" + held);
            player.discard();
            return;
        }
        if (helper.getLevel().getBlockEntity(abs) instanceof FilteringShelfBlockEntity be
                && !be.isEmpty()) {
            helper.fail("Multi-enchant book must not be inserted into the shelf");
            player.discard();
            return;
        }
        player.discard();
        helper.succeed();
    }
}
