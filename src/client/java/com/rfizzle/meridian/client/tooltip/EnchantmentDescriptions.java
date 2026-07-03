package com.rfizzle.meridian.client.tooltip;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.Optional;

/**
 * Resolves the plain-language lore of an enchantment — the
 * {@code enchantment.<namespace>.<path>.desc} lang key that sits alongside the display-name key.
 * In the {@link Enchantment} API, {@code value().description()} is the localized display name,
 * not this lore; the {@code .desc} key is optional and only present for enchantments that ship a
 * description, so callers get an {@link Optional} and skip the line when it is empty.
 */
public final class EnchantmentDescriptions {

    private EnchantmentDescriptions() {
    }

    /** The unstyled description component for the enchantment, if it has a {@code .desc} lang key. */
    public static Optional<Component> resolve(Holder<Enchantment> ench) {
        return ench.unwrapKey().flatMap(EnchantmentDescriptions::resolve);
    }

    /** The unstyled description component for the enchantment key, if it has a {@code .desc} lang key. */
    public static Optional<Component> resolve(ResourceKey<Enchantment> key) {
        String descKey = key.location().toLanguageKey("enchantment") + ".desc";
        return I18n.exists(descKey) ? Optional.of(Component.translatable(descKey)) : Optional.empty();
    }
}
