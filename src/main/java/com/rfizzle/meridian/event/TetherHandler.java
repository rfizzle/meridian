package com.rfizzle.meridian.event;

import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class TetherHandler {

    private static final Map<UUID, List<ItemStack>> tetheredItems = new HashMap<>();

    private TetherHandler() {}

    public static void register() {
        ServerPlayerEvents.COPY_FROM.register(TetherHandler::onPlayerRespawn);
    }

    public static void saveTetheredItems(UUID playerId, List<ItemStack> items) {
        tetheredItems.put(playerId, items);
    }

    private static void onPlayerRespawn(ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean alive) {
        if (alive) return;

        List<ItemStack> items = tetheredItems.remove(newPlayer.getUUID());
        if (items == null || items.isEmpty()) return;

        for (ItemStack stack : items) {
            if (!newPlayer.getInventory().add(stack)) {
                newPlayer.drop(stack, false);
            }
        }
    }
}
