package com.rfizzle.meridian.shelf;

import com.rfizzle.meridian.MeridianRegistry;
import com.rfizzle.meridian.enchanting.BlacklistSource;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChiseledBookShelfBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Block entity for {@link FilteringShelfBlock}. Holds up to 6 enchanted books and exposes their
 * union of enchantments through {@link BlacklistSource} so the in-range enchanting table's stat
 * scan can suppress those enchantments from rolls.
 *
 * <p>Implements {@link Container} directly rather than extending vanilla
 * {@code ChiseledBookShelfBlockEntity}: vanilla's {@code BlockEntity#validateBlockState} reads
 * the {@code type} field (always {@code BlockEntityType.CHISELED_BOOKSHELF} for any subclass)
 * and rejects placement on a non-vanilla block. Mirroring vanilla's small storage surface here
 * is cheaper than fighting that validator with a startup-time mutation that doesn't apply in
 * unit tests.
 *
 * <p>Slot occupancy is reflected through the {@link ChiseledBookShelfBlock#SLOT_OCCUPIED_PROPERTIES}
 * blockstate properties inherited from the parent block — {@link #setItem} flips the matching
 * property so the chiseled-bookshelf model JSON renders the right books without any custom
 * client code.
 *
 * <p>Inserts are restricted to enchanted books carrying exactly one enchantment; multi-enchant
 * books are rejected at {@link #canPlaceItem} time so the blacklist semantics stay
 * one-book-one-enchant.
 */
public class FilteringShelfBlockEntity extends BlockEntity implements Container, BlacklistSource {

    public static final int CONTAINER_SIZE = 6;

    private final NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);

    public FilteringShelfBlockEntity(BlockPos pos, BlockState state) {
        super(MeridianRegistry.FILTERING_SHELF_BE, pos, state);
    }

    // --- Container ---

    @Override
    public int getContainerSize() {
        return CONTAINER_SIZE;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot >= 0 && slot < items.size() ? items.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        ItemStack removed = ContainerHelper.removeItem(items, slot, count);
        if (!removed.isEmpty()) {
            propagateChange(slot);
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack removed = ContainerHelper.takeItem(items, slot);
        if (!removed.isEmpty()) {
            propagateChange(slot);
        }
        return removed;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= items.size()) return;
        ItemStack capped = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        items.set(slot, capped);
        propagateChange(slot);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot >= 0 && slot < items.size()
                && items.get(slot).isEmpty()
                && canInsert(stack);
    }

    @Override
    public void clearContent() {
        items.clear();
        if (this.level != null) {
            BlockState state = this.getBlockState();
            for (int i = 0; i < CONTAINER_SIZE; i++) {
                state = state.setValue(ChiseledBookShelfBlock.SLOT_OCCUPIED_PROPERTIES.get(i), false);
            }
            this.level.setBlock(this.getBlockPos(), state, Block.UPDATE_ALL);
        }
        setChanged();
    }

    // --- NBT ---

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.clear();
        ContainerHelper.loadAllItems(tag, items, registries);
    }

    // --- Client sync ---

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveCustomOnly(registries);
    }

    // --- Filter / blacklist ---

    /**
     * Single-enchant filter shared by {@link FilteringShelfBlock}'s {@code useItemOn} and our
     * {@link #canPlaceItem}. A book with zero or multiple enchantments is rejected so the
     * blacklist semantics stay one-book-one-enchant.
     */
    public static boolean canInsert(ItemStack stack) {
        if (stack.isEmpty() || !stack.is(Items.ENCHANTED_BOOK)) {
            return false;
        }
        ItemEnchantments enchantments = stack.getOrDefault(
                DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
        return enchantments.size() == 1;
    }

    @Override
    public Set<ResourceKey<Enchantment>> getEnchantmentBlacklist() {
        if (isEmpty()) {
            return Collections.emptySet();
        }
        Set<ResourceKey<Enchantment>> blacklist = null;
        for (ItemStack stack : items) {
            if (stack.isEmpty()) continue;
            ItemEnchantments enchantments = stack.getOrDefault(
                    DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
            // Defensive: canPlaceItem rejects multi-enchant books at insert, but the scan
            // re-checks so a hand-edited NBT save can't smuggle multi-enchant books past the
            // blacklist contract.
            if (enchantments.size() != 1) continue;
            if (blacklist == null) {
                blacklist = new HashSet<>();
            }
            Set<ResourceKey<Enchantment>> sink = blacklist;
            enchantments.keySet().forEach(holder ->
                    holder.unwrapKey().ifPresent(sink::add));
        }
        return blacklist == null ? Collections.emptySet() : Set.copyOf(blacklist);
    }

    /** Number of non-empty slots — useful for tests + the analog-signal hook later. */
    public int count() {
        int n = 0;
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) n++;
        }
        return n;
    }

    private void propagateChange(int slot) {
        if (this.level != null) {
            BlockState state = this.getBlockState()
                    .setValue(ChiseledBookShelfBlock.SLOT_OCCUPIED_PROPERTIES.get(slot),
                            !items.get(slot).isEmpty());
            this.level.setBlock(this.getBlockPos(), state, Block.UPDATE_ALL);
        }
        setChanged();
    }
}
