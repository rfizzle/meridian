package com.rfizzle.meridian.client.screen;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.enchanting.EnchantmentInfoRegistry;
import com.rfizzle.meridian.library.EnchantmentLibraryBlockEntity;
import com.rfizzle.meridian.library.EnchantmentLibraryMenu;

import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import org.jetbrains.annotations.Nullable;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Client screen for the Basic and Ender enchantment libraries. Renders a textured UI matching the
 * Zenith/Apotheosis "Library of Alexandria" aesthetic — dark wooden panel with scrollable enchant
 * entries, per-entry progress bars, a search filter, and detailed extraction-cost tooltips on hover.
 */
public class EnchantmentLibraryScreen extends AbstractContainerScreen<EnchantmentLibraryMenu> {

    private static final ResourceLocation TEXTURE = Meridian.id("textures/gui/library.png");

    private static final int MAX_VISIBLE = 5;
    private static final int ENTRY_WIDTH = 113;
    private static final int ENTRY_HEIGHT = 20;

    private static final int TEX_W = 307;
    private static final int TEX_H = 256;

    private float scrollOffs;
    private boolean scrolling;
    private int startIndex;

    private final List<LibrarySlot> data = new ArrayList<>();
    private EditBox filter;

    public EnchantmentLibraryScreen(EnchantmentLibraryMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageHeight = 230;
        menu.setNotifier(this::containerChanged);
    }

    @Override
    protected void init() {
        super.init();
        this.filter = this.addRenderableWidget(new EditBox(
                this.font, this.leftPos + 16, this.topPos + 16, 110, 11, this.filter, Component.literal("")));
        this.filter.setBordered(false);
        this.filter.setTextColor(0x97714F);
        this.filter.setResponder(t -> this.containerChanged());
        this.setFocused(this.filter);
        this.containerChanged();
    }

    @Override
    protected void renderLabels(GuiGraphics gfx, int mouseX, int mouseY) {
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.minecraft != null && this.minecraft.options.keyInventory.matches(keyCode, scanCode)
                && this.getFocused() == this.filter) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTicks) {
        renderBackground(gfx, mouseX, mouseY, partialTicks);
        super.render(gfx, mouseX, mouseY, partialTicks);
        renderTooltip(gfx, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(GuiGraphics gfx, int mouseX, int mouseY) {
        super.renderTooltip(gfx, mouseX, mouseY);
        LibrarySlot slot = getHoveredSlot(mouseX, mouseY);
        if (slot == null) return;

        List<Component> lines = new ArrayList<>();

        MutableComponent displayName = slot.name().copy()
                .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFFFF80)).withUnderlined(true));
        lines.add(displayName);

        String descKey = "enchantment." + slot.key().location().getNamespace()
                + "." + slot.key().location().getPath().replace('/', '.') + ".desc";
        if (I18n.exists(descKey)) {
            lines.add(Component.translatable(descKey)
                    .setStyle(Style.EMPTY.withColor(ChatFormatting.GRAY).withItalic(true)));
            lines.add(Component.literal(""));
        }

        if (slot.disabled()) {
            lines.add(Component.translatable("tooltip.meridian.enchlib.disabled")
                    .withStyle(ChatFormatting.RED));
        } else {
            lines.add(Component.translatable("tooltip.meridian.enchlib.max_lvl",
                    Component.translatable("enchantment.level." + slot.maxLvl()))
                    .withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable("tooltip.meridian.enchlib.points",
                    formatNumber(slot.points()), formatNumber(getPointCap()))
                    .withStyle(ChatFormatting.GRAY));
            lines.add(Component.literal(""));

            ItemStack outSlot = this.menu.ioInv.getItem(EnchantmentLibraryMenu.EXTRACT_SLOT);
            int current = getCurrentLevel(outSlot, slot.key());
            boolean shift = Screen.hasShiftDown();
            int targetLevel;
            if (shift) {
                targetLevel = Math.min(slot.maxLvl(),
                        1 + (int) (Math.log(slot.points() + EnchantmentLibraryBlockEntity.points(current))
                                / Math.log(2)));
            } else {
                targetLevel = current + 1;
            }
            if (targetLevel == current) targetLevel++;

            int cost = EnchantmentLibraryBlockEntity.points(targetLevel)
                    - EnchantmentLibraryBlockEntity.points(current);

            if (targetLevel > slot.maxLvl()) {
                lines.add(Component.translatable("tooltip.meridian.enchlib.unavailable")
                        .withStyle(ChatFormatting.RED));
            } else {
                lines.add(Component.translatable("tooltip.meridian.enchlib.extracting",
                        Component.translatable("enchantment.level." + targetLevel))
                        .withStyle(ChatFormatting.BLUE));
                lines.add(Component.translatable("tooltip.meridian.enchlib.cost", cost)
                        .withStyle(cost > slot.points() ? ChatFormatting.RED : ChatFormatting.GOLD));
            }
        }

        int tooltipX = this.leftPos - 16 - lines.stream()
                .mapToInt(this.font::width).max().orElse(100);
        gfx.renderComponentTooltip(this.font, lines, tooltipX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics gfx, float partialTicks, int mouseX, int mouseY) {
        int left = this.leftPos;
        int top = this.topPos;
        gfx.blit(TEXTURE, left, top, 0, 0, this.imageWidth, this.imageHeight, TEX_W, TEX_H);

        int scrollbarPos = (int) (90F * this.scrollOffs);
        int scrollbarV = isScrollBarActive() ? 40 : 52;
        gfx.blit(TEXTURE, left + 13, top + 29 + scrollbarPos, 303, scrollbarV, 4, 12, TEX_W, TEX_H);

        int idx = this.startIndex;
        while (idx < this.startIndex + MAX_VISIBLE && idx < this.data.size()) {
            renderEntry(gfx, this.data.get(idx), left + 20,
                    top + 30 + ENTRY_HEIGHT * (idx - this.startIndex), mouseX, mouseY);
            idx++;
        }
    }

    private void renderEntry(GuiGraphics gfx, LibrarySlot slot, int x, int y, int mouseX, int mouseY) {
        LibrarySlot hover = getHoveredSlot(mouseX, mouseY);
        int entryV = (slot == hover) ? ENTRY_HEIGHT : 0;
        gfx.blit(TEXTURE, x, y, 194, entryV, ENTRY_WIDTH, ENTRY_HEIGHT, TEX_W, TEX_H);

        int pointCap = getPointCap();
        int progress = (pointCap > 0)
                ? (int) Math.round(85 * Math.sqrt(slot.points()) / Math.sqrt(pointCap))
                : 0;
        gfx.blit(TEXTURE, x + 3, y + 14, 197, 42, progress, 3, TEX_W, TEX_H);

        PoseStack pose = gfx.pose();
        pose.pushPose();
        Component txt = slot.name();
        float scale = 1;
        if (this.font.width(txt) > ENTRY_WIDTH - 6) {
            scale = 60F / this.font.width(txt);
        }
        pose.scale(scale, scale, 1);
        int textColor = slot.disabled() ? 0x666666 : 0x8EE14D;
        gfx.drawString(this.font, txt, (int) ((x + 3) / scale), (int) ((y + 3) / scale), textColor, false);
        pose.popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        this.scrolling = false;

        if (this.isHovering(14, 29, 4, 103, mouseX, mouseY)) {
            this.scrolling = true;
            this.mouseDragged(mouseX, mouseY, button, 0, 0);
            return true;
        }

        LibrarySlot slot = getHoveredSlot((int) mouseX, (int) mouseY);
        if (slot != null) {
            if (slot.disabled()) return true;
            int id = slot.registryIndex();
            if (Screen.hasShiftDown()) id |= EnchantmentLibraryMenu.SHIFT_BIT;
            if (this.minecraft != null && this.minecraft.gameMode != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
            }
            if (this.minecraft != null) {
                this.minecraft.getSoundManager().play(
                        SimpleSoundInstance.forUI(SoundEvents.UI_STONECUTTER_SELECT_RECIPE, 1.0F));
            }
            return true;
        }

        if (this.filter != null && this.filter.isHovered() && button == 1) {
            this.filter.setValue("");
            return true;
        }

        this.setFocused(null);
        if (this.filter != null) this.filter.setFocused(false);

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (this.scrolling && isScrollBarActive()) {
            int barTop = this.topPos + 14;
            int barBot = barTop + 103;
            this.scrollOffs = ((float) mouseY - barTop - 6F) / (barBot - barTop - 12F) - 0.12F;
            this.scrollOffs = Mth.clamp(this.scrollOffs, 0.0F, 1.0F);
            this.startIndex = (int) (this.scrollOffs * getOffscreenRows() + 0.5D);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double dx, double dy) {
        if (isScrollBarActive()) {
            int offscreen = getOffscreenRows();
            this.scrollOffs = (float) (this.scrollOffs - dy / offscreen);
            this.scrollOffs = Mth.clamp(this.scrollOffs, 0.0F, 1.0F);
            this.startIndex = (int) (this.scrollOffs * offscreen + 0.5D);
        }
        return true;
    }

    private boolean isScrollBarActive() {
        return this.data.size() > MAX_VISIBLE;
    }

    private int getOffscreenRows() {
        return Math.max(1, this.data.size() - MAX_VISIBLE);
    }

    @Nullable
    private LibrarySlot getHoveredSlot(int mouseX, int mouseY) {
        for (int i = 0; i < MAX_VISIBLE; i++) {
            if (this.startIndex + i < this.data.size()) {
                if (this.isHovering(21, 31 + i * ENTRY_HEIGHT, ENTRY_WIDTH, ENTRY_HEIGHT - 2, mouseX, mouseY)) {
                    return this.data.get(this.startIndex + i);
                }
            }
        }
        return null;
    }

    private void containerChanged() {
        this.data.clear();
        EnchantmentLibraryBlockEntity tile = this.menu.getTile();
        if (tile == null || this.minecraft == null || this.minecraft.player == null) return;

        Registry<Enchantment> registry = this.minecraft.player.level().registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT);
        for (Object2IntMap.Entry<ResourceKey<Enchantment>> entry
                : tile.getPoints().object2IntEntrySet()) {
            int points = entry.getIntValue();
            if (points <= 0) continue;
            ResourceKey<Enchantment> key = entry.getKey();
            Enchantment ench = registry.get(key);
            if (ench == null) continue;
            int maxLvl = tile.getMaxLevels().getInt(key);
            int idx = registry.getId(ench);
            Component name = ench.description();
            if (!isAllowedBySearch(name.getString())) continue;
            if (!isAllowedByItem(registry, key)) continue;
            Holder<Enchantment> holder = registry.wrapAsHolder(ench);
            boolean disabled = !EnchantmentInfoRegistry.getInfo(holder).enabled();
            this.data.add(new LibrarySlot(key, name, maxLvl, points, idx, disabled));
        }
        this.data.sort(Comparator.comparing(s -> s.name().getString()));

        if (!isScrollBarActive()) {
            this.scrollOffs = 0F;
            this.startIndex = 0;
        } else if (this.startIndex > getOffscreenRows()) {
            this.startIndex = getOffscreenRows();
            this.scrollOffs = 1F;
        }
    }

    private boolean isAllowedBySearch(String enchantName) {
        String search = (this.filter == null) ? "" : this.filter.getValue().trim().toLowerCase(Locale.ROOT);
        if (search.isEmpty()) return true;
        String stripped = ChatFormatting.stripFormatting(enchantName);
        return stripped != null && stripped.toLowerCase(Locale.ROOT).contains(search);
    }

    private boolean isAllowedByItem(Registry<Enchantment> registry, ResourceKey<Enchantment> key) {
        ItemStack stack = this.menu.ioInv.getItem(EnchantmentLibraryMenu.SCRATCH_SLOT);
        if (stack.isEmpty()) return true;
        Enchantment ench = registry.get(key);
        if (ench == null) return false;
        Holder<Enchantment> holder = registry.wrapAsHolder(ench);
        if (!ench.canEnchant(stack)) return false;
        ItemEnchantments existing = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        for (var entry : existing.entrySet()) {
            if (entry.getKey().is(key)) continue;
            if (!Enchantment.areCompatible(entry.getKey(), holder)) return false;
        }
        return true;
    }

    private int getCurrentLevel(ItemStack stack, ResourceKey<Enchantment> key) {
        if (stack.isEmpty() || this.minecraft == null || this.minecraft.player == null) return 0;
        Registry<Enchantment> registry = this.minecraft.player.level().registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT);
        Enchantment ench = registry.get(key);
        if (ench == null) return 0;
        Holder<Enchantment> holder = registry.wrapAsHolder(ench);
        ItemEnchantments stored = stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
        return stored.getLevel(holder);
    }

    private int getPointCap() {
        EnchantmentLibraryBlockEntity tile = this.menu.getTile();
        return (tile != null) ? tile.getMaxPoints() : 32_768;
    }

    private static final DecimalFormat DECIMAL_FMT = new DecimalFormat("##.#");

    static String formatNumber(int n) {
        if (n <= 0) return "0";
        int log = (int) StrictMath.log10(n);
        if (log <= 4) return String.valueOf(n);
        if (log == 5) return DECIMAL_FMT.format(n / 1000D) + "K";
        if (log <= 8) return DECIMAL_FMT.format(n / 1000000D) + "M";
        return DECIMAL_FMT.format(n / 1000000000D) + "B";
    }

    private record LibrarySlot(ResourceKey<Enchantment> key, Component name,
                               int maxLvl, int points, int registryIndex, boolean disabled) {}
}
