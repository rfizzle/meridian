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
            dropSelf(block);
        }
    }
}
