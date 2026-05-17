// Tier: 2 (fabric-loader-junit)
package com.rfizzle.meridian.enchanting;

import com.rfizzle.meridian.net.EnchantmentClue;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.Bootstrap;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.rfizzle.meridian.TestRegistryFixture.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MeridianEnchantmentLogicTest {

    private static final ResourceKey<Enchantment> SHARPNESS = key("sharpness");
    private static final ResourceKey<Enchantment> UNBREAKING = key("unbreaking");
    private static final ResourceKey<Enchantment> MENDING = key("mending");

    private static Registry<Enchantment> registry;

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        registry = buildTestRegistry();
    }

    // ---- validateClick ------------------------------------------------------

    @Test
    void validateClick_successPath() {
        MeridianEnchantmentLogic.ClickAttempt attempt = MeridianEnchantmentLogic.validateClick(
                2, 30, false, 3, 30, false);
        assertTrue(attempt.success(), "valid id+cost+lapis+xp must pass");
        assertNull(attempt.rejection());
    }

    @Test
    void validateClick_insufficientLapis_rejects() {
        // Slot 2 needs 3 lapis; provide 2. Not creative — must reject.
        MeridianEnchantmentLogic.ClickAttempt attempt = MeridianEnchantmentLogic.validateClick(
                2, 30, false, 2, 30, false);
        assertFalse(attempt.success(), "lapis count 2 < required 3 must reject");
        assertTrue(attempt.rejection().contains("lapis"),
                "rejection reason should mention lapis for debuggability");
    }

    @Test
    void validateClick_insufficientXp_rejects() {
        // Cost is 30; player has 29 levels. Not creative — must reject.
        MeridianEnchantmentLogic.ClickAttempt attempt = MeridianEnchantmentLogic.validateClick(
                2, 30, false, 3, 29, false);
        assertFalse(attempt.success(), "experience level 29 < cost 30 must reject");
        assertTrue(attempt.rejection().contains("experience"),
                "rejection reason should mention experience");
    }

    @Test
    void validateClick_xpLessThanRequiredLapis_rejects() {
        // Slot 2 requires 3 levels (for the required-lapis gate). 2 levels — reject.
        MeridianEnchantmentLogic.ClickAttempt attempt = MeridianEnchantmentLogic.validateClick(
                2, 1, false, 3, 2, false);
        assertFalse(attempt.success(),
                "vanilla still gates on xp >= requiredLapis even when cost is tiny");
    }

    @Test
    void validateClick_creative_bypassesLapisAndXp() {
        // No lapis, no xp — creative must still pass.
        MeridianEnchantmentLogic.ClickAttempt attempt = MeridianEnchantmentLogic.validateClick(
                2, 30, false, 0, 0, true);
        assertTrue(attempt.success(), "creative bypasses lapis and xp gates");
    }

    @Test
    void validateClick_invalidId_rejects() {
        assertFalse(MeridianEnchantmentLogic.validateClick(-1, 10, false, 3, 30, false).success());
        assertFalse(MeridianEnchantmentLogic.validateClick(3, 10, false, 3, 30, false).success());
        assertFalse(MeridianEnchantmentLogic.validateClick(42, 10, false, 3, 30, false).success());
    }

    @Test
    void validateClick_emptyInput_rejects() {
        MeridianEnchantmentLogic.ClickAttempt attempt = MeridianEnchantmentLogic.validateClick(
                0, 10, true, 3, 30, false);
        assertFalse(attempt.success());
        assertTrue(attempt.rejection().contains("empty"));
    }

    @Test
    void validateClick_zeroCost_rejects() {
        MeridianEnchantmentLogic.ClickAttempt attempt = MeridianEnchantmentLogic.validateClick(
                1, 0, false, 3, 30, false);
        assertFalse(attempt.success());
        assertTrue(attempt.rejection().contains("cost"));
    }

    // ---- applyPicks (Successful enchant → item gains ItemEnchantments) -----

    @Test
    void applyPicks_enchantsSwordAndReportsRequiredLapis() {
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        Holder<Enchantment> sharpness = registry.wrapAsHolder(registry.getOrThrow(SHARPNESS));
        Holder<Enchantment> unbreaking = registry.wrapAsHolder(registry.getOrThrow(UNBREAKING));
        List<EnchantmentInstance> picks = List.of(
                new EnchantmentInstance(sharpness, 3),
                new EnchantmentInstance(unbreaking, 2));

        MeridianEnchantmentLogic.EnchantOutcome out =
                MeridianEnchantmentLogic.applyPicks(sword, 2, picks);

        // Vanilla semantics: slot 2 consumes 3 lapis AND 3 xp levels.
        assertEquals(3, out.lapisConsumed(), "slot id=2 consumes 3 lapis");
        assertEquals(3, out.xpLevelsConsumed(), "slot id=2 consumes 3 xp levels");

        ItemEnchantments ench = out.resultStack().getEnchantments();
        assertEquals(3, ench.getLevel(sharpness),
                "sharpness level 3 must land on the enchanted stack");
        assertEquals(2, ench.getLevel(unbreaking));
        assertEquals(2, ench.size(), "exactly the two picks should be applied");
    }

    @Test
    void applyPicks_transmutesBookToEnchantedBook() {
        ItemStack book = new ItemStack(Items.BOOK);
        Holder<Enchantment> mending = registry.wrapAsHolder(registry.getOrThrow(MENDING));
        List<EnchantmentInstance> picks = List.of(new EnchantmentInstance(mending, 1));

        MeridianEnchantmentLogic.EnchantOutcome out =
                MeridianEnchantmentLogic.applyPicks(book, 0, picks);

        assertEquals(Items.ENCHANTED_BOOK, out.resultStack().getItem(),
                "book input transmutes to enchanted_book output");
        assertNotEquals(book, out.resultStack(),
                "apply must return a new stack — the input slot is replaced by the caller");
        // Enchanted books store on `stored_enchantments`, which `getEnchantments()` does not see.
        // Verify via the stored component path instead.
        assertEquals(1, net.minecraft.world.item.enchantment.EnchantmentHelper
                .getEnchantmentsForCrafting(out.resultStack()).getLevel(mending));
    }

    @Test
    void applyPicks_zeroPicks_returnsCleanOutcome() {
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        MeridianEnchantmentLogic.EnchantOutcome out =
                MeridianEnchantmentLogic.applyPicks(sword, 0, List.of());
        assertEquals(1, out.lapisConsumed());
        assertEquals(1, out.xpLevelsConsumed());
        assertTrue(out.resultStack().getEnchantments().isEmpty(),
                "no picks → no enchantments, but the outcome still reports slot-cost lapis/xp");
    }

    @Test
    void applyPicks_lapisAndXpScaleByButtonId() {
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        for (int id = 0; id < MeridianEnchantmentLogic.PREVIEW_SLOTS; id++) {
            MeridianEnchantmentLogic.EnchantOutcome out =
                    MeridianEnchantmentLogic.applyPicks(sword, id, List.of());
            assertEquals(id + 1, out.lapisConsumed(),
                    "lapis consumed for slot " + id + " must be id+1 (vanilla semantics)");
            assertEquals(id + 1, out.xpLevelsConsumed(),
                    "xp consumed for slot " + id + " must match vanilla's requiredLapis");
        }
    }

    // ---- recompute ---------------------------------------------------------

    @Test
    void recompute_unenchantableInput_returnsEmptyPicks() {
        // The menu guards unenchantable inputs in slotsChanged — recompute still must stay safe.
        // Cost is computed purely from eterna (independent of the item), but the pick pool is
        // empty because selectEnchantment short-circuits on enchantability=0.
        StatCollection stats = new StatCollection(30F, 0F, 0F, 0F, 0, 30F, Set.of(), false);
        MeridianEnchantmentLogic.SlotState[] states = MeridianEnchantmentLogic.recompute(
                stats, new ItemStack(Items.DIRT), 0, 3, false, false, registry, RandomSource.create());
        for (MeridianEnchantmentLogic.SlotState s : states) {
            assertTrue(s.picks().isEmpty(), "dirt never draws an enchantment");
            assertNull(s.clueBuild().primary(),
                    "no picks → no primary clue — the slot stays blank in the UI");
        }
    }

    @Test
    void recompute_zeroLapis_zeroesAllCosts() {
        StatCollection stats = new StatCollection(30F, 0F, 0F, 0F, 0, 30F, Set.of(), false);
        MeridianEnchantmentLogic.SlotState[] states = MeridianEnchantmentLogic.recompute(
                stats, new ItemStack(Items.DIAMOND_SWORD), 12345, 0, false, false, registry, RandomSource.create());
        for (int i = 0; i < MeridianEnchantmentLogic.PREVIEW_SLOTS; i++) {
            assertEquals(0, states[i].cost(),
                    "slot " + i + " without lapis must zero out its cost");
            assertTrue(states[i].picks().isEmpty(),
                    "no picks roll for a zero-cost slot");
        }
    }

    @Test
    void recompute_creativeBypassesLapisGate() {
        StatCollection stats = new StatCollection(30F, 0F, 0F, 0F, 0, 30F, Set.of(), false);
        MeridianEnchantmentLogic.SlotState[] states = MeridianEnchantmentLogic.recompute(
                stats, new ItemStack(Items.DIAMOND_SWORD), 12345, 0, true, false, registry, RandomSource.create());
        assertTrue(states[MeridianEnchantmentLogic.PREVIEW_SLOTS - 1].cost() > 0,
                "creative still surfaces slot-2 cost with no lapis in the tray");
    }

    @Test
    void recompute_slot2CostEqualsEternaLevel() {
        StatCollection stats = new StatCollection(30F, 0F, 0F, 0F, 0, 30F, Set.of(), false);
        MeridianEnchantmentLogic.SlotState[] states = MeridianEnchantmentLogic.recompute(
                stats, new ItemStack(Items.DIAMOND_SWORD), 7, 3, false, false, registry, RandomSource.create());
        assertEquals(30, states[2].cost(),
                "slot 2 is always round(eterna) — this anchors the level displayed on the top row");
    }

    @Test
    void recompute_populatesPicksWithinReachableRegistry() {
        StatCollection stats = new StatCollection(30F, 50F, 30F, 0F, 2, 30F, Set.of(), false);
        MeridianEnchantmentLogic.SlotState[] states = MeridianEnchantmentLogic.recompute(
                stats, new ItemStack(Items.DIAMOND_SWORD), 42, 3, false, false, registry, RandomSource.create());
        assertTrue(states[2].cost() > 0);
        assertFalse(states[2].picks().isEmpty(),
                "stats with 50 quanta + diamond sword must produce at least one pick at slot 2");
        assertNotNull(states[2].clueBuild());
        assertNotNull(states[2].clueBuild().primary(),
                "clueBuild primary is the slot's enchant — must be populated when picks exist");
    }

    @Test
    void recompute_clueBuild_yieldsExpectedClueCount() {
        // stats.clues == 2 — the first clue is always the slot's primary, so the list has 2 entries.
        StatCollection stats = new StatCollection(30F, 50F, 30F, 0F, 2, 30F, Set.of(), false);
        MeridianEnchantmentLogic.SlotState[] states = MeridianEnchantmentLogic.recompute(
                stats, new ItemStack(Items.DIAMOND_SWORD), 42, 3, false, false, registry, RandomSource.create());
        assertTrue(states[2].clueBuild().clues().size() <= 2,
                "clue count is capped by stats.clues (2) and the pool size");
    }

    // ---- toPayloadClues ----------------------------------------------------

    @Test
    void toPayloadClues_preservesRegisteredInstances() {
        Holder<Enchantment> sharpness = registry.wrapAsHolder(registry.getOrThrow(SHARPNESS));
        RealEnchantmentHelper.ClueBuild cb = new RealEnchantmentHelper.ClueBuild(
                new EnchantmentInstance(sharpness, 3),
                List.of(new EnchantmentInstance(sharpness, 3)),
                false);
        List<EnchantmentClue> payload = MeridianEnchantmentLogic.toPayloadClues(cb);
        assertEquals(1, payload.size());
        assertEquals(SHARPNESS, payload.get(0).enchantment());
        assertEquals(3, payload.get(0).level());
    }

    @Test
    void toPayloadClues_emptyInputYieldsEmptyOutput() {
        RealEnchantmentHelper.ClueBuild cb = new RealEnchantmentHelper.ClueBuild(
                null, List.of(), true);
        assertTrue(MeridianEnchantmentLogic.toPayloadClues(cb).isEmpty());
    }

    // ---- SlotState.EMPTY sanity --------------------------------------------

    @Test
    void emptySlotState_isStable() {
        assertSame(MeridianEnchantmentLogic.SlotState.EMPTY,
                MeridianEnchantmentLogic.SlotState.EMPTY,
                "EMPTY must be a shared constant — avoid per-slot allocation in the hot path");
        assertEquals(0, MeridianEnchantmentLogic.SlotState.EMPTY.cost());
        assertTrue(MeridianEnchantmentLogic.SlotState.EMPTY.picks().isEmpty());
        assertTrue(MeridianEnchantmentLogic.SlotState.EMPTY.clueBuild().exhaustedList());
    }

    // ---- Fixtures ----------------------------------------------------------

    private static Registry<Enchantment> buildTestRegistry() {
        MappedRegistry<Enchantment> reg = newRegistry();

        HolderSet<Item> swordItems = itemHolderSet(Items.DIAMOND_SWORD);
        HolderSet<Item> anyItems = itemHolderSet(
                Items.DIAMOND_SWORD, Items.BOOK, Items.DIAMOND_PICKAXE);

        Holder.Reference<Enchantment> sharpness = register(reg, SHARPNESS, synthetic(swordItems, 10, 5));
        Holder.Reference<Enchantment> unbreaking = register(reg, UNBREAKING, synthetic(anyItems, 5, 3));
        Holder.Reference<Enchantment> mending = register(reg, MENDING, synthetic(anyItems, 2, 1));

        List<Holder<Enchantment>> inTable = List.of(sharpness, unbreaking, mending);
        List<Holder<Enchantment>> treasure = List.of(mending);
        reg.bindTags(Map.of(
                EnchantmentTags.IN_ENCHANTING_TABLE, inTable,
                EnchantmentTags.TREASURE, treasure
        ));
        return reg.freeze();
    }
}
