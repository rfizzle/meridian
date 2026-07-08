package com.rfizzle.meridian.client.config;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.config.MeridianConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Behavioral coverage for the server→client config-fallback precedence in {@link ClientMeridianConfig}
 * (#186). The wire codec is covered separately by {@code PayloadCodecTest}; this pins the resolution
 * semantics {@code effective()} implements: server-synced values win while connected, and the local
 * config is the fallback both before a sync arrives and after disconnect.
 *
 * <p>Tier 1 — no {@code net.minecraft}/{@code Bootstrap} needed: {@link MeridianConfig} is a plain
 * POJO and precedence is resolved from static holders. The local config is seeded into
 * {@link Meridian}'s private static field by reflection (there is no public setter, and
 * {@link Meridian#reloadConfig()} would read the real on-disk {@code config/meridian.json}), then
 * restored in teardown so no static state leaks across test classes.
 */
class ClientConfigPrecedenceTest {

    // Deliberately non-default (the class default is 50) so the value assertions fail if effective()
    // ever returns a freshly-constructed config instead of the seeded local instance.
    private static final int LOCAL_MAX_ETERNA = 33;
    private static final int SERVER_MAX_ETERNA = 77;

    private MeridianConfig localConfig;
    private MeridianConfig priorConfig;

    @BeforeEach
    void seedLocalConfig() throws ReflectiveOperationException {
        localConfig = new MeridianConfig();
        localConfig.enchantingTable.maxEterna = LOCAL_MAX_ETERNA;

        Field field = Meridian.class.getDeclaredField("config");
        field.setAccessible(true);
        priorConfig = (MeridianConfig) field.get(null);
        field.set(null, localConfig);

        ClientMeridianConfig.clear();
    }

    @AfterEach
    void restore() throws ReflectiveOperationException {
        ClientMeridianConfig.clear();

        Field field = Meridian.class.getDeclaredField("config");
        field.setAccessible(true);
        field.set(null, priorConfig);
    }

    @Test
    void unsynced_resolvesToLocalConfig() {
        // AC1: with no server sync received, the client resolves to its own config.
        assertSame(localConfig, ClientMeridianConfig.effective(),
                "unsynced client must resolve to the local config");
        assertEquals(LOCAL_MAX_ETERNA, ClientMeridianConfig.effective().enchantingTable.maxEterna,
                "unsynced client must read local values");
    }

    @Test
    void afterSync_serverValuesWin() {
        // AC2: after a sync is applied, the server's values win over the client's local config.
        MeridianConfig serverConfig = new MeridianConfig();
        serverConfig.enchantingTable.maxEterna = SERVER_MAX_ETERNA;

        ClientMeridianConfig.setServerConfig(serverConfig);

        assertSame(serverConfig, ClientMeridianConfig.effective(),
                "synced client must resolve to the server config");
        assertEquals(SERVER_MAX_ETERNA, ClientMeridianConfig.effective().enchantingTable.maxEterna,
                "server value must win over the local value while synced");
    }

    @Test
    void afterDisconnect_fallsBackToLocalConfig() {
        // AC3: after disconnect the synced copy is cleared and the client falls back to its own config.
        MeridianConfig serverConfig = new MeridianConfig();
        serverConfig.enchantingTable.maxEterna = SERVER_MAX_ETERNA;
        ClientMeridianConfig.setServerConfig(serverConfig);
        assertSame(serverConfig, ClientMeridianConfig.effective(), "precondition: synced value active");

        ClientMeridianConfig.clear();

        assertSame(localConfig, ClientMeridianConfig.effective(),
                "after disconnect the client must fall back to the local config");
        assertEquals(LOCAL_MAX_ETERNA, ClientMeridianConfig.effective().enchantingTable.maxEterna,
                "after disconnect the client must read local values again");
    }
}
