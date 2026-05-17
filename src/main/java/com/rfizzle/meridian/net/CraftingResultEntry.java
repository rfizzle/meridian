package com.rfizzle.meridian.net;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Fourth-row crafting-result preview piggybacked on {@link StatsPayload}. The recipe id is
 * included so JEI/EMI adapters can highlight the source recipe when the player hovers the row.
 */
public record CraftingResultEntry(ItemStack result, int xpCost, ResourceLocation recipeId) {

    public static final StreamCodec<RegistryFriendlyByteBuf, CraftingResultEntry> STREAM_CODEC =
            StreamCodec.composite(
                    ItemStack.OPTIONAL_STREAM_CODEC, CraftingResultEntry::result,
                    ByteBufCodecs.VAR_INT, CraftingResultEntry::xpCost,
                    ResourceLocation.STREAM_CODEC, CraftingResultEntry::recipeId,
                    CraftingResultEntry::new);
}
