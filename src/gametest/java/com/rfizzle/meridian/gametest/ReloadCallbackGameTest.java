// Tier: 3 (Fabric Gametest)
package com.rfizzle.meridian.gametest;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.api.MeridianReloadCallback;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Verifies {@link MeridianReloadCallback} fires server-side at the end of the
 * {@code /meridian reload} code path ({@link Meridian#reloadConfig(MinecraftServer)} — the
 * exact method the command handler invokes).
 *
 * <p>Runs in its own batch: the reload re-reads the shared {@code config/meridian.json} and
 * repopulates the global enchantment info registry, so it must not interleave with the other
 * config-mutating batches (see ConfigReloadGameTest).
 */
public class ReloadCallbackGameTest implements FabricGameTest {

    @GameTest(template = "meridian:empty_3x3", batch = "meridianConfigMutation7")
    public void reloadFiresCallback(GameTestHelper helper) {
        AtomicInteger fired = new AtomicInteger();
        AtomicReference<MinecraftServer> received = new AtomicReference<>();
        MeridianReloadCallback.EVENT.register(server -> {
            fired.incrementAndGet();
            received.set(server);
        });

        MinecraftServer server = helper.getLevel().getServer();
        Meridian.reloadConfig(server);

        if (fired.get() != 1) {
            helper.fail("Expected reload callback to fire exactly once, fired " + fired.get());
            return;
        }
        if (received.get() != server) {
            helper.fail("Callback received the wrong server instance");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3", batch = "meridianConfigMutation8")
    public void throwingListenerIsIsolated(GameTestHelper helper) {
        AtomicInteger fired = new AtomicInteger();
        MeridianReloadCallback.EVENT.register(server -> {
            throw new IllegalStateException("deliberately misbehaving listener");
        });
        MeridianReloadCallback.EVENT.register(server -> fired.incrementAndGet());

        MinecraftServer server = helper.getLevel().getServer();
        try {
            Meridian.reloadConfig(server);
        } catch (Exception e) {
            helper.fail("Host must isolate listener exceptions, but reload threw: " + e);
            return;
        }
        if (fired.get() != 1) {
            helper.fail("Listener after the throwing one did not run (fired=" + fired.get() + ")");
            return;
        }
        helper.succeed();
    }
}
