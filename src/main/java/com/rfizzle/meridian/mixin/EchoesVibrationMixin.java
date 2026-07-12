package com.rfizzle.meridian.mixin;

import com.rfizzle.meridian.enchanting.EchoesVibrationMath;
import com.rfizzle.meridian.enchanting.EnchantmentEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.gameevent.EuclideanGameEventListenerRegistry;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.gameevent.GameEventListenerRegistry;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Optional;

/**
 * Curse of Echoes: the inverse of Hush. Hush drops a wearer's movement vibrations so sculk
 * sensors and the Warden never hear them; Echoes widens the distance at which those same
 * listeners still accept the wearer's movement, so they detect them from much farther away.
 *
 * <p>Vanilla's {@link EuclideanGameEventListenerRegistry#visitInRangeListeners} range-gates each
 * listener through the private static {@code getPostableListenerPosition}, which compares the
 * squared event-to-listener distance against the listener's own squared radius. That helper does
 * not receive the {@link GameEvent.Context}, so it cannot know who produced the event — but the
 * enclosing method does. Redirecting the call lets us reimplement the range test with the source's
 * context in scope and inflate the effective radius (via {@link EchoesVibrationMath}) only for a
 * movement event whose source wears Echoes.
 *
 * <p>This rides a per-listener, per-event path, so the guards run cheapest-first: the vanilla
 * in-range case returns immediately with zero Echoes overhead, and the Echoes lookup runs only for
 * a listener vanilla would otherwise reject — exactly the events Echoes is meant to rescue.
 */
@Mixin(EuclideanGameEventListenerRegistry.class)
public abstract class EchoesVibrationMixin {

    @Redirect(
            method = "visitInRangeListeners",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/gameevent/EuclideanGameEventListenerRegistry;"
                            + "getPostableListenerPosition(Lnet/minecraft/server/level/ServerLevel;"
                            + "Lnet/minecraft/world/phys/Vec3;"
                            + "Lnet/minecraft/world/level/gameevent/GameEventListener;)Ljava/util/Optional;"))
    private Optional<Vec3> meridian$echoesWidenRange(ServerLevel level, Vec3 eventPos,
                                                     GameEventListener listener,
                                                     Holder<GameEvent> event, Vec3 vec3,
                                                     GameEvent.Context context,
                                                     GameEventListenerRegistry.ListenerVisitor visitor) {
        Optional<Vec3> listenerPos = listener.getListenerSource().getPosition(level);
        if (listenerPos.isEmpty()) {
            return Optional.empty();
        }

        double distSq = BlockPos.containing(listenerPos.get()).distSqr(BlockPos.containing(eventPos));
        int baseRadius = listener.getListenerRadius();
        // In native range: exactly vanilla behavior, no Echoes work at all.
        if (distSq <= (double) (baseRadius * baseRadius)) {
            return listenerPos;
        }

        // Out of native range — only an Echoes wearer's movement can still reach this listener.
        int level$echoes = meridian$echoesLevel(context, event);
        if (level$echoes <= 0) {
            return Optional.empty();
        }
        int radiusSq = EchoesVibrationMath.effectiveRadiusSq(baseRadius, level$echoes);
        return distSq > (double) radiusSq ? Optional.empty() : listenerPos;
    }

    @Unique
    private static int meridian$echoesLevel(GameEvent.Context context, Holder<GameEvent> event) {
        if (!(context.sourceEntity() instanceof LivingEntity living)) {
            return 0;
        }
        if (!meridian$isMovementEvent(event.value())) {
            return 0;
        }
        return EnchantmentEffects.getEquippedLevel(living, EnchantmentEffects.CURSE_OF_ECHOES,
                EquipmentSlot.LEGS, EquipmentSlot.FEET);
    }

    /**
     * Identity comparison against the movement GameEvents — the same set Hush suppresses. The
     * registry hands out one singleton per event, so reference identity is exact and
     * allocation-free (value equality can't be used: {@link GameEvent} is a record over a single
     * notification-radius int, so unrelated events with the same radius compare equal).
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
