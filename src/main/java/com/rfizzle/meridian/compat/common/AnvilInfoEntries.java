package com.rfizzle.meridian.compat.common;

import java.util.List;
import java.util.stream.Stream;

import com.rfizzle.meridian.MeridianRegistry;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/**
 * Viewer-agnostic source of truth for the anvil economy's info pages. The EMI / REI / JEI
 * adapters each map {@link #snapshot()} onto their own info-page type, so the set of anvil
 * interactions surfaced to the player is declared exactly once.
 *
 * <p>Kept render-free (plain {@link ItemStack} + {@link Component}) so it lives in {@code main}
 * and carries no client-only classpath. The description lines reuse the same {@code info.meridian.*}
 * translation keys the items' tooltips already use, so a wording change lands in both places.
 */
public final class AnvilInfoEntries {

    private AnvilInfoEntries() {
    }

    /** One anvil interaction: the item the player hovers and the lines describing its anvil use. */
    public record AnvilInfoEntry(ItemStack item, List<Component> description) {
    }

    /**
     * The ordered anvil interactions covered by the recipe-viewer info pages: the three salvage
     * tomes, Prismatic Web curse removal, Tempered Core unbreaking, and iron-block anvil repair.
     */
    public static List<AnvilInfoEntry> snapshot() {
        return List.of(
                entry(MeridianRegistry.SCRAP_TOME.getDefaultInstance(),
                        "info.meridian.scrap_tome", "info.meridian.scrap_tome2", "info.meridian.scrap_tome3"),
                entry(MeridianRegistry.IMPROVED_SCRAP_TOME.getDefaultInstance(),
                        "info.meridian.improved_scrap_tome", "info.meridian.improved_scrap_tome2",
                        "info.meridian.improved_scrap_tome3"),
                entry(MeridianRegistry.EXTRACTION_TOME.getDefaultInstance(),
                        "info.meridian.extraction_tome", "info.meridian.extraction_tome2",
                        "info.meridian.extraction_tome3"),
                entry(MeridianRegistry.PRISMATIC_WEB.getDefaultInstance(), "info.meridian.prismatic_web"),
                entry(MeridianRegistry.TEMPERED_CORE.getDefaultInstance(), "info.meridian.tempered_core"),
                entry(new ItemStack(Blocks.IRON_BLOCK), "info.meridian.iron_block_repair"));
    }

    private static AnvilInfoEntry entry(ItemStack item, String... translationKeys) {
        List<Component> lines = Stream.of(translationKeys)
                .map(key -> (Component) Component.translatable(key))
                .toList();
        return new AnvilInfoEntry(item, lines);
    }
}
