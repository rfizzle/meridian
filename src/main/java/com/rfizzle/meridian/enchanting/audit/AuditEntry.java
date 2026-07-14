package com.rfizzle.meridian.enchanting.audit;

/**
 * One enchantment's audit facts, as plain data.
 *
 * @param id                 the enchantment's full id (e.g. {@code "somemod:frostbite"})
 * @param namespace          the id's namespace (e.g. {@code "somemod"})
 * @param missingDescription whether the enchantment lacks an {@code enchantment.<ns>.<path>.desc} lang key
 * @param status             its enchanting-table obtainability
 */
public record AuditEntry(String id, String namespace, boolean missingDescription, TableStatus status) {

    /**
     * Builds an entry from a full id string, splitting the namespace off at the first {@code ':'}.
     * A bare id with no colon is treated as the {@code minecraft} namespace, matching
     * {@code ResourceLocation} parsing.
     */
    public static AuditEntry of(String id, boolean missingDescription, TableStatus status) {
        int colon = id.indexOf(':');
        String namespace = colon < 0 ? "minecraft" : id.substring(0, colon);
        return new AuditEntry(id, namespace, missingDescription, status);
    }
}
