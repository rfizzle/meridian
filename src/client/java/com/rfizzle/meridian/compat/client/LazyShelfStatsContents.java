package com.rfizzle.meridian.compat.client;

import com.mojang.serialization.MapCodec;
import com.rfizzle.meridian.compat.common.RecipeInfoFormatter;
import com.rfizzle.meridian.enchanting.EnchantingStatRegistry;
import com.rfizzle.meridian.enchanting.EnchantingStats;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

/**
 * A {@link ComponentContents} whose text is resolved from
 * {@link EnchantingStatRegistry#blockEntries()} at render time (each {@code visit} call)
 * rather than captured when the component is built. This lets the JEI / EMI / REI shelf
 * info panels be registered before the {@code EnchantingStatRegistry} sync arrives on a
 * dedicated server — the panel exists immediately, and its stat lines fill in once the
 * registry is populated.
 *
 * <p>Stat lines are joined with {@code \n} so each viewer's word-wrap renders them as
 * separate visual lines. The content is render-only and never serialized, so {@link #type()}
 * returns a private dummy {@link ComponentContents.Type} that is not registered.
 */
public final class LazyShelfStatsContents implements ComponentContents {

    private static final ComponentContents.Type<LazyShelfStatsContents> TYPE =
            new ComponentContents.Type<>(MapCodec.unit(() -> null), "meridian:lazy_shelf_stats");

    private final ResourceLocation blockId;

    private LazyShelfStatsContents(ResourceLocation blockId) {
        this.blockId = blockId;
    }

    /** Wraps a lazy shelf-stats component for the given shelf block. */
    public static MutableComponent component(ResourceLocation blockId) {
        return MutableComponent.create(new LazyShelfStatsContents(blockId));
    }

    @Override
    public <T> Optional<T> visit(FormattedText.ContentConsumer<T> visitor) {
        return visitor.accept(computeText());
    }

    @Override
    public <T> Optional<T> visit(FormattedText.StyledContentConsumer<T> visitor, Style style) {
        return visitor.accept(style, computeText());
    }

    @Override
    public ComponentContents.Type<?> type() {
        return TYPE;
    }

    private String computeText() {
        EnchantingStats stats = EnchantingStatRegistry.getInstance().blockEntries()
                .getOrDefault(blockId, EnchantingStats.ZERO);
        return String.join("\n", RecipeInfoFormatter.shelfStatLines(stats));
    }

    @Override
    public String toString() {
        return "lazyShelfStats(" + blockId + ")";
    }
}
