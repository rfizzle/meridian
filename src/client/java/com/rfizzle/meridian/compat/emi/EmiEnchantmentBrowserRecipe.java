package com.rfizzle.meridian.compat.emi;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.compat.common.EnchantmentBrowserRecord;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * EMI display for one enchantment in the "Enchantments" browser.
 * Renders the name, level chips, and exclusivity tags.
 * Detailed power thresholds and override info are shown on hover.
 */
public final class EmiEnchantmentBrowserRecipe extends BasicEmiRecipe {

    private static final ResourceLocation GUI_TEXTURE = Meridian.id("textures/gui/enchanting_table.png");

    private final EnchantmentBrowserRecord record;

    public EmiEnchantmentBrowserRecipe(EmiRecipeCategory category, EnchantmentBrowserRecord record) {
        super(category, record.ench().unwrapKey().orElseThrow().location(), 144, 26);
        this.record = record;
        for (var itemHolder : record.compatibleItems()) {
            this.inputs.add(EmiStack.of(itemHolder.value()));
        }
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        Component name = record.ench().value().description();
        MutableComponent styledName = name.copy();
        if (!record.isEnabled()) {
            styledName.withStyle(ChatFormatting.STRIKETHROUGH, ChatFormatting.RED);
        } else if (record.isTreasure()) {
            styledName.withStyle(ChatFormatting.GOLD);
        }

        widgets.addText(styledName, 2, 2, 0xFFFFFF, true);

        int x = 2;
        // Levels
        String levels = "I-" + record.maxLevel();
        if (record.maxLevel() == 1) levels = "I";
        MutableComponent levelsComp = Component.literal(levels).withStyle(ChatFormatting.GRAY);
        widgets.addText(levelsComp, x, 14, 0xFFFFFF, false);
        x += 24;

        // Exclusivity Tags
        for (String setName : record.exclusiveSetNames()) {
            Component tagText = Component.literal("[" + setName + "]").withStyle(ChatFormatting.AQUA);
            widgets.addText(tagText, x, 14, 0xFFFFFF, false);
            x += (setName.length() * 6) + 12;
        }

        // Config Override Icon
        if (record.isConfigOverridden()) {
            widgets.addTexture(GUI_TEXTURE, 126, 2, 12, 12, 224, 0, 16, 16, 256, 256)
                    .tooltip((mx, my) -> List.of(ClientTooltipComponent.create(Component.translatable("gui.meridian.enchant_info.overridden").getVisualOrderText())));
        }

        // Background / Tooltip area
        widgets.addDrawable(0, 0, 144, 26, (graphics, mx, my, delta) -> {
            // Invisible, just for tooltip
        }).tooltip((mx, my) -> getTooltip());
    }

    private List<ClientTooltipComponent> getTooltip() {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(record.ench().value().description().copy().withStyle(ChatFormatting.WHITE));

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

        return tooltip.stream().map(c -> ClientTooltipComponent.create(c.getVisualOrderText())).toList();
    }
}
