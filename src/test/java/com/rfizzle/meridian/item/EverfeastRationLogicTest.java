// Tier: 2 (fabric-loader-junit)
package com.rfizzle.meridian.item;

import com.rfizzle.meridian.config.MeridianConfig;
import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EverfeastRationLogicTest {

    private static EverfeastRationItem ration;

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        // Constructed directly rather than through MeridianRegistry so the test doesn't
        // trigger the full registry class initializer (blocks, BE types) in a unit JVM.
        ration = new EverfeastRationItem(new Item.Properties().stacksTo(1));
    }

    @Test
    void stampBites_sizesThePoolAndZeroesUsage() {
        ItemStack stack = new ItemStack(ration);
        EverfeastRationItem.stampBites(stack, 5);

        assertEquals(5, EverfeastRationItem.maxBites(stack));
        assertEquals(5, EverfeastRationItem.remainingBites(stack));
    }

    @Test
    void consumeBite_decrementsUntilTheFinalBiteConsumesTheItem() {
        ItemStack stack = new ItemStack(ration);
        EverfeastRationItem.stampBites(stack, 3);

        ItemStack afterOne = EverfeastRationItem.consumeBite(stack);
        assertFalse(afterOne.isEmpty());
        assertEquals(2, EverfeastRationItem.remainingBites(afterOne));

        ItemStack afterTwo = EverfeastRationItem.consumeBite(afterOne);
        assertFalse(afterTwo.isEmpty());
        assertEquals(1, EverfeastRationItem.remainingBites(afterTwo));

        assertTrue(EverfeastRationItem.consumeBite(afterTwo).isEmpty(),
                "the final bite must consume the ration");
    }

    @Test
    void consumeBite_doesNotMutateTheInputStack() {
        ItemStack stack = new ItemStack(ration);
        EverfeastRationItem.stampBites(stack, 4);

        EverfeastRationItem.consumeBite(stack);

        assertEquals(4, EverfeastRationItem.remainingBites(stack),
                "consumeBite must return a copy, not tick the input");
    }

    @Test
    void unstampedStack_fallsBackToConfiguredBitesAndFreezesOnFirstBite() {
        int configured = new MeridianConfig().everfeast.bites;
        ItemStack stack = new ItemStack(ration);

        assertEquals(configured, EverfeastRationItem.maxBites(stack));

        ItemStack afterOne = EverfeastRationItem.consumeBite(stack);
        assertEquals(configured, afterOne.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                        .copyTag().getInt(EverfeastRationItem.TAG_MAX_BITES),
                "first bite must stamp the pool so later config changes can't resize it");
        assertEquals(configured - 1, EverfeastRationItem.remainingBites(afterOne));
    }

    @Test
    void stampedCount_survivesAConfigChange() {
        ItemStack stack = new ItemStack(ration);
        EverfeastRationItem.stampBites(stack, 64);

        // maxBites must read the stack's own component, never the live config value.
        assertEquals(64, EverfeastRationItem.maxBites(stack));
    }

    @Test
    void singleBiteRation_isConsumedImmediately() {
        ItemStack stack = new ItemStack(ration);
        EverfeastRationItem.stampBites(stack, 1);

        assertTrue(EverfeastRationItem.consumeBite(stack).isEmpty());
    }

    @Test
    void configuredBites_clampsToAtLeastOneAndDefaultsWithoutConfig() {
        MeridianConfig config = new MeridianConfig();
        config.everfeast.bites = -5;
        assertEquals(1, EverfeastRationItem.configuredBites(config));

        assertEquals(new MeridianConfig().everfeast.bites,
                EverfeastRationItem.configuredBites(null),
                "a missing config must fall back to the default bite count");
    }

    @Test
    void configDefault_is128WithinClampRange() {
        assertEquals(128, new MeridianConfig().everfeast.bites);
    }

    @Test
    void stampedRation_isNotDamageableAndShowsNoBar() {
        // Bites must not ride the vanilla damage components: a damageable ration would open
        // every durability repair channel (grindstone merge, anvil combine, Extraction Tome,
        // Tempered Core) as a bite-restore exploit, and would show a durability bar.
        ItemStack stack = new ItemStack(ration);
        EverfeastRationItem.stampBites(stack, 8);
        ItemStack bitten = EverfeastRationItem.consumeBite(stack);

        assertFalse(bitten.isDamageableItem(), "ration must never be damageable");
        assertFalse(bitten.has(DataComponents.MAX_DAMAGE));
        assertFalse(bitten.has(DataComponents.DAMAGE));
        assertFalse(ration.isBarVisible(bitten),
                "durability bar must stay suppressed in favor of the Bites tooltip");
    }

    @Test
    void stampedRation_isNotEnchantable() {
        ItemStack stack = new ItemStack(ration);
        EverfeastRationItem.stampBites(stack, 8);
        assertFalse(ration.isEnchantable(stack),
                "rations must not be valid enchanting targets");
    }
}
