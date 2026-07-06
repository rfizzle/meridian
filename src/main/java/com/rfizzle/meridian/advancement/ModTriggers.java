package com.rfizzle.meridian.advancement;

import com.rfizzle.meridian.Meridian;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

public final class ModTriggers {

    public static final EnchantedAtTableTrigger ENCHANTED_AT_TABLE = new EnchantedAtTableTrigger();

    /** Fired when a player deposits a book into an Enchantment Library through its menu. */
    public static final SimpleUsageTrigger LIBRARY_DEPOSIT = new SimpleUsageTrigger();
    /** Fired when a player extracts a book from an Enchantment Library. */
    public static final SimpleUsageTrigger LIBRARY_EXTRACT = new SimpleUsageTrigger();
    /** Fired when a player blacklists an enchantment by shelving a book in a Filtering Shelf. */
    public static final SimpleUsageTrigger FILTERING_BLACKLIST = new SimpleUsageTrigger();
    /** Fired when a player takes the output of any salvage tome in the anvil. */
    public static final SimpleUsageTrigger TOME_SALVAGE = new SimpleUsageTrigger();
    /** Fired when a player strips a curse with a Prismatic Web in the anvil. */
    public static final SimpleUsageTrigger CURSE_STRIP = new SimpleUsageTrigger();
    /** Fired when a player makes an item unbreakable with a Tempered Core in the anvil. */
    public static final SimpleUsageTrigger TEMPERED_CORE = new SimpleUsageTrigger();

    private static boolean registered = false;

    private ModTriggers() {
    }

    public static void register() {
        if (registered) return;
        registered = true;

        Registry.register(BuiltInRegistries.TRIGGER_TYPES,
                Meridian.id("enchanted_at_table"), ENCHANTED_AT_TABLE);
        Registry.register(BuiltInRegistries.TRIGGER_TYPES,
                Meridian.id("library_deposit"), LIBRARY_DEPOSIT);
        Registry.register(BuiltInRegistries.TRIGGER_TYPES,
                Meridian.id("library_extract"), LIBRARY_EXTRACT);
        Registry.register(BuiltInRegistries.TRIGGER_TYPES,
                Meridian.id("filtering_blacklist"), FILTERING_BLACKLIST);
        Registry.register(BuiltInRegistries.TRIGGER_TYPES,
                Meridian.id("tome_salvage"), TOME_SALVAGE);
        Registry.register(BuiltInRegistries.TRIGGER_TYPES,
                Meridian.id("curse_strip"), CURSE_STRIP);
        Registry.register(BuiltInRegistries.TRIGGER_TYPES,
                Meridian.id("tempered_core"), TEMPERED_CORE);
    }
}
