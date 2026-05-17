package com.rfizzle.meridian.library;

import it.unimi.dsi.fastutil.objects.Object2IntMap;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * Transactional snapshot for {@link LibraryStorageAdapter}'s {@link
 * net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant} contract. Captures
 * defensive copies of the BE's {@code points} and {@code maxLevels} maps, the BE's
 * {@code lastInsertTick} throttle stamp, and the adapter's own pending-{@code setChanged} flag,
 * so a Fabric Transfer API abort can roll all four back without touching the live BE state
 * mid-transaction.
 *
 * <p>The maps are copied at snapshot time (not aliased) — the adapter mutates the BE's live maps
 * in place via {@code depositBookSilent}, so a shared reference would be useless for revert.
 * The {@code dirty} flag tracks whether the adapter has accumulated mutations awaiting an
 * {@code onFinalCommit} {@code setChanged()} fire; saving it in the snapshot means an abort
 * restores the adapter to its pre-transaction "no pending fire" state. The
 * {@code lastInsertTick} stamp is restored alongside the maps so a rolled-back insert does not
 * "burn" a throttle slot against the rate-limit window in
 * {@code config.library.ioRateLimitTicks}.
 */
public record LibrarySnapshot(
        Object2IntMap<ResourceKey<Enchantment>> points,
        Object2IntMap<ResourceKey<Enchantment>> maxLevels,
        boolean dirty,
        long lastInsertTick) {
}
