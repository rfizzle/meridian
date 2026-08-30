package com.rfizzle.meridian.anvil;

import java.util.concurrent.atomic.AtomicBoolean;
import com.rfizzle.meridian.Meridian;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Central entry point consulted by {@link com.rfizzle.meridian.mixin.AnvilMenuMixin} at
 * the tail of {@code AnvilMenu#createResult}. The mixin passes the current left/right stacks, the
 * interacting player, and vanilla's just-computed cost; if {@link #handle} returns a result, the
 * mixin overwrites {@code resultSlots}, {@code cost}, and {@code repairItemCountCost}.
 *
 * <p>Handlers register via {@link #register(AnvilHandler)} during mod init and are walked in
 * insertion order. The first handler whose {@link AnvilHandler#handle} returns a non-empty result
 * claims the pairing — later handlers are not consulted, matching the "first non-empty wins"
 * contract.
 */
public final class AnvilDispatcher {

    private static final List<AnvilHandler> HANDLERS = new CopyOnWriteArrayList<>();

    private AnvilDispatcher() {
    }

    /**
     * Appends {@code handler} to the dispatch chain. Insertion order is preserved on
     * {@link #handle}, so callers should register more-specific handlers before more-permissive
     * ones.
     */
    public static void register(AnvilHandler handler) {
        HANDLERS.add(handler);
    }

    /**
     * Removes every registered handler. Production code does not need this — it exists so unit
     * tests can isolate dispatch state between cases.
     */
    public static void clear() {
        HANDLERS.clear();
    }

    /**
     * Snapshot of currently registered handlers, in dispatch order. Returned as an unmodifiable
     * list so tests can inspect ordering without being able to mutate the live chain.
     */
    public static List<AnvilHandler> handlers() {
        return Collections.unmodifiableList(new ArrayList<>(HANDLERS));
    }

    /**
     * Returns the first claim against the given left/right pairing, or {@link Optional#empty()} if
     * no handler takes responsibility. {@code menu} and {@code currentCost} are accepted to keep
     * the mixin call site stable across future handlers, even though the {@link AnvilHandler}
     * contract only forwards {@code left}/{@code right}/{@code player}.
     */
    /** One-shot gate: the anvil menu re-dispatches on every slot change. */
    private static final AtomicBoolean HANDLER_FAILURE_LOGGED = new AtomicBoolean(false);

    public static Optional<AnvilResult> handle(
            AnvilMenu menu, ItemStack left, ItemStack right, Player player, int currentCost) {
        for (AnvilHandler handler : HANDLERS) {
            Optional<AnvilResult> result;
            try {
                result = handler.handle(left, right, player);
            } catch (VirtualMachineError e) {
                throw e;
            } catch (Throwable t) {
                // Per-handler isolation (API Standard §3.1): one bad handler yields no claim and
                // the rest of the chain still gets its turn.
                if (HANDLER_FAILURE_LOGGED.compareAndSet(false, true)) {
                    Meridian.LOGGER.warn("AnvilHandler {} threw; skipping it for this pairing",
                            handler.getClass().getName(), t);
                }
                continue;
            }
            if (result != null && result.isPresent()) {
                return result;
            }
        }
        return Optional.empty();
    }
}
