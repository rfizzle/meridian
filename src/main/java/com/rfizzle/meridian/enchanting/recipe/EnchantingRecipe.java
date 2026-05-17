package com.rfizzle.meridian.enchanting.recipe;

import com.rfizzle.meridian.enchanting.StatCollection;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.OptionalInt;

/**
 * Data-driven enchantment-table crafting recipe. Ported from Zenith's
 * {@code dev.shadowsoffire.apotheosis.ench.table.EnchantingRecipe} onto 1.21.1's codec-based
 * recipe API.
 *
 * <p>The vanilla {@link Recipe#matches(net.minecraft.world.item.crafting.RecipeInput, Level) matches}
 * hook only validates the input ingredient — the stat-threshold half of the match runs against
 * {@link StatCollection} via {@link #matches(ItemStack, float, float, float)}. Call sites:
 * <ul>
 *   <li>{@code MeridianEnchantmentMenu#slotsChanged} (Epic 5) — looks up a result preview.</li>
 *   <li>{@code EnchantingRecipeRegistry#findMatch} — the authoritative dual check.</li>
 * </ul>
 *
 * <p>See DESIGN.md § "Enchantment-Table Crafting" for the design intent. Field layout matches
 * TODO T-4.6.1 verbatim.
 */
public class EnchantingRecipe implements Recipe<SingleRecipeInput> {

    protected final Ingredient input;
    protected final StatRequirements requirements;
    protected final StatRequirements maxRequirements;
    protected final ItemStack result;
    protected final OptionalInt displayLevel;
    protected final int xpCost;

    public EnchantingRecipe(Ingredient input,
                            StatRequirements requirements,
                            StatRequirements maxRequirements,
                            ItemStack result,
                            OptionalInt displayLevel,
                            int xpCost) {
        this.input = input;
        this.requirements = requirements;
        this.maxRequirements = maxRequirements;
        this.result = result;
        this.displayLevel = displayLevel;
        this.xpCost = xpCost;
    }

    public Ingredient getInput() {
        return input;
    }

    public StatRequirements getRequirements() {
        return requirements;
    }

    public StatRequirements getMaxRequirements() {
        return maxRequirements;
    }

    public ItemStack getResult() {
        return result;
    }

    public OptionalInt getDisplayLevel() {
        return displayLevel;
    }

    public int getXpCost() {
        return xpCost;
    }

    /**
     * Returns the XP level cost to perform this infusion. If the recipe specifies an explicit
     * {@code xp_cost}, that value is used. Otherwise, derives the cost from the recipe's eterna
     * requirement — matching Zenith's slot-2 behavior where the level cost equals
     * {@code Math.round(eterna)}.
     */
    public int getEffectiveXpCost() {
        if (xpCost > 0) return xpCost;
        return Math.max(1, Math.round(requirements.eterna()));
    }

    /**
     * Dual-axis match: the ingredient must accept the stack and each stat axis must be within the
     * {@code [requirements, maxRequirements]} window. A {@code -1} entry on
     * {@link #maxRequirements} disables that axis' upper bound (Zenith convention).
     */
    public boolean matches(ItemStack stack, float eterna, float quanta, float arcana) {
        if (maxRequirements.eterna() > -1 && eterna > maxRequirements.eterna()) return false;
        if (maxRequirements.quanta() > -1 && quanta > maxRequirements.quanta()) return false;
        if (maxRequirements.arcana() > -1 && arcana > maxRequirements.arcana()) return false;
        return input.test(stack)
                && eterna >= requirements.eterna()
                && quanta >= requirements.quanta()
                && arcana >= requirements.arcana();
    }

    @Override
    public boolean matches(SingleRecipeInput pInput, Level level) {
        return input.test(pInput.item());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput pInput, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return EnchantingRecipeRegistry.ENCHANTING_SERIALIZER;
    }

    @Override
    public RecipeType<?> getType() {
        return EnchantingRecipeRegistry.ENCHANTING_TYPE;
    }

    /** Factory surface for {@link Serializer} — sub-types override to return a different class. */
    @FunctionalInterface
    protected interface Factory<T extends EnchantingRecipe> {
        T create(Ingredient input,
                 StatRequirements requirements,
                 StatRequirements maxRequirements,
                 ItemStack result,
                 OptionalInt displayLevel,
                 int xpCost);
    }

    protected static <T extends EnchantingRecipe> MapCodec<T> buildMapCodec(Factory<T> factory) {
        return RecordCodecBuilder.mapCodec(instance -> instance.group(
                Ingredient.CODEC_NONEMPTY.fieldOf("input").forGetter(r -> r.input),
                StatRequirements.CODEC.fieldOf("requirements").forGetter(r -> r.requirements),
                StatRequirements.CODEC
                        .optionalFieldOf("max_requirements", StatRequirements.NO_MAX)
                        .forGetter(r -> r.maxRequirements),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(r -> r.result),
                Codec.INT.optionalFieldOf("display_level").xmap(
                        opt -> opt.map(OptionalInt::of).orElseGet(OptionalInt::empty),
                        oi -> oi.isPresent() ? Optional.of(oi.getAsInt()) : Optional.empty()
                ).forGetter(r -> r.displayLevel),
                Codec.INT.optionalFieldOf("xp_cost", 0).forGetter(r -> r.xpCost)
        ).apply(instance, factory::create));
    }

    protected static <T extends EnchantingRecipe> StreamCodec<RegistryFriendlyByteBuf, T> buildStreamCodec(Factory<T> factory) {
        return StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, r -> r.input,
                StatRequirements.STREAM_CODEC, r -> r.requirements,
                StatRequirements.STREAM_CODEC, r -> r.maxRequirements,
                ItemStack.STREAM_CODEC, r -> r.result,
                ByteBufCodecs.optional(ByteBufCodecs.VAR_INT).map(
                        opt -> opt.map(OptionalInt::of).orElseGet(OptionalInt::empty),
                        oi -> oi.isPresent() ? Optional.of(oi.getAsInt()) : Optional.<Integer>empty()
                ), r -> r.displayLevel,
                ByteBufCodecs.VAR_INT, r -> r.xpCost,
                factory::create);
    }

    /** Concrete serializer for {@code meridian:enchanting}. */
    public static class Serializer implements RecipeSerializer<EnchantingRecipe> {
        private final MapCodec<EnchantingRecipe> mapCodec;
        private final StreamCodec<RegistryFriendlyByteBuf, EnchantingRecipe> streamCodec;

        public Serializer() {
            this.mapCodec = buildMapCodec(EnchantingRecipe::new);
            this.streamCodec = buildStreamCodec(EnchantingRecipe::new);
        }

        @Override
        public MapCodec<EnchantingRecipe> codec() {
            return mapCodec;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, EnchantingRecipe> streamCodec() {
            return streamCodec;
        }
    }
}
