package com.rfizzle.meridian.enchanting;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.MeridianRegistry;
import com.rfizzle.meridian.advancement.ModTriggers;
import com.rfizzle.meridian.config.MeridianConfig;
import com.rfizzle.meridian.enchanting.recipe.EnchantingRecipe;
import com.rfizzle.meridian.enchanting.recipe.EnchantingRecipeRegistry;
import com.rfizzle.meridian.mixin.EnchantmentMenuAccessor;
import com.rfizzle.meridian.net.CluesPayload;
import com.rfizzle.meridian.net.CraftingResultEntry;
import com.rfizzle.meridian.net.EnchantmentClue;
import com.rfizzle.meridian.net.StatsPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Stat-driven enchantment table menu. Replaces vanilla {@link EnchantmentMenu} via
 * {@code EnchantmentTableBlockMixin} (T-2.5.2) — this class only replaces the
 * {@link #slotsChanged}/{@link #clickMenuButton} logic, inheriting slot layout, data-slot sync,
 * and the input/lapis container from vanilla.
 *
 * <p>Design references: DESIGN.md §"Table Menu Implementation" and
 * §"Crafting-Result Row". Pure logic lives in {@link MeridianEnchantmentLogic}.
 *
 * <p>The three private fields on {@link EnchantmentMenu} ({@code enchantSlots}, {@code random},
 * {@code enchantmentSeed}) are reached through {@link EnchantmentMenuAccessor} (T-2.5.3); the
 * {@link EnchantmentMenu} instance doubles as the accessor at runtime.
 */
public class MeridianEnchantmentMenu extends EnchantmentMenu {

    private static final int INPUT_SLOT = 0;
    private static final int LAPIS_SLOT = 1;

    private final ContainerLevelAccess access;
    private final Inventory playerInventory;

    /** Last-gathered stats — retained for the client HUD (T-2.5.4) and click-time replay. */
    private StatCollection lastStats = StatCollection.EMPTY;
    /** Slot picks from the most recent {@link #slotsChanged} — consumed on click. */
    private List<List<EnchantmentInstance>> slotPicks = List.of(List.of(), List.of(), List.of());
    /**
     * Stat-gated crafting recipe that matches the current input + shelf totals. When present,
     * slot 2 is overridden with the recipe's XP cost (Zenith's INFUSION pattern). Projected onto
     * the outgoing {@link StatsPayload} for the client tooltip. Reset to {@link Optional#empty()}
     * whenever the input slot is cleared or the stack becomes unenchantable.
     */
    private Optional<RecipeHolder<? extends Recipe<SingleRecipeInput>>> currentRecipe = Optional.empty();
    /**
     * Client mirror of the server's projected {@link CraftingResultEntry}. Populated from the
     * incoming {@link StatsPayload} in {@link #applyClientStats} so {@code MeridianEnchantmentScreen}
     * can render the slot-2 crafting tooltip. The server-side {@link #currentRecipe} stays
     * authoritative; this field exists solely for the client-side render path.
     */
    private Optional<CraftingResultEntry> lastCraftingResult = Optional.empty();
    @SuppressWarnings("unchecked")
    private List<EnchantmentClue>[] clientClues = new List[]{List.of(), List.of(), List.of()};
    private boolean[] clientCluesExhausted = new boolean[]{true, true, true};

    public MeridianEnchantmentMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, ContainerLevelAccess.NULL);
    }

    public MeridianEnchantmentMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access) {
        super(containerId, playerInventory, access);
        this.access = access;
        this.playerInventory = playerInventory;
        for (Slot slot : this.slots) {
            if (slot.container == playerInventory) {
                slot.y += 31;
            }
        }
    }

    /**
     * Routes the open-screen packet to our MenuType so the client builds a
     * {@link MeridianEnchantmentMenu} (and therefore the {@code MeridianEnchantmentScreen} wired via
     * {@code MenuScreens.register}) instead of vanilla's {@link EnchantmentMenu}. Vanilla's
     * constructor stashes {@link MenuType#ENCHANTMENT} in the private {@code menuType} field;
     * overriding {@link #getType()} is cheaper than an accessor mixin to re-set it.
     */
    @Override
    public MenuType<?> getType() {
        return MeridianRegistry.ENCHANTING_TABLE_MENU;
    }

    /** Alias for vanilla's private {@code enchantSlots} — slot 0 and slot 1 share this container. */
    private Container enchantSlots() {
        return ((EnchantmentMenuAccessor) this).meridian$getEnchantSlots();
    }

    /** Alias for vanilla's private {@link RandomSource} — shared across slot recomputes. */
    private RandomSource random() {
        return ((EnchantmentMenuAccessor) this).meridian$getRandom();
    }

    public StatCollection getLastStats() {
        return lastStats;
    }

    /**
     * Server-resolved recipe holder for the slot-2 crafting override, if any. Only ever
     * populated during a {@link #recompute} pass on the server — clients read the projected
     * {@code CraftingResultEntry} off {@link StatsPayload} for the tooltip.
     */
    public Optional<RecipeHolder<? extends Recipe<SingleRecipeInput>>> currentRecipe() {
        return currentRecipe;
    }

    /**
     * Client-side mirror of the outgoing {@link StatsPayload}'s {@code craftingResult}. The menu
     * caches it here on {@link #applyClientStats} so {@code MeridianEnchantmentScreen} can render
     * the slot-2 crafting tooltip.
     */
    public Optional<CraftingResultEntry> lastCraftingResult() {
        return lastCraftingResult;
    }

    /**
     * Testable seam for the recipe lookup run inside {@link #recompute}. Kept package-private so
     * the menu's {@code slotsChanged} integration can be verified without standing up a full
     * {@link Level} — T-5.3.1's acceptance test drives this helper directly. Empty input stacks
     * short-circuit to avoid scanning the recipe manager for a match that will always miss.
     */
    static Optional<RecipeHolder<? extends Recipe<SingleRecipeInput>>> lookupCraftingResult(
            RecipeManager recipes, ItemStack input, StatCollection stats) {
        if (input.isEmpty()) {
            return Optional.empty();
        }
        return EnchantingRecipeRegistry.findMatch(recipes, input, stats);
    }

    /**
     * Projects the cached {@link #currentRecipe} (if any) into the wire-friendly
     * {@link CraftingResultEntry} carried on {@link StatsPayload}. Static + package-private so
     * T-5.3.2's round-trip test can drive the projection without instantiating a menu — the
     * server-only {@code currentRecipe} field would otherwise need a {@link Level} +
     * {@link Player} chain to populate. Holders whose value isn't an {@link EnchantingRecipe}
     * subclass collapse to empty as a safety net for future recipe types that wander into the
     * generic {@link Recipe} bound; today {@link EnchantingRecipeRegistry#findMatch} only ever
     * returns enchanting recipes.
     */
    static Optional<CraftingResultEntry> projectCraftingResult(
            Optional<RecipeHolder<? extends Recipe<SingleRecipeInput>>> recipe) {
        return recipe.flatMap(holder -> {
            if (holder.value() instanceof EnchantingRecipe enchanting) {
                return Optional.of(new CraftingResultEntry(
                        enchanting.getResult().copy(),
                        enchanting.getEffectiveXpCost(),
                        holder.id()));
            }
            return Optional.empty();
        });
    }

    public void applyClientStats(StatsPayload payload) {
        this.lastStats = new StatCollection(
                payload.eterna(),
                payload.quanta(),
                payload.arcana(),
                payload.rectification(),
                payload.clues(),
                payload.maxEterna(),
                new java.util.HashSet<>(payload.blacklist()),
                payload.treasure());
        this.lastCraftingResult = payload.craftingResult();
    }

    public void applyClientClues(int slot, List<EnchantmentClue> clues, boolean exhausted) {
        if (slot >= 0 && slot < 3) {
            this.clientClues[slot] = clues;
            this.clientCluesExhausted[slot] = exhausted;
        }
    }

    public List<EnchantmentClue> getClientClues(int slot) {
        return (slot >= 0 && slot < 3) ? clientClues[slot] : List.of();
    }

    public boolean isClientCluesExhausted(int slot) {
        return (slot >= 0 && slot < 3) && clientCluesExhausted[slot];
    }

    @Override
    public void slotsChanged(Container container) {
        if (container != enchantSlots()) {
            return;
        }
        ItemStack input = container.getItem(INPUT_SLOT);
        if (input.isEmpty()) {
            clearSlotState();
            access.execute((level, pos) -> {
                StatCollection stats = EnchantingStatRegistry.gatherStats(level, pos);
                this.lastStats = stats;
                broadcastStats(stats);
            });
            for (int slot = 0; slot < MeridianEnchantmentLogic.PREVIEW_SLOTS; slot++) {
                broadcastClues(slot, List.of(), true);
            }
            return;
        }
        access.execute((level, pos) -> recompute(level, pos, input));
    }

    private static boolean isEnchantableEnough(ItemStack stack) {
        if (!stack.isEnchanted()) return true;
        ItemEnchantments enchantments = EnchantmentHelper.getEnchantmentsForCrafting(stack);
        return enchantments.entrySet().stream()
                .allMatch(entry -> entry.getKey().is(EnchantmentTags.CURSE));
    }

    /**
     * Applies Zenith-equivalent baseline stats on top of raw shelf contributions. The enchanting
     * table inherently provides quanta, arcana, and a clue independent of surrounding shelves:
     * <ul>
     *   <li>{@code +15} quanta (fixed)</li>
     *   <li>{@code +itemEnchantability / 2} arcana (item-dependent)</li>
     *   <li>{@code +1} clue (fixed)</li>
     * </ul>
     * Without these baselines, recipes ported from Zenith would require unreachable stat thresholds.
     */
    private static StatCollection applyBaselines(StatCollection raw, int itemEnchantability) {
        return new StatCollection(
                raw.eterna(),
                Math.max(0F, Math.min(raw.quanta() + 15F, 100F)),
                Math.max(0F, Math.min(raw.arcana() + itemEnchantability / 2F, 100F)),
                raw.rectification(),
                raw.clues() + 1,
                raw.maxEterna(),
                raw.blacklist(),
                raw.treasureAllowed()
        );
    }

    private void recompute(Level level, BlockPos pos, ItemStack input) {
        StatCollection rawStats = EnchantingStatRegistry.gatherStats(level, pos);
        StatCollection stats = applyBaselines(rawStats, input.getItem().getEnchantmentValue());
        this.lastStats = stats;
        this.currentRecipe = lookupCraftingResult(level.getRecipeManager(), input, stats);

        boolean canEnchant = input.getCount() == 1
                && (input.getItem().isEnchantable(input) || input.is(Items.BOOK))
                && isEnchantableEnough(input);
        if (!canEnchant && currentRecipe.isEmpty()) {
            clearSlotState();
            broadcastStats(stats);
            for (int slot = 0; slot < MeridianEnchantmentLogic.PREVIEW_SLOTS; slot++) {
                broadcastClues(slot, List.of(), true);
            }
            return;
        }

        Registry<Enchantment> registry = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        Player player = playerInventory.player;
        int seed = player.getEnchantmentSeed();
        boolean hasInf = player.hasInfiniteMaterials();
        int lapisCount = enchantSlots().getItem(LAPIS_SLOT).getCount();

        MeridianConfig config = Meridian.getConfig();
        boolean allowTreasureOverride = config != null && config.enchantingTable.allowTreasureWithoutShelf;

        MeridianEnchantmentLogic.SlotState[] states = canEnchant
                ? MeridianEnchantmentLogic.recompute(
                        stats, input, seed, lapisCount, hasInf, allowTreasureOverride, registry, random())
                : new MeridianEnchantmentLogic.SlotState[]{
                        MeridianEnchantmentLogic.SlotState.EMPTY,
                        MeridianEnchantmentLogic.SlotState.EMPTY,
                        MeridianEnchantmentLogic.SlotState.EMPTY};

        if (currentRecipe.isPresent()) {
            int xpCost = currentRecipe
                    .map(h -> h.value() instanceof EnchantingRecipe e ? e.getEffectiveXpCost() : 1)
                    .orElse(1);
            states[MeridianEnchantmentLogic.CRAFTING_SLOT] = new MeridianEnchantmentLogic.SlotState(
                    xpCost, List.of(),
                    new RealEnchantmentHelper.ClueBuild(null, List.of(), true));
        }

        List<List<EnchantmentInstance>> newPicks = new ArrayList<>(MeridianEnchantmentLogic.PREVIEW_SLOTS);
        List<List<EnchantmentClue>> cluePayloads = new ArrayList<>(MeridianEnchantmentLogic.PREVIEW_SLOTS);
        List<Boolean> exhaustedFlags = new ArrayList<>(MeridianEnchantmentLogic.PREVIEW_SLOTS);
        for (int slot = 0; slot < MeridianEnchantmentLogic.PREVIEW_SLOTS; slot++) {
            MeridianEnchantmentLogic.SlotState s = states[slot];
            costs[slot] = s.cost();
            newPicks.add(s.picks());
            EnchantmentInstance primary = s.clueBuild().primary();
            if (primary != null) {
                enchantClue[slot] = MeridianEnchantmentLogic.idForHolder(registry, primary.enchantment);
                levelClue[slot] = primary.level;
            } else if (slot == MeridianEnchantmentLogic.CRAFTING_SLOT && currentRecipe.isPresent()) {
                enchantClue[slot] = 0;
                levelClue[slot] = 1;
            } else {
                enchantClue[slot] = -1;
                levelClue[slot] = -1;
            }
            cluePayloads.add(MeridianEnchantmentLogic.toPayloadClues(s.clueBuild()));
            exhaustedFlags.add(s.clueBuild().exhaustedList());
        }
        this.slotPicks = newPicks;

        broadcastStats(stats);
        for (int slot = 0; slot < MeridianEnchantmentLogic.PREVIEW_SLOTS; slot++) {
            broadcastClues(slot, cluePayloads.get(slot), exhaustedFlags.get(slot));
        }
    }

    private void clearSlotState() {
        for (int i = 0; i < MeridianEnchantmentLogic.PREVIEW_SLOTS; i++) {
            costs[i] = 0;
            enchantClue[i] = -1;
            levelClue[i] = -1;
        }
        this.slotPicks = List.of(List.of(), List.of(), List.of());
        this.lastStats = StatCollection.EMPTY;
        this.currentRecipe = Optional.empty();
    }

    private void broadcastStats(StatCollection stats) {
        if (!(playerInventory.player instanceof ServerPlayer sp)) {
            return;
        }
        List<ResourceKey<Enchantment>> blacklist = List.copyOf(stats.blacklist());
        StatsPayload payload = new StatsPayload(
                stats.eterna(), stats.quanta(), stats.arcana(), stats.rectification(),
                stats.clues(), stats.maxEterna(), blacklist, stats.treasureAllowed(),
                projectCraftingResult(currentRecipe));
        ServerPlayNetworking.send(sp, payload);
    }

    private void broadcastClues(int slot, List<EnchantmentClue> clues, boolean exhausted) {
        if (!(playerInventory.player instanceof ServerPlayer sp)) {
            return;
        }
        ServerPlayNetworking.send(sp, new CluesPayload(slot, clues, exhausted));
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == MeridianEnchantmentLogic.CRAFTING_SLOT && currentRecipe.isPresent()) {
            return handleCraftingClick(player);
        }

        Container enchantSlots = enchantSlots();
        ItemStack input = enchantSlots.getItem(INPUT_SLOT);
        ItemStack lapis = enchantSlots.getItem(LAPIS_SLOT);
        int cost = (id >= 0 && id < costs.length) ? costs[id] : 0;
        boolean hasInf = player.hasInfiniteMaterials();

        MeridianEnchantmentLogic.ClickAttempt attempt = MeridianEnchantmentLogic.validateClick(
                id, cost, input.isEmpty(), lapis.isEmpty() ? 0 : lapis.getCount(),
                player.experienceLevel, hasInf);
        if (!attempt.success()) {
            return false;
        }

        List<EnchantmentInstance> picks = id < slotPicks.size() ? slotPicks.get(id) : List.of();
        if (picks.isEmpty()) {
            return false;
        }

        access.execute((level, pos) -> applyEnchantment(level, pos, player, id, picks));
        return true;
    }

    private void applyEnchantment(
            Level level, BlockPos pos, Player player, int id, List<EnchantmentInstance> picks) {
        Container enchantSlots = enchantSlots();
        ItemStack input = enchantSlots.getItem(INPUT_SLOT);
        ItemStack lapis = enchantSlots.getItem(LAPIS_SLOT);
        MeridianEnchantmentLogic.EnchantOutcome outcome =
                MeridianEnchantmentLogic.applyPicks(input, id, picks);

        ItemStack result = outcome.resultStack();
        if (input.is(Items.BOOK)) {
            enchantSlots.setItem(INPUT_SLOT, result);
        } else {
            for (EnchantmentInstance inst : picks) {
                input.enchant(inst.enchantment, inst.level);
            }
            result = input;
        }

        player.onEnchantmentPerformed(result, outcome.xpLevelsConsumed());
        lapis.consume(outcome.lapisConsumed(), player);
        if (lapis.isEmpty()) {
            enchantSlots.setItem(LAPIS_SLOT, ItemStack.EMPTY);
        }

        player.awardStat(Stats.ENCHANT_ITEM);
        if (player instanceof ServerPlayer sp) {
            CriteriaTriggers.ENCHANTED_ITEM.trigger(sp, result, outcome.xpLevelsConsumed());
            ModTriggers.ENCHANTED_AT_TABLE.trigger(sp, result, outcome.xpLevelsConsumed(),
                    lastStats.eterna(), lastStats.quanta(), lastStats.arcana(), lastStats.rectification());
        }
        enchantSlots.setChanged();
        level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F,
                level.random.nextFloat() * 0.1F + 0.9F);
    }

    /**
     * Handles a click on slot 2 when a stat-gated crafting recipe matches (Zenith's INFUSION
     * pattern). Validates lapis (3) and XP against the recipe's cost, then delegates to
     * {@link #applyCraftingRecipe}. The result replaces the input in the enchanting slot.
     */
    private boolean handleCraftingClick(Player player) {
        if (currentRecipe.isEmpty()) return false;

        Container enchantSlots = enchantSlots();
        ItemStack input = enchantSlots.getItem(INPUT_SLOT);
        ItemStack lapis = enchantSlots.getItem(LAPIS_SLOT);
        boolean hasInf = player.hasInfiniteMaterials();
        int lapisRequired = MeridianEnchantmentLogic.CRAFTING_SLOT + 1;
        int xpCost = costs[MeridianEnchantmentLogic.CRAFTING_SLOT];

        if (input.isEmpty()) return false;
        if (!hasInf && (lapis.isEmpty() || lapis.getCount() < lapisRequired)) return false;
        if (!hasInf && player.experienceLevel < xpCost) return false;

        RecipeHolder<? extends Recipe<SingleRecipeInput>> holder = currentRecipe.get();
        access.execute((level, pos) -> applyCraftingRecipe(level, pos, player, holder, xpCost));
        return true;
    }

    /**
     * Server-side application of the cached crafting recipe. Mirrors Zenith's slot-2 INFUSION
     * flow: the recipe's {@link Recipe#assemble} result replaces the input in the enchanting slot,
     * lapis is consumed (3 for slot 2), XP is deducted, and the shelf scan re-runs via
     * {@link Container#setChanged()}.
     */
    private void applyCraftingRecipe(
            Level level, BlockPos pos, Player player,
            RecipeHolder<? extends Recipe<SingleRecipeInput>> holder, int xpCost) {
        Container enchantSlots = enchantSlots();
        ItemStack input = enchantSlots.getItem(INPUT_SLOT);
        if (input.isEmpty()) return;

        ItemStack result = holder.value().assemble(new SingleRecipeInput(input), level.registryAccess());

        if (input.getCount() > 1) {
            ItemStack excess = input.copyWithCount(input.getCount() - 1);
            if (!player.getInventory().add(excess)) {
                player.drop(excess, false);
            }
        }

        enchantSlots.setItem(INPUT_SLOT, result);

        if (!player.hasInfiniteMaterials()) {
            int lapisRequired = MeridianEnchantmentLogic.CRAFTING_SLOT + 1;
            ItemStack lapis = enchantSlots.getItem(LAPIS_SLOT);
            lapis.shrink(lapisRequired);
            if (lapis.isEmpty()) {
                enchantSlots.setItem(LAPIS_SLOT, ItemStack.EMPTY);
            }
        }

        player.onEnchantmentPerformed(result, 0);
        if (!player.hasInfiniteMaterials() && xpCost > 0) {
            player.giveExperienceLevels(-xpCost);
        }

        player.awardStat(Stats.ENCHANT_ITEM);
        if (player instanceof ServerPlayer sp) {
            CriteriaTriggers.ENCHANTED_ITEM.trigger(sp, result, xpCost);
            ModTriggers.ENCHANTED_AT_TABLE.trigger(sp, result, xpCost,
                    lastStats.eterna(), lastStats.quanta(), lastStats.arcana(), lastStats.rectification());
        }

        enchantSlots.setChanged();
        level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F,
                level.random.nextFloat() * 0.1F + 0.9F);
    }
}
