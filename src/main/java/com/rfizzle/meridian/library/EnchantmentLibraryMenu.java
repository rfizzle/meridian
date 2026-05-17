package com.rfizzle.meridian.library;

import com.rfizzle.meridian.MeridianRegistry;
import com.rfizzle.meridian.enchanting.EnchantmentInfoRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Server-and-client menu for the Enchantment Library blocks. Ports Zenith's
 * {@code EnchLibraryContainer} layout onto vanilla 1.21.1 (no Placebo): three IO slots backed by a
 * {@link SimpleContainer} plus the player inventory. Slot 0 auto-absorbs deposited books into the
 * BE's point pool the moment the slot's {@link Slot#setChanged} fires; slot 1 is the extract
 * target the click handler upgrades; slot 2 is a scratch buffer for shift-click overflow and
 * carrying the finished book out (per DESIGN.md § "GUI — three slots").
 *
 * <p>Click encoding follows DESIGN: {@code id = (shift << 31) | enchantIndex}. The shift flag
 * picks shift-click extraction (max-affordable level from the pool); a plain click requests
 * {@code curLvl + 1}. The enchant index is resolved against the player's dynamic enchantment
 * registry — 1.21.1 enchantments are not in {@link net.minecraft.core.registries.BuiltInRegistries}
 * so the lookup must go through {@code level().registryAccess()} per request, not against a
 * cached static.
 *
 * <p>Listener registration: the constructors call {@link EnchantmentLibraryBlockEntity#addListener
 * tile.addListener(this)} and {@link #removed(Player)} pairs with
 * {@link EnchantmentLibraryBlockEntity#removeListener tile.removeListener(this)}. Pool mutations
 * call {@link #onChanged()}, which runs the screen-supplied {@link #setNotifier notifier}.
 */
public class EnchantmentLibraryMenu extends AbstractContainerMenu {

    /** Slot 0 — deposit. Auto-absorbed on {@link Slot#setChanged}. */
    public static final int DEPOSIT_SLOT = 0;
    /** Slot 1 — extract target. Click handler upgrades whatever book sits here. */
    public static final int EXTRACT_SLOT = 1;
    /** Slot 2 — scratch buffer. Player-managed; container does not touch it. */
    public static final int SCRATCH_SLOT = 2;
    /** Three IO slots backing the {@link SimpleContainer}. */
    public static final int IO_SLOT_COUNT = 3;

    /** High-bit flag on the click ID — set means shift-click. Mirrors Zenith's encoding. */
    public static final int SHIFT_BIT = 0x80000000;
    /** Mask that strips the shift bit, leaving the enchant registry index. */
    public static final int INDEX_MASK = 0x7FFFFFFF;

    /**
     * BE this menu reads/writes against. {@code null} only on a client menu opened against an
     * unloaded chunk — every server-side path requires a non-null tile, so all mutating methods
     * short-circuit when {@code tile == null}.
     */
    final EnchantmentLibraryBlockEntity tile;

    /** Three-slot IO container shared by deposit, extract, and scratch slots. */
    public final SimpleContainer ioInv;

    private final ContainerLevelAccess access;
    private final boolean clientSide;

    /**
     * Optional callback fired by {@link #onChanged()} after the BE notifies of a pool mutation.
     * The screen sets this so it can repaint its row list without polling. {@code null} on tests
     * and on plain server-side menus.
     */
    private Runnable notifier;

    /**
     * Production server-side constructor — invoked from the screen-open path with the BE the
     * player right-clicked. Derives a {@link ContainerLevelAccess} from the BE's level/pos so
     * {@link #stillValid} can fall back to vanilla's distance + block-identity check.
     */
    public EnchantmentLibraryMenu(int containerId, Inventory playerInv, EnchantmentLibraryBlockEntity tile) {
        super(MeridianRegistry.LIBRARY_MENU, containerId);
        this.tile = tile;
        Level level = (tile != null) ? tile.getLevel() : null;
        this.access = (level != null)
                ? ContainerLevelAccess.create(level, tile.getBlockPos())
                : ContainerLevelAccess.NULL;
        this.clientSide = playerInv.player.level().isClientSide();
        this.ioInv = new SimpleContainer(IO_SLOT_COUNT);
        addIoSlots();
        addPlayerSlots(playerInv);
        if (this.tile != null) {
            this.tile.addListener(this);
        }
    }

    /**
     * Client-side constructor — Fabric's {@code ExtendedScreenHandlerType} hands us the
     * {@link BlockPos} the player opened the menu on; we resolve the BE through
     * {@link Level#getBlockEntity(BlockPos)}. Resolution may fail on a client whose chunk hasn't
     * finished loading; in that degenerate case the menu opens as a no-op until the chunk catches
     * up and the next sync packet swaps the BE in.
     */
    public EnchantmentLibraryMenu(int containerId, Inventory playerInv, BlockPos pos) {
        this(containerId, playerInv, resolveTile(playerInv, pos));
    }

    /**
     * Test-only constructor — skips the player-inventory slots so unit tests can build the menu
     * without minting a {@code Player}. The IO slots and click logic are exactly the same as the
     * production path; only the inventory grid is missing.
     */
    EnchantmentLibraryMenu(int containerId, EnchantmentLibraryBlockEntity tile) {
        super(MeridianRegistry.LIBRARY_MENU, containerId);
        this.tile = tile;
        this.access = ContainerLevelAccess.NULL;
        this.clientSide = false;
        this.ioInv = new SimpleContainer(IO_SLOT_COUNT);
        addIoSlots();
        if (this.tile != null) {
            this.tile.addListener(this);
        }
    }

    private static EnchantmentLibraryBlockEntity resolveTile(Inventory inv, BlockPos pos) {
        BlockEntity be = inv.player.level().getBlockEntity(pos);
        return (be instanceof EnchantmentLibraryBlockEntity ele) ? ele : null;
    }

    private void addIoSlots() {
        addSlot(new Slot(this.ioInv, DEPOSIT_SLOT, 142, 77) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.ENCHANTED_BOOK);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }

            /**
             * Auto-absorb on every change: vanilla calls {@link Slot#setChanged} the moment a
             * player drops a book into the slot via {@code Slot#set} or shift-click, so this
             * is the natural hook for "drop book → pool absorbs → slot clears" without polling.
             * Slot.setChanged() is also re-invoked when we clear the slot to empty, but the
             * absorb path no-ops on empty stacks so the second pass is a cheap fall-through.
             */
            @Override
            public void setChanged() {
                super.setChanged();
                EnchantmentLibraryMenu.this.absorbDepositSlot();
            }
        });
        addSlot(new Slot(this.ioInv, EXTRACT_SLOT, 142, 106) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.ENCHANTED_BOOK);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        addSlot(new Slot(this.ioInv, SCRATCH_SLOT, 142, 18) {
            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public void setChanged() {
                super.setChanged();
                EnchantmentLibraryMenu.this.onChanged();
            }
        });
    }

    private void addPlayerSlots(Inventory inv) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 148 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, 8 + col * 18, 206));
        }
    }

    /**
     * Server-side absorb step. Idempotent on empty slots so we can fire it from the slot's
     * {@code setChanged} hook even on the secondary "clear to empty" pass without a guard there.
     */
    void absorbDepositSlot() {
        if (clientSide || tile == null) return;
        ItemStack stack = ioInv.getItem(DEPOSIT_SLOT);
        if (stack.isEmpty()) return;
        tile.depositBook(stack);
        ioInv.setItem(DEPOSIT_SLOT, ItemStack.EMPTY);
    }

    /**
     * Decode the bit-packed click ID per DESIGN, resolve the enchant against the player's dynamic
     * registry, and hand off to {@link #attemptExtract}. Returns {@code false} on the client side
     * (the server is authoritative for extraction) and on every resolution miss — vanilla
     * interprets {@code false} as "no-op", which is the correct degenerate behavior.
     */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (clientSide || tile == null) return false;
        boolean shift = (id & SHIFT_BIT) != 0;
        int index = id & INDEX_MASK;
        Registry<Enchantment> registry = player.level().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        Enchantment ench = registry.byId(index);
        if (ench == null) return false;
        Holder<Enchantment> holder = registry.wrapAsHolder(ench);
        return attemptExtract(holder, shift);
    }

    /**
     * Pure-logic extraction path used by both {@link #clickMenuButton} and unit tests. Computes
     * {@code target} from the shift flag (shift = {@link EnchantmentLibraryBlockEntity#maxLevelAffordable}
     * clamped to the per-enchant {@code maxLevels} cap; non-shift = {@code curLvl + 1}), gates
     * through {@link EnchantmentLibraryBlockEntity#canExtract}, and on success mutates the extract
     * slot's stack and debits the BE's point pool through
     * {@link EnchantmentLibraryBlockEntity#extract}.
     *
     * <p>Stack mutation upgrades an existing book in place when one is present; an empty extract
     * slot is filled with a fresh {@link Items#ENCHANTED_BOOK}. Either way the resulting stack
     * carries the union of any prior {@code STORED_ENCHANTMENTS} with the newly extracted level
     * — matches Zenith's "upgrade in place" UX.
     */
    boolean attemptExtract(Holder<Enchantment> holder, boolean shift) {
        if (tile == null) return false;
        if (!EnchantmentInfoRegistry.getInfo(holder).enabled()) return false;
        ResourceKey<Enchantment> key = holder.unwrapKey().orElse(null);
        if (key == null) return false;
        ItemStack outSlot = ioInv.getItem(EXTRACT_SLOT);
        ItemEnchantments stored = outSlot.isEmpty()
                ? ItemEnchantments.EMPTY
                : outSlot.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
        int curLvl = stored.getLevel(holder);

        int target;
        if (shift) {
            int pool = tile.getPoints().getInt(key);
            int cap = tile.getMaxLevels().getInt(key);
            int affordable = EnchantmentLibraryBlockEntity.maxLevelAffordable(pool, curLvl);
            target = Math.min(cap, affordable);
        } else {
            target = curLvl + 1;
        }

        if (!tile.canExtract(key, target, curLvl)) return false;

        ItemStack mutated = outSlot.isEmpty() ? new ItemStack(Items.ENCHANTED_BOOK) : outSlot;
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(
                mutated.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY));
        mutable.set(holder, target);
        mutated.set(DataComponents.STORED_ENCHANTMENTS, mutable.toImmutable());
        ioInv.setItem(EXTRACT_SLOT, mutated);

        tile.extract(key, target, curLvl);
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = this.slots.get(slotIndex);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;

        ItemStack current = slot.getItem();
        ItemStack result = current.copy();

        if (slotIndex < IO_SLOT_COUNT) {
            if (!this.moveItemStackTo(current, IO_SLOT_COUNT, IO_SLOT_COUNT + 36, true)) {
                return ItemStack.EMPTY;
            }
        } else if (current.is(Items.ENCHANTED_BOOK)) {
            if (!this.moveItemStackTo(current, DEPOSIT_SLOT, DEPOSIT_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (!this.moveItemStackTo(current, SCRATCH_SLOT, SCRATCH_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (current.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return result;
    }

    /**
     * Distance + block-identity check. Falls back to {@code false} on a missing tile so a client
     * menu opened against an unloaded chunk closes itself rather than dangling.
     */
    @Override
    public boolean stillValid(Player player) {
        if (tile == null) return false;
        return stillValid(access, player, tile.getBlockState().getBlock());
    }

    /**
     * Read-only handle to the bound BE so client-side code (the screen) can paint per-enchant rows
     * off the live point + max-level maps. May be {@code null} on a client menu opened against an
     * unloaded chunk; callers must treat that as the "no rows" degenerate case.
     */
    public EnchantmentLibraryBlockEntity getTile() {
        return this.tile;
    }

    /**
     * Wire a screen-side callback fired by {@link #onChanged()}. Mirrors Zenith's
     * {@code setNotifier} hook — the screen registers a thunk that rebuilds its row list off the
     * BE's live maps so external mutations (other menus, hopper inserts, datapack reload) repaint
     * without polling. Pass {@code null} to clear.
     */
    public void setNotifier(Runnable notifier) {
        this.notifier = notifier;
    }

    /**
     * Invoked by {@link EnchantmentLibraryBlockEntity#notifyListeners()} after every pool
     * mutation. Runs the screen's notifier if one is registered; otherwise a cheap no-op so
     * server-side menus and tests pay nothing.
     */
    public void onChanged() {
        if (this.notifier != null) {
            this.notifier.run();
        }
    }

    /**
     * Vanilla close hook. Deregister from the BE's listener set so the closed menu cannot leak
     * callbacks into a screen that no longer exists. {@code super.removed} handles the carried-
     * stack drop on the player; we add the listener cleanup on top.
     */
    @Override
    public void removed(Player player) {
        super.removed(player);
        if (this.tile != null) {
            this.tile.removeListener(this);
        }
    }
}
