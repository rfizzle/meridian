package com.rfizzle.meridian.event;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.config.MeridianConfig;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;

import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Config-driven gate for the two Warden tendril pools. Reads
 * {@link Meridian#getConfig()} on every {@link #test(LootContext)} call rather than
 * baking the chance at MODIFY time, so operators running {@code /meridian reload} see
 * updated values on the very next kill — no full datapack reload required.
 *
 * <p>Two shapes keyed by {@link Kind}:
 * <ul>
 *   <li>{@link Kind#DROP_CHANCE} — flat random roll against
 *       {@code config.warden.tendrilDropChance}.</li>
 *   <li>{@link Kind#LOOTING_BONUS} — random roll against
 *       {@code lootingLevel * config.warden.tendrilLootingBonus}, where {@code lootingLevel}
 *       is pulled from the {@link LootContextParams#ATTACKING_ENTITY ATTACKING_ENTITY}'s gear
 *       at roll time. No attacker → level zero → chance zero (matches the vanilla
 *       {@code LootItemRandomChanceWithEnchantedBonusCondition} contract).</li>
 * </ul>
 *
 * <p>Chance is clamped to {@code [0, 1]} before each random draw — guards against racey
 * mutations during a mid-reload state where a half-written config could briefly hold a value
 * outside the documented range.
 */
public record WardenPoolCondition(Kind kind) implements LootItemCondition {

    public enum Kind implements StringRepresentable {
        DROP_CHANCE("drop_chance"),
        LOOTING_BONUS("looting_bonus");

        public static final Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);

        private final String serializedName;

        Kind(String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }
    }

    public static final MapCodec<WardenPoolCondition> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Kind.CODEC.fieldOf("kind").forGetter(WardenPoolCondition::kind)
    ).apply(inst, WardenPoolCondition::new));

    /**
     * Condition type registered with {@code BuiltInRegistries.LOOT_CONDITION_TYPE} in
     * {@code MeridianRegistry.register()}. Registration must run before the loot-
     * condition registry freezes — {@link WardenLootHandler} emits this type when the MODIFY
     * listener fires, which can happen immediately on server start.
     */
    public static final LootItemConditionType TYPE = new LootItemConditionType(CODEC);

    private static volatile Supplier<MeridianConfig> configSupplier = Meridian::getConfig;

    /**
     * Test seam — lets fixtures plug an alternate config source without mutating the runtime
     * singleton. Production never calls this; paired with {@link #resetConfigSupplier()} so
     * tests restore the default in their tear-down.
     */
    static void overrideConfigSupplier(Supplier<MeridianConfig> supplier) {
        configSupplier = supplier;
    }

    /** Restores the default supplier ({@link Meridian#getConfig()}). */
    static void resetConfigSupplier() {
        configSupplier = Meridian::getConfig;
    }

    @Override
    public LootItemConditionType getType() {
        return TYPE;
    }

    @Override
    public Set<LootContextParam<?>> getReferencedContextParams() {
        // DROP_CHANCE doesn't touch the context; LOOTING_BONUS needs the attacker to read
        // looting level. Declaring this lets vanilla's loot-table validator warn if a future
        // refactor wires the condition onto a table shape that won't provide the param.
        return kind == Kind.LOOTING_BONUS ? Set.of(LootContextParams.ATTACKING_ENTITY) : Set.of();
    }

    @Override
    public boolean test(LootContext context) {
        int lootingLevel = kind == Kind.LOOTING_BONUS ? resolveLootingLevel(context) : 0;
        return roll(context.getRandom(), lootingLevel);
    }

    /**
     * Full roll — reads the current config through the static supplier and draws against the
     * computed chance. Split out from {@link #test(LootContext)} so unit tests can exercise
     * the reload pathway with a supplier override and a seeded {@link RandomSource}.
     */
    boolean roll(RandomSource random, int lootingLevel) {
        MeridianConfig config = configSupplier.get();
        if (config == null) {
            // Config is null between onInitialize wire-up steps; safer to decline than NPE.
            return false;
        }
        float chance = resolveChance(lootingLevel, config);
        return chance > 0.0F && random.nextFloat() < chance;
    }

    /**
     * Pure chance calculation — no side effects, no supplier lookup. Exposed package-private
     * so tests can pin the math per {@link Kind} independently of the supplier plumbing.
     */
    float resolveChance(int lootingLevel, MeridianConfig config) {
        return switch (kind) {
            case DROP_CHANCE -> clamp((float) config.warden.tendrilDropChance);
            case LOOTING_BONUS -> clamp(lootingLevel * (float) config.warden.tendrilLootingBonus);
        };
    }

    private static float clamp(float value) {
        if (value <= 0.0F) return 0.0F;
        if (value >= 1.0F) return 1.0F;
        return value;
    }

    private static int resolveLootingLevel(LootContext context) {
        Entity attacker = context.getParamOrNull(LootContextParams.ATTACKING_ENTITY);
        if (!(attacker instanceof LivingEntity le)) return 0;
        HolderGetter.Provider resolver = context.getResolver();
        Optional<Holder.Reference<Enchantment>> looting = resolver.get(Registries.ENCHANTMENT, Enchantments.LOOTING);
        return looting.map(h -> EnchantmentHelper.getEnchantmentLevel(h, le)).orElse(0);
    }
}
