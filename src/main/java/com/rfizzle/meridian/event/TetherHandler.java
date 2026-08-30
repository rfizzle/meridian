package com.rfizzle.meridian.event;

import com.rfizzle.meridian.attachment.MeridianAttachments;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class TetherHandler {

    private TetherHandler() {}

    public static void register() {
        ServerPlayerEvents.COPY_FROM.register(TetherHandler::onPlayerRespawn);
    }

    /**
     * Stashes tether-enchanted items on the player so they survive death — and, because the
     * attachment persists with the player entity, a disconnect on the death screen or a mid-death
     * server stop. The items are returned on the next respawn.
     */
    public static void saveTetheredItems(Player player, List<ItemStack> items) {
        if (items.isEmpty()) return;
        player.setAttached(MeridianAttachments.TETHERED_ITEMS, List.copyOf(items));
    }

    private static void onPlayerRespawn(ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean alive) {
        if (alive) return;
        EffectGuard.run("tether", newPlayer, () -> restoreTetheredItems(oldPlayer, newPlayer));
    }

    // Package-private: moves any items stashed on `from` into `to`'s inventory (dropping the
    // overflow), consuming the stash so a second restore is a no-op. Split from the respawn hook so
    // it can be driven directly in a gametest.
    public static void restoreTetheredItems(Player from, Player to) {
        List<ItemStack> items = from.getAttachedOrElse(MeridianAttachments.TETHERED_ITEMS, List.of());
        if (items.isEmpty()) return;
        from.removeAttached(MeridianAttachments.TETHERED_ITEMS);

        for (ItemStack stack : items) {
            ItemStack copy = stack.copy();
            if (!to.getInventory().add(copy)) {
                to.drop(copy, false);
            }
        }
    }
}
