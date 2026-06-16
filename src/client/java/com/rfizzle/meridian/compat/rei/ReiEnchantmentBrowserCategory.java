package com.rfizzle.meridian.compat.rei;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.compat.common.EnchantmentBrowserRecord;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * REI category that renders one {@link ReiEnchantmentBrowserDisplay}.
 */
public final class ReiEnchantmentBrowserCategory implements DisplayCategory<ReiEnchantmentBrowserDisplay> {

    private static final ResourceLocation OVERRIDE_TEXTURE = Meridian.id("textures/gui/enchanting_table.png");

    private final CategoryIdentifier<ReiEnchantmentBrowserDisplay> identifier;
    private final Component title;

    public ReiEnchantmentBrowserCategory(CategoryIdentifier<ReiEnchantmentBrowserDisplay> identifier, Component title) {
        this.identifier = identifier;
        this.title = title;
    }

    @Override
    public Renderer getIcon() {
        return EntryStacks.of(Items.ENCHANTING_TABLE);
    }

    @Override
    public Component getTitle() {
        return title;
    }

    @Override
    public CategoryIdentifier<? extends ReiEnchantmentBrowserDisplay> getCategoryIdentifier() {
        return identifier;
    }

    @Override
    public int getDisplayHeight() {
        return 26;
    }

    @Override
    public List<Widget> setupDisplay(ReiEnchantmentBrowserDisplay display, Rectangle bounds) {
        EnchantmentBrowserRecord record = display.record();
        List<Widget> widgets = new ArrayList<>();

        widgets.add(Widgets.createRecipeBase(bounds));

        MutableComponent name = Component.translatable(record.ench().value().descriptionId());
        if (!record.isEnabled()) {
            name.withStyle(ChatFormatting.STRIKETHROUGH, ChatFormatting.RED);
        } else if (record.isTreasure()) {
            name.withStyle(ChatFormatting.GOLD);
        }

        widgets.add(Widgets.createLabel(new Point(bounds.x + 2, bounds.y + 2), name).leftAligned().noShadow());

        int x = bounds.x + 2;
        String levels = "I-" + record.maxLevel();
        if (record.maxLevel() == 1) levels = "I";
        Component levelsComp = Component.literal(levels).withStyle(ChatFormatting.GRAY);
        widgets.add(Widgets.createLabel(new Point(x, bounds.y + 14), levelsComp).leftAligned().noShadow());
        x += (levels.length() * 6) + 6;

        for (String setName : record.exclusiveSetNames()) {
            Component tagText = Component.literal("[" + setName + "]").withStyle(ChatFormatting.AQUA);
            widgets.add(Widgets.createLabel(new Point(x, bounds.y + 14), tagText).leftAligned().noShadow());
            x += (setName.length() * 6) + 12;
        }

        if (record.isConfigOverridden()) {
            widgets.add(Widgets.createDrawableWidget((graphics, mouseX, mouseY, delta) -> {
                graphics.pose().pushPose();
                graphics.pose().translate(bounds.x + 128, bounds.y + 2, 0);
                graphics.pose().scale(0.75f, 0.75f, 1);
                graphics.blit(OVERRIDE_TEXTURE, 0, 0, 224, 0, 16, 16);
                graphics.pose().popPose();
            }));
        }

        widgets.add(Widgets.createTooltipArea(bounds, getTooltip(record)));

        return widgets;
    }

    private List<Component> getTooltip(EnchantmentBrowserRecord record) {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.translatable(record.ench().value().descriptionId()).withStyle(ChatFormatting.WHITE));

        if (!record.isEnabled()) {
            tooltip.add(Component.translatable("tooltip.meridian.enchlib.disabled").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        }

        if (record.isTreasure()) {
            tooltip.add(Component.translatable("info.meridian.shelf.allows_treasure").withStyle(ChatFormatting.GOLD));
        }

        if (!record.exclusiveSetNames().isEmpty()) {
            tooltip.add(Component.empty());
            tooltip.add(Component.translatable("gui.meridian.enchant_info.exclusive", "").withStyle(ChatFormatting.GRAY));
            for (String setName : record.exclusiveSetNames()) {
                tooltip.add(Component.literal(" - " + setName).withStyle(ChatFormatting.AQUA));
            }
        }

        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("gui.meridian.enchant_info.title").withStyle(ChatFormatting.GRAY));
        for (int i = 0; i < record.powerWindows().size(); i++) {
            int level = i + 1;
            int[] window = record.powerWindows().get(i);
            tooltip.add(Component.literal("Level " + level + ": Eterna " + window[0] + " - " + window[1]).withStyle(ChatFormatting.DARK_GREEN));
        }

        if (record.isConfigOverridden()) {
            tooltip.add(Component.empty());
            tooltip.add(Component.translatable("gui.meridian.enchant_info.overridden").withStyle(ChatFormatting.ITALIC, ChatFormatting.DARK_GRAY));
        }

        return tooltip;
    }
}
