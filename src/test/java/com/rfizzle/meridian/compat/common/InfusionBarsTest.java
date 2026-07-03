package com.rfizzle.meridian.compat.common;

import com.rfizzle.meridian.enchanting.MeridianStatColors;
import com.rfizzle.meridian.enchanting.recipe.StatRequirements;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the bar models and hover-tooltip composition behind the recipe-viewer Infusions bars —
 * the data layer the shared {@code InfusionCardRenderer} draws, so axis suppression, scaling,
 * and tooltip lines can be asserted without a client classpath.
 */
class InfusionBarsTest {

    private static final float MAX_ETERNA = 50F;

    @Test
    void of_ungatedAxes_drawNoBar() {
        StatRequirements reqs = new StatRequirements(40F, 0F, 0F);
        List<InfusionBars.Bar> bars = InfusionBars.of(reqs, StatRequirements.NO_MAX, MAX_ETERNA);

        assertEquals(1, bars.size(), "Only the gated Eterna axis may produce a bar");
        assertEquals("gui.meridian.enchant.eterna", bars.get(0).labelKey());
        assertEquals(MeridianStatColors.ETERNA, bars.get(0).color());
    }

    @Test
    void of_allGated_ordersEternaQuantaArcana() {
        StatRequirements reqs = new StatRequirements(40F, 15F, 60F);
        List<InfusionBars.Bar> bars = InfusionBars.of(reqs, StatRequirements.NO_MAX, MAX_ETERNA);

        assertEquals(List.of(
                        "gui.meridian.enchant.eterna",
                        "gui.meridian.enchant.quanta",
                        "gui.meridian.enchant.arcana"),
                bars.stream().map(InfusionBars.Bar::labelKey).toList());
        assertEquals(MeridianStatColors.QUANTA, bars.get(1).color());
        assertEquals(MeridianStatColors.ARCANA, bars.get(2).color());
    }

    @Test
    void of_maxOnlyAxis_stillProducesBar() {
        StatRequirements maxReqs = new StatRequirements(-1F, 40F, -1F);
        List<InfusionBars.Bar> bars =
                InfusionBars.of(new StatRequirements(0F, 0F, 0F), maxReqs, MAX_ETERNA);

        assertEquals(1, bars.size(), "A cap without a floor still gates the axis");
        InfusionBars.Bar bar = bars.get(0);
        assertTrue(bar.bounded());
        assertEquals(0F, bar.minFraction(), "No floor means no filled segment");
        assertEquals(0.4F, bar.maxFraction(), 1e-6F, "Quanta scales against 100");
    }

    @Test
    void fractions_scaleAgainstAxisMaxAndClamp() {
        StatRequirements reqs = new StatRequirements(40F, 0F, 150F);
        List<InfusionBars.Bar> bars = InfusionBars.of(reqs, StatRequirements.NO_MAX, MAX_ETERNA);

        InfusionBars.Bar eterna = bars.get(0);
        assertEquals(0.8F, eterna.minFraction(), 1e-6F, "Eterna scales against the table cap (40/50)");
        assertFalse(eterna.bounded());
        assertEquals(eterna.minFraction(), eterna.maxFraction(),
                "Unbounded axis has no ghost segment — maxFraction collapses onto minFraction");

        InfusionBars.Bar arcana = bars.get(1);
        assertEquals(1F, arcana.minFraction(), "Over-cap requirements clamp to a full bar");
    }

    @Test
    void tooltip_boundedWindow_hasBothLines() {
        InfusionBars.Bar bar = InfusionBars.of(
                new StatRequirements(0F, 25F, 0F),
                new StatRequirements(-1F, 50F, -1F),
                MAX_ETERNA).get(0);
        List<Component> lines = InfusionBars.tooltip(bar);

        assertEquals(3, lines.size());
        assertEquals("gui.meridian.enchant.quanta", key(lines.get(0)));
        assertEquals("gui.meridian.infusion.requires_at_least", key(lines.get(1)));
        assertEquals(List.of("25", "100"), args(lines.get(1)));
        assertEquals("gui.meridian.infusion.but_no_more_than", key(lines.get(2)));
        assertEquals(List.of("50", "100"), args(lines.get(2)));
    }

    @Test
    void tooltip_unbounded_omitsCeilingLine() {
        InfusionBars.Bar bar = InfusionBars.of(
                new StatRequirements(22.5F, 0F, 0F), StatRequirements.NO_MAX, MAX_ETERNA).get(0);
        List<Component> lines = InfusionBars.tooltip(bar);

        assertEquals(2, lines.size(), "No cap means no 'But no more than' line");
        assertEquals("gui.meridian.infusion.requires_at_least", key(lines.get(1)));
        assertEquals(List.of("22.5", "50"), args(lines.get(1)),
                "Fractional floors keep one decimal; round caps drop the .0");
    }

    private static String key(Component component) {
        return ((TranslatableContents) component.getContents()).getKey();
    }

    private static List<Object> args(Component component) {
        return List.of(((TranslatableContents) component.getContents()).getArgs());
    }
}
