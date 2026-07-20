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

    static final int CURRENT_VERSION = 11;

    @FunctionalInterface
    interface Migration {
        void apply(JsonObject json);
    }

    // Index i = the v(i) → v(i+1) transition. Append only; never reorder.
    private static final Migration[] MIGRATIONS = {
            // v0 → v1: baseline. Pre-versioned files carry no configVersion; they are treated as v0
            // and stamped current. No structural change — future renames append a lambda here.
            json -> { },
            // v1 → v2: recipe-module toggles (#163) — tableCrafting.allowDuplication and
            // everfeast.enabled. Purely additive; Gson field initializers supply the defaults, and
            // the post-migration re-save writes them into the file so operators can discover them.
            json -> { },
            // v2 → v3: anvil.temperedCoreEnabled (#158). Unlike the additive toggles above, a missing
            // boolean must be written explicitly: Gson deserializes an absent boolean as false, not the
            // Java field default true, which would silently disable the handler for existing configs.
            // Seed true only when the key is absent so an operator's explicit false is preserved.
            json -> {
                JsonElement anvil = json.get("anvil");
                if (anvil != null && anvil.isJsonObject()) {
                    JsonObject anvilObj = anvil.getAsJsonObject();
                    if (!anvilObj.has("temperedCoreEnabled")) {
                        anvilObj.addProperty("temperedCoreEnabled", true);
                    }
                }
            },
            // v3 → v4: drop the dead enchantingTable.showLevelIndicator toggle (#161). No code ever
            // read it, so removing the field would leave a stale key in existing files; strip it from
            // the raw object so the re-saved config no longer advertises an inert option.
            json -> {
                JsonElement table = json.get("enchantingTable");
                if (table != null && table.isJsonObject()) {
                    table.getAsJsonObject().remove("showLevelIndicator");
                }
            },
            // v4 → v5: the attunement group (#206) — radius, intervalTicks, minEterna. Purely
            // additive; Gson field initializers supply the defaults, and the post-migration re-save
            // writes the group into the file so operators can discover it.
            json -> { },
            // v5 → v6: combat.markAffectsPlayers (#201). Purely additive; Gson deserializes an absent
            // boolean as false, which already matches the field default, and the post-migration re-save
            // writes it into the file so operators can discover the toggle.
            json -> { },
            // v6 → v7: combat.undertowAffectsPlayers (#218). Purely additive; Gson deserializes an
            // absent boolean as false, which already matches the field default, and the post-migration
            // re-save writes it into the file so operators can discover the toggle.
            json -> { },
            // v7 → v8: combat.pinAffectsPlayers (#219). Purely additive; Gson deserializes an absent
            // boolean as false, which already matches the field default, and the post-migration
            // re-save writes it into the file so operators can discover the toggle.
            json -> { },
            // v8 → v9: combat.bullrushAffectsPlayers (#221). Purely additive; Gson deserializes an
            // absent boolean as false, which already matches the field default, and the
            // post-migration re-save writes it into the file so operators can discover the toggle.
            json -> { },
            // v9 → v10: the groom section (#224) — chanceLevel1, chanceLevel2, cooldownTicks. Purely
            // additive; Gson field initializers supply the defaults, and the post-migration re-save
            // writes the section into the file so operators can discover the levers.
            json -> { },
            // v10 → v11: combat.trackersLensAffectsPlayers (#228). Purely additive; Gson deserializes
            // an absent boolean as false, which already matches the field default, and the
            // post-migration re-save writes it into the file so operators can discover the toggle.
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
