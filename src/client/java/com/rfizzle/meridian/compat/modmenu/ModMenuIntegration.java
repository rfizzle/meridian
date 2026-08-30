package com.rfizzle.meridian.compat.modmenu;

import java.util.Optional;
import java.util.List;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.locale.Language;
import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.config.MeridianConfig;
import com.rfizzle.meridian.enchanting.GroomMath;
import com.rfizzle.meridian.network.MeridianNetworking;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            // Deep working copy — never the live instance (mc-config). Every save consumer writes
            // into `current`, and publishConfig() swaps it in as one store, so a reader mid-tick
            // cannot see one edited field beside an unedited one.
            MeridianConfig current = Meridian.getConfig().copy();

            ConfigBuilder builder = ConfigBuilder.create()
                    .setParentScreen(parent)
                    .setTitle(Component.translatable("config.meridian.title"));

            ConfigEntryBuilder entry = builder.entryBuilder();

            // Enchanting Table
            ConfigCategory tableCategory = builder.getOrCreateCategory(
                    Component.translatable("config.meridian.category.enchanting_table"));
            tableCategory.addEntry(entry.startBooleanToggle(
                            Component.translatable("config.meridian.enchantingTable.allowTreasureWithoutShelf"),
                            current.enchantingTable.allowTreasureWithoutShelf)
                    .setDefaultValue(false)
                    .setSaveConsumer(v -> current.enchantingTable.allowTreasureWithoutShelf = v)
                    .build());
            tableCategory.addEntry(entry.startIntSlider(
                            Component.translatable("config.meridian.enchantingTable.maxEterna"),
                            current.enchantingTable.maxEterna, 1, 100)
                    .setDefaultValue(50)
                    .setSaveConsumer(v -> current.enchantingTable.maxEterna = v)
                    .build());
            tableCategory.addEntry(entry.startIntSlider(
                            Component.translatable("config.meridian.enchantingTable.globalMinEnchantability"),
                            current.enchantingTable.globalMinEnchantability, 0, 100)
                    .setDefaultValue(1)
                    .setSaveConsumer(v -> current.enchantingTable.globalMinEnchantability = v)
                    .build());

            // Table Crafting
            ConfigCategory tableCraftingCategory = builder.getOrCreateCategory(
                    Component.translatable("config.meridian.category.table_crafting"));
            tableCraftingCategory.addEntry(entry.startBooleanToggle(
                            Component.translatable("config.meridian.tableCrafting.allowDuplication"),
                            current.tableCrafting.allowDuplication)
                    .setDefaultValue(true)
                    .setSaveConsumer(v -> current.tableCrafting.allowDuplication = v)
                    .build());

            // Shelves
            ConfigCategory shelvesCategory = builder.getOrCreateCategory(
                    Component.translatable("config.meridian.category.shelves"));
            shelvesCategory.addEntry(entry.startDoubleField(
                            Component.translatable("config.meridian.shelves.sculkShelfShriekerChance"),
                            current.shelves.sculkShelfShriekerChance)
                    .setDefaultValue(0.02)
                    .setMin(0.0).setMax(1.0)
                    .setSaveConsumer(v -> current.shelves.sculkShelfShriekerChance = v)
                    .build());
            shelvesCategory.addEntry(entry.startDoubleField(
                            Component.translatable("config.meridian.shelves.sculkParticleChance"),
                            current.shelves.sculkParticleChance)
                    .setDefaultValue(0.05)
                    .setMin(0.0).setMax(1.0)
                    .setSaveConsumer(v -> current.shelves.sculkParticleChance = v)
                    .build());

            // Anvil
            ConfigCategory anvilCategory = builder.getOrCreateCategory(
                    Component.translatable("config.meridian.category.anvil"));
            anvilCategory.addEntry(entry.startBooleanToggle(
                            Component.translatable("config.meridian.anvil.prismaticWebRemovesCurses"),
                            current.anvil.prismaticWebRemovesCurses)
                    .setDefaultValue(true)
                    .setSaveConsumer(v -> current.anvil.prismaticWebRemovesCurses = v)
                    .build());
            anvilCategory.addEntry(entry.startIntField(
                            Component.translatable("config.meridian.anvil.prismaticWebLevelCost"),
                            current.anvil.prismaticWebLevelCost)
                    .setDefaultValue(30)
                    .setMin(0)
                    .setSaveConsumer(v -> current.anvil.prismaticWebLevelCost = v)
                    .build());
            anvilCategory.addEntry(entry.startBooleanToggle(
                            Component.translatable("config.meridian.anvil.ironBlockRepairsAnvil"),
                            current.anvil.ironBlockRepairsAnvil)
                    .setDefaultValue(true)
                    .setSaveConsumer(v -> current.anvil.ironBlockRepairsAnvil = v)
                    .build());
            anvilCategory.addEntry(entry.startBooleanToggle(
                            Component.translatable("config.meridian.anvil.temperedCoreEnabled"),
                            current.anvil.temperedCoreEnabled)
                    .setDefaultValue(true)
                    .setSaveConsumer(v -> current.anvil.temperedCoreEnabled = v)
                    .build());
            anvilCategory.addEntry(entry.startIntField(
                            Component.translatable("config.meridian.anvil.temperedCoreLevelCost"),
                            current.anvil.temperedCoreLevelCost)
                    .setDefaultValue(10)
                    .setMin(0)
                    .setSaveConsumer(v -> current.anvil.temperedCoreLevelCost = v)
                    .build());

            // Library
            ConfigCategory libraryCategory = builder.getOrCreateCategory(
                    Component.translatable("config.meridian.category.library"));
            libraryCategory.addEntry(entry.startIntField(
                            Component.translatable("config.meridian.library.ioRateLimitTicks"),
                            current.library.ioRateLimitTicks)
                    .setDefaultValue(0)
                    .setMin(0)
                    .setSaveConsumer(v -> current.library.ioRateLimitTicks = v)
                    .build());

            // Tomes
            ConfigCategory tomesCategory = builder.getOrCreateCategory(
                    Component.translatable("config.meridian.category.tomes"));
            tomesCategory.addEntry(entry.startIntField(
                            Component.translatable("config.meridian.tomes.scrapTomeXpCost"),
                            current.tomes.scrapTomeXpCost)
                    .setDefaultValue(3).setMin(0)
                    .setSaveConsumer(v -> current.tomes.scrapTomeXpCost = v)
                    .build());
            tomesCategory.addEntry(entry.startIntField(
                            Component.translatable("config.meridian.tomes.improvedScrapTomeXpCost"),
                            current.tomes.improvedScrapTomeXpCost)
                    .setDefaultValue(5).setMin(0)
                    .setSaveConsumer(v -> current.tomes.improvedScrapTomeXpCost = v)
                    .build());
            tomesCategory.addEntry(entry.startIntField(
                            Component.translatable("config.meridian.tomes.extractionTomeXpCost"),
                            current.tomes.extractionTomeXpCost)
                    .setDefaultValue(10).setMin(0)
                    .setSaveConsumer(v -> current.tomes.extractionTomeXpCost = v)
                    .build());
            tomesCategory.addEntry(entry.startIntField(
                            Component.translatable("config.meridian.tomes.extractionTomeItemDamage"),
                            current.tomes.extractionTomeItemDamage)
                    .setDefaultValue(50).setMin(0)
                    .setSaveConsumer(v -> current.tomes.extractionTomeItemDamage = v)
                    .build());
            tomesCategory.addEntry(entry.startDoubleField(
                            Component.translatable("config.meridian.tomes.extractionTomeRepairPercent"),
                            current.tomes.extractionTomeRepairPercent)
                    .setDefaultValue(0.25).setMin(0.0).setMax(1.0)
                    .setSaveConsumer(v -> current.tomes.extractionTomeRepairPercent = v)
                    .build());

            // Everfeast
            ConfigCategory everfeastCategory = builder.getOrCreateCategory(
                    Component.translatable("config.meridian.category.everfeast"));
            everfeastCategory.addEntry(entry.startBooleanToggle(
                            Component.translatable("config.meridian.everfeast.enabled"),
                            current.everfeast.enabled)
                    .setDefaultValue(true)
                    .setSaveConsumer(v -> current.everfeast.enabled = v)
                    .build());
            everfeastCategory.addEntry(entry.startIntField(
                            Component.translatable("config.meridian.everfeast.bites"),
                            current.everfeast.bites)
                    .setDefaultValue(128)
                    .setMin(1).setMax(4096)
                    .setSaveConsumer(v -> current.everfeast.bites = v)
                    .build());

            // Warden
            ConfigCategory wardenCategory = builder.getOrCreateCategory(
                    Component.translatable("config.meridian.category.warden"));
            wardenCategory.addEntry(entry.startDoubleField(
                            Component.translatable("config.meridian.warden.tendrilDropChance"),
                            current.warden.tendrilDropChance)
                    .setDefaultValue(1.0).setMin(0.0).setMax(1.0)
                    .setSaveConsumer(v -> current.warden.tendrilDropChance = v)
                    .build());
            wardenCategory.addEntry(entry.startDoubleField(
                            Component.translatable("config.meridian.warden.tendrilLootingBonus"),
                            current.warden.tendrilLootingBonus)
                    .setDefaultValue(0.10).setMin(0.0).setMax(1.0)
                    .setSaveConsumer(v -> current.warden.tendrilLootingBonus = v)
                    .build());

            // Groom
            ConfigCategory groomCategory = builder.getOrCreateCategory(
                    Component.translatable("config.meridian.category.groom"));
            groomCategory.addEntry(entry.startDoubleField(
                            Component.translatable("config.meridian.groom.chanceLevel1"),
                            current.groom.chanceLevel1)
                    .setDefaultValue(GroomMath.DEFAULT_CHANCE_LEVEL_1)
                    .setMin(0.0).setMax(1.0)
                    .setSaveConsumer(v -> current.groom.chanceLevel1 = v)
                    .build());
            groomCategory.addEntry(entry.startDoubleField(
                            Component.translatable("config.meridian.groom.chanceLevel2"),
                            current.groom.chanceLevel2)
                    .setDefaultValue(GroomMath.DEFAULT_CHANCE_LEVEL_2)
                    .setMin(0.0).setMax(1.0)
                    .setSaveConsumer(v -> current.groom.chanceLevel2 = v)
                    .build());
            groomCategory.addEntry(entry.startIntField(
                            Component.translatable("config.meridian.groom.cooldownTicks"),
                            current.groom.cooldownTicks)
                    .setDefaultValue(GroomMath.DEFAULT_COOLDOWN_TICKS)
                    .setMin(0).setMax(1728000)
                    .setSaveConsumer(v -> current.groom.cooldownTicks = v)
                    .build());

            // Combat
            ConfigCategory combatCategory = builder.getOrCreateCategory(
                    Component.translatable("config.meridian.category.combat"));
            combatCategory.addEntry(entry.startBooleanToggle(
                            Component.translatable("config.meridian.combat.sunderAffectsPlayers"),
                            current.combat.sunderAffectsPlayers)
                    .setDefaultValue(false)
                    .setSaveConsumer(v -> current.combat.sunderAffectsPlayers = v)
                    .build());
            combatCategory.addEntry(entry.startBooleanToggle(
                            Component.translatable("config.meridian.combat.seekerTargetsPlayers"),
                            current.combat.seekerTargetsPlayers)
                    .setDefaultValue(false)
                    .setSaveConsumer(v -> current.combat.seekerTargetsPlayers = v)
                    .build());
            combatCategory.addEntry(entry.startBooleanToggle(
                            Component.translatable("config.meridian.combat.harpoonAffectsPlayers"),
                            current.combat.harpoonAffectsPlayers)
                    .setDefaultValue(false)
                    .setSaveConsumer(v -> current.combat.harpoonAffectsPlayers = v)
                    .build());
            combatCategory.addEntry(entry.startBooleanToggle(
                            Component.translatable("config.meridian.combat.undertowAffectsPlayers"),
                            current.combat.undertowAffectsPlayers)
                    .setDefaultValue(false)
                    .setSaveConsumer(v -> current.combat.undertowAffectsPlayers = v)
                    .build());
            combatCategory.addEntry(entry.startBooleanToggle(
                            Component.translatable("config.meridian.combat.markAffectsPlayers"),
                            current.combat.markAffectsPlayers)
                    .setDefaultValue(false)
                    .setSaveConsumer(v -> current.combat.markAffectsPlayers = v)
                    .build());
            combatCategory.addEntry(entry.startBooleanToggle(
                            Component.translatable("config.meridian.combat.pinAffectsPlayers"),
                            current.combat.pinAffectsPlayers)
                    .setDefaultValue(false)
                    .setSaveConsumer(v -> current.combat.pinAffectsPlayers = v)
                    .build());
            combatCategory.addEntry(entry.startBooleanToggle(
                            Component.translatable("config.meridian.combat.staggerAffectsPlayers"),
                            current.combat.staggerAffectsPlayers)
                    .setDefaultValue(false)
                    .setSaveConsumer(v -> current.combat.staggerAffectsPlayers = v)
                    .build());
            combatCategory.addEntry(entry.startBooleanToggle(
                            Component.translatable("config.meridian.combat.bullrushAffectsPlayers"),
                            current.combat.bullrushAffectsPlayers)
                    .setDefaultValue(false)
                    .setSaveConsumer(v -> current.combat.bullrushAffectsPlayers = v)
                    .build());
            combatCategory.addEntry(entry.startBooleanToggle(
                            Component.translatable("config.meridian.combat.trackersLensAffectsPlayers"),
                            current.combat.trackersLensAffectsPlayers)
                    .setDefaultValue(false)
                    .setSaveConsumer(v -> current.combat.trackersLensAffectsPlayers = v)
                    .build());

            // Attunement
            ConfigCategory attunementCategory = builder.getOrCreateCategory(
                    Component.translatable("config.meridian.category.attunement"));
            attunementCategory.addEntry(entry.startIntField(
                            Component.translatable("config.meridian.attunement.radius"),
                            current.attunement.radius)
                    .setDefaultValue(8).setMin(1).setMax(32)
                    .setSaveConsumer(v -> current.attunement.radius = v)
                    .build());
            attunementCategory.addEntry(entry.startIntField(
                            Component.translatable("config.meridian.attunement.intervalTicks"),
                            current.attunement.intervalTicks)
                    .setDefaultValue(80).setMin(20).setMax(1200)
                    .setSaveConsumer(v -> current.attunement.intervalTicks = v)
                    .build());
            attunementCategory.addEntry(entry.startIntField(
                            Component.translatable("config.meridian.attunement.minEterna"),
                            current.attunement.minEterna)
                    .setDefaultValue(15).setMin(0).setMax(100)
                    .setSaveConsumer(v -> current.attunement.minEterna = v)
                    .build());

            // Display
            ConfigCategory displayCategory = builder.getOrCreateCategory(
                    Component.translatable("config.meridian.category.display"));
            displayCategory.addEntry(entry.startBooleanToggle(
                            Component.translatable("config.meridian.display.showBookTooltips"),
                            current.display.showBookTooltips)
                    .setDefaultValue(true)
                    .setSaveConsumer(v -> current.display.showBookTooltips = v)
                    .build());
            displayCategory.addEntry(entry.startStrField(
                            Component.translatable("config.meridian.display.overLeveledColor"),
                            current.display.overLeveledColor)
                    .setDefaultValue("#FF6600")
                    .setSaveConsumer(v -> current.display.overLeveledColor = v)
                    .build());
            displayCategory.addEntry(entry.startBooleanToggle(
                            Component.translatable("config.meridian.display.enableInlineEnchDescs"),
                            current.display.enableInlineEnchDescs)
                    .setDefaultValue(false)
                    .setSaveConsumer(v -> current.display.enableInlineEnchDescs = v)
                    .build());

            builder.setSavingRunnable(() -> {
                // Clamp before persisting: the number fields let a player type an out-of-range value,
                // and the setters above write it straight into current.* with no bounds check. Clamping
                // here mirrors the mc-config skill's "re-clamp on save" so the file never stores an
                // out-of-range value.
                // Clamps, persists, and swaps the copy in as the live config in one store.
                Meridian.publishConfig(current);
                // On an integrated server the editing client is also the host, so refresh the synced
                // copy the client reads first (#149) — otherwise gameplay-affecting UI keeps showing
                // the pre-edit values until a rejoin. On a dedicated server the client's local edit is
                // irrelevant (the server's config stays authoritative), so there is nothing to push.
                MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
                if (server != null) {
                    server.execute(() -> MeridianNetworking.syncConfigToAll(server));
                }
            });

            for (ConfigCategory category : List.of(tableCategory, tableCraftingCategory, shelvesCategory,
                    anvilCategory, libraryCategory, tomesCategory, everfeastCategory, wardenCategory,
                    groomCategory, combatCategory, attunementCategory, displayCategory)) {
                applyTooltips(category);
            }

            return builder.build();
        };
    }

    /**
     * Attach a tooltip to every entry in a category by deriving its tooltip lang key from the
     * entry's own label key ({@code <label>.tooltip}, DESIGN-SYSTEM §10). The {@link Language#has}
     * guard means a missing string degrades to no tooltip rather than a raw key on screen.
     */
    private static void applyTooltips(ConfigCategory category) {
        for (Object o : category.getEntries()) {
            if (o instanceof TooltipListEntry<?> tip
                    && tip.getFieldName().getContents() instanceof TranslatableContents tc) {
                String key = tc.getKey() + ".tooltip";
                if (Language.getInstance().has(key)) {
                    tip.setTooltipSupplier(() ->
                            Optional.of(new Component[]{Component.translatable(key)}));
                }
            }
        }
    }
}
