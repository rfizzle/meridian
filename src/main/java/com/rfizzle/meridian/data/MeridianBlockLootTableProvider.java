package com.rfizzle.meridian.data;

import com.rfizzle.meridian.MeridianRegistry;

import java.util.concurrent.CompletableFuture;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.block.Block;

/**
 * Generates {@code dropSelf} loot tables for every block registered through
 * {@link MeridianRegistry}. Shelves are pure cosmetic/stat blocks with no special drop
 * behaviour — a single-pool table that yields the block itself on any break is exactly what
 * every shelf, utility shelf, filtering shelf, treasure shelf, and library variant needs. If a
 * future block requires conditional drops (silk touch, fortune, etc.) it should opt out of this
 * walk and add an explicit entry in {@link #generate()}.
 *
 * <p>The iteration source is {@link MeridianRegistry#BLOCKS}, so filtering/treasure
 * shelves and library blocks picked up in later stories automatically gain tables the moment
 * they are registered — no provider edit required.
 */
public class MeridianBlockLootTableProvider extends FabricBlockLootTableProvider {

    public MeridianBlockLootTableProvider(FabricDataOutput output,
                                        CompletableFuture<HolderLookup.Provider> registryLookup) {
        super(output, registryLookup);
    }

    @Override
    public void generate() {
        for (Block block : MeridianRegistry.BLOCKS.values()) {
            dropSelfWithSequence(block);
        }
    }

    /**
     * {@link #dropSelf(Block)} with the table's random sequence restored.
     *
     * <p>Vanilla's own {@code LootTableProvider} stamps every table with
     * {@code random_sequence = <its own id>} before setting the param set;
     * {@code FabricLootTableProviderImpl.run} only sets the param set, so a bare
     * {@code dropSelf} silently omits the key. It selects the per-table RNG stream —
     * seeded off the world seed and persisted in the level's {@code random_sequences}
     * data — that the {@code survives_explosion} condition rolls against, so a table
     * without it sits outside the sequence state vanilla puts every table into.
     * See the {@code mc-datagen} skill.
     */
    private void dropSelfWithSequence(Block block) {
        add(block, createSingleItemTable(block).setRandomSequence(block.getLootTable().location()));
    }
}
