// Tier: 2 (fabric-loader-junit)
package com.rfizzle.meridian.enchanting;

import com.rfizzle.meridian.enchanting.recipe.EnchantingRecipe;
import com.rfizzle.meridian.enchanting.recipe.KeepNbtEnchantingRecipe;
import com.rfizzle.meridian.enchanting.recipe.StatRequirements;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the crafting-result click path wired through slot 2 (Zenith's INFUSION pattern).
 * The actual menu click requires a live {@code ServerPlayer} + {@code Level} pair, so the
 * click-time gate is exercised through {@link MeridianEnchantmentLogic#validateClick} targeting
 * slot 2 (which requires 3 lapis), and the keep-nbt path is verified against the same
 * {@link net.minecraft.world.item.crafting.Recipe#assemble} call the server handler performs.
 */
class CraftingResultFlowTest {

    private static HolderLookup.Provider lookup;

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        lookup = VanillaRegistries.createLookup();
    }

    private static Holder<Enchantment> sharpness() {
        return lookup.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS);
    }

    // ---- validateClick for slot 2 (crafting slot gate) --------------------------

    @Test
    void craftingSlotClick_insufficientLapis_rejects() {
        MeridianEnchantmentLogic.ClickAttempt attempt = MeridianEnchantmentLogic
                .validateClick(MeridianEnchantmentLogic.CRAFTING_SLOT, 10, false, 2, 30, false);
        assertFalse(attempt.success(),
                "slot 2 requires 3 lapis — 2 is insufficient");
    }

    @Test
    void craftingSlotClick_insufficientXp_rejects() {
        MeridianEnchantmentLogic.ClickAttempt attempt = MeridianEnchantmentLogic
                .validateClick(MeridianEnchantmentLogic.CRAFTING_SLOT, 15, false, 3, 12, false);
        assertFalse(attempt.success(),
                "player xp below the recipe's cost must reject the click");
    }

    @Test
    void craftingSlotClick_enoughResources_passes() {
        MeridianEnchantmentLogic.ClickAttempt attempt = MeridianEnchantmentLogic
                .validateClick(MeridianEnchantmentLogic.CRAFTING_SLOT, 10, false, 3, 10, false);
        assertTrue(attempt.success(),
                "3 lapis + xp exactly equal to cost must pass");
    }

    @Test
    void craftingSlotClick_creative_bypassesResourceGate() {
        MeridianEnchantmentLogic.ClickAttempt attempt = MeridianEnchantmentLogic
                .validateClick(MeridianEnchantmentLogic.CRAFTING_SLOT, 30, false, 0, 0, true);
        assertTrue(attempt.success(), "creative bypasses both lapis and xp gates");
    }

    // ---- assemble path (keep-nbt preserves stored books) --------------------

    @Test
    void keepNbtAssemble_preservesStoredEnchantmentsOnLibraryUpgrade() {
        ItemStack libraryIn = new ItemStack(Items.BOOK);
        ItemEnchantments.Mutable stored = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        stored.set(sharpness(), 5);
        libraryIn.set(DataComponents.ENCHANTMENTS, stored.toImmutable());

        KeepNbtEnchantingRecipe upgrade = new KeepNbtEnchantingRecipe(
                Ingredient.of(Items.BOOK),
                new StatRequirements(50F, 45F, 100F),
                new StatRequirements(50F, 50F, 100F),
                new ItemStack(Items.ENCHANTED_BOOK),
                OptionalInt.empty(),
                30);

        ItemStack crafted = upgrade.assemble(new SingleRecipeInput(libraryIn), null);

        ItemEnchantments transferred = crafted.get(DataComponents.ENCHANTMENTS);
        assertNotNull(transferred, "keep-nbt assemble must set the enchantments component on the output");
        assertEquals(1, transferred.size(),
                "the output must carry exactly the stored enchantment from the input");
        assertEquals(5, transferred.getLevel(sharpness()),
                "sharpness level 5 survives the upgrade — the library's stored books are preserved");
        assertEquals(Items.ENCHANTED_BOOK, crafted.getItem(),
                "the output item type comes from the recipe's static result, not the input");
    }

    @Test
    void baseAssemble_doesNotPropagateInputEnchantments() {
        ItemStack swordIn = new ItemStack(Items.DIAMOND_SWORD);
        ItemEnchantments.Mutable stored = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        stored.set(sharpness(), 5);
        swordIn.set(DataComponents.ENCHANTMENTS, stored.toImmutable());

        EnchantingRecipe plain = new EnchantingRecipe(
                Ingredient.of(Items.DIAMOND_SWORD),
                new StatRequirements(40F, 0F, 0F),
                StatRequirements.NO_MAX,
                new ItemStack(Items.DIAMOND),
                OptionalInt.empty(),
                12);

        ItemStack crafted = plain.assemble(new SingleRecipeInput(swordIn), null);

        ItemEnchantments carried = crafted.get(DataComponents.ENCHANTMENTS);
        assertTrue(carried == null || carried.isEmpty(),
                "non-keep-nbt recipes must not leak input enchantments onto the output");
        assertEquals(Items.DIAMOND, crafted.getItem(),
                "base enchanting recipes return their static result verbatim");
    }
}
