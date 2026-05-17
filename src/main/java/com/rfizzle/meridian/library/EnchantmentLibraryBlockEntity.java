package com.rfizzle.meridian.library;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.config.MeridianConfig;
import com.rfizzle.meridian.enchanting.EnchantmentInfoRegistry;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.Set;

/**
 * Shared storage engine for the Basic and Ender library block entities. Ports Zenith's
 * {@code EnchLibraryTile} point-pool model onto 1.21.1's {@code ResourceKey<Enchantment>}-keyed
 * enchantment registry.
 *
 * <p>Each library tracks two parallel maps per enchantment:
 * <ul>
 *   <li>{@link #points} — accumulated point pool where {@code points(level) = 2^(level-1)}.</li>
 *   <li>{@link #maxLevels} — highest single-book level ever deposited, clamped to the tier's
 *       {@link #maxLevel}. Both caps (points and max-level) are deliberate gates against
 *       "grind commons, extract rares" strategies.</li>
 * </ul>
 *
 * <p>Subclasses (Basic/Ender in T-4.3.2) supply a {@link BlockEntityType} and the tier-specific
 * {@code maxLevel}. NBT persistence lands in T-4.3.4; client sync lands in T-4.3.5. The menu
 * (T-4.4.2) calls {@link #depositBook}, {@link #canExtract}, and {@link #extract} against this
 * surface and handles the stack-side mutations separately — keeping the stored-state math pure
 * lets it be unit-tested without a live world.
 */
public abstract class EnchantmentLibraryBlockEntity extends BlockEntity {

    /** NBT key for the per-enchant accumulated point pool. Mirrors Zenith's schema. */
    public static final String TAG_POINTS = "Points";
    /** NBT key for the per-enchant highest-deposited-level map. Mirrors Zenith's schema. */
    public static final String TAG_LEVELS = "Levels";

    /**
     * Point pool per enchantment. Values clamp to {@link #maxPoints} on deposit; overflow is
     * destroyed silently (DESIGN "silent void" — required for safe hopper automation).
     */
    protected final Object2IntMap<ResourceKey<Enchantment>> points = new Object2IntOpenHashMap<>();

    /**
     * Highest individual book level ever deposited per enchantment, clamped to {@link #maxLevel}.
     * Depositing 32 768 Sharpness-I books gives the point budget for Sharpness V but leaves
     * {@code maxLevels[Sharpness] = 1} — extraction is gated by both maps.
     */
    protected final Object2IntMap<ResourceKey<Enchantment>> maxLevels = new Object2IntOpenHashMap<>();

    /** Tier cap: 16 for Basic, 31 for Ender. Code constant, not config — see DESIGN. */
    protected final int maxLevel;

    /** {@code points(maxLevel)} = per-enchant point ceiling (32 768 Basic, 2^30 Ender). */
    protected final int maxPoints;

    /**
     * Shared {@code Storage<ItemVariant>} adapter handed to {@code ItemStorage.SIDED} so hoppers
     * and pipes can push enchanted books into the pool without opening the menu. Owned by the BE
     * so the lookup's {@code (be, side) -> be.getStorageAdapter()} binding resolves to a stable
     * instance across queries. One per BE; the adapter itself carries no state, so recreating it
     * would be safe but wasteful.
     */
    protected final LibraryStorageAdapter storageAdapter = new LibraryStorageAdapter(this);

    /**
     * World-tick at which the last {@link LibraryStorageAdapter} insert ran against this library,
     * used by the {@code config.library.ioRateLimitTicks} throttle. {@link Long#MIN_VALUE} is the
     * sentinel for "never inserted" so the first hopper push always goes through regardless of how
     * far the world has ticked. Snapshotted by {@link LibraryStorageAdapter} so an aborted Fabric
     * transaction rolls this back alongside the point pool — otherwise a rolled-back insert would
     * still "burn" a throttle slot and block subsequent attempts.
     *
     * <p>Not persisted to NBT: a server restart resets the sentinel, which is the right behaviour
     * (the throttle is meant to dampen in-session auto-farms, not survive reboots).
     */
    long lastInsertTick = Long.MIN_VALUE;

    /**
     * Open menus listening for pool mutations. Mirrors Zenith's {@code activeContainers} pattern so
     * a screen can repaint its row list without polling — every mutation path eventually reaches
     * {@link #notifyListeners()}, which fires {@link EnchantmentLibraryMenu#onChanged()} on each
     * registered menu. Server-side hits the set on {@link #setChanged()} (debit/deposit paths);
     * client-side hits it on {@link #loadAdditional} when the chunk-update packet lands.
     *
     * <p>Identity-keyed via {@link HashSet} (menus do not override equals/hashCode), so the same
     * menu cannot register twice and {@link #removeListener} is O(1).
     */
    private final Set<EnchantmentLibraryMenu> activeContainers = new HashSet<>();

    protected EnchantmentLibraryBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int maxLevel) {
        super(type, pos, state);
        this.maxLevel = maxLevel;
        this.maxPoints = points(maxLevel);
    }

    /**
     * Absorb every stored enchantment on an enchanted book into the point pool. Non-books and
     * empty stacks are rejected without side effects. Curses are accepted — a Curse-of-Vanishing
     * book is legitimate pool fuel for a player who wants to dispense curses later. Callers are
     * responsible for consuming the book stack themselves; this method does not mutate the input.
     *
     * <p>Point accumulation is saturating: the sum is clamped to {@link #maxPoints} via
     * {@link Math#min}, and any int overflow from extreme Ender-tier totals falls through to the
     * same ceiling (DESIGN silent-void semantics).
     */
    public void depositBook(ItemStack book) {
        if (depositBookSilent(book)) {
            setChanged();
        }
    }

    /**
     * Adapter-facing variant of {@link #depositBook} that performs the same pool/level math but
     * skips the trailing {@link #setChanged()} fire. Returned to the caller (the {@link
     * LibraryStorageAdapter}'s {@code SnapshotParticipant} hook) so a single
     * {@code setChanged()} can be deferred to {@code onFinalCommit} — calling it eagerly inside
     * the transaction would dispatch the chunk-update packet and mark persistent state dirty
     * before the transaction had a chance to commit, defeating the rollback contract.
     *
     * <p>Returns {@code true} iff the deposit mutated state; the adapter ORs these together to
     * decide whether {@code onFinalCommit} needs to fire {@code setChanged}.
     */
    boolean depositBookSilent(ItemStack book) {
        if (book == null || book.isEmpty() || !book.is(Items.ENCHANTED_BOOK)) return false;
        ItemEnchantments stored = book.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (stored.isEmpty()) return false;
        boolean changed = false;
        for (Holder<Enchantment> holder : stored.keySet()) {
            ResourceKey<Enchantment> key = holder.unwrapKey().orElse(null);
            if (key == null) continue;
            if (!EnchantmentInfoRegistry.getInfo(holder).enabled()) continue;
            int level = stored.getLevel(holder);
            if (level <= 0) continue;
            int current = this.points.getInt(key);
            int sum = current + points(level);
            int newPoints = Math.min(this.maxPoints, sum);
            // int overflow safety — adding two positive ints can wrap negative at Ender tier.
            if (sum < 0) newPoints = this.maxPoints;
            this.points.put(key, newPoints);

            int existingMax = this.maxLevels.getInt(key);
            int newMax = Math.min(this.maxLevel, Math.max(existingMax, level));
            this.maxLevels.put(key, newMax);
            changed = true;
        }
        return changed;
    }

    /**
     * Gate the extract-slot button click. Returns {@code true} iff the target level is above the
     * current level on the book, the library has ever seen a book at that level for this
     * enchantment, and the pool has enough points to cover the delta.
     */
    public boolean canExtract(ResourceKey<Enchantment> key, int target, int currentLevel) {
        if (target <= 0 || target <= currentLevel) return false;
        if (target > this.maxLevel) return false;
        if (this.maxLevels.getInt(key) < target) return false;
        int cost = points(target) - points(currentLevel);
        return this.points.getInt(key) >= cost;
    }

    /**
     * Debit the extraction cost from the pool. Returns {@code true} on a successful debit,
     * {@code false} if {@link #canExtract} would have declined. Stack-side mutation (setting the
     * enchantment onto the book in slot 1) is the menu's responsibility (T-4.4.2) — this method
     * only manages the point map, keeping pure state-math in the BE.
     */
    public boolean extract(ResourceKey<Enchantment> key, int target, int currentLevel) {
        if (!canExtract(key, target, currentLevel)) return false;
        int cost = points(target) - points(currentLevel);
        int remaining = Math.max(0, this.points.getInt(key) - cost);
        this.points.put(key, remaining);
        setChanged();
        return true;
    }

    /**
     * Writes {@link #points} and {@link #maxLevels} as two sibling compound tags ({@link #TAG_POINTS}
     * and {@link #TAG_LEVELS}) keyed by the enchantment's {@link ResourceLocation#toString()} —
     * matching Zenith's on-disk schema so an operator migrating from a Zenith save reads cleanly.
     *
     * <p>The registries parameter is unused for serialization (keys are written as plain strings
     * independent of the live registry) but is forwarded to {@code super} so vanilla-owned NBT on
     * subclasses still round-trips correctly.
     */
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        CompoundTag pointsTag = new CompoundTag();
        for (Object2IntMap.Entry<ResourceKey<Enchantment>> entry : this.points.object2IntEntrySet()) {
            pointsTag.putInt(entry.getKey().location().toString(), entry.getIntValue());
        }
        tag.put(TAG_POINTS, pointsTag);

        CompoundTag levelsTag = new CompoundTag();
        for (Object2IntMap.Entry<ResourceKey<Enchantment>> entry : this.maxLevels.object2IntEntrySet()) {
            levelsTag.putInt(entry.getKey().location().toString(), entry.getIntValue());
        }
        tag.put(TAG_LEVELS, levelsTag);
    }

    /**
     * Reads the paired {@code Points} / {@code Levels} compound tags, lazily resolving each key
     * against {@code registries.lookupOrThrow(Registries.ENCHANTMENT)}. Enchantments are a dynamic
     * registry in 1.21.1 — a datapack uninstall can leave stored keys unresolvable. Those entries
     * are dropped with a {@link org.slf4j.Logger#warn} rather than crashing the BE; the remainder
     * of the map survives intact so one stale key never eats the whole library.
     *
     * <p>Malformed ids (parse failure on {@link ResourceLocation#tryParse}) are also dropped —
     * same warn-and-continue contract. No schema version field is serialized in MVP; the maps
     * self-describe through their keys.
     */
    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.points.clear();
        this.maxLevels.clear();
        HolderLookup.RegistryLookup<Enchantment> lookup = registries.lookupOrThrow(Registries.ENCHANTMENT);
        readResolvedMap(tag.getCompound(TAG_POINTS), lookup, this.points);
        readResolvedMap(tag.getCompound(TAG_LEVELS), lookup, this.maxLevels);
        // Client-side packet receipt path lands here via the default onDataPacket → loadAdditional
        // dispatch. Server-side world load also lands here, but the listener set is empty before
        // any menu opens, so the notify is a cheap no-op then.
        notifyListeners();
    }

    /**
     * Shared reader for both sibling maps. Factored out so the resolution contract (invalid id →
     * warn + drop, unresolved key → warn + drop, live key → put) is expressed in one place.
     */
    private static void readResolvedMap(CompoundTag source,
                                        HolderLookup.RegistryLookup<Enchantment> lookup,
                                        Object2IntMap<ResourceKey<Enchantment>> sink) {
        for (String raw : source.getAllKeys()) {
            ResourceLocation id = ResourceLocation.tryParse(raw);
            if (id == null) {
                Meridian.LOGGER.warn("Dropping malformed library NBT key {}", raw);
                continue;
            }
            ResourceKey<Enchantment> key = ResourceKey.create(Registries.ENCHANTMENT, id);
            if (lookup.get(key).isEmpty()) {
                Meridian.LOGGER.warn("Dropping unresolved enchantment {} from library NBT", id);
                continue;
            }
            sink.put(key, source.getInt(raw));
        }
    }

    /**
     * Server → client sync packet. Returns the standard
     * {@link ClientboundBlockEntityDataPacket}, which on the receiving side calls
     * {@link #loadAdditional} with the body returned by {@link #getUpdateTag} — so the same
     * point/level serialization used for save() also drives the live tracker push.
     */
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /**
     * Client-bound payload for the chunk-load and per-mutation pushes. Delegates to
     * {@link #saveCustomOnly} so both maps land on the wire under the same {@link #TAG_POINTS} /
     * {@link #TAG_LEVELS} schema as the on-disk state — DESIGN: "full map resend on any mutation",
     * incremental sync explicitly deferred since the maximum payload (a few hundred ints) is small.
     */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveCustomOnly(registries);
    }

    /**
     * Centralized server → client push: {@link #depositBook} and {@link #extract} both end in
     * {@link #setChanged()}, so wiring the resend here covers every mutation site without each
     * caller having to remember to dispatch. The {@link Block#UPDATE_CLIENTS} flag asks the chunk
     * tracker to forward {@link #getUpdatePacket()} to every player tracking this block; the tag
     * built by {@link #getUpdateTag} carries the full point/level state per DESIGN's "full resend"
     * contract.
     *
     * <p>Guarded on {@code level != null && !level.isClientSide()} so client-side mutations and
     * unit-test instances (no live world) skip the dispatch silently — the super-call still marks
     * the chunk dirty so persistence is unaffected.
     */
    @Override
    public void setChanged() {
        super.setChanged();
        notifyListeners();
        if (this.level != null && !this.level.isClientSide()) {
            BlockState state = this.getBlockState();
            this.level.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    /**
     * Register {@code menu} as a listener — its {@link EnchantmentLibraryMenu#onChanged()} will be
     * invoked on every subsequent pool mutation. Idempotent (the underlying {@link HashSet} drops
     * the duplicate). Called from the menu's constructor; pairs with {@link #removeListener}.
     */
    public void addListener(EnchantmentLibraryMenu menu) {
        this.activeContainers.add(menu);
    }

    /**
     * Drop {@code menu} from the listener set. Called from {@link EnchantmentLibraryMenu#removed}
     * so closed screens stop receiving callbacks even if the BE outlives the menu (which it
     * always does on a long-lived block).
     */
    public void removeListener(EnchantmentLibraryMenu menu) {
        this.activeContainers.remove(menu);
    }

    /**
     * Fire {@link EnchantmentLibraryMenu#onChanged()} on every registered listener. Called from
     * {@link #setChanged()} (covers server-side {@link #depositBook} / {@link #extract} paths) and
     * from {@link #loadAdditional} (covers the client-side packet receipt path — the BE's update
     * tag flows through {@code loadAdditional} on the receiving side).
     *
     * <p>Tolerates an empty set without iteration overhead; safe to call from server-load before
     * any menu is open.
     */
    protected void notifyListeners() {
        if (this.activeContainers.isEmpty()) return;
        for (EnchantmentLibraryMenu menu : this.activeContainers) {
            menu.onChanged();
        }
    }

    /** Test-only view of the listener set so the unit test can assert add/remove without leaks. */
    Set<EnchantmentLibraryMenu> activeContainersForTest() {
        return this.activeContainers;
    }

    /** {@code 2^(level - 1)}; non-positive levels map to zero. */
    public static int points(int level) {
        if (level <= 0) return 0;
        return 1 << (level - 1);
    }

    /**
     * Shift-click resolver. Given a raw {@code points} pool and the current level on the target
     * book, returns the highest level a single shift-click could afford, per DESIGN:
     * {@code 1 + log₂(points + points(curLvl))}. Callers must still clamp the result against
     * {@code maxLevels[e]} and the tier cap — this helper intentionally does not.
     *
     * <p>The pool and the curLvl contribution are summed as {@code long} so Ender-tier totals
     * (points(31) = 2^30) cannot wrap negative before the log. Zero or negative budgets return 0.
     */
    public static int maxLevelAffordable(int points, int curLvl) {
        long budget = (long) Math.max(0, points) + (long) points(curLvl);
        if (budget <= 0L) return 0;
        return 1 + (int) (Math.log(budget) / Math.log(2));
    }

    /** Tier max level (read-only view). */
    public int getMaxLevel() {
        return this.maxLevel;
    }

    /** Tier max points per enchantment (read-only view). */
    public int getMaxPoints() {
        return this.maxPoints;
    }

    /** Live point pool. Menus iterate this to paint the extract list. */
    public Object2IntMap<ResourceKey<Enchantment>> getPoints() {
        return this.points;
    }

    /** Live max-level map. Menus clamp extract targets against this. */
    public Object2IntMap<ResourceKey<Enchantment>> getMaxLevels() {
        return this.maxLevels;
    }

    /** Hopper-side insert-only storage view — see {@link LibraryStorageAdapter}. */
    public LibraryStorageAdapter getStorageAdapter() {
        return this.storageAdapter;
    }

    /**
     * Current world-tick used by the hopper throttle. Overridable so unit tests can drive the
     * rate-limit clock without attaching a live {@code Level} — production returns
     * {@code level.getGameTime()}, test fixtures without a level return {@code 0} so they can
     * advance the clock via a subclass override.
     */
    protected long currentGameTime() {
        return this.level != null ? this.level.getGameTime() : 0L;
    }

    /**
     * Hopper-insert cooldown window in ticks — {@code 0} disables throttling entirely. Production
     * reads {@code config.library.ioRateLimitTicks}; when the config is not yet loaded (e.g. unit
     * tests constructing a BE before {@code onInitialize}) we return {@code 0} so the throttle is
     * off by default. Overridable in test subclasses to inject a specific window.
     */
    protected int rateLimitTicks() {
        MeridianConfig config = Meridian.getConfig();
        return config != null ? config.library.ioRateLimitTicks : 0;
    }
}
