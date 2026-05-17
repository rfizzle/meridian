package com.rfizzle.meridian.library;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
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
class LibraryStorageTest {

    private static final ResourceKey<Enchantment> SHARPNESS = Enchantments.SHARPNESS;

    private static HolderLookup.Provider lookup;
    private static BlockState state;

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        lookup = VanillaRegistries.createLookup();
        state = Blocks.CHEST.defaultBlockState();
    }

    @Test
    void insert_rejectsNonBookVariants() {
        TestLibraryBlockEntity lib = newBasic();
        ItemStack swordStack = new ItemStack(Items.DIAMOND_SWORD);
        swordStack.set(DataComponents.CUSTOM_NAME, Component.literal("not a book"));
        ItemVariant sword = ItemVariant.of(swordStack);
        try (Transaction tx = Transaction.openOuter()) {
            long accepted = lib.getStorageAdapter().insert(sword, 1, tx);
            tx.commit();
            assertEquals(0, accepted, "non-book variant → zero accepted");
        }
        assertTrue(lib.getPoints().isEmpty(), "non-book insert must not touch the pool");
    }

    @Test
    void insert_bookAtCap_returnsFullAmountWithVoidOverflow() {
        TestLibraryBlockEntity lib = newBasic();
        lib.getPoints().put(SHARPNESS, lib.getMaxPoints());
        lib.getMaxLevels().put(SHARPNESS, 1);

        ItemVariant bookVariant = ItemVariant.of(singleEnchantBook(SHARPNESS, 1));
        try (Transaction tx = Transaction.openOuter()) {
            long accepted = lib.getStorageAdapter().insert(bookVariant, 10, tx);
            tx.commit();
            assertEquals(10, accepted,
                    "saturated library still reports every book as accepted — pipes never see a full library");
        }
        assertEquals(lib.getMaxPoints(), lib.getPoints().getInt(SHARPNESS),
                "overflow voided inside depositBook; pool stays at the cap");
    }

    @Test
    void insert_freshPool_accumulatesPerUnit() {
        TestLibraryBlockEntity lib = newBasic();
        ItemVariant bookVariant = ItemVariant.of(singleEnchantBook(SHARPNESS, 1));
        try (Transaction tx = Transaction.openOuter()) {
            long accepted = lib.getStorageAdapter().insert(bookVariant, 3, tx);
            tx.commit();
            assertEquals(3, accepted, "three books → three accepted");
        }
        assertEquals(3, lib.getPoints().getInt(SHARPNESS),
                "per-unit depositBook: three Sharpness-I books → 3 * points(1) = 3 points");
        assertEquals(1, lib.getMaxLevels().getInt(SHARPNESS));
    }

    @Test
    void abort_rollsBackInsertedPoints() {
        TestLibraryBlockEntity lib = newBasic();
        ItemVariant bookVariant = ItemVariant.of(singleEnchantBook(SHARPNESS, 3));

        try (Transaction tx = Transaction.openOuter()) {
            long accepted = lib.getStorageAdapter().insert(bookVariant, 5, tx);
            assertEquals(5, accepted);
            assertEquals(5 * EnchantmentLibraryBlockEntity.points(3),
                    lib.getPoints().getInt(SHARPNESS),
                    "in-flight insert mutates BE state; rollback comes from the snapshot, not deferred apply");
        }
        assertTrue(lib.getPoints().isEmpty(), "abort restored pre-insert pool");
        assertTrue(lib.getMaxLevels().isEmpty(), "abort restored pre-insert max-levels");
        assertEquals(0, lib.setChangedCount,
                "aborted transaction must not fire setChanged — onFinalCommit is success-only");
    }

    @Test
    void commit_appliesMutationAndFiresSetChangedOnce() {
        TestLibraryBlockEntity lib = newBasic();
        ItemVariant bookVariant = ItemVariant.of(singleEnchantBook(SHARPNESS, 2));
        try (Transaction tx = Transaction.openOuter()) {
            lib.getStorageAdapter().insert(bookVariant, 4, tx);
            tx.commit();
        }
        assertEquals(4 * EnchantmentLibraryBlockEntity.points(2),
                lib.getPoints().getInt(SHARPNESS),
                "committed insert persists; per-unit math: 4 books × points(2)");
        assertEquals(2, lib.getMaxLevels().getInt(SHARPNESS));
        assertEquals(1, lib.setChangedCount,
                "onFinalCommit fires setChanged exactly once per outer transaction, regardless of deposit count");
    }

    @Test
    void abort_preservesPriorMutationsFromEarlierCommit() {
        TestLibraryBlockEntity lib = newBasic();
        ItemVariant bookVariant = ItemVariant.of(singleEnchantBook(SHARPNESS, 2));

        try (Transaction tx = Transaction.openOuter()) {
            lib.getStorageAdapter().insert(bookVariant, 1, tx);
            tx.commit();
        }
        int afterFirst = lib.getPoints().getInt(SHARPNESS);
        assertEquals(EnchantmentLibraryBlockEntity.points(2), afterFirst,
                "sanity: first commit landed before the abort case runs");
        int firstCommitSetChanged = lib.setChangedCount;

        try (Transaction tx = Transaction.openOuter()) {
            lib.getStorageAdapter().insert(bookVariant, 3, tx);
        }
        assertEquals(afterFirst, lib.getPoints().getInt(SHARPNESS),
                "abort restores the snapshot taken at insert-time, not an empty pool");
        assertEquals(firstCommitSetChanged, lib.setChangedCount,
                "aborted second txn must not bump the setChanged counter past the first commit");
    }

    @Test
    void insert_rateLimitedSecondCallDrops() {
        TestLibraryBlockEntity lib = newBasic();
        lib.rateLimit = 20;
        lib.gameTime = 0L;
        ItemVariant bookVariant = ItemVariant.of(singleEnchantBook(SHARPNESS, 1));

        try (Transaction tx = Transaction.openOuter()) {
            long accepted = lib.getStorageAdapter().insert(bookVariant, 1, tx);
            tx.commit();
            assertEquals(1, accepted, "first insert at tick 0 has no prior stamp → allowed");
        }
        assertEquals(1, lib.getPoints().getInt(SHARPNESS), "first insert landed in the pool");
        assertEquals(0L, lib.lastInsertTick, "lastInsertTick stamped at commit time");

        lib.gameTime = 5L;
        try (Transaction tx = Transaction.openOuter()) {
            long accepted = lib.getStorageAdapter().insert(bookVariant, 1, tx);
            tx.commit();
            assertEquals(0, accepted, "second insert within the 20-tick window → dropped");
        }
        assertEquals(1, lib.getPoints().getInt(SHARPNESS),
                "dropped insert must not accumulate points");
        assertEquals(0L, lib.lastInsertTick,
                "dropped insert does not restart the cooldown clock");

        lib.gameTime = 20L;
        try (Transaction tx = Transaction.openOuter()) {
            long accepted = lib.getStorageAdapter().insert(bookVariant, 1, tx);
            tx.commit();
            assertEquals(1, accepted, "insert at tick = rateLimit is past the window → allowed");
        }
        assertEquals(2, lib.getPoints().getInt(SHARPNESS));
        assertEquals(20L, lib.lastInsertTick);
    }

    @Test
    void insert_rateLimitZeroNeverDrops() {
        TestLibraryBlockEntity lib = newBasic();
        lib.rateLimit = 0;
        lib.gameTime = 0L;
        ItemVariant bookVariant = ItemVariant.of(singleEnchantBook(SHARPNESS, 1));

        for (int i = 0; i < 3; i++) {
            try (Transaction tx = Transaction.openOuter()) {
                long accepted = lib.getStorageAdapter().insert(bookVariant, 1, tx);
                tx.commit();
                assertEquals(1, accepted, "rate limit off → every insert accepted");
            }
        }
        assertEquals(3, lib.getPoints().getInt(SHARPNESS));
    }

    @Test
    void insert_rateLimitedAbortDoesNotBurnCooldown() {
        TestLibraryBlockEntity lib = newBasic();
        lib.rateLimit = 20;
        lib.gameTime = 0L;
        ItemVariant bookVariant = ItemVariant.of(singleEnchantBook(SHARPNESS, 1));

        try (Transaction tx = Transaction.openOuter()) {
            lib.getStorageAdapter().insert(bookVariant, 1, tx);
        }
        assertEquals(Long.MIN_VALUE, lib.lastInsertTick,
                "abort restores the pre-insert sentinel; no phantom cooldown");

        lib.gameTime = 1L;
        try (Transaction tx = Transaction.openOuter()) {
            long accepted = lib.getStorageAdapter().insert(bookVariant, 1, tx);
            tx.commit();
            assertEquals(1, accepted, "post-abort insert is allowed — aborted call did not throttle");
        }
        assertEquals(1L, lib.lastInsertTick);
    }

    @Test
    void extract_alwaysReturnsZero() {
        TestLibraryBlockEntity lib = newBasic();
        lib.getPoints().put(SHARPNESS, lib.getMaxPoints());
        lib.getMaxLevels().put(SHARPNESS, 5);

        ItemVariant bookVariant = ItemVariant.of(singleEnchantBook(SHARPNESS, 1));
        try (Transaction tx = Transaction.openOuter()) {
            long extracted = lib.getStorageAdapter().extract(bookVariant, 64, tx);
            assertEquals(0, extracted, "extract always returns zero — hoppers can't pull from a library");
            tx.commit();
        }
        assertFalse(lib.getStorageAdapter().supportsExtraction(),
                "supportsExtraction() false → pipes skip the extraction path entirely");
    }

    // ---- helpers ------------------------------------------------------------

    private static TestLibraryBlockEntity newBasic() {
        return new TestLibraryBlockEntity(BlockEntityType.CHEST, BlockPos.ZERO, state, 16);
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
        int setChangedCount = 0;
        long gameTime = 0L;
        int rateLimit = 0;

        TestLibraryBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int maxLevel) {
            super(type, pos, state, maxLevel);
        }

        @Override
        public void setChanged() {
            this.setChangedCount++;
            super.setChanged();
        }

        @Override
        protected long currentGameTime() {
            return this.gameTime;
        }

        @Override
        protected int rateLimitTicks() {
            return this.rateLimit;
        }
    }
}
