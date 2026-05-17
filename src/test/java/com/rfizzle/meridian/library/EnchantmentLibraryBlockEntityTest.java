package com.rfizzle.meridian.library;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Tier: 2
class EnchantmentLibraryBlockEntityTest {

    private static final ResourceKey<Enchantment> SHARPNESS = Enchantments.SHARPNESS;
    private static final ResourceKey<Enchantment> UNBREAKING = Enchantments.UNBREAKING;
    private static final ResourceKey<Enchantment> UNKNOWN = ResourceKey.create(
            Registries.ENCHANTMENT,
            ResourceLocation.fromNamespaceAndPath("minecraft", "never_registered"));

    private static HolderLookup.Provider lookup;
    private static BlockState state;

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        lookup = VanillaRegistries.createLookup();
        state = Blocks.CHEST.defaultBlockState();
    }

    // ---- ctor sanity --------------------------------------------------------

    @Test
    void construction_exposesTierConstants() {
        TestLibraryBlockEntity basic = newBasic();
        assertEquals(16, basic.getMaxLevel(), "Basic tier carries the tier-cap constant");
        assertEquals(32_768, basic.getMaxPoints(), "Basic tier points ceiling = points(16)");

        TestLibraryBlockEntity ender = newEnder();
        assertEquals(31, ender.getMaxLevel());
        assertEquals(1_073_741_824, ender.getMaxPoints(), "Ender tier points ceiling = points(31)");
        assertTrue(basic.getPoints().isEmpty(), "fresh library has empty point map");
        assertTrue(basic.getMaxLevels().isEmpty(), "fresh library has empty max-level map");
    }

    // ---- deposit ------------------------------------------------------------

    @Test
    void depositBook_accumulatesPointsAndMaxLevel() {
        TestLibraryBlockEntity lib = newBasic();
        lib.depositBook(singleEnchantBook(SHARPNESS, 3));
        assertEquals(4, lib.getPoints().getInt(SHARPNESS), "Sharpness-3 book contributes 2^(3-1) = 4");
        assertEquals(3, lib.getMaxLevels().getInt(SHARPNESS), "max level tracks the deposited level");

        lib.depositBook(singleEnchantBook(SHARPNESS, 1));
        assertEquals(5, lib.getPoints().getInt(SHARPNESS), "second deposit adds points(1) = 1 → 5 total");
        assertEquals(3, lib.getMaxLevels().getInt(SHARPNESS), "max level does not regress when a lower book is added");

        lib.depositBook(singleEnchantBook(SHARPNESS, 5));
        assertEquals(21, lib.getPoints().getInt(SHARPNESS), "third deposit adds 2^(5-1) = 16 → 21");
        assertEquals(5, lib.getMaxLevels().getInt(SHARPNESS), "max level raises when a higher book is added");
    }

    @Test
    void depositBook_clampsMaxLevelToTierCap() {
        TestLibraryBlockEntity basic = newBasic();
        basic.depositBook(singleEnchantBook(SHARPNESS, 31));
        assertEquals(16, basic.getMaxLevels().getInt(SHARPNESS), "Basic tier never exposes maxLevel > 16");
    }

    @Test
    void depositBook_voidsOverflowAtMaxPoints() {
        TestLibraryBlockEntity basic = newBasic();
        basic.getPoints().put(SHARPNESS, basic.getMaxPoints() - 1);
        basic.depositBook(singleEnchantBook(SHARPNESS, 3)); // +4 points
        assertEquals(basic.getMaxPoints(), basic.getPoints().getInt(SHARPNESS),
                "sum clamps to maxPoints; overflow is destroyed (silent void)");
    }

    @Test
    void depositBook_rejectsNonBookStacks() {
        TestLibraryBlockEntity lib = newBasic();
        lib.depositBook(ItemStack.EMPTY);
        lib.depositBook(new ItemStack(Items.DIAMOND));
        assertTrue(lib.getPoints().isEmpty(), "non-book deposits must not mutate state");
        assertTrue(lib.getMaxLevels().isEmpty());
    }

    @Test
    void depositBook_ignoresBlankEnchantedBook() {
        TestLibraryBlockEntity lib = newBasic();
        lib.depositBook(new ItemStack(Items.ENCHANTED_BOOK));
        assertTrue(lib.getPoints().isEmpty(),
                "enchanted book with no STORED_ENCHANTMENTS component is a no-op deposit");
    }

    @Test
    void depositBook_acceptsMultipleEnchantmentsInOneBook() {
        TestLibraryBlockEntity lib = newBasic();
        ItemStack stack = new ItemStack(Items.ENCHANTED_BOOK);
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        mutable.set(holderFor(SHARPNESS), 2);
        mutable.set(holderFor(UNBREAKING), 4);
        stack.set(DataComponents.STORED_ENCHANTMENTS, mutable.toImmutable());
        lib.depositBook(stack);
        assertEquals(2, lib.getPoints().getInt(SHARPNESS), "Sharpness-2 → 2 points");
        assertEquals(8, lib.getPoints().getInt(UNBREAKING), "Unbreaking-4 → 8 points");
        assertEquals(2, lib.getMaxLevels().getInt(SHARPNESS));
        assertEquals(4, lib.getMaxLevels().getInt(UNBREAKING));
    }

    // ---- canExtract ---------------------------------------------------------

    @Test
    void canExtract_gatesOnBothMaxLevelAndPoints() {
        TestLibraryBlockEntity lib = newBasic();
        lib.getPoints().put(SHARPNESS, lib.getMaxPoints());
        lib.getMaxLevels().put(SHARPNESS, 1);
        assertFalse(lib.canExtract(SHARPNESS, 5, 0),
                "points suffice but maxLevels = 1 blocks Sharpness-V extraction");

        lib.getMaxLevels().put(SHARPNESS, 10);
        lib.getPoints().put(SHARPNESS, 4); // only enough for level 3
        assertFalse(lib.canExtract(SHARPNESS, 5, 0),
                "maxLevels high enough but pool only holds 4 points → decline level 5");
    }

    @Test
    void canExtract_allowsUpgradePathingAtReducedCost() {
        TestLibraryBlockEntity lib = newBasic();
        lib.getMaxLevels().put(SHARPNESS, 5);
        lib.getPoints().put(SHARPNESS, 8);
        assertTrue(lib.canExtract(SHARPNESS, 5, 4),
                "upgrade IV→V costs points(5) - points(4) = 8; pool has exactly 8");

        lib.getPoints().put(SHARPNESS, 7);
        assertFalse(lib.canExtract(SHARPNESS, 5, 4),
                "one point short → decline");
    }

    @Test
    void canExtract_declinesWhenTargetIsNotAnUpgrade() {
        TestLibraryBlockEntity lib = newBasic();
        lib.getMaxLevels().put(SHARPNESS, 5);
        lib.getPoints().put(SHARPNESS, 1024);
        assertFalse(lib.canExtract(SHARPNESS, 3, 3), "same-level target is a no-op, declined");
        assertFalse(lib.canExtract(SHARPNESS, 2, 3), "downgrade request declined");
        assertFalse(lib.canExtract(SHARPNESS, 0, 0), "target 0 declined");
    }

    @Test
    void canExtract_declinesWhenTargetExceedsTierCap() {
        TestLibraryBlockEntity basic = newBasic();
        basic.getMaxLevels().put(SHARPNESS, 31);
        basic.getPoints().put(SHARPNESS, basic.getMaxPoints());
        assertFalse(basic.canExtract(SHARPNESS, 17, 0),
                "Basic tier never extracts past level 16, even if the map were tampered with");
    }

    @Test
    void canExtract_declinesForUnseenEnchantment() {
        TestLibraryBlockEntity lib = newBasic();
        assertFalse(lib.canExtract(UNKNOWN, 1, 0),
                "no deposits ever seen → both maps zero → decline");
    }

    // ---- extract ------------------------------------------------------------

    @Test
    void extract_debitsPointsOnSuccess() {
        TestLibraryBlockEntity lib = newBasic();
        lib.getMaxLevels().put(SHARPNESS, 5);
        lib.getPoints().put(SHARPNESS, 100);
        assertTrue(lib.extract(SHARPNESS, 3, 0),
                "target 3 costs 4 points; pool has 100 → success");
        assertEquals(96, lib.getPoints().getInt(SHARPNESS),
                "points debited by points(3) - points(0) = 4");
    }

    @Test
    void extract_upgradesAreCheaperThanFreshPulls() {
        TestLibraryBlockEntity lib = newBasic();
        lib.getMaxLevels().put(SHARPNESS, 5);
        lib.getPoints().put(SHARPNESS, 32);
        assertTrue(lib.extract(SHARPNESS, 5, 0));
        assertEquals(16, lib.getPoints().getInt(SHARPNESS));
        assertTrue(lib.extract(SHARPNESS, 5, 4));
        assertEquals(8, lib.getPoints().getInt(SHARPNESS));
    }

    @Test
    void extract_returnsFalseAndLeavesStateUntouchedOnDecline() {
        TestLibraryBlockEntity lib = newBasic();
        lib.getMaxLevels().put(SHARPNESS, 1);
        lib.getPoints().put(SHARPNESS, 999);
        assertFalse(lib.extract(SHARPNESS, 5, 0),
                "declined when target exceeds maxLevels");
        assertEquals(999, lib.getPoints().getInt(SHARPNESS),
                "pool untouched on decline — no partial debit");
        assertEquals(1, lib.getMaxLevels().getInt(SHARPNESS),
                "maxLevels untouched on decline");
    }

    // ---- helpers ------------------------------------------------------------

    private static TestLibraryBlockEntity newBasic() {
        return new TestLibraryBlockEntity(BlockEntityType.CHEST, BlockPos.ZERO, state, 16);
    }

    private static TestLibraryBlockEntity newEnder() {
        return new TestLibraryBlockEntity(BlockEntityType.CHEST, BlockPos.ZERO, state, 31);
    }

    private static ItemStack singleEnchantBook(ResourceKey<Enchantment> enchant, int level) {
        ItemStack stack = new ItemStack(Items.ENCHANTED_BOOK);
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        mutable.set(holderFor(enchant), level);
        stack.set(DataComponents.STORED_ENCHANTMENTS, mutable.toImmutable());
        return stack;
    }

    private static Holder<Enchantment> holderFor(ResourceKey<Enchantment> key) {
        return lookup.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
    }

    private static final class TestLibraryBlockEntity extends EnchantmentLibraryBlockEntity {
        TestLibraryBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int maxLevel) {
            super(type, pos, state, maxLevel);
        }
    }
}
