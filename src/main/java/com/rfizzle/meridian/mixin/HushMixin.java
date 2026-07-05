package com.rfizzle.meridian.mixin;

import com.rfizzle.meridian.enchanting.EnchantmentEffects;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hush: a wearer of Hush boots emits no movement game events, so sculk sensors and the Warden —
 * both of which listen through the {@link ServerLevel} game-event dispatch — never hear the
 * wearer walk, swim, or land. This is the single server-side funnel every vibration listener
 * subscribes to; cancelling here at HEAD suppresses only the vibration channel. Footstep sounds
 * and particles are emitted through separate paths and are untouched, as are non-vibration
 * detection routes (a Sniffer's scent, a Warden's line-of-sight suspicion).
 *
 * <p>Only movement-originated events from a Hush wearer are dropped; every other game event the
 * wearer produces (breaking a block, attacking, opening a container) propagates normally.
 */
@Mixin(ServerLevel.class)
public abstract class HushMixin {

    @Inject(
            method = "gameEvent(Lnet/minecraft/core/Holder;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/level/gameevent/GameEvent$Context;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void meridian$suppressHushVibrations(Holder<GameEvent> event, Vec3 position,
                                                 GameEvent.Context context, CallbackInfo ci) {
        // This fires for every server-side game event (block changes, container opens, entity
        // actions), so the guards run cheapest-and-most-selective first: the source-entity read is
        // a zero-allocation record access that rejects the bulk of events (block-only, null
        // source) before any event-type or equipment work. The event-type check is a handful of
        // identity comparisons against the registry's singleton GameEvents — no set lookup, no
        // Optional. The equipment scan runs only for a living entity's movement event.
        if (!(context.sourceEntity() instanceof LivingEntity living)) return;
        if (!meridian$isMovementEvent(event.value())) return;

        if (EnchantmentEffects.getEquippedLevel(living, EnchantmentEffects.HUSH, EquipmentSlot.FEET) > 0) {
            ci.cancel();
        }
    }

    /**
     * Identity comparison against the movement GameEvents. Value equality can't be used —
     * {@link GameEvent} is a record over a single notification-radius int, so unrelated events
     * with the same radius compare equal — but the registry hands out one singleton per event, so
     * reference identity is exact and allocation-free.
     */
    @Unique
    private static boolean meridian$isMovementEvent(GameEvent event) {
        return event == GameEvent.STEP.value()
                || event == GameEvent.SWIM.value()
                || event == GameEvent.HIT_GROUND.value()
                || event == GameEvent.SPLASH.value()
                || event == GameEvent.FLAP.value();
    }
}
