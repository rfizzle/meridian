package com.rfizzle.meridian.mixin;

import com.rfizzle.meridian.event.DragonLootHandler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Drops one {@code dormant_core} when the Ender Dragon's death sequence completes (#158) — see
 * {@link DragonLootHandler} for why a mixin is used instead of a death event or a loot table.
 *
 * <p>{@link EnderDragon#tickDeath()} removes the entity directly on the final frame at
 * {@code dragonDeathTime == 200}; injecting there fires exactly once per kill — first and every
 * respawn — since the entity is gone before {@code tickDeath} could run again.
 */
@Mixin(EnderDragon.class)
public abstract class EnderDragonMixin {

    @Shadow
    public int dragonDeathTime;

    @Inject(method = "tickDeath", at = @At("TAIL"))
    private void meridian$dropDormantCore(CallbackInfo ci) {
        // tickDeath runs every tick of the death animation; the terminal removal happens only on the
        // 200th tick and only on the server. Guarding on both makes the drop fire exactly once.
        if (this.dragonDeathTime != 200) {
            return;
        }
        EnderDragon self = (EnderDragon) (Object) this;
        if (self.level() instanceof ServerLevel level) {
            DragonLootHandler.dropDormantCore(level, self.getX(), self.getY(), self.getZ());
        }
    }
}
