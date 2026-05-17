package com.rfizzle.meridian.anvil;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * Single anvil interaction recognizer. {@link AnvilDispatcher} walks its registered handlers in
 * insertion order; the first handler to return a non-empty {@link AnvilResult} claims the
 * left/right pairing and short-circuits the rest of the chain. A handler that does not recognize
 * the pairing returns {@link Optional#empty()} so the next handler — or vanilla, when none claim —
 * gets a turn.
 */
@FunctionalInterface
public interface AnvilHandler {

    Optional<AnvilResult> handle(ItemStack left, ItemStack right, Player player);
}
