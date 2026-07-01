package com.rfizzle.meridian.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.rfizzle.meridian.Meridian;

/**
 * Raw-JSON schema migration for {@link MeridianConfig}. Migrations run on the parsed JSON tree
 * <em>before</em> Gson deserialization, so a renamed or restructured key is carried forward rather
 * than silently dropped by a lenient deserialize.
 */
final class ConfigMigrator {

    static final int CURRENT_VERSION = 1;

    @FunctionalInterface
    interface Migration {
        void apply(JsonObject json);
    }

    // Index i = the v(i) → v(i+1) transition. Append only; never reorder.
    private static final Migration[] MIGRATIONS = {
            // v0 → v1: baseline. Pre-versioned files carry no configVersion; they are treated as v0
            // and stamped current. No structural change — future renames append a lambda here.
            json -> { },
    };

    private ConfigMigrator() {}

    /**
     * Applies every pending migration to {@code json} in place and stamps {@code configVersion} to
     * {@link #CURRENT_VERSION}. A file without {@code configVersion} is treated as v0 so
     * pre-versioned configs migrate. Returns {@code true} if the JSON was changed (so the caller
     * re-persists the upgraded schema).
     */
    static boolean migrate(JsonObject json) {
        int version = readVersion(json);
        if (version >= CURRENT_VERSION) return false;
        int from = version;
        boolean changed = false;
        for (int i = version; i < CURRENT_VERSION && i < MIGRATIONS.length; i++) {
            try {
                MIGRATIONS[i].apply(json);
                changed = true;
            } catch (Exception e) {
                Meridian.LOGGER.warn("Config migration v{} to v{} failed; skipping: {}", i, i + 1, e.getMessage());
            }
        }
        if (changed) {
            json.addProperty("configVersion", CURRENT_VERSION);
            Meridian.LOGGER.info("Migrated config from version {} to {}", from, CURRENT_VERSION);
        }
        return changed;
    }

    private static int readVersion(JsonObject json) {
        JsonElement v = json.get("configVersion");
        if (v != null && v.isJsonPrimitive() && v.getAsJsonPrimitive().isNumber()) {
            return v.getAsInt();
        }
        return 0;
    }
}
