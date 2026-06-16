package com.rfizzle.meridian.tome;

import com.rfizzle.meridian.MeridianRegistry;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class XpTomeItemTest {

    @Test
    public void testBarWidth() {
        XpTomeItem tome = new XpTomeItem(new XpTomeItem.Properties(), 10);
        ItemStack stack = new ItemStack(tome);

        // 0% fill
        assertEquals(0, tome.getBarWidth(stack));

        // 50% fill
        stack.set(MeridianRegistry.STORED_XP, 5);
        assertEquals(Math.round(5 * 13.0F / 10.0F), tome.getBarWidth(stack));

        // 100% fill
        stack.set(MeridianRegistry.STORED_XP, 10);
        assertEquals(13, tome.getBarWidth(stack));
    }

    @Test
    public void testBarVisibility() {
        XpTomeItem tome = new XpTomeItem(new XpTomeItem.Properties(), 10);
        ItemStack stack = new ItemStack(tome);

        assertFalse(tome.isBarVisible(stack));

        stack.set(MeridianRegistry.STORED_XP, 1);
        assertTrue(tome.isBarVisible(stack));
    }
}
