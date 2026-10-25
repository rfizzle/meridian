package com.rfizzle.meridian.enchanting.recipe;

import com.mojang.serialization.MapCodec;
import com.rfizzle.meridian.Meridian;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.OptionalInt;

/**
 * Variant of {@link EnchantingRecipe} that preserves the input stack's enchantments on the output.
 *
 * <p>Zenith's 1.20.1 implementation copied the whole NBT compound via {@code out.setTag(...)};
 * 1.21.1 replaced item NBT with a component map, so only the {@link DataComponents#ENCHANTMENTS}
 * component is copied here, plus {@code meridian:library_contents} — the fields the shipped
 * recipes actually need to carry (the Basic → Ender Library upgrade path in DESIGN § "Enchantment-Table Crafting").
 *
 * <p>Everything else — ingredient/ stat matching, fields, stream codec — is inherited from
 * {@link EnchantingRecipe}; only {@link #assemble}, {@link #getSerializer}, and {@link #getType}
 * are re-bound.
 */
public class KeepNbtEnchantingRecipe extends EnchantingRecipe {

    public KeepNbtEnchantingRecipe(Ingredient input,
                                   StatRequirements requirements,
                                   StatRequirements maxRequirements,
                                   ItemStack result,
                                   OptionalInt displayLevel,
                                   int xpCost,
                                   RecipeModule module) {
        super(input, requirements, maxRequirements, result, displayLevel, xpCost, module);
    }

    public KeepNbtEnchantingRecipe(Ingredient input,
                                   StatRequirements requirements,
                                   StatRequirements maxRequirements,
                                   ItemStack result,
                                   OptionalInt displayLevel,
                                   int xpCost) {
        this(input, requirements, maxRequirements, result, displayLevel, xpCost, RecipeModule.CORE);
    }

    @Override
    public ItemStack assemble(SingleRecipeInput pInput, HolderLookup.Provider registries) {
        ItemStack out = result.copy();
        ItemEnchantments enchantments = pInput.item().get(DataComponents.ENCHANTMENTS);
        if (enchantments != null && !enchantments.isEmpty()) {
            out.set(DataComponents.ENCHANTMENTS, enchantments);
        }
        // Looked up by id rather than through MeridianRegistry, whose static init (block/item/menu
        // registration) can't run on the unit-test classpath that exercises this recipe.
        DataComponentType<?> libraryContents =
                BuiltInRegistries.DATA_COMPONENT_TYPE.get(Meridian.id("library_contents"));
        if (libraryContents != null) {
            copyComponent(libraryContents, pInput.item(), out);
        }
        return out;
    }

    private static <T> void copyComponent(DataComponentType<T> type, ItemStack from, ItemStack to) {
        T value = from.get(type);
        if (value != null) {
            to.set(type, value);
        }
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return EnchantingRecipeRegistry.KEEP_NBT_SERIALIZER;
    }

    @Override
    public RecipeType<?> getType() {
        return EnchantingRecipeRegistry.KEEP_NBT_TYPE;
    }

    /** Concrete serializer for {@code meridian:keep_nbt_enchanting}. */
    public static class Serializer implements RecipeSerializer<KeepNbtEnchantingRecipe> {
        private final MapCodec<KeepNbtEnchantingRecipe> mapCodec;
        private final StreamCodec<RegistryFriendlyByteBuf, KeepNbtEnchantingRecipe> streamCodec;

        public Serializer() {
            this.mapCodec = buildMapCodec(KeepNbtEnchantingRecipe::new);
            this.streamCodec = buildStreamCodec(KeepNbtEnchantingRecipe::new);
        }

        @Override
        public MapCodec<KeepNbtEnchantingRecipe> codec() {
            return mapCodec;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, KeepNbtEnchantingRecipe> streamCodec() {
            return streamCodec;
        }
    }
}
