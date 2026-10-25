package com.rfizzle.meridian.library;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * Item-side snapshot of an Enchantment Library's stored pools ({@code Points} and {@code Levels},
 * keyed by enchantment id). Carried on the dropped library item so breaking, placing, and the
 * Library to Library of Alexandria upgrade keep the stored books.
 */
public record LibraryContents(Map<ResourceLocation, Integer> points, Map<ResourceLocation, Integer> levels) {

    private static final Codec<Map<ResourceLocation, Integer>> MAP_CODEC =
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.INT);

    public static final Codec<LibraryContents> CODEC = RecordCodecBuilder.create(i -> i.group(
            MAP_CODEC.fieldOf("points").forGetter(LibraryContents::points),
            MAP_CODEC.fieldOf("levels").forGetter(LibraryContents::levels)
    ).apply(i, LibraryContents::new));

    private static final StreamCodec<RegistryFriendlyByteBuf, Map<ResourceLocation, Integer>> MAP_STREAM =
            ByteBufCodecs.map(HashMap::new, ResourceLocation.STREAM_CODEC, ByteBufCodecs.VAR_INT);

    public static final StreamCodec<RegistryFriendlyByteBuf, LibraryContents> STREAM_CODEC =
            StreamCodec.composite(MAP_STREAM, LibraryContents::points, MAP_STREAM, LibraryContents::levels,
                    LibraryContents::new);

    public boolean isEmpty() {
        return points.isEmpty() && levels.isEmpty();
    }
}
