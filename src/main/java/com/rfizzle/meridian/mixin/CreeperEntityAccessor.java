package com.rfizzle.meridian.mixin;

import net.minecraft.world.entity.monster.Creeper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Creeper.class)
public interface CreeperEntityAccessor {
    @Accessor("swell")
    int meridian$getSwell();

    @Accessor("swell")
    void meridian$setSwell(int swell);

    @Accessor("oldSwell")
    void meridian$setOldSwell(int oldSwell);
}
