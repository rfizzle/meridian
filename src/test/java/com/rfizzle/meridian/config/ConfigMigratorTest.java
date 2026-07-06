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
