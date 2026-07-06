package com.rfizzle.meridian.event;

import com.rfizzle.meridian.MeridianRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

/**
 * The Ender Dragon's Dormant Core drop (#158) — the interim survival source for the core until
 * Concord-level chest wiring lands. Gating unbreakable gear behind the end-game boss fits the
 * "dormant power" fiction.
 *
 * <p>The drop is triggered from {@link com.rfizzle.meridian.mixin.EnderDragonMixin}, which injects at
 * the terminal frame of {@link net.minecraft.world.entity.boss.enderdragon.EnderDragon#tickDeath()}.
 * A mixin is required rather than a {@code ServerLivingEntityEvents.AFTER_DEATH} handler or an
 * {@code entities/ender_dragon} loot table: the dragon never routes through {@code LivingEntity.die()}
 * (so the death event never fires) and its hardcoded death rolls no loot table (so a static resource
 * would never fire).
 *
 * <p>The spawn itself lives here as a static routine so it can be gametested directly — a live dragon
 * cannot complete its ten second death sequence inside a bounded gametest structure without being
 * culled first.
 */
public final class DragonLootHandler {

    private DragonLootHandler() {
    }

    /** Spawns exactly one Dormant Core as a free item entity at the given position. */
    public static void dropDormantCore(ServerLevel level, double x, double y, double z) {
        ItemEntity drop = new ItemEntity(level, x, y, z, new ItemStack(MeridianRegistry.DORMANT_CORE));
        drop.setDefaultPickUpDelay();
        level.addFreshEntity(drop);
    }
}
