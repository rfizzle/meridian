package com.rfizzle.meridian.client.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.client.tooltip.EnchantmentDescriptions;
import com.rfizzle.meridian.enchanting.EnchantmentInfoRegistry;
import com.rfizzle.meridian.enchanting.audit.AuditEntry;
import com.rfizzle.meridian.enchanting.audit.AuditReport;
import com.rfizzle.meridian.enchanting.audit.AuditReportFormatter;
import com.rfizzle.meridian.enchanting.audit.EnchantmentAuditScanner;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.enchantment.Enchantment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/**
 * {@code /meridian audit} — a client-side diagnostic that scans the synced enchantment registry and
 * reports which third-party enchantments would integrate poorly with Meridian: those with no
 * {@code enchantment.<ns>.<path>.desc} lang key (so the library, info screen, tooltips, and recipe
 * viewers show nothing) and those that can never roll at Meridian's enchanting table.
 *
 * <p>Client-side because the {@code .desc} presence check goes through {@code I18n}, which only
 * exists on the client; the enchantment registry and its tags are synced, so it works in
 * single-player and as a connected client. It registers on the client command dispatcher, so it
 * coexists with the server's {@code /meridian reload} — an unknown {@code /meridian} subcommand that
 * doesn't resolve here falls through to the server.
 *
 * <p>Localization scoping (per the command surface convention): the framing lines — header, "clean",
 * "nothing to audit", "written to file" — are translatable {@code command.meridian.audit.*} keys.
 * The dense per-namespace counts and the flagged-id enumeration stay {@link Component#literal} —
 * namespaces, ids, and counts are identifiers and numbers, not translatable prose.
 */
public final class EnchantmentAuditCommand {

    /** Namespaces the audit never reports on: vanilla and Meridian's own (guarded by datagen/tests). */
    private static final Set<String> EXCLUDED_NAMESPACES = Set.of("minecraft", Meridian.MOD_ID);

    /** Above this many flagged ids the full list is written to a file rather than flooding chat. */
    static final int CHAT_ID_THRESHOLD = 20;

    private static final String DUMP_FILE_NAME = "meridian-enchant-audit.txt";

    private static final String HEADER_KEY = "command.meridian.audit.header";
    private static final String NONE_KEY = "command.meridian.audit.none";
    private static final String CLEAN_KEY = "command.meridian.audit.clean";
    private static final String NOT_READY_KEY = "command.meridian.audit.not_ready";
    private static final String FILE_KEY = "command.meridian.audit.file";
    private static final String FILE_ERROR_KEY = "command.meridian.audit.file_error";

    private EnchantmentAuditCommand() {
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal(Meridian.MOD_ID)
                        .then(ClientCommandManager.literal("audit")
                                .executes(EnchantmentAuditCommand::runAudit))));
    }

    private static int runAudit(CommandContext<FabricClientCommandSource> ctx) {
        FabricClientCommandSource source = ctx.getSource();

        ClientLevel level = source.getWorld();
        if (level == null || !EnchantmentInfoRegistry.hasSyncBeenReceived()) {
            source.sendError(Component.translatable(NOT_READY_KEY));
            return 0;
        }

        Registry<Enchantment> registry = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        List<AuditEntry> entries = EnchantmentAuditScanner.scan(registry,
                holder -> EnchantmentDescriptions.resolve(holder).isPresent());
        AuditReport report = AuditReport.build(entries, EXCLUDED_NAMESPACES);

        if (report.scannedCount() == 0) {
            source.sendFeedback(Component.translatable(NONE_KEY));
            return Command.SINGLE_SUCCESS;
        }

        source.sendFeedback(Component.translatable(HEADER_KEY,
                report.scannedCount(), report.namespaceCount()));

        if (report.flaggedCount() == 0) {
            // Nothing flagged at all — every scanned enchantment is described and table-obtainable.
            // Gating on flaggedCount rather than isClean() keeps the treasure/disabled buckets (which
            // isClean() ignores) from being suppressed here and reported below instead.
            source.sendFeedback(Component.translatable(CLEAN_KEY));
            return Command.SINGLE_SUCCESS;
        }

        for (String line : AuditReportFormatter.summaryLines(report)) {
            source.sendFeedback(Component.literal(line));
        }

        if (AuditReportFormatter.shouldWriteFile(report, CHAT_ID_THRESHOLD)) {
            writeDump(source, report);
        } else {
            for (String line : AuditReportFormatter.detailLines(report)) {
                source.sendFeedback(Component.literal(line));
            }
        }
        return Command.SINGLE_SUCCESS;
    }

    private static void writeDump(FabricClientCommandSource source, AuditReport report) {
        Path path = FabricLoader.getInstance().getGameDir().resolve(DUMP_FILE_NAME);
        try {
            Files.writeString(path, AuditReportFormatter.dump(report));
            source.sendFeedback(Component.translatable(FILE_KEY, path.toString()));
        } catch (IOException e) {
            Meridian.LOGGER.error("Failed to write enchantment audit dump to {}", path, e);
            source.sendError(Component.translatable(FILE_ERROR_KEY));
        }
    }
}
