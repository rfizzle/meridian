package com.rfizzle.meridian.anvil;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Central entry point consulted by {@link com.rfizzle.meridian.mixin.AnvilMenuMixin} at
 * the tail of {@code AnvilMenu#createResult}. The mixin passes the current left/right stacks, the
 * interacting player, and vanilla's just-computed cost; if {@link #handle} returns a result, the
 * mixin overwrites {@code resultSlots}, {@code cost}, and {@code repairItemCountCost}.
 *
 * <p>Handlers register via {@link #register(AnvilHandler)} during mod init and are walked in
 * insertion order. The first handler whose {@link AnvilHandler#handle} returns a non-empty result
 * claims the pairing — later handlers are not consulted, matching the "first non-empty wins"
 * contract from T-4.1.2.
 */
public final class AnvilDispatcher {

    private static final List<AnvilHandler> HANDLERS = new ArrayList<>();

    private AnvilDispatcher() {
    }

    /**
     * Appends {@code handler} to the dispatch chain. Insertion order is preserved on
     * {@link #handle}, so callers should register more-specific handlers before more-permissive
     * ones.
     */
    public static synchronized void register(AnvilHandler handler) {
        HANDLERS.add(handler);
    }

    /**
     * Removes every registered handler. Production code does not need this — it exists so unit
     * tests can isolate dispatch state between cases.
     */
    public static synchronized void clear() {
        HANDLERS.clear();
    }

    /**
     * Snapshot of currently registered handlers, in dispatch order. Returned as an unmodifiable
     * list so tests can inspect ordering without being able to mutate the live chain.
     */
    public static synchronized List<AnvilHandler> handlers() {
        return Collections.unmodifiableList(new ArrayList<>(HANDLERS));
    }

    /**
     * Returns the first claim against the given left/right pairing, or {@link Optional#empty()} if
     * no handler takes responsibility. {@code menu} and {@code currentCost} are accepted to keep
     * the mixin call site stable across future handlers, even though the {@link AnvilHandler}
     * contract in MVP only forwards {@code left}/{@code right}/{@code player}.
     */
    public static Optional<AnvilResult> handle(
            AnvilMenu menu, ItemStack left, ItemStack right, Player player, int currentCost) {
        List<AnvilHandler> snapshot;
        synchronized (AnvilDispatcher.class) {
            snapshot = new ArrayList<>(HANDLERS);
        }
        for (AnvilHandler handler : snapshot) {
            Optional<AnvilResult> result = handler.handle(left, right, player);
            if (result.isPresent()) {
                return result;
            }
        }
        return Optional.empty();
    }
}
