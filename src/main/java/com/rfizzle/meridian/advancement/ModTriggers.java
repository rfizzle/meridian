package com.rfizzle.meridian.advancement;

import com.rfizzle.meridian.Meridian;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

public final class ModTriggers {

    public static final EnchantedAtTableTrigger ENCHANTED_AT_TABLE = new EnchantedAtTableTrigger();

    private static boolean registered = false;

    private ModTriggers() {
    }

    public static void register() {
        if (registered) return;
        registered = true;

        Registry.register(BuiltInRegistries.TRIGGER_TYPES,
                Meridian.id("enchanted_at_table"), ENCHANTED_AT_TABLE);
    }
}
