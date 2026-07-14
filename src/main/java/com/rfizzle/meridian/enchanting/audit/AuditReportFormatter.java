package com.rfizzle.meridian.enchanting.audit;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders an {@link AuditReport} to plain strings — the per-namespace count lines always shown in
 * chat, the full flagged-id enumeration shown in chat when short or written to a file when long,
 * and the file body itself.
 *
 * <p>These are the dense, identifier-heavy lines of the audit. They stay literal (namespaces, ids,
 * and counts are not translatable content); the surrounding framing — the header, the "clean",
 * "nothing to audit", and "written to file" lines — is what the command layer localises.
 */
public final class AuditReportFormatter {

    private AuditReportFormatter() {
    }

    /**
     * One count line per namespace that has anything to report, e.g.
     * {@code "somemod: 3 missing description, 1 no obtainability tag, 2 disabled, 4 treasure (of 12)"}.
     * Fully clean namespaces contribute nothing.
     */
    public static List<String> summaryLines(AuditReport report) {
        List<String> lines = new ArrayList<>();
        for (NamespaceReport ns : report.namespaces()) {
            if (ns.flaggedCount() == 0) {
                continue;
            }
            List<String> parts = new ArrayList<>();
            if (!ns.missingDescription().isEmpty()) {
                parts.add(ns.missingDescription().size() + " missing description");
            }
            if (!ns.noTag().isEmpty()) {
                parts.add(ns.noTag().size() + " no obtainability tag");
            }
            if (!ns.disabled().isEmpty()) {
                parts.add(ns.disabled().size() + " disabled");
            }
            if (!ns.treasure().isEmpty()) {
                parts.add(ns.treasure().size() + " treasure");
            }
            lines.add(ns.namespace() + ": " + String.join(", ", parts)
                    + " (of " + ns.scannedCount() + ")");
        }
        return lines;
    }

    /**
     * The full flagged-id enumeration: for each namespace, a heading followed by one id per line
     * grouped under its bucket. This is what floods chat, so the command layer diverts it to a file
     * when {@link #shouldWriteFile(AuditReport, int)} says the list is long.
     */
    public static List<String> detailLines(AuditReport report) {
        List<String> lines = new ArrayList<>();
        for (NamespaceReport ns : report.namespaces()) {
            if (ns.flaggedCount() == 0) {
                continue;
            }
            lines.add("[" + ns.namespace() + "]");
            appendBucket(lines, "missing description", ns.missingDescription());
            appendBucket(lines, "no obtainability tag", ns.noTag());
            appendBucket(lines, "disabled", ns.disabled());
            appendBucket(lines, "treasure (excluded by design)", ns.treasure());
        }
        return lines;
    }

    private static void appendBucket(List<String> lines, String label, List<String> ids) {
        for (String id : ids) {
            lines.add("  " + label + ": " + id);
        }
    }

    /** The plain-text file body: a header line plus every detail line. */
    public static String dump(AuditReport report) {
        StringBuilder sb = new StringBuilder();
        sb.append("Meridian third-party enchantment audit\n");
        sb.append("scanned ").append(report.scannedCount())
                .append(" enchantment(s) across ").append(report.namespaceCount())
                .append(" namespace(s), ").append(report.flaggedCount()).append(" flagged\n\n");
        for (String line : detailLines(report)) {
            sb.append(line).append('\n');
        }
        return sb.toString();
    }

    /** Whether the flagged list is long enough to divert to a file rather than print in chat. */
    public static boolean shouldWriteFile(AuditReport report, int threshold) {
        return report.flaggedCount() > threshold;
    }
}
