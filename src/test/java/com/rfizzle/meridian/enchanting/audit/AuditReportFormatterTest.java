// Tier: 1 (pure JUnit — no Minecraft types)
package com.rfizzle.meridian.enchanting.audit;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuditReportFormatterTest {

    private static final Set<String> EXCLUDED = Set.of("meridian", "minecraft");

    private static AuditReport report(AuditEntry... entries) {
        return AuditReport.build(List.of(entries), EXCLUDED);
    }

    @Test
    void summaryLineListsOnlyNonEmptyBucketsWithScannedCount() {
        AuditReport report = report(
                AuditEntry.of("somemod:a", true, TableStatus.NO_TAG),
                AuditEntry.of("somemod:b", false, TableStatus.TREASURE),
                AuditEntry.of("somemod:c", false, TableStatus.OBTAINABLE)
        );
        List<String> lines = AuditReportFormatter.summaryLines(report);
        assertEquals(1, lines.size());
        assertEquals("somemod: 1 missing description, 1 no obtainability tag, 1 treasure (of 3)",
                lines.get(0));
    }

    @Test
    void summaryOmitsFullyCleanNamespaces() {
        AuditReport report = report(
                AuditEntry.of("cleanmod:a", false, TableStatus.OBTAINABLE),
                AuditEntry.of("dirtymod:b", false, TableStatus.NO_TAG)
        );
        List<String> lines = AuditReportFormatter.summaryLines(report);
        assertEquals(1, lines.size());
        assertTrue(lines.get(0).startsWith("dirtymod:"));
    }

    @Test
    void detailLinesEnumerateEveryFlaggedIdUnderItsBucket() {
        AuditReport report = report(
                AuditEntry.of("somemod:a", true, TableStatus.NO_TAG),
                AuditEntry.of("somemod:b", false, TableStatus.DISABLED),
                AuditEntry.of("somemod:c", false, TableStatus.TREASURE)
        );
        List<String> lines = AuditReportFormatter.detailLines(report);
        assertTrue(lines.contains("[somemod]"));
        assertTrue(lines.contains("  missing description: somemod:a"));
        assertTrue(lines.contains("  no obtainability tag: somemod:a"));
        assertTrue(lines.contains("  disabled: somemod:b"));
        assertTrue(lines.contains("  treasure (excluded by design): somemod:c"));
    }

    @Test
    void dumpContainsHeaderCountsAndEveryFlaggedId() {
        AuditReport report = report(
                AuditEntry.of("somemod:a", true, TableStatus.NO_TAG),
                AuditEntry.of("other:b", false, TableStatus.TREASURE)
        );
        String dump = AuditReportFormatter.dump(report);
        assertTrue(dump.contains("scanned 2 enchantment(s) across 2 namespace(s), 3 flagged"));
        assertTrue(dump.contains("somemod:a"));
        assertTrue(dump.contains("other:b"));
    }

    @Test
    void shouldWriteFileOnlyWhenFlaggedCountExceedsThreshold() {
        AuditReport twoFlags = report(
                AuditEntry.of("somemod:a", false, TableStatus.NO_TAG),
                AuditEntry.of("somemod:b", false, TableStatus.NO_TAG)
        );
        assertFalse(AuditReportFormatter.shouldWriteFile(twoFlags, 2));
        assertTrue(AuditReportFormatter.shouldWriteFile(twoFlags, 1));
    }
}
