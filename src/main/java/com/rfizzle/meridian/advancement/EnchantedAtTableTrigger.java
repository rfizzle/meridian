package com.rfizzle.meridian.advancement;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class EnchantedAtTableTrigger extends SimpleCriterionTrigger<EnchantedAtTableTrigger.TriggerInstance> {

    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer player, ItemStack stack, int level,
                        float eterna, float quanta, float arcana, float rectification) {
        this.trigger(player, inst -> inst.test(stack, level, eterna, quanta, arcana, rectification));
    }

    public record TriggerInstance(
            Optional<ContextAwarePredicate> player,
            Optional<ItemPredicate> item,
            MinMaxBounds.Ints levels,
            MinMaxBounds.Doubles eterna,
            MinMaxBounds.Doubles quanta,
            MinMaxBounds.Doubles arcana,
            MinMaxBounds.Doubles rectification
    ) implements SimpleCriterionTrigger.SimpleInstance {

        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(inst -> inst
                .group(
                        EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                        ItemPredicate.CODEC.optionalFieldOf("item").forGetter(TriggerInstance::item),
                        MinMaxBounds.Ints.CODEC.optionalFieldOf("levels", MinMaxBounds.Ints.ANY).forGetter(TriggerInstance::levels),
                        MinMaxBounds.Doubles.CODEC.optionalFieldOf("eterna", MinMaxBounds.Doubles.ANY).forGetter(TriggerInstance::eterna),
                        MinMaxBounds.Doubles.CODEC.optionalFieldOf("quanta", MinMaxBounds.Doubles.ANY).forGetter(TriggerInstance::quanta),
                        MinMaxBounds.Doubles.CODEC.optionalFieldOf("arcana", MinMaxBounds.Doubles.ANY).forGetter(TriggerInstance::arcana),
                        MinMaxBounds.Doubles.CODEC.optionalFieldOf("rectification", MinMaxBounds.Doubles.ANY).forGetter(TriggerInstance::rectification))
                .apply(inst, TriggerInstance::new));

        public boolean test(ItemStack stack, int level, float eterna, float quanta, float arcana, float rectification) {
            return (this.item.isEmpty() || this.item.get().test(stack))
                    && this.levels.matches(level)
                    && this.eterna.matches(eterna)
                    && this.quanta.matches(quanta)
                    && this.arcana.matches(arcana)
                    && this.rectification.matches(rectification);
        }
    }
}
