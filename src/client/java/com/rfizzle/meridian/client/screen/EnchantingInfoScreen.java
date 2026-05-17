package com.rfizzle.meridian.client.screen;

import com.rfizzle.meridian.enchanting.MeridianEnchantmentMenu;
import com.rfizzle.meridian.enchanting.RealEnchantmentHelper;
import com.rfizzle.meridian.enchanting.StatCollection;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.Mth;
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.util.random.WeightedRandom;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Enchantment info browser screen, opened from the enchanting table GUI's info button.
 * Design follows Zenith's EnchantingInfoScreen: 240x170 panel with slot tabs,
 * a scrollable enchantment list, a power slider, and an arcana weight table.
 */
public class EnchantingInfoScreen extends Screen {

    private static final int BG = 0xFF0E0E1A;
    private static final int PANEL_BG = 0xFF1C1C30;
    private static final int PANEL_BORDER = 0xFF3A3A5C;
    private static final int SLOT_NORMAL = 0xFF2A2A44;
    private static final int SLOT_HOVER = 0xFF3A3A5A;
    private static final int SLOT_ACTIVE = 0xFF4A4A7A;
    private static final int SLOT_EMPTY = 0xFF1A1A2A;
    private static final int ROW_DEFAULT = 0xFF14142A;
    private static final int ROW_HOVER = 0xFF1E2E3E;
    private static final int ROW_BLACKLISTED = 0xFF1A0A0A;

    private final MeridianEnchantmentScreen parent;
    private final MeridianEnchantmentMenu menu;
    private final ItemStack toEnchant;
    private final int[] costs;
    private final int[][] powers = new int[3][];

    private final int imageWidth = 240;
    private final int imageHeight = 170;

    private int selectedSlot = -1;
    private int leftPos, topPos;
    private PowerSlider slider;
    private int currentPower;
    private float scrollOffs;
    private boolean scrolling;
    private int startIndex;
    private List<EnchantmentDataWrapper> enchantments = Collections.emptyList();
    private Map<Holder<Enchantment>, List<Holder<Enchantment>>> exclusions = new HashMap<>();

    public EnchantingInfoScreen(MeridianEnchantmentScreen parent) {
        super(Component.translatable("gui.meridian.enchant_info.title"));
        this.parent = parent;
        this.menu = (MeridianEnchantmentMenu) parent.getMenu();
        this.toEnchant = menu.getSlot(0).getItem();
        this.costs = menu.costs;

        StatCollection stats = menu.getLastStats();
        float quantaPercent = stats.quanta() / 100F;
        float rectPercent = stats.rectification() / 100F;
        int powerCap = RealEnchantmentHelper.DEFAULT_MAX_ETERNA * 4;

        for (int i = 0; i < 3; i++) {
            if (costs[i] > 0) {
                int level = costs[i];
                int minPow = Math.round(Mth.clamp(level * (1F - quantaPercent * (1F - rectPercent)), 1, powerCap));
                int maxPow = Math.round(Mth.clamp(level * (1F + quantaPercent), 1, powerCap));
                this.powers[i] = new int[]{minPow, maxPow};
                this.selectedSlot = i;
            }
        }
    }

    @Override
    protected void init() {
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
        this.slider = this.addRenderableWidget(new PowerSlider(this.leftPos + 5, this.topPos + 80, 80, 20));
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        gfx.fill(0, 0, this.width, this.height, 0xC0101010);

        int lp = this.leftPos;
        int tp = this.topPos;

        gfx.fill(lp, tp, lp + imageWidth, tp + imageHeight, PANEL_BG);
        gfx.renderOutline(lp, tp, imageWidth, imageHeight, PANEL_BORDER);

        for (int i = 0; i < 3; i++) {
            int sy = tp + 18 + 19 * i;
            int bg;
            if (powers[i] == null) {
                bg = SLOT_EMPTY;
            } else if (selectedSlot == i) {
                bg = SLOT_ACTIVE;
            } else if (isHovering(8, 18 + 18 * i, 18, 16, mouseX, mouseY)) {
                bg = SLOT_HOVER;
            } else {
                bg = SLOT_NORMAL;
            }
            gfx.fill(lp + 8, sy, lp + 26, sy + 19, bg);
            if (selectedSlot == i) {
                gfx.renderOutline(lp + 8, sy, 18, 19, 0xFF7A7ABB);
            }

            String slotLabel = String.valueOf(i + 1);
            int color = powers[i] == null ? 0x555555 : (selectedSlot == i ? 0xFFFFFF : 0xBBBBBB);
            gfx.drawString(this.font, slotLabel,
                    lp + 8 + (18 - this.font.width(slotLabel)) / 2,
                    sy + 6, color, false);
        }

        int scrollbarPos = (int) (128F * this.scrollOffs);
        int scrollColor = isScrollBarActive() ? 0xFFAAAAAA : 0xFF555555;
        gfx.fill(lp + 220, tp + 18, lp + 232, tp + 161, 0xFF0E0E1A);
        gfx.fill(lp + 222, tp + 18 + scrollbarPos, lp + 230, tp + 33 + scrollbarPos, scrollColor);

        EnchantmentDataWrapper hover = getHovered(mouseX, mouseY);
        for (int i = 0; i < 11; i++) {
            if (enchantments.size() - 1 < i) break;
            int rowY = tp + 18 + 13 * i;
            EnchantmentDataWrapper data = enchantments.get(startIndex + i);
            int rowBg;
            if (data.isBlacklisted) rowBg = ROW_BLACKLISTED;
            else if (hover == enchantments.get(startIndex + i)) rowBg = ROW_HOVER;
            else rowBg = ROW_DEFAULT;
            gfx.fill(lp + 89, rowY, lp + 217, rowY + 13, rowBg);
        }

        for (int i = 0; i < 11; i++) {
            if (enchantments.size() - 1 < i) break;
            EnchantmentDataWrapper data = enchantments.get(startIndex + i);
            int textY = tp + 21 + 13 * i;
            if (data.isBlacklisted) {
                Component name = data.getEnch().value().description().plainCopy()
                        .withStyle(s -> s.withColor(0x58B0CC).withStrikethrough(true));
                gfx.drawString(this.font, name, lp + 91, textY, 0xFFFF80, false);
            } else {
                String name = data.getEnch().value().description().getString();
                gfx.drawString(this.font, name, lp + 91, textY, 0xFFFF80, false);
            }
        }

        List<Component> list = new ArrayList<>();
        RealEnchantmentHelper.Arcana arc = RealEnchantmentHelper.Arcana.getForThreshold(
                menu.getLastStats().arcana());
        int[] rarities = arc.getRarities();
        list.add(Component.translatable("gui.meridian.enchant_info.weights")
                .withStyle(ChatFormatting.UNDERLINE, ChatFormatting.YELLOW));
        list.add(Component.translatable("gui.meridian.enchant_info.weight_entry",
                I18n.get("gui.meridian.enchant_info.rarity.common"), rarities[0])
                .withStyle(ChatFormatting.GRAY));
        list.add(Component.translatable("gui.meridian.enchant_info.weight_entry",
                I18n.get("gui.meridian.enchant_info.rarity.uncommon"), rarities[1])
                .withStyle(ChatFormatting.GREEN));
        list.add(Component.translatable("gui.meridian.enchant_info.weight_entry",
                I18n.get("gui.meridian.enchant_info.rarity.rare"), rarities[2])
                .withStyle(ChatFormatting.BLUE));
        list.add(Component.translatable("gui.meridian.enchant_info.weight_entry",
                I18n.get("gui.meridian.enchant_info.rarity.very_rare"), rarities[3])
                .withStyle(ChatFormatting.GOLD));
        gfx.renderComponentTooltip(this.font, list, lp + 1, tp + 120);

        gfx.drawString(this.font, this.title, lp + 7, tp + 4, 4210752, false);

        for (int i = 0; i < 3; i++) {
            if (powers[i] != null && isHovering(8, 18 + 18 * i, 18, 16, mouseX, mouseY)) {
                list.clear();
                list.add(Component.translatable("gui.meridian.enchant_info.slot", i + 1)
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.UNDERLINE));
                list.add(Component.translatable("gui.meridian.enchant_info.level", costs[i])
                        .withStyle(ChatFormatting.GREEN));
                list.add(Component.translatable("gui.meridian.enchant_info.min_power", powers[i][0])
                        .withStyle(ChatFormatting.RED));
                list.add(Component.translatable("gui.meridian.enchant_info.max_power", powers[i][1])
                        .withStyle(ChatFormatting.BLUE));
                gfx.renderComponentTooltip(this.font, list, mouseX, mouseY);
            }
        }

        if (hover != null) {
            list.clear();
            ChatFormatting nameColor = hover.getEnch().is(EnchantmentTags.TREASURE)
                    ? ChatFormatting.GOLD : ChatFormatting.GREEN;
            list.add(hover.getEnch().value().description().plainCopy()
                    .withStyle(nameColor, ChatFormatting.UNDERLINE));
            list.add(Component.translatable("gui.meridian.enchant_info.ench_level",
                    Component.translatable("enchantment.level." + hover.getLevel()))
                    .withStyle(ChatFormatting.DARK_AQUA));

            int weight = hover.getEnch().value().definition().weight();
            int realWeight = hover.getWeight().asInt();
            String rarityName = getRarityName(weight);
            list.add(Component.translatable("gui.meridian.enchant_info.ench_weight",
                    realWeight, rarityName).withStyle(ChatFormatting.DARK_AQUA));

            int totalWeight = WeightedRandom.getTotalWeight(enchantments);
            String chance = totalWeight > 0
                    ? String.format("%.2f%%", 100F * realWeight / totalWeight) : "0%";
            list.add(Component.translatable("gui.meridian.enchant_info.ench_chance", chance)
                    .withStyle(ChatFormatting.DARK_AQUA));

            hover.getEnch().unwrapKey().ifPresent(key -> {
                String descKey = key.location().toLanguageKey("enchantment") + ".desc";
                if (I18n.exists(descKey)) {
                    list.add(Component.translatable(descKey).withStyle(ChatFormatting.DARK_AQUA));
                }
            });

            List<Holder<Enchantment>> excls = exclusions.getOrDefault(hover.getEnch(), List.of());
            if (!excls.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < excls.size(); i++) {
                    sb.append(excls.get(i).value().description().getString());
                    if (i != excls.size() - 1) sb.append(", ");
                }
                list.add(Component.translatable("gui.meridian.enchant_info.exclusive", sb.toString())
                        .withStyle(ChatFormatting.RED));
            }
            gfx.renderComponentTooltip(this.font, list, mouseX, mouseY);
        }

        gfx.renderFakeItem(toEnchant, lp + 49, tp + 39);
        if (isHovering(49, 39, 18, 18, mouseX, mouseY)) {
            AbstractContainerScreen.renderSlotHighlight(gfx, lp + 49, tp + 39, 0);
            gfx.renderTooltip(font, toEnchant, mouseX, mouseY);
        }

        if (this.slider != null) {
            this.slider.render(gfx, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        this.scrolling = false;

        int left = this.leftPos + 220;
        int top = this.topPos + 18;
        if (mouseX >= left && mouseX < left + 12 && mouseY >= top && mouseY < top + 143) {
            this.scrolling = true;
            this.mouseDragged(mouseX, mouseY, 0, mouseX, mouseY);
        }

        for (int i = 0; i < 3; i++) {
            if (powers[i] != null && selectedSlot != i && isHovering(8, 18 + 18 * i, 18, 16, mouseX, mouseY)) {
                this.selectedSlot = i;
                this.slider.setValue((this.slider.min() + this.slider.max()) / 2);
                Minecraft.getInstance().getSoundManager().play(
                        SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.scrolling && this.isScrollBarActive()) {
            int i = this.topPos + 18;
            int j = i + 143;
            this.scrollOffs = ((float) mouseY - i - 7.5F) / (j - i - 15.0F);
            this.scrollOffs = Mth.clamp(this.scrollOffs, 0.0F, 1.0F);
            this.startIndex = (int) (this.scrollOffs * this.getOffscreenRows() + 0.5D);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.isScrollBarActive()) {
            int i = this.getOffscreenRows();
            this.scrollOffs = (float) (this.scrollOffs - scrollY / i);
            this.scrollOffs = Mth.clamp(this.scrollOffs, 0.0F, 1.0F);
            this.startIndex = (int) (this.scrollOffs * i + 0.5D);
        }
        return true;
    }

    private boolean isScrollBarActive() {
        return this.enchantments.size() > 11;
    }

    private int getOffscreenRows() {
        return this.enchantments.size() - 11;
    }

    protected boolean isHovering(int x, int y, int w, int h, double mouseX, double mouseY) {
        double mx = mouseX - this.leftPos;
        double my = mouseY - this.topPos;
        return mx >= x - 1 && mx < x + w + 1 && my >= y - 1 && my < y + h + 1;
    }

    private void recomputeEnchantments() {
        if (selectedSlot < 0 || powers[selectedSlot] == null) {
            enchantments = Collections.emptyList();
            return;
        }

        StatCollection stats = menu.getLastStats();
        RealEnchantmentHelper.Arcana arc = RealEnchantmentHelper.Arcana.getForThreshold(stats.arcana());
        Set<ResourceKey<Enchantment>> blacklist = stats.blacklist();
        boolean treasure = stats.treasureAllowed();

        Registry<Enchantment> registry = this.minecraft.level.registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT);

        List<EnchantmentInstance> available = RealEnchantmentHelper.getAvailableEnchantmentResults(
                currentPower, toEnchant, registry, treasure, Set.of());

        this.enchantments = available.stream()
                .map(e -> new RealEnchantmentHelper.ArcanaEnchantmentData(arc, e))
                .map(a -> new EnchantmentDataWrapper(a, blacklist.contains(a.getInstance().enchantment.unwrapKey().orElse(null))))
                .collect(Collectors.toList());

        if (this.startIndex + 11 >= this.enchantments.size()) {
            this.startIndex = 0;
            this.scrollOffs = 0;
        }

        this.exclusions.clear();
        for (EnchantmentDataWrapper d : this.enchantments) {
            if (d.isBlacklisted) continue;
            List<Holder<Enchantment>> excls = new ArrayList<>();
            for (EnchantmentDataWrapper d2 : this.enchantments) {
                if (d != d2 && !Enchantment.areCompatible(d.getEnch(), d2.getEnch())) {
                    excls.add(d2.getEnch());
                }
            }
            this.exclusions.put(d.getEnch(), excls);
        }
    }

    private EnchantmentDataWrapper getHovered(double mouseX, double mouseY) {
        for (int i = 0; i < 11; i++) {
            if (enchantments.size() - 1 < i) break;
            if (isHovering(89, 18 + i * 13, 128, 13, mouseX, mouseY)) {
                EnchantmentDataWrapper data = enchantments.get(startIndex + i);
                return data.isBlacklisted ? null : data;
            }
        }
        return null;
    }

    private static String getRarityName(int weight) {
        if (weight >= 10) return I18n.get("gui.meridian.enchant_info.rarity.common");
        if (weight >= 5) return I18n.get("gui.meridian.enchant_info.rarity.uncommon");
        if (weight >= 2) return I18n.get("gui.meridian.enchant_info.rarity.rare");
        return I18n.get("gui.meridian.enchant_info.rarity.very_rare");
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }

    public class PowerSlider extends AbstractSliderButton {

        public PowerSlider(int x, int y, int width, int height) {
            super(x, y, width, height, Component.empty(), 0);
            if (EnchantingInfoScreen.this.selectedSlot != -1 && this.value == 0) {
                this.value = normalizeValue(
                        EnchantingInfoScreen.this.currentPower == 0
                                ? (max() + min()) / 2
                                : EnchantingInfoScreen.this.currentPower);
                applyValue();
            }
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable("gui.meridian.enchant_info.slider_power",
                    EnchantingInfoScreen.this.currentPower));
        }

        @Override
        protected void applyValue() {
            EnchantingInfoScreen.this.currentPower = denormalizeValue(this.value);
            EnchantingInfoScreen.this.recomputeEnchantments();
        }

        public void setValue(int value) {
            if (!EnchantingInfoScreen.this.isDragging()) {
                this.value = normalizeValue(value);
                applyValue();
                updateMessage();
            }
        }

        public double normalizeValue(double value) {
            return Mth.clamp((snapToStepClamp(value) - min()) / (max() - min()), 0.0D, 1.0D);
        }

        public int denormalizeValue(double value) {
            return (int) snapToStepClamp(Mth.lerp(Mth.clamp(value, 0.0D, 1.0D), min(), max()));
        }

        private double snapToStepClamp(double valueIn) {
            if (step() > 0.0F) {
                valueIn = step() * Math.round(valueIn / step());
            }
            return Mth.clamp(valueIn, min(), max());
        }

        int min() {
            int slot = EnchantingInfoScreen.this.selectedSlot;
            return slot >= 0 && EnchantingInfoScreen.this.powers[slot] != null
                    ? EnchantingInfoScreen.this.powers[slot][0] : 1;
        }

        int max() {
            int slot = EnchantingInfoScreen.this.selectedSlot;
            return slot >= 0 && EnchantingInfoScreen.this.powers[slot] != null
                    ? EnchantingInfoScreen.this.powers[slot][1] : 1;
        }

        private float step() {
            return 1F / Math.max(max() - min(), 1);
        }
    }

    static class EnchantmentDataWrapper extends WeightedEntry.IntrusiveBase {
        final RealEnchantmentHelper.ArcanaEnchantmentData data;
        final boolean isBlacklisted;

        EnchantmentDataWrapper(RealEnchantmentHelper.ArcanaEnchantmentData data, boolean isBlacklisted) {
            super(isBlacklisted ? 0 : data.getWeight().asInt());
            this.data = data;
            this.isBlacklisted = isBlacklisted;
        }

        Holder<Enchantment> getEnch() {
            return data.getInstance().enchantment;
        }

        int getLevel() {
            return data.getInstance().level;
        }
    }
}
