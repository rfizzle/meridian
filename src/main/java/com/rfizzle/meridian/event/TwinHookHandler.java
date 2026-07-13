package com.rfizzle.meridian.event;

import com.rfizzle.meridian.enchanting.EnchantmentEffects;
import com.rfizzle.meridian.enchanting.FishingEnchantMath;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Twin Hook (fishing rod): a catch has a per-level chance to reel in a second copy. Driven from
 * {@link com.rfizzle.meridian.mixin.FishingHookLootMixin}, which wraps the catch's item-entity
 * spawn inside {@code FishingHook#retrieve} and, on a successful roll, spawns one extra copy of
 * the same drop — the item only. Vanilla's separate experience-orb and {@code FISH_CAUGHT}
 * statements sit outside the wrapped call, so the duplicate grants neither: Twin Hook is a pure
 * yield bonus, not a stealth doubling of fishing XP.
 */
public final class TwinHookHandler {

    private TwinHookHandler() {}

    /**
     * Called for each entity vanilla spawns from a completed catch. When the entity is the
     * dropped item and the rod's Twin Hook level rolls, spawns one extra copy of it.
     *
     * @param hook    the retrieving fishing hook (source of level, RNG, and world)
     * @param rod     the fishing rod being reeled in, read for its Twin Hook level
     * @param spawned the entity vanilla just added — only an {@link ItemEntity} is duplicated
     */
    public static void maybeDuplicate(FishingHook hook, ItemStack rod, Entity spawned) {
        if (!(spawned instanceof ItemEntity caught)) return;
        Level level = hook.level();
        if (level.isClientSide()) return;

        int enchantLevel = EnchantmentEffects.getEnchantmentLevel(rod, EnchantmentEffects.TWIN_HOOK);
        if (enchantLevel <= 0) return;
        if (!FishingEnchantMath.shouldDuplicate(enchantLevel, hook.getRandom().nextDouble())) return;

        Player owner = hook.getPlayerOwner();
        EffectGuard.run("twin_hook", owner != null ? owner : hook,
                () -> spawnDuplicate(hook.level(), caught));
    }

    /**
     * Spawns one extra {@link ItemEntity} matching {@code caught} — same stack, same toss
     * trajectory toward the player — without any experience or stat award. The deterministic
     * seam Twin Hook's gametest drives directly.
     */
    public static void spawnDuplicate(Level level, ItemEntity caught) {
        ItemEntity copy = new ItemEntity(level, caught.getX(), caught.getY(), caught.getZ(),
                caught.getItem().copy());
        copy.setDeltaMovement(caught.getDeltaMovement());
        level.addFreshEntity(copy);
    }
}
