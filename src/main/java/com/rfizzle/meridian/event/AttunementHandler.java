package com.rfizzle.meridian.event;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.config.MeridianConfig;
import com.rfizzle.meridian.enchanting.EnchantingStatRegistry;
import com.rfizzle.meridian.enchanting.EnchantmentEffects;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Attunement: gear carrying the enchantment slowly repairs while its holder stands near an
 * enchanting table whose shelf scan reaches {@code attunement.minEterna} Eterna — the home-base
 * counterpart to Mending (XP) and Vital Mend (healing) in the mending exclusive set.
 *
 * <p>Every {@code attunement.intervalTicks} server ticks, each online player's inventory is checked
 * for damaged Attunement items; only when at least one exists does the handler look for a
 * qualifying table within {@code attunement.radius} blocks. Candidate tables come from the
 * block-entity maps of the few loaded chunks overlapping that radius (an enchanting table always
 * has a block entity), so the search never walks the block grid; each candidate is then scored
 * with the same bounded 15-offset shelf scan the table menu uses. Each pulse repairs
 * {@code level} durability per item (I&nbsp;=&nbsp;1, II&nbsp;=&nbsp;2), flat rate.
 */
public final class AttunementHandler {

    private static long tickCounter = 0;

    private AttunementHandler() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(AttunementHandler::onServerTick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> tickCounter = 0);
    }

    private static void onServerTick(MinecraftServer server) {
        MeridianConfig.Attunement config = Meridian.getConfig().attunement;
        if (++tickCounter % config.intervalTicks != 0) return;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            EffectGuard.run("attunement", player, () -> repairPulse(player, config));
        }
    }

    /**
     * One repair pulse for one player: repairs every damaged Attunement item in the player's
     * inventory by its enchantment level, provided a qualifying table is in range. The inventory
     * check runs first so players carrying no attuned gear — the common case — never pay for the
     * table search. Package-private so {@code AttunementHandlerGameTest} drives it directly with
     * an explicit config instead of waiting out real tick intervals.
     */
    public static void repairPulse(ServerPlayer player, MeridianConfig.Attunement config) {
        Inventory inventory = player.getInventory();
        boolean anyDamagedAttuned = false;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isDamaged() && EnchantmentEffects.getEnchantmentLevel(stack, EnchantmentEffects.ATTUNEMENT) > 0) {
                anyDamagedAttuned = true;
                break;
            }
        }
        if (!anyDamagedAttuned) return;

        if (!nearQualifyingTable(player, config.radius, config.minEterna)) return;

        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isDamaged()) continue;
            int level = EnchantmentEffects.getEnchantmentLevel(stack, EnchantmentEffects.ATTUNEMENT);
            if (level <= 0) continue;
            stack.setDamageValue(Math.max(0, stack.getDamageValue() - level));
        }
    }

    /**
     * Whether an enchanting table within {@code radius} blocks of the player has a shelf scan of
     * at least {@code minEterna} Eterna. Candidates are found through the block-entity maps of the
     * loaded chunks overlapping the radius cube — never by iterating block positions — and
     * unloaded chunks are skipped, never loaded.
     */
    private static boolean nearQualifyingTable(ServerPlayer player, int radius, int minEterna) {
        ServerLevel level = player.serverLevel();
        BlockPos center = player.blockPosition();
        int minChunkX = (center.getX() - radius) >> 4;
        int maxChunkX = (center.getX() + radius) >> 4;
        int minChunkZ = (center.getZ() - radius) >> 4;
        int maxChunkZ = (center.getZ() + radius) >> 4;

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) continue;
                for (BlockPos pos : chunk.getBlockEntities().keySet()) {
                    if (!chunk.getBlockState(pos).is(Blocks.ENCHANTING_TABLE)) continue;
                    if (!pos.closerToCenterThan(player.position(), radius)) continue;
                    if (EnchantingStatRegistry.gatherStats(level, pos).eterna() >= minEterna) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
