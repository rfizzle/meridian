// Tier: 1 (pure JUnit — no Minecraft types)
package com.rfizzle.meridian.enchanting.audit;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuditReportTest {

    private static final Set<String> EXCLUDED = Set.of("meridian", "minecraft");

    @Test
    void excludesMeridianAndMinecraftNamespaces() {
        AuditReport report = AuditReport.build(List.of(
                AuditEntry.of("minecraft:sharpness", false, TableStatus.OBTAINABLE),
                AuditEntry.of("meridian:thrift", false, TableStatus.OBTAINABLE),
                AuditEntry.of("somemod:frost", true, TableStatus.NO_TAG)
        ), EXCLUDED);

        assertEquals(1, report.namespaceCount());
        assertEquals(1, report.scannedCount());
        assertEquals("somemod", report.namespaces().get(0).namespace());
    }

    @Test
    void bucketsEntriesByStatusAndDescription() {
        AuditReport report = AuditReport.build(List.of(
                AuditEntry.of("somemod:a", true, TableStatus.NO_TAG),
                AuditEntry.of("somemod:b", false, TableStatus.DISABLED),
                AuditEntry.of("somemod:c", false, TableStatus.TREASURE),
                AuditEntry.of("somemod:d", true, TableStatus.OBTAINABLE),
                AuditEntry.of("somemod:e", false, TableStatus.OBTAINABLE)
        ), EXCLUDED);

        NamespaceReport ns = report.namespaces().get(0);
        assertEquals(5, ns.scannedCount());
        assertEquals(List.of("somemod:a", "somemod:d"), ns.missingDescription());
        assertEquals(List.of("somemod:a"), ns.noTag());
        assertEquals(List.of("somemod:b"), ns.disabled());
        assertEquals(List.of("somemod:c"), ns.treasure());
    }

    @Test
    void sortsNamespacesAndIds() {
        AuditReport report = AuditReport.build(List.of(
                AuditEntry.of("zmod:z", false, TableStatus.NO_TAG),
                AuditEntry.of("amod:c", false, TableStatus.NO_TAG),
                AuditEntry.of("amod:a", false, TableStatus.NO_TAG)
        ), EXCLUDED);

        assertEquals(List.of("amod", "zmod"),
                report.namespaces().stream().map(NamespaceReport::namespace).toList());
        assertEquals(List.of("amod:a", "amod:c"), report.namespaces().get(0).noTag());
    }

    @Test
    void cleanWhenNoMissingDescriptionOrNoTag() {
        // Disabled + treasure alone do not make a namespace dirty — those are expected states.
        AuditReport report = AuditReport.build(List.of(
                AuditEntry.of("somemod:a", false, TableStatus.OBTAINABLE),
                AuditEntry.of("somemod:b", false, TableStatus.DISABLED),
                AuditEntry.of("somemod:c", false, TableStatus.TREASURE)
        ), EXCLUDED);

        assertTrue(report.isClean());
        assertTrue(report.namespaces().get(0).isClean());
    }

    @Test
    void dirtyWhenAnyMissingDescriptionOrNoTag() {
        AuditReport withMissing = AuditReport.build(List.of(
                AuditEntry.of("somemod:a", true, TableStatus.OBTAINABLE)
        ), EXCLUDED);
        assertFalse(withMissing.isClean());

        AuditReport withNoTag = AuditReport.build(List.of(
                AuditEntry.of("somemod:a", false, TableStatus.NO_TAG)
        ), EXCLUDED);
        assertFalse(withNoTag.isClean());
    }

    @Test
    void treasureOrDisabledOnly_isCleanButStillFlagged() {
        // Regression guard: isClean() ignores disabled/treasure, so the command must gate its
        // "everything is fine" message on flaggedCount(), not isClean() — otherwise a described,
        // treasure/disabled-only namespace is falsely reported as fully table-obtainable and its
        // buckets are suppressed.
        AuditReport report = AuditReport.build(List.of(
                AuditEntry.of("somemod:a", false, TableStatus.TREASURE),
                AuditEntry.of("somemod:b", false, TableStatus.DISABLED)
        ), EXCLUDED);

        assertTrue(report.isClean());
        assertTrue(report.flaggedCount() > 0);
    }

    @Test
    void emptyReportIsCleanWithNoNamespaces() {
        AuditReport report = AuditReport.build(List.of(), EXCLUDED);
        assertEquals(0, report.namespaceCount());
        assertEquals(0, report.scannedCount());
        assertEquals(0, report.flaggedCount());
        assertTrue(report.isClean());
    }

    @Test
    void flaggedCountSumsEveryBucketAcrossNamespaces() {
        AuditReport report = AuditReport.build(List.of(
                AuditEntry.of("amod:a", true, TableStatus.NO_TAG),   // missing + noTag -> 2
                AuditEntry.of("bmod:b", false, TableStatus.TREASURE) // treasure -> 1
        ), EXCLUDED);
        assertEquals(3, report.flaggedCount());
    }
}
