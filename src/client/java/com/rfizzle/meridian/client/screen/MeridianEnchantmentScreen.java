package com.rfizzle.meridian.client.screen;

import com.rfizzle.meridian.enchanting.MeridianEnchantmentLogic;
import com.rfizzle.meridian.enchanting.MeridianEnchantmentMenu;
import com.rfizzle.meridian.enchanting.RealEnchantmentHelper;
import com.rfizzle.meridian.enchanting.StatCollection;
import com.rfizzle.meridian.enchanting.recipe.EnchantingRecipeRegistry;
import com.rfizzle.meridian.net.CraftingResultEntry;
import com.rfizzle.meridian.net.EnchantmentClue;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EnchantmentNames;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.List;
import java.util.Optional;

public class MeridianEnchantmentScreen extends EnchantmentScreen {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("meridian", "textures/gui/enchanting_table.png");

    private static final float FALLBACK_MAX_ETERNA = 50.0F;

    private final MeridianEnchantmentMenu fizzleMenu;

    private float eterna, lastEterna;
    private float quanta, lastQuanta;
    private float arcana, lastArcana;
    private int[] savedEnchantClue;

    public MeridianEnchantmentScreen(EnchantmentMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.fizzleMenu = menu instanceof MeridianEnchantmentMenu fm ? fm : null;
        this.imageHeight = 197;
    }

    @Override
    public void containerTick() {
        super.containerTick();
        if (fizzleMenu == null) return;
        StatCollection stats = fizzleMenu.getLastStats();

        float target = stats.eterna();
        if (target != this.eterna) {
            if (target > this.eterna)
                this.eterna += Math.min(target - this.eterna, Math.max(0.16F, (target - this.eterna) * 0.1F));
            else
                this.eterna = Math.max(this.eterna - this.lastEterna * 0.075F, target);
        }
        if (target > 0) this.lastEterna = target;

        target = stats.quanta();
        if (target != this.quanta) {
            if (target > this.quanta)
                this.quanta += Math.min(target - this.quanta, Math.max(0.04F, (target - this.quanta) * 0.1F));
            else
                this.quanta = Math.max(this.quanta - this.lastQuanta * 0.075F, target);
        }
        if (target > 0) this.lastQuanta = target;

        target = stats.arcana();
        if (target != this.arcana) {
            if (target > this.arcana)
                this.arcana += Math.min(target - this.arcana, Math.max(0.04F, (target - this.arcana) * 0.1F));
            else
                this.arcana = Math.max(this.arcana - this.lastArcana * 0.075F, target);
        }
        if (target > 0) this.lastArcana = target;
    }

    private void renderBgImpl(GuiGraphics gfx, float partialTicks, int mouseX, int mouseY) {
        int xCenter = this.leftPos;
        int yCenter = this.topPos;

        gfx.blit(TEXTURE, xCenter, yCenter, 0, 0, this.imageWidth, this.imageHeight);

        this.renderBook(gfx, xCenter, yCenter, partialTicks);

        EnchantmentNames.getInstance().initSeed(this.menu.getEnchantmentSeed());
        int lapis = this.menu.getGoldCount();

        for (int slot = 0; slot < 3; ++slot) {
            int j1 = xCenter + 60;
            int k1 = j1 + 20;
            int level = this.menu.costs[slot];
            if (level == 0) {
                gfx.blit(TEXTURE, j1, yCenter + 14 + 19 * slot, 148, 218, 108, 19);
            } else {
                String s = "" + level;
                int width = 86 - this.font.width(s);
                FormattedText randomName = EnchantmentNames.getInstance().getRandomName(this.font, width);
                int color = 6839882;
                if ((lapis < slot + 1 || this.minecraft.player.experienceLevel < level)
                        && !this.minecraft.player.getAbilities().instabuild
                        || this.menu.enchantClue[slot] == -1) {
                    gfx.blit(TEXTURE, j1, yCenter + 14 + 19 * slot, 148, 218, 108, 19);
                    gfx.blit(TEXTURE, j1 + 1, yCenter + 15 + 19 * slot, 16 * slot, 239, 16, 16);
                    gfx.drawWordWrap(this.font, randomName, k1, yCenter + 16 + 19 * slot, width, (color & 16711422) >> 1);
                    color = 4226832;
                } else {
                    int k2 = mouseX - (xCenter + 60);
                    int l2 = mouseY - (yCenter + 14 + 19 * slot);
                    if (k2 >= 0 && l2 >= 0 && k2 < 108 && l2 < 19) {
                        gfx.blit(TEXTURE, j1, yCenter + 14 + 19 * slot, 148, 237, 108, 19);
                        color = 16777088;
                    } else {
                        gfx.blit(TEXTURE, j1, yCenter + 14 + 19 * slot, 148, 199, 108, 19);
                    }
                    gfx.blit(TEXTURE, j1 + 1, yCenter + 15 + 19 * slot, 16 * slot, 223, 16, 16);
                    gfx.drawWordWrap(this.font, randomName, k1, yCenter + 16 + 19 * slot, width, color);
                    color = 8453920;
                }
                gfx.drawString(this.font, s, k1 + 86 - this.font.width(s), yCenter + 16 + 19 * slot + 7, color);
            }
        }

        if (this.eterna > 0) {
            float barMax = fizzleMenu != null && fizzleMenu.getLastStats().maxEterna() > 0
                    ? fizzleMenu.getLastStats().maxEterna() : FALLBACK_MAX_ETERNA;
            gfx.blit(TEXTURE, xCenter + 59, yCenter + 75, 0, 197,
                    (int) (Math.min(this.eterna / barMax, 1.0F) * 110), 5);
        }
        if (this.quanta > 0) {
            gfx.blit(TEXTURE, xCenter + 59, yCenter + 85, 0, 202,
                    (int) (this.quanta / 100F * 110), 5);
        }
        if (this.arcana > 0) {
            gfx.blit(TEXTURE, xCenter + 59, yCenter + 95, 0, 207,
                    (int) (this.arcana / 100F * 110), 5);
        }

        if (isInfoButtonVisible()) {
            int btnX = xCenter + 148;
            int btnY = yCenter + 1;
            boolean hovered = mouseX >= btnX && mouseX < btnX + 20 && mouseY >= btnY && mouseY < btnY + 12;
            int bg = hovered ? 0xFF4A6A8A : 0xFF2A3A5A;
            int border = hovered ? 0xFF8AB0DD : 0xFF5A7AAA;
            gfx.fill(btnX, btnY, btnX + 20, btnY + 12, bg);
            gfx.renderOutline(btnX, btnY, 20, 12, border);
            int textColor = hovered ? 0xFFFFFF : 0xCCDDEE;
            gfx.drawString(this.font, "i", btnX + 8, btnY + 2, textColor, false);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics gfx, int mouseX, int mouseY) {
        gfx.drawString(this.font, this.title, 12, 5, 4210752, false);
        gfx.drawString(this.font, this.playerInventoryTitle, 7, this.imageHeight - 96 + 4, 4210752, false);

        if (fizzleMenu == null) return;

        gfx.drawString(this.font, I18n.get("gui.meridian.enchant.eterna"), 19, 74, 0x3DB53D, false);
        gfx.drawString(this.font, I18n.get("gui.meridian.enchant.quanta"), 19, 84, 0xFC5454, false);
        gfx.drawString(this.font, I18n.get("gui.meridian.enchant.arcana"), 19, 94, 0xA800A8, false);
    }

    @Override
    protected void renderBg(GuiGraphics gfx, float partialTicks, int mouseX, int mouseY) {
        if (savedEnchantClue != null) {
            System.arraycopy(savedEnchantClue, 0, this.menu.enchantClue, 0, 3);
        }
        renderBgImpl(gfx, partialTicks, mouseX, mouseY);
        if (savedEnchantClue != null) {
            this.menu.enchantClue[0] = -1;
            this.menu.enchantClue[1] = -1;
            this.menu.enchantClue[2] = -1;
        }
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTicks) {
        savedEnchantClue = new int[]{this.menu.enchantClue[0], this.menu.enchantClue[1], this.menu.enchantClue[2]};
        this.menu.enchantClue[0] = -1;
        this.menu.enchantClue[1] = -1;
        this.menu.enchantClue[2] = -1;
        super.render(gfx, mouseX, mouseY, partialTicks);
        System.arraycopy(savedEnchantClue, 0, this.menu.enchantClue, 0, 3);
        savedEnchantClue = null;

        if (fizzleMenu == null) return;

        boolean creative = this.minecraft.player.getAbilities().instabuild;
        int lapis = this.menu.getGoldCount();

        for (int slot = 0; slot < 3; slot++) {
            int level = this.menu.costs[slot];
            if (level <= 0) continue;
            if (!isHovering(60, 14 + 19 * slot, 108, 17, mouseX, mouseY)) continue;

            List<Component> lines = Lists.newArrayList();

            if (slot == MeridianEnchantmentLogic.CRAFTING_SLOT && craftingResult().isPresent()) {
                CraftingResultEntry entry = craftingResult().get();
                lines.add(entry.result().getHoverName().copy()
                        .withStyle(ChatFormatting.YELLOW, ChatFormatting.UNDERLINE));
                if (!creative) {
                    lines.add(Component.literal(""));
                    int cost = slot + 1;
                    if (this.minecraft.player.experienceLevel < level) {
                        lines.add(Component.translatable("container.enchant.level.requirement", level)
                                .withStyle(ChatFormatting.RED));
                    } else {
                        ChatFormatting lapisColor = lapis >= cost ? ChatFormatting.GRAY : ChatFormatting.RED;
                        lines.add(Component.translatable(cost == 1
                                ? "container.enchant.lapis.one" : "container.enchant.lapis.many", cost)
                                .withStyle(lapisColor));
                        lines.add(Component.translatable(cost == 1
                                ? "container.enchant.level.one" : "container.enchant.level.many", cost)
                                .withStyle(ChatFormatting.GRAY));
                    }
                }
            } else {
                List<EnchantmentClue> clues = fizzleMenu.getClientClues(slot);
                boolean exhausted = fizzleMenu.isClientCluesExhausted(slot);

                boolean isInfusionFailed = slot == MeridianEnchantmentLogic.CRAFTING_SLOT
                        && clues.isEmpty()
                        && isInfusionFailedForInput();

                if (isInfusionFailed) {
                    lines.add(Component.translatable("info.meridian.enchant.infusion")
                            .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
                    lines.add(Component.literal(""));
                    lines.add(Component.translatable("info.meridian.enchant.infusion_failed")
                            .withStyle(ChatFormatting.RED));
                } else if (!clues.isEmpty()) {
                    lines.add(Component.translatable("info.meridian.enchant.clues"
                            + (exhausted ? "_all" : ""))
                            .withStyle(ChatFormatting.YELLOW, ChatFormatting.UNDERLINE));
                    Registry<Enchantment> registry = this.minecraft.level.registryAccess()
                            .registryOrThrow(Registries.ENCHANTMENT);
                    for (EnchantmentClue clue : clues) {
                        Optional<Holder.Reference<Enchantment>> holder = registry.getHolder(clue.enchantment());
                        holder.ifPresent(ref -> lines.add(Enchantment.getFullname(ref, clue.level())));
                    }
                    if (slot == MeridianEnchantmentLogic.CRAFTING_SLOT && isInfusionFailedForInput()) {
                        lines.add(Component.literal(""));
                        lines.add(Component.translatable("info.meridian.enchant.infusion_failed")
                                .withStyle(ChatFormatting.RED, ChatFormatting.ITALIC));
                    }
                } else {
                    lines.add(Component.translatable("info.meridian.enchant.no_clue")
                            .withStyle(ChatFormatting.DARK_RED, ChatFormatting.UNDERLINE));
                }

                if (this.menu.enchantClue[slot] != -1 && !creative) {
                    lines.add(Component.literal(""));
                    int cost = slot + 1;
                    if (this.minecraft.player.experienceLevel < level) {
                        lines.add(Component.translatable("container.enchant.level.requirement", level)
                                .withStyle(ChatFormatting.RED));
                    } else {
                        ChatFormatting lapisColor = lapis >= cost ? ChatFormatting.GRAY : ChatFormatting.RED;
                        lines.add(Component.translatable(cost == 1
                                ? "container.enchant.lapis.one" : "container.enchant.lapis.many", cost)
                                .withStyle(lapisColor));
                        lines.add(Component.translatable(cost == 1
                                ? "container.enchant.level.one" : "container.enchant.level.many", cost)
                                .withStyle(ChatFormatting.GRAY));
                    }
                }
            }

            gfx.renderComponentTooltip(this.font, lines, mouseX, mouseY);
            break;
        }

        StatCollection stats = fizzleMenu.getLastStats();
        if (isHovering(60, 76, 110, 5, mouseX, mouseY)) {
            List<Component> list = Lists.newArrayList();
            list.add(eternaLabel().append(Component.translatable("gui.meridian.stat.eterna.desc1")));
            list.add(Component.translatable("gui.meridian.stat.eterna.desc2").withStyle(ChatFormatting.GRAY));
            if (stats.eterna() > 0) {
                float displayMax = stats.maxEterna() > 0 ? stats.maxEterna() : FALLBACK_MAX_ETERNA;
                list.add(Component.literal(""));
                list.add(Component.translatable("gui.meridian.stat.eterna.value",
                        f(stats.eterna()), (int) displayMax).withStyle(ChatFormatting.GRAY));
            }
            gfx.renderComponentTooltip(this.font, list, mouseX, mouseY);
        }
        else if (isHovering(60, 86, 110, 5, mouseX, mouseY)) {
            List<Component> list = Lists.newArrayList();
            list.add(quantaLabel().append(Component.translatable("gui.meridian.stat.quanta.desc1")));
            list.add(Component.translatable("gui.meridian.stat.quanta.desc2").withStyle(ChatFormatting.GRAY));
            list.add(rectLabel().append(Component.translatable("gui.meridian.stat.quanta.desc3").withStyle(ChatFormatting.GRAY)));
            if (stats.quanta() > 0) {
                list.add(CommonComponents.EMPTY);
                list.add(Component.translatable("gui.meridian.stat.quanta.value",
                        f(stats.quanta())).withStyle(ChatFormatting.GRAY));
            }
            gfx.renderComponentTooltip(this.font, list, mouseX, mouseY);
            if (stats.quanta() > 0) {
                list.clear();
                list.add(Component.translatable("info.meridian.quanta_buff")
                        .withStyle(ChatFormatting.UNDERLINE, ChatFormatting.RED));
                list.add(Component.translatable("info.meridian.quanta_growth", f(stats.quanta()))
                        .withStyle(ChatFormatting.BLUE));
                drawOnLeft(gfx, list, this.topPos + 29);
            }
        }
        else if (isHovering(60, 96, 110, 5, mouseX, mouseY)) {
            List<Component> list = Lists.newArrayList();
            PoseStack pose = gfx.pose();
            pose.pushPose();
            pose.translate(0, 0, 4);
            list.add(arcanaLabel().append(Component.translatable("gui.meridian.stat.arcana.desc1")));
            list.add(Component.translatable("gui.meridian.stat.arcana.desc2").withStyle(ChatFormatting.GRAY));
            list.add(Component.translatable("gui.meridian.stat.arcana.desc3").withStyle(ChatFormatting.GRAY));
            if (stats.arcana() > 0) {
                list.add(Component.literal(""));
                ItemStack inputItem = this.menu.slots.get(0).getItem();
                float enchBonus = inputItem.isEmpty() ? 0F : inputItem.getItem().getEnchantmentValue() / 2F;
                float baseArcana = Math.max(0F, stats.arcana() - enchBonus);
                if (enchBonus > 0) {
                    list.add(Component.translatable("gui.meridian.stat.arcana.base",
                            f(baseArcana)).withStyle(ChatFormatting.GRAY));
                    list.add(Component.translatable("gui.meridian.stat.arcana.ench_bonus",
                            f(enchBonus)).withStyle(ChatFormatting.GRAY));
                    list.add(Component.translatable("gui.meridian.stat.arcana.total",
                            f(stats.arcana())).withStyle(ChatFormatting.GOLD));
                } else {
                    list.add(Component.translatable("gui.meridian.stat.arcana.value",
                            f(stats.arcana())).withStyle(ChatFormatting.GOLD));
                }
            }
            gfx.renderComponentTooltip(this.font, list, mouseX, mouseY);
            pose.popPose();
            if (stats.arcana() > 0) {
                list.clear();
                RealEnchantmentHelper.Arcana a = RealEnchantmentHelper.Arcana.getForThreshold(stats.arcana());
                list.add(Component.translatable("info.meridian.arcana_bonus")
                        .withStyle(ChatFormatting.UNDERLINE, ChatFormatting.DARK_PURPLE));
                if (a != RealEnchantmentHelper.Arcana.EMPTY) {
                    list.add(Component.translatable("info.meridian.weights_changed")
                            .withStyle(ChatFormatting.BLUE));
                }
                int minEnchants = guaranteedPicks(stats.arcana());
                if (minEnchants > 1) {
                    list.add(Component.translatable("info.meridian.min_enchants", minEnchants)
                            .withStyle(ChatFormatting.BLUE));
                }
                drawOnLeft(gfx, list, this.topPos + 29);
                int offset = 20 + list.size() * this.font.lineHeight;
                list.clear();
                list.add(Component.translatable("info.meridian.rel_weights")
                        .withStyle(ChatFormatting.UNDERLINE, ChatFormatting.YELLOW));
                list.add(Component.translatable("info.meridian.weight",
                        I18n.get("gui.meridian.enchant_info.rarity.common"), a.getRarities()[0])
                        .withStyle(ChatFormatting.GRAY));
                list.add(Component.translatable("info.meridian.weight",
                        I18n.get("gui.meridian.enchant_info.rarity.uncommon"), a.getRarities()[1])
                        .withStyle(ChatFormatting.GREEN));
                list.add(Component.translatable("info.meridian.weight",
                        I18n.get("gui.meridian.enchant_info.rarity.rare"), a.getRarities()[2])
                        .withStyle(ChatFormatting.BLUE));
                list.add(Component.translatable("info.meridian.weight",
                        I18n.get("gui.meridian.enchant_info.rarity.very_rare"), a.getRarities()[3])
                        .withStyle(ChatFormatting.GOLD));
                drawOnLeft(gfx, list, this.topPos + 29 + offset);
            }
        }
        else if (isInfoButtonVisible() && mouseX >= this.leftPos + 148 && mouseX < this.leftPos + 168
                && mouseY >= this.topPos + 1 && mouseY < this.topPos + 13) {
            gfx.renderComponentTooltip(this.font, Lists.newArrayList(
                    Component.translatable("gui.meridian.enchant_info.info_button")),
                    mouseX, mouseY);
        }

        ItemStack enchanting = this.menu.getSlot(0).getItem();
        if (!enchanting.isEmpty() && this.menu.costs[2] > 0) {
            for (int slot = 0; slot < 3; slot++) {
                if (isHovering(60, 14 + 19 * slot, 108, 18, mouseX, mouseY)) {
                    List<Component> list = Lists.newArrayList();
                    int level = this.menu.costs[slot];
                    list.add(Component.translatable("info.meridian.ench_at", level)
                            .withStyle(ChatFormatting.UNDERLINE, ChatFormatting.GREEN));
                    list.add(Component.literal(""));
                    int levelCost = slot + 1;
                    list.add(Component.translatable("info.meridian.level_cost",
                            Component.literal("" + levelCost).withStyle(ChatFormatting.GREEN)));
                    float quantaFrac = stats.quanta() / 100F;
                    float rectFrac = stats.rectification() / 100F;
                    int minPow = Math.round(Mth.clamp(level - level * quantaFrac * (1F - rectFrac), 1, 200));
                    int maxPow = Math.round(Mth.clamp(level + level * quantaFrac, 1, 200));
                    list.add(Component.translatable("info.meridian.power_range",
                            Component.literal("" + minPow).withStyle(ChatFormatting.DARK_RED),
                            Component.literal("" + maxPow).withStyle(ChatFormatting.BLUE)));
                    list.add(Component.translatable("info.meridian.item_ench",
                            Component.literal("" + enchanting.getItem().getEnchantmentValue()).withStyle(ChatFormatting.GREEN)));
                    list.add(Component.translatable("info.meridian.num_clues",
                            Component.literal("" + (1 + stats.clues())).withStyle(ChatFormatting.DARK_AQUA)));
                    drawOnLeft(gfx, list, this.topPos + 29);
                    break;
                }
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;

        if (isInfoButtonVisible()) {
            int btnX = i + 148;
            int btnY = j + 1;
            if (mouseX >= btnX && mouseX < btnX + 20 && mouseY >= btnY && mouseY < btnY + 12) {
                this.minecraft.getSoundManager().play(
                        SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                this.minecraft.setScreen(new EnchantingInfoScreen(this));
                return true;
            }
        }

        for (int k = 0; k < 3; ++k) {
            double d0 = mouseX - (double) (i + 60);
            double d1 = mouseY - (double) (j + 14 + 19 * k);
            if (d0 >= 0.0D && d1 >= 0.0D && d0 < 108.0D && d1 < 19.0D) {
                int cost = this.menu.costs[k];
                if (cost <= 0 || this.menu.enchantClue[k] == -1) continue;
                int lapis = this.menu.getGoldCount();
                boolean canAfford = this.minecraft.player.getAbilities().instabuild
                        || (lapis >= k + 1 && this.minecraft.player.experienceLevel >= cost);
                if (canAfford) {
                    this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, k);
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void drawOnLeft(GuiGraphics gfx, List<Component> list, int y) {
        if (list.isEmpty()) return;
        int lineHeight = this.font.lineHeight;
        int maxWidth = 0;
        for (Component c : list) {
            maxWidth = Math.max(maxWidth, this.font.width(c));
        }
        int textHeight = list.size() * lineHeight;
        int pad = 4;
        int gap = 5;
        int boxX = this.leftPos - maxWidth - pad * 2 - gap;
        if (boxX < 0) return;
        int boxY = y - pad;
        int boxW = maxWidth + pad * 2;
        int boxH = textHeight + pad * 2;

        gfx.pose().pushPose();
        gfx.pose().translate(0, 0, 10);
        gfx.fill(boxX, boxY, boxX + boxW, boxY + boxH, 0xF0100010);
        int c1 = 0x505000FF;
        int c2 = 0x5028007F;
        gfx.fillGradient(boxX, boxY + 1, boxX + 1, boxY + boxH - 1, c1, c2);
        gfx.fillGradient(boxX + boxW - 1, boxY + 1, boxX + boxW, boxY + boxH - 1, c1, c2);
        gfx.fill(boxX + 1, boxY, boxX + boxW - 1, boxY + 1, c1);
        gfx.fill(boxX + 1, boxY + boxH - 1, boxX + boxW - 1, boxY + boxH, c2);
        for (int i = 0; i < list.size(); i++) {
            gfx.drawString(this.font, list.get(i), boxX + pad, y + i * lineHeight, 0xFFFFFF, true);
        }
        gfx.pose().popPose();
    }

    private static int guaranteedPicks(float arcana) {
        int picks = 0;
        for (int i = 0; i <= 66; i += 33) {
            if (arcana >= i) picks++;
        }
        return picks;
    }

    private static MutableComponent eternaLabel() {
        return Component.translatable("gui.meridian.enchant.eterna").withStyle(ChatFormatting.GREEN);
    }

    private static MutableComponent quantaLabel() {
        return Component.translatable("gui.meridian.enchant.quanta").withStyle(ChatFormatting.RED);
    }

    private static MutableComponent arcanaLabel() {
        return Component.translatable("gui.meridian.enchant.arcana").withStyle(ChatFormatting.DARK_PURPLE);
    }

    private static MutableComponent rectLabel() {
        return Component.translatable("gui.meridian.stat.rectification").withStyle(ChatFormatting.AQUA);
    }

    private static String f(float f) {
        return String.format("%.2f", f);
    }

    private Optional<CraftingResultEntry> craftingResult() {
        return fizzleMenu != null ? fizzleMenu.lastCraftingResult() : Optional.empty();
    }

    private boolean isInfoButtonVisible() {
        if (fizzleMenu == null) return false;
        return this.menu.getSlot(0).hasItem()
                && (this.menu.costs[0] > 0 || this.menu.costs[1] > 0 || this.menu.costs[2] > 0);
    }

    private boolean isInfusionFailedForInput() {
        if (fizzleMenu == null || this.minecraft == null || this.minecraft.level == null) return false;
        ItemStack input = this.menu.getSlot(0).getItem();
        if (input.isEmpty()) return false;
        return craftingResult().isEmpty()
                && EnchantingRecipeRegistry.hasItemMatch(this.minecraft.level.getRecipeManager(), input);
    }
}
