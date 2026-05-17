package com.rfizzle.meridian.library;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.storage.base.InsertionOnlyStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.world.item.Items;

/**
 * Hopper-facing {@code Storage<ItemVariant>} surface for an {@link EnchantmentLibraryBlockEntity}.
 * Ports Zenith's {@code EnchLibraryTile.storage} contract to 1.21.1 — minus Zenith's
 * {@code SingleStackStorage} fake-slot wrapper — and layers in transactional snapshotting per
 * DESIGN § "Enchantment Library" so an aborted Fabric Transfer transaction rolls back cleanly.
 *
 * <p>Insert policy:
 * <ul>
 *     <li>Insert accepts only {@link Items#ENCHANTED_BOOK} variants; every other variant is
 *         rejected at the variant check and returns zero units accepted.</li>
 *     <li>Each book is dispatched to {@link EnchantmentLibraryBlockEntity#depositBookSilent} one
 *         unit at a time — deposit sums points per-book, so an eight-book pipe push delivers
 *         eight calls (one per book), not one call with {@code count = 8}.</li>
 *     <li>Silent-void overflow: when the pool has already saturated at
 *         {@link EnchantmentLibraryBlockEntity#getMaxPoints()}, further deposits no-op inside
 *         {@code depositBookSilent} but {@code insert} still reports the full {@code maxAmount}
 *         as accepted. From the hopper's perspective the library is never "full", so pipes
 *         cannot jam on a saturated library.</li>
 *     <li>Extraction is permanently disabled — hoppers can fill a library but never pull books
 *         out. {@link InsertionOnlyStorage} pins {@code supportsExtraction=false} and both
 *         {@code extract} and {@code iterator} to no-ops for us.</li>
 * </ul>
 *
 * <p>Transaction policy ({@link SnapshotParticipant} contract):
 * <ul>
 *     <li>{@link #insert} calls {@link #updateSnapshots} before mutating, so the first deposit in
 *         a transaction triggers a defensive copy of {@code points}/{@code maxLevels} into a
 *         {@link LibrarySnapshot}; subsequent deposits at the same nesting depth piggy-back on
 *         the existing snapshot (per the base class' idempotency contract).</li>
 *     <li>{@link #createSnapshot} clones both maps and the adapter's internal {@code dirty} flag.
 *         The clone is required because the BE's maps are mutated in place by
 *         {@code depositBookSilent} — a shared reference would alias the live state and defeat
 *         rollback.</li>
 *     <li>{@link #readSnapshot} restores both maps in place by clearing and re-copying. The BE's
 *         own map references stay valid (menus and the storage adapter both hold onto them), so
 *         we cannot simply swap references.</li>
 *     <li>{@link #onFinalCommit} fires {@code library.setChanged()} exactly once per outer
 *         transaction that mutated state, then resets {@code dirty}. The BE's {@code setChanged}
 *         drives both the persistent-state mark and the chunk-update packet; deferring it here
 *         keeps both side effects out of the transaction's reversible window.</li>
 * </ul>
 */
public class LibraryStorageAdapter extends SnapshotParticipant<LibrarySnapshot>
        implements InsertionOnlyStorage<ItemVariant> {

    private final EnchantmentLibraryBlockEntity library;

    /**
     * Pending-{@code setChanged} flag. Flips to {@code true} when at least one deposit in the
     * current outer transaction actually mutated the pool; consumed (and reset) by
     * {@link #onFinalCommit}. Captured into {@link LibrarySnapshot#dirty()} so abort restores it.
     */
    private boolean dirty = false;

    public LibraryStorageAdapter(EnchantmentLibraryBlockEntity library) {
        this.library = library;
    }

    @Override
    public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction) {
        StoragePreconditions.notBlankNotNegative(resource, maxAmount);
        if (!resource.isOf(Items.ENCHANTED_BOOK)) {
            return 0;
        }
        // Hopper throttle (config.library.ioRateLimitTicks): when the last successful insert
        // landed inside the configured window, drop the whole call. Returning 0 back-pressures
        // the pipe (unlike the void-overflow semantic below) — this is intentional, since the
        // throttle exists to dampen pathological auto-farms, not to silently accept them.
        int rateLimit = library.rateLimitTicks();
        long now = library.currentGameTime();
        if (rateLimit > 0
                && library.lastInsertTick != Long.MIN_VALUE
                && now - library.lastInsertTick < rateLimit) {
            return 0;
        }
        // Snapshot before any mutation so abort can restore the pre-insert pool + throttle stamp.
        updateSnapshots(transaction);
        // Per-unit deposit: depositBookSilent keys off per-book enchantment levels, not stack
        // count, so a multi-book pipe push must loop. The stack built from the variant carries
        // the shared STORED_ENCHANTMENTS component for every unit.
        for (long i = 0; i < maxAmount; i++) {
            if (library.depositBookSilent(resource.toStack(1))) {
                this.dirty = true;
            }
        }
        // Stamp the throttle before returning — saturated or not, the pipe successfully delivered
        // to this library on this tick, so the cooldown starts now. Snapshotted above, so an
        // aborted transaction rolls this back too (no phantom cooldown on rollback).
        library.lastInsertTick = now;
        // Void-overflow contract: saturated pools drop further points inside depositBookSilent,
        // but from the pipe's perspective every unit was "accepted".
        return maxAmount;
    }

    @Override
    protected LibrarySnapshot createSnapshot() {
        return new LibrarySnapshot(
                new Object2IntOpenHashMap<>(library.getPoints()),
                new Object2IntOpenHashMap<>(library.getMaxLevels()),
                this.dirty,
                library.lastInsertTick);
    }

    @Override
    protected void readSnapshot(LibrarySnapshot snapshot) {
        library.getPoints().clear();
        library.getPoints().putAll(snapshot.points());
        library.getMaxLevels().clear();
        library.getMaxLevels().putAll(snapshot.maxLevels());
        this.dirty = snapshot.dirty();
        library.lastInsertTick = snapshot.lastInsertTick();
    }

    @Override
    protected void onFinalCommit() {
        if (this.dirty) {
            this.dirty = false;
            library.setChanged();
        }
    }
}
