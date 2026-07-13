package com.rfizzle.meridian.config;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tier 1 (pure JUnit) coverage for {@link ConfigMigrator}. The migrator operates only on a parsed
 * {@link JsonObject}, so no Minecraft bootstrap is required.
 */
class ConfigMigratorTest {

    @Test
    void migrate_v3_dropsShowLevelIndicator_andStampsCurrentVersion() {
        JsonObject json = new JsonObject();
        json.addProperty("configVersion", 3);
        JsonObject table = new JsonObject();
        table.addProperty("showLevelIndicator", true);
        table.addProperty("maxEterna", 50);
        json.add("enchantingTable", table);

        boolean changed = ConfigMigrator.migrate(json);

        assertTrue(changed, "migrating a v3 config must report a change");
        assertFalse(json.getAsJsonObject("enchantingTable").has("showLevelIndicator"),
                "showLevelIndicator must be removed by the v3 to v4 migration");
        assertEquals(50, json.getAsJsonObject("enchantingTable").get("maxEterna").getAsInt(),
                "unrelated keys must survive the migration");
        assertEquals(ConfigMigrator.CURRENT_VERSION, json.get("configVersion").getAsInt(),
                "configVersion must be stamped to the current version");
    }

    @Test
    void migrate_v4_advancesToCurrentWithoutStructuralChange() {
        // v4 → v5 (the attunement group, #206) is purely additive: the migration only advances the
        // version stamp; Gson field initializers supply the new group's defaults on deserialize.
        JsonObject json = new JsonObject();
        json.addProperty("configVersion", 4);
        JsonObject table = new JsonObject();
        table.addProperty("maxEterna", 50);
        json.add("enchantingTable", table);

        boolean changed = ConfigMigrator.migrate(json);

        assertTrue(changed, "migrating a v4 config must report a change");
        assertEquals(50, json.getAsJsonObject("enchantingTable").get("maxEterna").getAsInt(),
                "unrelated keys must survive the migration");
        assertEquals(ConfigMigrator.CURRENT_VERSION, json.get("configVersion").getAsInt(),
                "configVersion must be stamped to the current version");
    }

    @Test
    void migrate_v6_advancesToCurrentWithoutStructuralChange() {
        // v6 → v7 (combat.undertowAffectsPlayers, #218) is purely additive: the migration only
        // advances the version stamp; Gson's absent-boolean default (false) already matches the
        // field default on deserialize.
        JsonObject json = new JsonObject();
        json.addProperty("configVersion", 6);
        JsonObject combat = new JsonObject();
        combat.addProperty("harpoonAffectsPlayers", true);
        json.add("combat", combat);

        boolean changed = ConfigMigrator.migrate(json);

        assertTrue(changed, "migrating a v6 config must report a change");
        assertTrue(json.getAsJsonObject("combat").get("harpoonAffectsPlayers").getAsBoolean(),
                "unrelated combat toggles must survive the migration");
        assertEquals(ConfigMigrator.CURRENT_VERSION, json.get("configVersion").getAsInt(),
                "configVersion must be stamped to the current version");
    }

    @Test
    void migrate_v7_advancesToCurrentWithoutStructuralChange() {
        // v7 → v8 (combat.pinAffectsPlayers, #219) is purely additive: the migration only
        // advances the version stamp; Gson's absent-boolean default (false) already matches the
        // field default on deserialize.
        JsonObject json = new JsonObject();
        json.addProperty("configVersion", 7);
        JsonObject combat = new JsonObject();
        combat.addProperty("markAffectsPlayers", true);
        json.add("combat", combat);

        boolean changed = ConfigMigrator.migrate(json);

        assertTrue(changed, "migrating a v7 config must report a change");
        assertTrue(json.getAsJsonObject("combat").get("markAffectsPlayers").getAsBoolean(),
                "unrelated combat toggles must survive the migration");
        assertEquals(ConfigMigrator.CURRENT_VERSION, json.get("configVersion").getAsInt(),
                "configVersion must be stamped to the current version");
    }

    @Test
    void migrate_currentVersion_isNoOp() {
        JsonObject json = new JsonObject();
        json.addProperty("configVersion", ConfigMigrator.CURRENT_VERSION);
        JsonObject table = new JsonObject();
        table.addProperty("maxEterna", 50);
        json.add("enchantingTable", table);

        boolean changed = ConfigMigrator.migrate(json);

        assertFalse(changed, "a config already at the current version must not be changed");
        assertEquals(50, json.getAsJsonObject("enchantingTable").get("maxEterna").getAsInt());
    }

    @Test
    void migrate_v3_withoutShowLevelIndicator_isSafe() {
        JsonObject json = new JsonObject();
        json.addProperty("configVersion", 3);
        JsonObject table = new JsonObject();
        table.addProperty("maxEterna", 50);
        json.add("enchantingTable", table);

        boolean changed = ConfigMigrator.migrate(json);

        assertTrue(changed, "a v3 config still advances to the current version");
        assertFalse(json.getAsJsonObject("enchantingTable").has("showLevelIndicator"),
                "absence of the key must not reintroduce it");
        assertEquals(ConfigMigrator.CURRENT_VERSION, json.get("configVersion").getAsInt());
    }
}
