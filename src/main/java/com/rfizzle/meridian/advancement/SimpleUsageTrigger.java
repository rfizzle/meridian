package com.rfizzle.meridian.advancement;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

/**
 * A criterion trigger that fires the instant a player performs a specific mechanic, with no extra
 * conditions beyond the optional {@code player} predicate every advancement criterion already
 * supports. One instance backs each usage-triggered advancement (library deposit/extract,
 * filtering-shelf blacklist, tome salvage, curse strip, tempered core) — the mechanics differ but
 * their advancement shape is identical, so they share this class and are told apart only by the
 * distinct {@link net.minecraft.resources.ResourceLocation} each instance registers under in
 * {@link ModTriggers}.
 */
public class SimpleUsageTrigger extends SimpleCriterionTrigger<SimpleUsageTrigger.TriggerInstance> {

    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    /** Fires the trigger for {@code player}; the criterion matches on any qualifying player. */
    public void trigger(ServerPlayer player) {
        this.trigger(player, inst -> true);
    }

    public record TriggerInstance(Optional<ContextAwarePredicate> player)
            implements SimpleCriterionTrigger.SimpleInstance {

        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(inst -> inst
                .group(EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player))
                .apply(inst, TriggerInstance::new));
    }
}
