// Tier: 3 (Fabric Gametest — reads real enchantment tags off a running server)
package com.rfizzle.meridian.gametest;

import com.rfizzle.meridian.enchanting.audit.AuditEntry;
import com.rfizzle.meridian.enchanting.audit.AuditReport;
import com.rfizzle.meridian.enchanting.audit.EnchantmentAuditScanner;
import com.rfizzle.meridian.enchanting.audit.TableStatus;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Exercises {@link EnchantmentAuditScanner} against the real, tag-loaded server registry — the tag
 * membership the scanner reads is only bound once a datapack has loaded, which is why this is a
 * gametest rather than a fabric-loader-junit test. The pure classification and reporting are covered
 * at Tier 1; this proves the scanner reads the right facts off real holders and wires the injected
 * description predicate through to each entry.
 */
public class EnchantmentAuditGameTest implements FabricGameTest {

    private static Registry<Enchantment> enchantments(GameTestHelper helper) {
        return helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
    }

    private static AuditEntry find(List<AuditEntry> entries, String id) {
        return entries.stream().filter(e -> e.id().equals(id)).findFirst().orElse(null);
    }

    @GameTest(template = "meridian:empty_3x3")
    public void scanClassifiesRealTagMembership(GameTestHelper helper) {
        List<AuditEntry> entries = EnchantmentAuditScanner.scan(enchantments(helper), holder -> true);

        AuditEntry sharpness = find(entries, "minecraft:sharpness");
        if (sharpness == null || sharpness.status() != TableStatus.OBTAINABLE) {
            helper.fail("minecraft:sharpness should scan as OBTAINABLE, got "
                    + (sharpness == null ? "absent" : sharpness.status()));
        }

        AuditEntry mending = find(entries, "minecraft:mending");
        if (mending == null || mending.status() != TableStatus.TREASURE) {
            helper.fail("minecraft:mending should scan as TREASURE, got "
                    + (mending == null ? "absent" : mending.status()));
        }

        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void reportExcludesVanillaAndMeridianNamespaces(GameTestHelper helper) {
        // The dev runtime carries only minecraft: and meridian: enchantments, so a report that
        // excludes both must come out empty — the "nothing to audit" path in a clean environment.
        List<AuditEntry> entries = EnchantmentAuditScanner.scan(enchantments(helper), holder -> true);
        AuditReport report = AuditReport.build(entries, Set.of("meridian", "minecraft"));

        if (report.scannedCount() != 0 || !report.namespaces().isEmpty()) {
            helper.fail("Excluding minecraft: and meridian: should leave nothing; scanned "
                    + report.scannedCount() + " across " + report.namespaceCount() + " namespaces");
        }

        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void descriptionPredicateFlowsToEntries(GameTestHelper helper) {
        Registry<Enchantment> registry = enchantments(helper);
        // Only sharpness "has" a description under this stub; everything else is flagged missing.
        Predicate<net.minecraft.core.Holder<Enchantment>> hasDescription =
                holder -> holder.is(Enchantments.SHARPNESS);

        List<AuditEntry> entries = EnchantmentAuditScanner.scan(registry, hasDescription);

        AuditEntry sharpness = find(entries, "minecraft:sharpness");
        if (sharpness == null || sharpness.missingDescription()) {
            helper.fail("minecraft:sharpness should not be flagged missing under the stub");
        }
        AuditEntry mending = find(entries, "minecraft:mending");
        if (mending == null || !mending.missingDescription()) {
            helper.fail("minecraft:mending should be flagged missing under the stub");
        }

        helper.succeed();
    }
}
