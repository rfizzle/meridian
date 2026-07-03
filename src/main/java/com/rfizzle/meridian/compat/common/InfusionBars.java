package com.rfizzle.meridian.compat.common;

import com.rfizzle.meridian.enchanting.MeridianStatColors;
import com.rfizzle.meridian.enchanting.recipe.StatRequirements;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Bar models and hover-tooltip lines for the stat requirements of one Infusions entry
 * (a {@code meridian:enchanting} / {@code keep_nbt_enchanting} recipe) in the EMI / REI / JEI
 * viewers. Render-free — the geometry fractions and tooltip composition are unit-testable
 * without a client classpath; the client-side {@code InfusionCardRenderer} turns the models
 * into pixels.
 *
 * <p>An axis produces a bar only when it is gated (a positive floor or a non-sentinel cap) —
 * a recipe that only gates Eterna shows one bar, not three. {@link StatRequirements#NO_MAX}'s
 * {@code -1} per-axis sentinel means "no upper bound" and suppresses both the ghost segment and
 * the "But no more than" tooltip line.
 */
public final class InfusionBars {

    /**
     * One gated stat axis: its label key and canonical color, the floor/cap values, and the
     * axis maximum the bar is scaled against ({@code max} is {@code -1} when unbounded).
     */
    public record Bar(String labelKey, int color, float min, float max, float axisMax) {

        public boolean bounded() {
            return max >= 0F;
        }

        /** Filled fraction of the track — the required floor scaled against the axis max. */
        public float minFraction() {
            return fraction(min);
        }

        /** End of the translucent allowed-window segment; equals {@link #minFraction} when unbounded. */
        public float maxFraction() {
            return bounded() ? fraction(max) : minFraction();
        }

        private float fraction(float value) {
            if (axisMax <= 0F) {
                return 0F;
            }
            return Math.min(1F, Math.max(0F, value / axisMax));
        }
    }

    private InfusionBars() {
    }

    /**
     * Bar models for the gated axes, in Eterna → Quanta → Arcana order. Eterna scales against
     * the caller-resolved table cap ({@code RealEnchantmentHelper.resolveMaxEterna()}); Quanta
     * and Arcana scale against their fixed 0–100 range, matching the live enchanting screen.
     */
    public static List<Bar> of(StatRequirements reqs, StatRequirements maxReqs, float maxEterna) {
        List<Bar> bars = new ArrayList<>(3);
        addBar(bars, "gui.meridian.enchant.eterna", MeridianStatColors.ETERNA,
                reqs.eterna(), maxReqs.eterna(), maxEterna);
        addBar(bars, "gui.meridian.enchant.quanta", MeridianStatColors.QUANTA,
                reqs.quanta(), maxReqs.quanta(), 100F);
        addBar(bars, "gui.meridian.enchant.arcana", MeridianStatColors.ARCANA,
                reqs.arcana(), maxReqs.arcana(), 100F);
        return bars;
    }

    private static void addBar(List<Bar> out, String labelKey, int color,
                               float min, float max, float axisMax) {
        if (min <= 0F && max < 0F) {
            return;
        }
        out.add(new Bar(labelKey, color, Math.max(0F, min), max, axisMax));
    }

    /**
     * Hover tooltip for one bar: the stat name in its color, then the floor and — when the
     * recipe caps the axis — the ceiling, both phrased against the axis max like the live
     * enchanting screen's stat tooltips.
     */
    public static List<Component> tooltip(Bar bar) {
        List<Component> lines = new ArrayList<>(3);
        lines.add(Component.translatable(bar.labelKey()).withColor(bar.color() & 0xFFFFFF));
        if (bar.min() > 0F) {
            lines.add(Component.translatable("gui.meridian.infusion.requires_at_least",
                            RecipeInfoFormatter.formatFloat(bar.min()),
                            RecipeInfoFormatter.formatFloat(bar.axisMax()))
                    .withStyle(ChatFormatting.GRAY));
        }
        if (bar.bounded()) {
            lines.add(Component.translatable("gui.meridian.infusion.but_no_more_than",
                            RecipeInfoFormatter.formatFloat(bar.max()),
                            RecipeInfoFormatter.formatFloat(bar.axisMax()))
                    .withStyle(ChatFormatting.GRAY));
        }
        return lines;
    }
}
