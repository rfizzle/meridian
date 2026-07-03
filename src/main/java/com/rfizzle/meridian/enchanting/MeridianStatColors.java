package com.rfizzle.meridian.enchanting;

/**
 * Canonical ARGB colors for the five enchanting stats, shared by every surface that colors a
 * stat label or bar (recipe-viewer Infusions bars, browser cards, screens). Referencing these
 * constants instead of inlining hex values keeps the stat color language consistent across
 * surfaces.
 */
public final class MeridianStatColors {

    public static final int ETERNA = 0xFF7BE0A0;
    public static final int QUANTA = 0xFFFC5454;
    public static final int ARCANA = 0xFFC060E0;
    public static final int RECTIFICATION = 0xFF5AD6D6;
    public static final int CLUES = 0xFFFFC24B;

    private MeridianStatColors() {
    }
}
