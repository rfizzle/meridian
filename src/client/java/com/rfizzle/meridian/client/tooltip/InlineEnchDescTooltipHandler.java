package com.rfizzle.meridian.client.tooltip;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.config.MeridianConfig;
import com.rfizzle.meridian.enchanting.EnchantmentInfoRegistry;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class InlineEnchDescTooltipHandler {

    private InlineEnchDescTooltipHandler() {}

    public static void register() {
        ItemTooltipCallback.EVENT.register(InlineEnchDescTooltipHandler::onTooltip);
    }

    private static void onTooltip(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context,
                                  net.minecraft.world.item.TooltipFlag flag, List<Component> lines) {
        MeridianConfig config = Meridian.getConfig();
        if (config == null || !config.display.enableInlineEnchDescs) return;

        List<EnchDescEntry> entries = new ArrayList<>();
        collectEntries(stack.get(DataComponents.ENCHANTMENTS), entries);
        collectEntries(stack.get(DataComponents.STORED_ENCHANTMENTS), entries);
        if (entries.isEmpty()) return;

        for (int i = lines.size() - 1; i >= 0; i--) {
            String lineStr = lines.get(i).getString();
            for (EnchDescEntry entry : entries) {
                if (lineStr.equals(entry.nameStr)) {
                    lines.add(i + 1, entry.desc);
                    break;
                }
            }
        }
    }

    private static void collectEntries(ItemEnchantments enchantments, List<EnchDescEntry> entries) {
        if (enchantments == null || enchantments.isEmpty()) return;
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
            Holder<Enchantment> holder = entry.getKey();
            if (!EnchantmentInfoRegistry.getInfo(holder).enabled()) continue;
            int level = entry.getIntValue();
            Optional<ResourceKey<Enchantment>> keyOpt = holder.unwrapKey();
            if (keyOpt.isEmpty()) continue;
            String descKey = keyOpt.get().location().toLanguageKey("enchantment") + ".desc";
            if (!I18n.exists(descKey)) continue;
            String nameStr = Enchantment.getFullname(holder, level).getString();
            entries.add(new EnchDescEntry(
                    nameStr,
                    Component.translatable(descKey).withStyle(ChatFormatting.DARK_GRAY)
            ));
        }
    }

    private record EnchDescEntry(String nameStr, Component desc) {}
}
