package com.rfizzle.meridian.tag;

import com.rfizzle.meridian.Meridian;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * Item tags Meridian's gameplay code reads at runtime. The datagen provider that emits them keeps
 * its own constants for the tags nothing outside datagen consults; these live here so the
 * enchanting menu never has to import a {@code com.rfizzle.meridian.data} class, which would drag
 * a datagen entrypoint onto the runtime classpath.
 */
public final class MeridianItemTags {

    /** The spyglass surface Tracker's Lens lands on. */
    public static final TagKey<Item> ENCHANTABLE_SPYGLASS =
            TagKey.create(Registries.ITEM, Meridian.id("enchantable/spyglass"));

    /**
     * Meridian surfaces the enchanting table accepts despite failing vanilla's
     * {@code Item#isEnchantable}, which requires a {@code minecraft:max_damage} component. Most
     * custom surfaces (shield, brush, elytra, horse armour) are durable items and clear that gate
     * on their own; the spyglass is not, so it joins this tag and
     * {@code MeridianEnchantmentMenu} consults it alongside the vanilla check. Widening the tag is
     * all a future durability-less surface needs.
     */
    public static final TagKey<Item> ENCHANTABLE_SURFACE =
            TagKey.create(Registries.ITEM, Meridian.id("enchantable_surface"));

    private MeridianItemTags() {}
}
