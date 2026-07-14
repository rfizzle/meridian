package com.rfizzle.meridian.enchanting.audit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The audit outcome for a single namespace: the flagged ids sorted into their buckets.
 *
 * <p>{@code missingDescription} and {@code noTag} are the compatibility problems; {@code disabled}
 * and {@code treasure} are reported but expected (a pack author disabling an enchantment, or a
 * treasure-only enchantment that is excluded from the table by design).
 */
public record NamespaceReport(
        String namespace,
        int scannedCount,
        List<String> missingDescription,
        List<String> disabled,
        List<String> noTag,
        List<String> treasure) {

    static NamespaceReport of(String namespace, List<AuditEntry> entries) {
        List<String> missing = new ArrayList<>();
        List<String> disabled = new ArrayList<>();
        List<String> noTag = new ArrayList<>();
        List<String> treasure = new ArrayList<>();
        for (AuditEntry e : entries) {
            if (e.missingDescription()) {
                missing.add(e.id());
            }
            switch (e.status()) {
                case DISABLED -> disabled.add(e.id());
                case NO_TAG -> noTag.add(e.id());
                case TREASURE -> treasure.add(e.id());
                case OBTAINABLE -> {
                }
            }
        }
        Collections.sort(missing);
        Collections.sort(disabled);
        Collections.sort(noTag);
        Collections.sort(treasure);
        return new NamespaceReport(namespace, entries.size(),
                List.copyOf(missing), List.copyOf(disabled), List.copyOf(noTag), List.copyOf(treasure));
    }

    /** Total flagged ids across every bucket — drives how much output this namespace contributes. */
    public int flaggedCount() {
        return missingDescription.size() + disabled.size() + noTag.size() + treasure.size();
    }

    /**
     * Whether this namespace is free of compatibility problems — every enchantment has a description
     * and none is barred from the table by a missing obtainability tag. Config-disabled and
     * treasure-only enchantments do not count against cleanliness.
     */
    public boolean isClean() {
        return missingDescription.isEmpty() && noTag.isEmpty();
    }
}
