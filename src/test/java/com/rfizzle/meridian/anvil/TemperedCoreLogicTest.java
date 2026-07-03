// Tier: 2 (fabric-loader-junit)
package com.rfizzle.meridian.anvil;

import com.rfizzle.meridian.config.MeridianConfig;
import com.rfizzle.meridian.item.TemperedCoreItem;
import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Unbreakable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TemperedCoreLogicTest {

    private static TemperedCoreItem temperedCore;

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        // Constructed directly rather than through MeridianRegistry so the test doesn't
        // trigger the full registry class initializer (blocks, BE types) in a unit JVM.
        temperedCore = new TemperedCoreItem(new Item.Properties());
    }

    private final TemperedCoreHandler handler = new TemperedCoreHandler(MeridianConfig::new);

    @Test
    void damagedTool_becomesUnbreakableAndFullyRepaired() {
        ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
        pickaxe.setDamageValue(500);

        Optional<AnvilResult> result = handler.handle(pickaxe, new ItemStack(temperedCore), null);

        assertTrue(result.isPresent(), "damaged pickaxe + core must be claimed");
        ItemStack output = result.get().output();
        assertTrue(output.has(DataComponents.UNBREAKABLE), "output must carry minecraft:unbreakable");
        assertEquals(0, output.getDamageValue(), "remaining damage must be healed");
        assertEquals(Items.DIAMOND_PICKAXE, output.getItem(), "item type must be preserved");
        assertEquals(new MeridianConfig().anvil.temperedCoreLevelCost, result.get().xpCost());
        assertEquals(1, result.get().rightConsumed(), "exactly one core consumed");
    }

    @Test
    void componentsOtherThanDurability_arePreserved() {
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Oathkeeper"));

        Optional<AnvilResult> result = handler.handle(sword, new ItemStack(temperedCore), null);

        assertTrue(result.isPresent());
        assertEquals("Oathkeeper", result.get().output().get(DataComponents.CUSTOM_NAME).getString(),
                "unrelated components must survive the application");
    }

    @Test
    void inputStack_isNotMutated() {
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.setDamageValue(100);

        handler.handle(sword, new ItemStack(temperedCore), null);

        assertFalse(sword.has(DataComponents.UNBREAKABLE), "left input must not be mutated");
        assertEquals(100, sword.getDamageValue());
    }

    @Test
    void alreadyUnbreakable_declines() {
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.set(DataComponents.UNBREAKABLE, new Unbreakable(true));

        assertTrue(handler.handle(sword, new ItemStack(temperedCore), null).isEmpty(),
                "one core per item — already-unbreakable input must be refused");
    }

    @Test
    void nonDamageableItem_declines() {
        assertTrue(handler.handle(new ItemStack(Items.STICK), new ItemStack(temperedCore), null).isEmpty(),
                "items without durability must be refused");
    }

    @Test
    void wrongRightItem_declines() {
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        assertTrue(handler.handle(sword, new ItemStack(Items.GOLD_INGOT), null).isEmpty());
    }

    @Test
    void nullConfig_declines() {
        TemperedCoreHandler noConfig = new TemperedCoreHandler(() -> null);
        assertTrue(noConfig.handle(new ItemStack(Items.DIAMOND_SWORD), new ItemStack(temperedCore), null).isEmpty());
    }

    @Test
    void emptySlots_decline() {
        assertTrue(handler.handle(ItemStack.EMPTY, new ItemStack(temperedCore), null).isEmpty());
        assertTrue(handler.handle(new ItemStack(Items.DIAMOND_SWORD), ItemStack.EMPTY, null).isEmpty());
    }
}
