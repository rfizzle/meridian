// Tier: 1 (pure JUnit)
package com.rfizzle.meridian.enchanting;

import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ObscurityTooltipMathTest {

    @Test
    void removeMatchingLines_stripsOnlyHiddenNames() {
        List<Component> lines = new ArrayList<>(List.of(
                Component.literal("Diamond Sword"),
                Component.literal("Sharpness V"),
                Component.literal("Curse of Obscurity"),
                Component.literal("Unbreaking III")));

        ObscurityTooltipMath.removeMatchingLines(lines, Set.of("Sharpness V", "Unbreaking III"));

        List<String> remaining = lines.stream().map(Component::getString).toList();
        assertEquals(List.of("Diamond Sword", "Curse of Obscurity"), remaining);
    }

    @Test
    void removeMatchingLines_emptyHideSetLeavesLinesUntouched() {
        List<Component> lines = new ArrayList<>(List.of(Component.literal("Sharpness V")));

        ObscurityTooltipMath.removeMatchingLines(lines, Set.of());

        assertEquals(1, lines.size());
    }
}
