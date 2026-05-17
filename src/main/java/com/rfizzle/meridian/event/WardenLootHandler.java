package com.rfizzle.meridian.event;

import com.rfizzle.meridian.MeridianRegistry;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

/**
 * Ships {@code warden_tendril} as a Warden-kill reward by layering two extra pools onto the
 * vanilla {@code entities/warden} loot table via Fabric's {@link LootTableEvents#MODIFY}.
 * Matches Zenith's behaviour 1:1: one guaranteed drop plus a looting-scaled second drop.
 *
 * <p>Pool layout:
 * <ul>
 *   <li><strong>Pool A</strong> — 1 tendril, gated by
 *       {@link WardenPoolCondition} in {@link WardenPoolCondition.Kind#DROP_CHANCE} mode.
 *       Default config makes this a guaranteed drop (chance {@code 1.0}).</li>
 *   <li><strong>Pool B</strong> — 1 extra tendril, gated by
 *       {@link WardenPoolCondition} in {@link WardenPoolCondition.Kind#LOOTING_BONUS} mode.
 *       Looking up the attacker's looting level at roll time and multiplying by
 *       {@code config.warden.tendrilLootingBonus} yields the per-kill probability; without
 *       looting the bonus collapses to zero.</li>
 * </ul>
 *
 * <p>Both pools carry the custom {@link WardenPoolCondition}, not the vanilla random-chance
 * variants — so the chance is read from the live config on every roll. Operators editing
 * {@code meridian.json} see the new values on the next Warden kill after running
 * {@code /meridian reload}; no full datapack reload is required.
 *
 * <p>Only the vanilla loot table is modified ({@code source.isBuiltin()} check) so datapacks
 * that fully replace the Warden table opt out of our additions.
 */
public final class WardenLootHandler {

    private static final ResourceKey<LootTable> WARDEN_LOOT_TABLE = EntityType.WARDEN.getDefaultLootTable();

    private WardenLootHandler() {
    }

    /** Production hook — registers the MODIFY listener. Condition-side config lookup happens at roll time. */
    public static void register() {
        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
            if (!WARDEN_LOOT_TABLE.equals(key) || !source.isBuiltin()) {
                return;
            }
            modify(tableBuilder, registries);
        });
    }

    /**
     * Appends the two tendril pools to {@code tableBuilder}. Split out from
     * {@link #register()} so tests can exercise the mutation directly without going through
     * Fabric's event pipeline — the conditions themselves read the live config, so tuning
     * values post-MODIFY is the condition's responsibility, not this method's.
     */
    static void modify(LootTable.Builder tableBuilder, HolderLookup.Provider registries) {
        tableBuilder.withPool(LootPool.lootPool()
                .setRolls(UniformGenerator.between(1.0F, 1.0F))
                .add(LootItem.lootTableItem(MeridianRegistry.WARDEN_TENDRIL))
                .when(() -> new WardenPoolCondition(WardenPoolCondition.Kind.DROP_CHANCE)));

        tableBuilder.withPool(LootPool.lootPool()
                .setRolls(UniformGenerator.between(1.0F, 1.0F))
                .add(LootItem.lootTableItem(MeridianRegistry.WARDEN_TENDRIL))
                .when(() -> new WardenPoolCondition(WardenPoolCondition.Kind.LOOTING_BONUS)));
    }
}
