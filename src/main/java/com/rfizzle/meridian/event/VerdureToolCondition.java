package com.rfizzle.meridian.event;

import com.mojang.serialization.MapCodec;
import com.rfizzle.meridian.enchanting.EnchantmentEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;

import java.util.Set;

/**
 * Gate for the Verdure sapling/apple pools: passes only when the harvesting tool carries the
 * Verdure enchantment. Reading {@link LootContextParams#TOOL} at roll time keeps the gate honest
 * against the live tool — a plain shears never trips it, a Verdure shears always does. The
 * enchantment level is read straight off the stack's components, so no registry lookup is needed.
 *
 * <p>Stateless singleton — the condition carries no data, so the codec is a unit and one shared
 * instance serves every pool.
 */
public final class VerdureToolCondition implements LootItemCondition {

    public static final VerdureToolCondition INSTANCE = new VerdureToolCondition();

    public static final MapCodec<VerdureToolCondition> CODEC = MapCodec.unit(INSTANCE);

    /**
     * Condition type registered with {@code BuiltInRegistries.LOOT_CONDITION_TYPE} in
     * {@code MeridianRegistry.register()}. Registration must run before the loot-condition
     * registry freezes — {@link VerdureLootHandler} emits this type when the MODIFY listener
     * fires, which can happen immediately on server start.
     */
    public static final LootItemConditionType TYPE = new LootItemConditionType(CODEC);

    private VerdureToolCondition() {
    }

    @Override
    public LootItemConditionType getType() {
        return TYPE;
    }

    @Override
    public Set<LootContextParam<?>> getReferencedContextParams() {
        // Declares the TOOL param so vanilla's loot-table validator warns if a future refactor
        // wires this onto a table shape that won't provide the harvesting tool.
        return Set.of(LootContextParams.TOOL);
    }

    @Override
    public boolean test(LootContext context) {
        ItemStack tool = context.getParamOrNull(LootContextParams.TOOL);
        return tool != null && EnchantmentEffects.getEnchantmentLevel(tool, EnchantmentEffects.VERDURE) > 0;
    }
}
