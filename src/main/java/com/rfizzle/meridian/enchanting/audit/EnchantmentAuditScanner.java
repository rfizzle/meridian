package com.rfizzle.meridian.enchanting.audit;

import com.rfizzle.meridian.enchanting.EnchantmentInfoRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Reads the raw audit facts off every enchantment in a registry and turns them into
 * {@link AuditEntry} rows — the thin bridge between the game's registry/tags/config and the pure
 * {@link AuditReport} pipeline.
 *
 * <p>The three obtainability facts mirror the tag/enabled portion of the live table gate in
 * {@code RealEnchantmentHelper#getAvailableEnchantmentResults}; keep them in step if that gate
 * changes. The one client-only fact — whether a {@code .desc} lang key exists, which needs
 * {@code I18n} — is injected as {@code hasDescription} so this class stays off the client classpath
 * and gametest-testable against a real tagged registry.
 */
public final class EnchantmentAuditScanner {

    private EnchantmentAuditScanner() {
    }

    /**
     * Builds one {@link AuditEntry} per enchantment in {@code registry}. {@code hasDescription} is
     * queried once per holder for the {@code .desc} lang-key presence; everything else is read from
     * the enchantment's tags and its {@link EnchantmentInfoRegistry} enabled flag. No namespace
     * filtering happens here — {@link AuditReport#build} drops the excluded namespaces.
     */
    public static List<AuditEntry> scan(Registry<Enchantment> registry,
                                        Predicate<Holder<Enchantment>> hasDescription) {
        List<AuditEntry> entries = new ArrayList<>();
        for (Holder.Reference<Enchantment> holder : (Iterable<Holder.Reference<Enchantment>>) registry.holders()::iterator) {
            boolean inTable = holder.is(EnchantmentTags.IN_ENCHANTING_TABLE);
            boolean isTreasure = holder.is(EnchantmentTags.TREASURE);
            boolean enabled = EnchantmentInfoRegistry.getInfo(holder).enabled();
            boolean missingDescription = !hasDescription.test(holder);
            TableStatus status = EnchantmentAudit.classify(inTable, isTreasure, enabled);
            entries.add(AuditEntry.of(holder.key().location().toString(), missingDescription, status));
        }
        return entries;
    }
}
