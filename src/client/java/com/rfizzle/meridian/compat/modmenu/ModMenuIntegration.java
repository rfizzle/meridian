package com.rfizzle.meridian.compat.modmenu;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.config.MeridianConfig;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;

public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            MeridianConfig config = Meridian.getConfig();
            if (config == null) config = new MeridianConfig();
            MeridianConfig current = config;

            ConfigBuilder builder = ConfigBuilder.create()
                    .setParentScreen(parent)
                    .setTitle(Component.translatable("config.meridian.title"));

            ConfigEntryBuilder entry = builder.entryBuilder();

            // Enchanting Table
            ConfigCategory tableCategory = builder.getOrCreateCategory(
                    Component.translatable("config.meridian.category.enchanting_table"));
            tableCategory.addEntry(entry.startBooleanToggle(
                            Component.translatable("config.meridian.allow_treasure_without_shelf"),
                            current.enchantingTable.allowTreasureWithoutShelf)
                    .setDefaultValue(false)
                    .setSaveConsumer(v -> current.enchantingTable.allowTreasureWithoutShelf = v)
                    .build());
            tableCategory.addEntry(entry.startIntSlider(
                            Component.translatable("config.meridian.max_eterna"),
                            current.enchantingTable.maxEterna, 1, 100)
                    .setDefaultValue(50)
                    .setSaveConsumer(v -> current.enchantingTable.maxEterna = v)
                    .build());
            tableCategory.addEntry(entry.startBooleanToggle(
                            Component.translatable("config.meridian.show_level_indicator"),
                            current.enchantingTable.showLevelIndicator)
                    .setDefaultValue(true)
                    .setSaveConsumer(v -> current.enchantingTable.showLevelIndicator = v)
                    .build());
            tableCategory.addEntry(entry.startIntSlider(
                            Component.translatable("config.meridian.global_min_enchantability"),
                            current.enchantingTable.globalMinEnchantability, 0, 100)
                    .setDefaultValue(1)
                    .setSaveConsumer(v -> current.enchantingTable.globalMinEnchantability = v)
                    .build());

            // Shelves
            ConfigCategory shelvesCategory = builder.getOrCreateCategory(
                    Component.translatable("config.meridian.category.shelves"));
            shelvesCategory.addEntry(entry.startDoubleField(
                            Component.translatable("config.meridian.sculk_shrieker_chance"),
                            current.shelves.sculkShelfShriekerChance)
                    .setDefaultValue(0.02)
                    .setMin(0.0).setMax(1.0)
                    .setSaveConsumer(v -> current.shelves.sculkShelfShriekerChance = v)
                    .build());
            shelvesCategory.addEntry(entry.startDoubleField(
                            Component.translatable("config.meridian.sculk_particle_chance"),
                            current.shelves.sculkParticleChance)
                    .setDefaultValue(0.05)
                    .setMin(0.0).setMax(1.0)
                    .setSaveConsumer(v -> current.shelves.sculkParticleChance = v)
                    .build());

            // Anvil
            ConfigCategory anvilCategory = builder.getOrCreateCategory(
                    Component.translatable("config.meridian.category.anvil"));
            anvilCategory.addEntry(entry.startBooleanToggle(
                            Component.translatable("config.meridian.prismatic_web_removes_curses"),
                            current.anvil.prismaticWebRemovesCurses)
                    .setDefaultValue(true)
                    .setSaveConsumer(v -> current.anvil.prismaticWebRemovesCurses = v)
                    .build());
            anvilCategory.addEntry(entry.startIntField(
                            Component.translatable("config.meridian.prismatic_web_level_cost"),
                            current.anvil.prismaticWebLevelCost)
                    .setDefaultValue(30)
                    .setMin(0)
                    .setSaveConsumer(v -> current.anvil.prismaticWebLevelCost = v)
                    .build());
            anvilCategory.addEntry(entry.startBooleanToggle(
                            Component.translatable("config.meridian.iron_block_repairs_anvil"),
                            current.anvil.ironBlockRepairsAnvil)
                    .setDefaultValue(true)
                    .setSaveConsumer(v -> current.anvil.ironBlockRepairsAnvil = v)
                    .build());

            // Library
            ConfigCategory libraryCategory = builder.getOrCreateCategory(
                    Component.translatable("config.meridian.category.library"));
            libraryCategory.addEntry(entry.startIntField(
                            Component.translatable("config.meridian.io_rate_limit_ticks"),
                            current.library.ioRateLimitTicks)
                    .setDefaultValue(0)
                    .setMin(0)
                    .setSaveConsumer(v -> current.library.ioRateLimitTicks = v)
                    .build());

            // Tomes
            ConfigCategory tomesCategory = builder.getOrCreateCategory(
                    Component.translatable("config.meridian.category.tomes"));
            tomesCategory.addEntry(entry.startIntField(
                            Component.translatable("config.meridian.scrap_tome_xp_cost"),
                            current.tomes.scrapTomeXpCost)
                    .setDefaultValue(3).setMin(0)
                    .setSaveConsumer(v -> current.tomes.scrapTomeXpCost = v)
                    .build());
            tomesCategory.addEntry(entry.startIntField(
                            Component.translatable("config.meridian.improved_scrap_tome_xp_cost"),
                            current.tomes.improvedScrapTomeXpCost)
                    .setDefaultValue(5).setMin(0)
                    .setSaveConsumer(v -> current.tomes.improvedScrapTomeXpCost = v)
                    .build());
            tomesCategory.addEntry(entry.startIntField(
                            Component.translatable("config.meridian.extraction_tome_xp_cost"),
                            current.tomes.extractionTomeXpCost)
                    .setDefaultValue(10).setMin(0)
                    .setSaveConsumer(v -> current.tomes.extractionTomeXpCost = v)
                    .build());
            tomesCategory.addEntry(entry.startIntField(
                            Component.translatable("config.meridian.extraction_tome_item_damage"),
                            current.tomes.extractionTomeItemDamage)
                    .setDefaultValue(50).setMin(0)
                    .setSaveConsumer(v -> current.tomes.extractionTomeItemDamage = v)
                    .build());
            tomesCategory.addEntry(entry.startDoubleField(
                            Component.translatable("config.meridian.extraction_tome_repair_percent"),
                            current.tomes.extractionTomeRepairPercent)
                    .setDefaultValue(0.25).setMin(0.0).setMax(1.0)
                    .setSaveConsumer(v -> current.tomes.extractionTomeRepairPercent = v)
                    .build());

            // Warden
            ConfigCategory wardenCategory = builder.getOrCreateCategory(
                    Component.translatable("config.meridian.category.warden"));
            wardenCategory.addEntry(entry.startDoubleField(
                            Component.translatable("config.meridian.tendril_drop_chance"),
                            current.warden.tendrilDropChance)
                    .setDefaultValue(1.0).setMin(0.0).setMax(1.0)
                    .setSaveConsumer(v -> current.warden.tendrilDropChance = v)
                    .build());
            wardenCategory.addEntry(entry.startDoubleField(
                            Component.translatable("config.meridian.tendril_looting_bonus"),
                            current.warden.tendrilLootingBonus)
                    .setDefaultValue(0.10).setMin(0.0).setMax(1.0)
                    .setSaveConsumer(v -> current.warden.tendrilLootingBonus = v)
                    .build());

            // Display
            ConfigCategory displayCategory = builder.getOrCreateCategory(
                    Component.translatable("config.meridian.category.display"));
            displayCategory.addEntry(entry.startBooleanToggle(
                            Component.translatable("config.meridian.show_book_tooltips"),
                            current.display.showBookTooltips)
                    .setDefaultValue(true)
                    .setSaveConsumer(v -> current.display.showBookTooltips = v)
                    .build());
            displayCategory.addEntry(entry.startStrField(
                            Component.translatable("config.meridian.over_leveled_color"),
                            current.display.overLeveledColor)
                    .setDefaultValue("#FF6600")
                    .setSaveConsumer(v -> current.display.overLeveledColor = v)
                    .build());
            displayCategory.addEntry(entry.startBooleanToggle(
                            Component.translatable("config.meridian.enable_inline_ench_descs"),
                            current.display.enableInlineEnchDescs)
                    .setDefaultValue(false)
                    .setSaveConsumer(v -> current.display.enableInlineEnchDescs = v)
                    .build());

            builder.setSavingRunnable(() -> {
                current.save();
                Meridian.reloadConfig();
            });

            return builder.build();
        };
    }
}
