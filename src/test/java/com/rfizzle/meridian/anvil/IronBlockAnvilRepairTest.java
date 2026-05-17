// Tier: 2 (fabric-loader-junit)
package com.rfizzle.meridian.anvil;

import com.rfizzle.meridian.config.MeridianConfig;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.rfizzle.meridian.TestRegistryFixture.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * T-4.2.1 + T-4.2.2 — behavioural contract for {@link IronBlockAnvilRepairHandler}. Covers the
 * acceptance tests listed under S-4.2:
 * <ol>
 *   <li>Damaged anvil + iron block → chipped anvil, flat 1-level cost, one iron consumed.</li>
 *   <li>Chipped anvil + iron block → normal anvil.</li>
 *   <li>Normal anvil → declines (nothing to repair).</li>
 *   <li>Iron <b>ingot</b> in right slot → declines (storage block only, matching the task
 *       narrowing over Zenith's {@code #c:iron_blocks} tag).</li>
 *   <li>Enchantments on the input anvil survive the repair.</li>
 *   <li>{@code config.anvil.ironBlockRepairsAnvil=false} → declines (T-4.2.2 operator toggle).</li>
 * </ol>
 *
 * <p>Vanilla {@link Bootstrap#bootStrap()} populates {@link BuiltInRegistries#ITEM} but leaves the
 * enchantment registry empty, so the enchantment-preservation case builds a synthetic
 * {@link MappedRegistry} of one Sharpness holder — just enough to construct a real
 * {@link ItemEnchantments} component for the fixture.
 */
class IronBlockAnvilRepairTest {

    private static final ResourceKey<Enchantment> SHARPNESS = key("sharpness");

    private final IronBlockAnvilRepairHandler handler =
            new IronBlockAnvilRepairHandler(IronBlockAnvilRepairTest::defaultConfig);

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void handle_damagedAnvil_outputsChippedAnvil() {
        AnvilResult r = repair(Items.DAMAGED_ANVIL);

        assertEquals(Items.CHIPPED_ANVIL, r.output().getItem(),
                "damaged → chipped is the lowest-tier repair step (DESIGN § Anvil tweaks)");
        assertEquals(1, r.output().getCount(),
                "output is always a single anvil — stack count must not inherit from the input");
        assertEquals(1, r.xpCost(),
                "DESIGN.md mandates a flat 1-level XP cost, intentionally diverging from Zenith's "
                        + "enchant-scaled formula");
        assertEquals(1, r.rightConsumed(),
                "one iron block per repair; the rest of the right-slot stack must be returned");
    }

    @Test
    void handle_chippedAnvil_outputsFullAnvil() {
        AnvilResult r = repair(Items.CHIPPED_ANVIL);

        assertEquals(Items.ANVIL, r.output().getItem(),
                "chipped → normal completes the two-step repair ladder");
        assertEquals(1, r.xpCost());
        assertEquals(1, r.rightConsumed());
    }

    @Test
    void handle_normalAnvil_declines() {
        ItemStack left = new ItemStack(Items.ANVIL);
        ItemStack right = new ItemStack(Items.IRON_BLOCK);

        assertTrue(handler.handle(left, right, null).isEmpty(),
                "pristine anvil has nothing to repair — the handler must decline so iron blocks "
                        + "aren't consumed on a no-op output");
    }

    @Test
    void handle_nonAnvilLeft_declines() {
        // Defensive parity: any non-anvil item in slot A must be left to vanilla / other handlers.
        ItemStack left = new ItemStack(Items.DIAMOND_SWORD);
        ItemStack right = new ItemStack(Items.IRON_BLOCK);

        assertTrue(handler.handle(left, right, null).isEmpty(),
                "only damaged/chipped anvils are targets; other items must pass through");
    }

    @Test
    void handle_ironIngotInRight_declines() {
        ItemStack left = new ItemStack(Items.DAMAGED_ANVIL);
        ItemStack right = new ItemStack(Items.IRON_INGOT);

        assertTrue(handler.handle(left, right, null).isEmpty(),
                "task explicitly narrows the trigger to storage blocks — ingots must decline "
                        + "(this is where we diverge from Zenith's #c:iron_blocks tag)");
    }

    @Test
    void handle_emptySlots_declines() {
        assertTrue(handler.handle(ItemStack.EMPTY, new ItemStack(Items.IRON_BLOCK), null).isEmpty(),
                "empty left slot must decline");
        assertTrue(handler.handle(new ItemStack(Items.DAMAGED_ANVIL), ItemStack.EMPTY, null).isEmpty(),
                "empty right slot must decline");
    }

    @Test
    void handle_preservesEnchantmentsComponent() {
        Registry<Enchantment> enchantments = buildSyntheticEnchantmentRegistry();
        Holder<Enchantment> sharpnessHolder = enchantments.getHolderOrThrow(SHARPNESS);

        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        mutable.set(sharpnessHolder, 4);
        ItemEnchantments expected = mutable.toImmutable();

        ItemStack left = new ItemStack(Items.DAMAGED_ANVIL);
        left.set(DataComponents.ENCHANTMENTS, expected);
        ItemStack right = new ItemStack(Items.IRON_BLOCK);

        Optional<AnvilResult> result = handler.handle(left, right, null);
        assertTrue(result.isPresent(), "damaged + iron block should still claim when enchantments ride along");

        ItemEnchantments carried = result.get().output().get(DataComponents.ENCHANTMENTS);
        assertNotNull(carried,
                "enchantments must survive the repair — Zenith preserves them and the task "
                        + "specifies the same behaviour");
        assertEquals(4, carried.getLevel(sharpnessHolder),
                "sharpness level copied verbatim onto the repaired anvil");
        assertEquals(1, carried.size(),
                "no spurious entries introduced by the copy");
    }

    @Test
    void handle_configGateOff_declines() {
        MeridianConfig config = defaultConfig();
        config.anvil.ironBlockRepairsAnvil = false;
        IronBlockAnvilRepairHandler gated = new IronBlockAnvilRepairHandler(() -> config);

        ItemStack left = new ItemStack(Items.DAMAGED_ANVIL);
        ItemStack right = new ItemStack(Items.IRON_BLOCK);

        assertTrue(gated.handle(left, right, null).isEmpty(),
                "config gate forces decline even on an otherwise-valid pairing — operator toggle "
                        + "must neutralise the handler without unregistration (T-4.2.2)");
    }

    @Test
    void handle_configMissing_declines() {
        // getConfig() returns null before onInitialize has run; the handler must treat that as
        // "feature off" rather than NPE.
        IronBlockAnvilRepairHandler unconfigured = new IronBlockAnvilRepairHandler(() -> null);

        ItemStack left = new ItemStack(Items.DAMAGED_ANVIL);
        ItemStack right = new ItemStack(Items.IRON_BLOCK);

        assertTrue(unconfigured.handle(left, right, null).isEmpty(),
                "null config means onInitialize has not finished — handler must decline instead of NPE");
    }

    private AnvilResult repair(Item anvilTier) {
        ItemStack left = new ItemStack(anvilTier);
        ItemStack right = new ItemStack(Items.IRON_BLOCK);
        return handler.handle(left, right, null).orElseThrow(
                () -> new AssertionError("handler declined a valid " + anvilTier + " + iron block pairing"));
    }

    private static MeridianConfig defaultConfig() {
        return new MeridianConfig();
    }

    private static Registry<Enchantment> buildSyntheticEnchantmentRegistry() {
        MappedRegistry<Enchantment> reg = newRegistry();
        register(reg, SHARPNESS, synthetic());
        return reg.freeze();
    }
}
