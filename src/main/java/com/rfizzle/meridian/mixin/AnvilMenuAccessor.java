package com.rfizzle.meridian.mixin;

import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.DataSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * T-4.1.1 — exposes the two {@link AnvilMenu}-private fields the
 * {@link com.rfizzle.meridian.anvil.AnvilDispatcher} pipeline needs to write when a
 * handler claims an output: the {@code cost} {@link DataSlot} (XP charge) and the
 * {@code repairItemCountCost} counter (right-slot consumption count).
 *
 * <p>Method names are prefixed with {@code meridian$} per /dev-companion's mixin rules —
 * the explicit {@link Accessor#value()} tells Mixin which field to bind, since the prefixed name
 * is otherwise unparseable as a getter/setter.
 */
@Mixin(AnvilMenu.class)
public interface AnvilMenuAccessor {

    @Accessor("cost")
    DataSlot meridian$getCost();

    @Accessor("repairItemCountCost")
    int meridian$getRepairItemCountCost();

    @Accessor("repairItemCountCost")
    void meridian$setRepairItemCountCost(int value);
}
