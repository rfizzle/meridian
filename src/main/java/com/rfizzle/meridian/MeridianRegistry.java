package com.rfizzle.meridian;

import com.rfizzle.meridian.anvil.PrismaticWebItem;
import com.rfizzle.meridian.enchanting.MeridianEnchantmentMenu;
import com.rfizzle.meridian.event.WardenPoolCondition;
import com.rfizzle.meridian.item.InfusedBreathItem;
import com.rfizzle.meridian.item.WardenTendrilItem;
import com.rfizzle.meridian.library.BasicLibraryBlockEntity;
import com.rfizzle.meridian.library.EnchantmentLibraryBlock;
import com.rfizzle.meridian.library.EnchantmentLibraryMenu;
import com.rfizzle.meridian.library.EnderLibraryBlockEntity;
import com.rfizzle.meridian.shelf.FilteringShelfBlock;
import com.rfizzle.meridian.shelf.FilteringShelfBlockEntity;
import com.rfizzle.meridian.shelf.MeridianShelves;
import com.rfizzle.meridian.shelf.TreasureShelfBlock;
import com.rfizzle.meridian.shelf.TreasureShelfBlockEntity;
import com.rfizzle.meridian.tome.ExtractionTomeItem;
import com.rfizzle.meridian.tome.ImprovedScrapTomeItem;
import com.rfizzle.meridian.tome.ScrapTomeItem;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Central registry for blocks, items, block entities, menus, and particles shipped by
 * Meridian. {@link #register()} must run during {@code onInitialize} so entries land
 * before {@code BuiltInRegistries} freezes.
 *
 * <p>Shelves, library blocks, and any other content registered through
 * {@link #registerBlock(String, Block, Item.Properties)} is also recorded in {@link #BLOCKS}
 * in insertion order, so datagen and integration compat modules can walk the full set without
 * re-parsing the registry.
 */
public final class MeridianRegistry {

    /**
     * Insertion-ordered view of every block registered via
     * {@link #registerBlock(String, Block, Item.Properties)}. Consumers may iterate this for
     * datagen or compat but must not mutate the map directly.
     */
    public static final Map<ResourceLocation, Block> BLOCKS = new LinkedHashMap<>();

    /**
     * Every standalone item (non-BlockItem) registered via {@link #registerItem(String, Item)},
     * in insertion order. Used by the creative tab builder to enumerate all mod items.
     */
    public static final List<Item> STANDALONE_ITEMS = new ArrayList<>();

    public static final MenuType<MeridianEnchantmentMenu> ENCHANTING_TABLE_MENU =
            new MenuType<>(MeridianEnchantmentMenu::new, FeatureFlags.VANILLA_SET);

    /**
     * Both library tiers share this {@link MenuType}. Differentiation is in the BE that
     * {@link EnchantmentLibraryMenu} reads/writes — the menu chrome, slot layout, and click
     * encoding are identical across Basic and Ender, so a single registered type avoids
     * duplicating the open-screen plumbing twice.
     *
     * <p>Backed by Fabric's {@link ExtendedScreenHandlerType} so the {@link BlockPos} the player
     * right-clicked syncs to the client constructor — vanilla {@link MenuType} can only carry the
     * sync ID + inventory.
     */
    public static final ExtendedScreenHandlerType<EnchantmentLibraryMenu, BlockPos> LIBRARY_MENU =
            new ExtendedScreenHandlerType<>(EnchantmentLibraryMenu::new, BlockPos.STREAM_CODEC);

    /**
     * Filtering-shelf block. Wood-tier base contribution lives in
     * {@code data/meridian/enchanting_stats/filtering_shelf.json}; the entity layers
     * the per-shelf blacklist on top via {@link FilteringShelfBlockEntity#getEnchantmentBlacklist}.
     */
    public static final FilteringShelfBlock FILTERING_SHELF =
            new FilteringShelfBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .sound(SoundType.WOOD)
                    .strength(0.75F));

    /** Block-entity type backing {@link #FILTERING_SHELF}. Registered alongside the block. */
    public static final BlockEntityType<FilteringShelfBlockEntity> FILTERING_SHELF_BE =
            BlockEntityType.Builder.of(FilteringShelfBlockEntity::new, FILTERING_SHELF).build(null);

    /**
     * Treasure-shelf block. Stat-scan effect is "treasure enchantments unlocked" — surfaced
     * through the BE's {@link com.rfizzle.meridian.enchanting.TreasureFlagSource}
     * marker. Stone-tier physical properties matched to Zenith
     * ({@code MapColor.COLOR_BLACK}, {@code SoundType.STONE}, {@code 1.75F}, requires correct
     * tool) so the block feels like every other stone-tier shelf to the player.
     */
    public static final TreasureShelfBlock TREASURE_SHELF =
            new TreasureShelfBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .sound(SoundType.STONE)
                    .strength(1.75F)
                    .requiresCorrectToolForDrops());

    /** Block-entity type backing {@link #TREASURE_SHELF}. */
    public static final BlockEntityType<TreasureShelfBlockEntity> TREASURE_SHELF_BE =
            BlockEntityType.Builder.of(TreasureShelfBlockEntity::new, TREASURE_SHELF).build(null);

    /**
     * Curse-stripping anvil ingredient. The {@code PrismaticWebHandler} lookup in T-4.1.4 keys
     * off this exact {@link Item} instance, so it is exposed on the registry for handler wiring
     * and for datagen / compat consumers to reference without re-resolving by id.
     */
    public static final PrismaticWebItem PRISMATIC_WEB =
            new PrismaticWebItem(new Item.Properties());

    /**
     * Specialty material gated behind the {@code meridian:enchanting} table-crafting
     * recipe on {@code minecraft:dragon_breath}. Tier-3 themed shelves and the Basic Library
     * all consume this item in their vanilla-shape crafts, so progression funnels players back
     * to the stat-driven table once they need an endgame shelf. {@link Rarity#EPIC} matches
     * Zenith's presentation — the purple name distinguishes it at a glance in inventories.
     */
    public static final InfusedBreathItem INFUSED_BREATH =
            new InfusedBreathItem(new Item.Properties().rarity(Rarity.EPIC));

    /**
     * Warden-drop specialty material. Required to craft the two tier-3 sculk shelves
     * ({@code echoing_sculkshelf}, {@code soul_touched_sculkshelf}). Rarity is left at the
     * default — tendrils are a routine Warden reward, not an epic find, so the inventory name
     * colouring matches other bulk crafting ingredients. Drop distribution is wired by
     * {@code WardenLootHandler} (T-5.4.3), not by item-side state.
     */
    public static final WardenTendrilItem WARDEN_TENDRIL =
            new WardenTendrilItem(new Item.Properties());

    /**
     * Scrap tome — consumed at the anvil to salvage one random enchantment onto a fresh
     * enchanted book. Single-stack because the anvil interaction is one tome per use, and
     * a higher stack cap would let players misread "how many salvages" from the slot count.
     * No durability: the item is destroyed on use by
     * {@code ScrapTomeHandler} (S-5.2), not ticked down per use.
     */
    public static final ScrapTomeItem SCRAP_TOME =
            new ScrapTomeItem(new Item.Properties().stacksTo(16));

    /**
     * Improved Scrap tome — same hostile-to-item workflow as {@link #SCRAP_TOME} but the output
     * book carries every enchantment. Same {@code stacksTo(1)} / no-durability contract applies
     * for the same reasons.
     */
    public static final ImprovedScrapTomeItem IMPROVED_SCRAP_TOME =
            new ImprovedScrapTomeItem(new Item.Properties().stacksTo(16));

    /**
     * Extraction tome — most expensive tier; preserves the source item (unenchanted, damaged)
     * and produces a book with every enchantment. Single-stack, no durability: the anvil
     * fuel-slot repair path handled in {@code ExtractionTomeFuelSlotRepairHandler} (S-5.2.4)
     * consumes the whole tome and is handler-side, not item-data state.
     */
    public static final ExtractionTomeItem EXTRACTION_TOME =
            new ExtractionTomeItem(new Item.Properties().stacksTo(16));

    /**
     * Tier-1 enchantment-library block. Shares {@link EnchantmentLibraryBlock} with the ender
     * variant; the only differentiation is the {@code BlockEntitySupplier} — here bound to
     * {@link BasicLibraryBlockEntity#BasicLibraryBlockEntity(BlockPos, BlockState)}. Ender-chest-
     * tier physical properties ({@code COLOR_RED}, strength {@code 5.0F} / resistance {@code
     * 1200.0F}) mirror Zenith so the block feels consistent with the storage it caps.
     */
    public static final EnchantmentLibraryBlock BASIC_LIBRARY =
            new EnchantmentLibraryBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_RED)
                            .strength(5.0F, 1200.0F)
                            .requiresCorrectToolForDrops(),
                    BasicLibraryBlockEntity::new);

    /**
     * Tier-2 enchantment-library block. Unreachable through a vanilla-shape recipe — the upgrade
     * path is the {@code meridian:keep_nbt_enchanting} table-crafting recipe that
     * preserves {@code Points}/{@code Levels} NBT from the consumed Basic Library (DESIGN §
     * Enchantment-Table Crafting). Physical properties match the Basic tier so operators cannot
     * tell the tiers apart by pick time / blast resistance.
     */
    public static final EnchantmentLibraryBlock ENDER_LIBRARY =
            new EnchantmentLibraryBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_RED)
                            .strength(5.0F, 1200.0F)
                            .requiresCorrectToolForDrops(),
                    EnderLibraryBlockEntity::new);

    /**
     * Block-entity type backing the tier-1 {@link BasicLibraryBlockEntity}. Bound to
     * {@link #BASIC_LIBRARY} so vanilla's {@code validateBlockState} check at chunk-load doesn't
     * warn on a type/block mismatch.
     */
    public static final BlockEntityType<BasicLibraryBlockEntity> BASIC_LIBRARY_BE =
            BlockEntityType.Builder.of(BasicLibraryBlockEntity::new, BASIC_LIBRARY).build(null);

    /**
     * Block-entity type backing the tier-2 {@link EnderLibraryBlockEntity}. Same binding contract
     * as {@link #BASIC_LIBRARY_BE} — paired with {@link #ENDER_LIBRARY}.
     */
    public static final BlockEntityType<EnderLibraryBlockEntity> ENDER_LIBRARY_BE =
            BlockEntityType.Builder.of(EnderLibraryBlockEntity::new, ENDER_LIBRARY).build(null);

    /**
     * Config-driven loot condition used by the Warden tendril pools. Exposed as a field so the
     * registration helper can thread it through the same {@code register<X>} pattern as every
     * other entry — and so a reflective "is this type registered?" smoke test can pin the
     * reference without going through the class initializer of {@link WardenPoolCondition}.
     */
    public static final LootItemConditionType WARDEN_POOL_CONDITION = WardenPoolCondition.TYPE;

    private static boolean registered = false;

    private MeridianRegistry() {
    }

    public static void register() {
        if (registered) return;
        registered = true;

        registerMenuType("enchanting_table", ENCHANTING_TABLE_MENU);
        registerMenuType("library", LIBRARY_MENU);
        MeridianShelves.register();
        registerBlock("filtering_shelf", FILTERING_SHELF, new Item.Properties());
        registerBlockEntityType("filtering_shelf", FILTERING_SHELF_BE);
        registerBlock("treasure_shelf", TREASURE_SHELF, new Item.Properties());
        registerBlockEntityType("treasure_shelf", TREASURE_SHELF_BE);
        registerItem("prismatic_web", PRISMATIC_WEB);
        registerItem("infused_breath", INFUSED_BREATH);
        registerItem("warden_tendril", WARDEN_TENDRIL);
        registerItem("scrap_tome", SCRAP_TOME);
        registerItem("improved_scrap_tome", IMPROVED_SCRAP_TOME);
        registerItem("extraction_tome", EXTRACTION_TOME);
        registerBlock("library", BASIC_LIBRARY, new Item.Properties());
        registerBlock("ender_library", ENDER_LIBRARY, new Item.Properties());
        registerBlockEntityType("library", BASIC_LIBRARY_BE);
        registerBlockEntityType("ender_library", ENDER_LIBRARY_BE);
        registerLootConditionType("warden_pool", WARDEN_POOL_CONDITION);
        registerCreativeTab();
    }

    private static void registerCreativeTab() {
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, Meridian.id("meridian"),
                FabricItemGroup.builder()
                        .title(Component.translatable("itemGroup.meridian"))
                        .icon(() -> new ItemStack(INFUSED_BREATH))
                        .displayItems((params, output) -> {
                            BLOCKS.values().forEach(block -> output.accept(block));
                            STANDALONE_ITEMS.forEach(output::accept);
                        })
                        .build());
    }

    /**
     * Register Fabric API lookups (hopper-facing storage etc). Held separately from
     * {@link #register()} because {@code BlockApiLookup.registerForBlockEntity} casts
     * {@code BlockEntityType} to a mixin-injected accessor interface, which only exists when
     * Fabric's mixins are applied at runtime. Unit tests bootstrap the core registry without
     * loading Fabric's mixin transformers, so this must be called only from the real
     * {@code onInitialize} path.
     */
    public static void registerApiLookups() {
        // Hopper-facing insert-only adapter. Both tiers share the lookup — the BE owns the adapter
        // instance, so the lambda just unwraps it. Direction is ignored because the library has no
        // per-face gating; a hopper on any side can push books in.
        ItemStorage.SIDED.registerForBlockEntity(
                (be, direction) -> be.getStorageAdapter(), BASIC_LIBRARY_BE);
        ItemStorage.SIDED.registerForBlockEntity(
                (be, direction) -> be.getStorageAdapter(), ENDER_LIBRARY_BE);
    }

    /**
     * Registers a block and its matching {@link BlockItem} under the same path. The block is
     * also added to {@link #BLOCKS}.
     *
     * @param name      path component; namespaced to {@link Meridian#MOD_ID}
     * @param block     pre-constructed block instance
     * @param itemProps item properties for the companion {@code BlockItem}
     * @return the registered block, for fluent assignment at call sites
     */
    public static <T extends Block> T registerBlock(String name, T block, Item.Properties itemProps) {
        ResourceLocation id = Meridian.id(name);
        Registry.register(BuiltInRegistries.BLOCK, id, block);
        BLOCKS.put(id, block);
        Registry.register(BuiltInRegistries.ITEM, id, new BlockItem(block, itemProps));
        return block;
    }

    /** Registers a standalone item under {@code meridian:<name>}. */
    public static <T extends Item> T registerItem(String name, T item) {
        Registry.register(BuiltInRegistries.ITEM, Meridian.id(name), item);
        STANDALONE_ITEMS.add(item);
        return item;
    }

    /** Registers a menu type under {@code meridian:<name>}. */
    public static <T extends AbstractContainerMenu> MenuType<T> registerMenuType(String name, MenuType<T> type) {
        Registry.register(BuiltInRegistries.MENU, Meridian.id(name), type);
        return type;
    }

    /** Registers a block-entity type under {@code meridian:<name>}. */
    public static <T extends BlockEntity> BlockEntityType<T> registerBlockEntityType(String name, BlockEntityType<T> type) {
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Meridian.id(name), type);
        return type;
    }

    /** Registers a loot-item condition type under {@code meridian:<name>}. */
    public static LootItemConditionType registerLootConditionType(String name, LootItemConditionType type) {
        Registry.register(BuiltInRegistries.LOOT_CONDITION_TYPE, Meridian.id(name), type);
        return type;
    }
}
