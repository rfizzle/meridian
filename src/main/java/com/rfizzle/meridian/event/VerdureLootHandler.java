package com.rfizzle.meridian.event;

import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.BonusLevelTableCondition;
import net.minecraft.world.level.storage.loot.predicates.ExplosionCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Verdure — lets a Verdure-enchanted shears reclaim from sheared leaves the sapling (and, on oak
 * and dark oak, the apple) that hand-breaking would have dropped. Vanilla's own leaf tables gate
 * those drops behind {@code NOT shears/silk-touch}, so shearing normally yields leaves only; this
 * handler layers a parallel pool gated the opposite way — {@link VerdureToolCondition} — that
 * mirrors each table's exact vanilla drop chances and Fortune scaling.
 *
 * <p>Only leaf types that have a sapling item are covered; mangrove and azalea leaves have none in
 * vanilla and are left alone. {@code source.isBuiltin()} guards the injection so a datapack that
 * fully replaces a leaf table opts out.
 */
public final class VerdureLootHandler {

    /** Fortune-scaled sapling chances shared by every leaf except jungle (vanilla values). */
    private static final float[] NORMAL_SAPLING_CHANCES = {0.05F, 0.0625F, 0.083333336F, 0.1F};
    /** Jungle's rarer sapling chances (vanilla values). */
    private static final float[] JUNGLE_SAPLING_CHANCES = {0.025F, 0.027777778F, 0.03125F, 0.041666668F, 0.1F};
    /** Fortune-scaled apple chances for oak and dark oak (vanilla values). */
    private static final float[] APPLE_CHANCES = {0.005F, 0.0055555557F, 0.00625F, 0.008333334F, 0.025F};

    private record LeafDrop(Block leaf, ItemLike sapling, float[] saplingChances, boolean dropsApple) {
    }

    private static final List<LeafDrop> LEAF_DROPS = List.of(
            new LeafDrop(Blocks.OAK_LEAVES, Items.OAK_SAPLING, NORMAL_SAPLING_CHANCES, true),
            new LeafDrop(Blocks.DARK_OAK_LEAVES, Items.DARK_OAK_SAPLING, NORMAL_SAPLING_CHANCES, true),
            new LeafDrop(Blocks.BIRCH_LEAVES, Items.BIRCH_SAPLING, NORMAL_SAPLING_CHANCES, false),
            new LeafDrop(Blocks.SPRUCE_LEAVES, Items.SPRUCE_SAPLING, NORMAL_SAPLING_CHANCES, false),
            new LeafDrop(Blocks.JUNGLE_LEAVES, Items.JUNGLE_SAPLING, JUNGLE_SAPLING_CHANCES, false),
            new LeafDrop(Blocks.ACACIA_LEAVES, Items.ACACIA_SAPLING, NORMAL_SAPLING_CHANCES, false),
            new LeafDrop(Blocks.CHERRY_LEAVES, Items.CHERRY_SAPLING, NORMAL_SAPLING_CHANCES, false));

    private static final Map<ResourceKey<LootTable>, LeafDrop> BY_TABLE = buildTableIndex();

    private VerdureLootHandler() {
    }

    private static Map<ResourceKey<LootTable>, LeafDrop> buildTableIndex() {
        Map<ResourceKey<LootTable>, LeafDrop> index = new HashMap<>();
        for (LeafDrop drop : LEAF_DROPS) {
            index.put(drop.leaf().getLootTable(), drop);
        }
        return Map.copyOf(index);
    }

    /** Production hook — registers the MODIFY listener. Pool conditions read the live tool at roll time. */
    public static void register() {
        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
            if (!source.isBuiltin()) {
                return;
            }
            LeafDrop drop = BY_TABLE.get(key);
            if (drop == null) {
                return;
            }
            modify(tableBuilder, registries, drop);
        });
    }

    /**
     * Appends the Verdure-gated sapling pool (and apple pool where applicable) to a leaf table.
     * Split out from {@link #register()} so tests can exercise the mutation directly without
     * Fabric's event pipeline.
     */
    static void modify(LootTable.Builder tableBuilder, HolderLookup.Provider registries, LeafDrop drop) {
        Holder<Enchantment> fortune = registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE);

        tableBuilder.withPool(LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0F))
                .add(LootItem.lootTableItem(drop.sapling()))
                .when(() -> VerdureToolCondition.INSTANCE)
                .when(ExplosionCondition.survivesExplosion())
                .when(BonusLevelTableCondition.bonusLevelFlatChance(fortune, drop.saplingChances())));

        if (drop.dropsApple()) {
            tableBuilder.withPool(LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1.0F))
                    .add(LootItem.lootTableItem(Items.APPLE))
                    .when(() -> VerdureToolCondition.INSTANCE)
                    .when(ExplosionCondition.survivesExplosion())
                    .when(BonusLevelTableCondition.bonusLevelFlatChance(fortune, APPLE_CHANCES)));
        }
    }
}
