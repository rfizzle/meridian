package com.rfizzle.meridian.enchanting.audit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * The full audit outcome: one {@link NamespaceReport} per third-party namespace, ordered by
 * namespace for deterministic output.
 *
 * <p>Built purely from a flat list of {@link AuditEntry} by {@link #build(Collection, Set)}, which
 * drops the excluded namespaces (typically {@code meridian} and {@code minecraft}) before grouping.
 */
public final class AuditReport {

    private final List<NamespaceReport> namespaces;
    private final int scannedCount;

    private AuditReport(List<NamespaceReport> namespaces, int scannedCount) {
        this.namespaces = namespaces;
        this.scannedCount = scannedCount;
    }

    public List<NamespaceReport> namespaces() {
        return namespaces;
    }

    /** Number of third-party enchantments scanned (after excluded namespaces are dropped). */
    public int scannedCount() {
        return scannedCount;
    }

    public int namespaceCount() {
        return namespaces.size();
    }

    /** Total flagged ids across every namespace and bucket. */
    public int flaggedCount() {
        int total = 0;
        for (NamespaceReport report : namespaces) {
            total += report.flaggedCount();
        }
        return total;
    }

    /** Whether no namespace has a compatibility problem (see {@link NamespaceReport#isClean()}). */
    public boolean isClean() {
        for (NamespaceReport report : namespaces) {
            if (!report.isClean()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Groups the entries into per-namespace reports, dropping any entry whose namespace is in
     * {@code excludedNamespaces}. Namespaces (and the id lists within each) come out sorted, so the
     * same registry always produces the same report.
     */
    public static AuditReport build(Collection<AuditEntry> entries, Set<String> excludedNamespaces) {
        Map<String, List<AuditEntry>> byNamespace = new TreeMap<>();
        int scanned = 0;
        for (AuditEntry entry : entries) {
            if (excludedNamespaces.contains(entry.namespace())) {
                continue;
            }
            scanned++;
            byNamespace.computeIfAbsent(entry.namespace(), key -> new ArrayList<>()).add(entry);
        }
        List<NamespaceReport> reports = new ArrayList<>();
        for (Map.Entry<String, List<AuditEntry>> group : byNamespace.entrySet()) {
            reports.add(NamespaceReport.of(group.getKey(), group.getValue()));
        }
        return new AuditReport(List.copyOf(reports), scanned);
    }
}
