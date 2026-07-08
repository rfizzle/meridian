// Tier: 3 (Fabric Gametest)
package com.rfizzle.meridian.event;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Proves the {@link EffectGuard} isolation boundary every enchantment-effect dispatch now flows
 * through: an effect that throws is logged and swallowed, so a sibling effect on the same entity —
 * and effects on other entities in the same tick/event — still run, and a value-returning effect
 * fails open to its caller's fallback instead of propagating into Fabric's event dispatch or a
 * mixin-injected vanilla method.
 *
 * <p>Mirrors the "deliberately misbehaving listener" pattern in {@code ReloadCallbackGameTest},
 * applied to the static-dispatch handlers, which — unlike {@code MeridianReloadCallback} — expose
 * no registrable event a test could plug a throwing lambda into; the shared guard is that seam.
 */
public class EnchantmentEffectIsolationGameTest implements FabricGameTest {

    @GameTest(template = "meridian:empty_3x3")
    public void throwingEffectDoesNotBreakSiblingsOrOtherEntities(GameTestHelper helper) {
        // Two distinct entity contexts stand in for two entities processed in a single handler pass.
        Pig first = helper.spawn(EntityType.PIG, new BlockPos(1, 1, 1));
        Pig second = helper.spawn(EntityType.PIG, new BlockPos(2, 1, 1));
        AtomicInteger siblingsRun = new AtomicInteger();

        // Per entity: a throwing effect immediately followed by a sentinel sibling — exactly the
        // shape of a handler's per-effect dispatch sequence. Neither the throw nor the loop over
        // entities may be interrupted.
        for (Pig context : new Pig[] {first, second}) {
            try {
                EffectGuard.run("test_throwing", context, () -> {
                    throw new IllegalStateException("deliberately misbehaving effect");
                });
                EffectGuard.run("test_sentinel", context, () -> siblingsRun.incrementAndGet());
            } catch (Exception e) {
                helper.fail("EffectGuard must isolate the throwing effect, but it escaped: " + e);
                return;
            }
        }

        if (siblingsRun.get() != 2) {
            helper.fail("Sibling effect after the throwing one must run for every entity (ran "
                    + siblingsRun.get() + " of 2)");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void throwingBooleanEffectFailsOpen(GameTestHelper helper) {
        Pig context = helper.spawn(EntityType.PIG, new BlockPos(1, 1, 1));

        // A value-returning effect (allow-damage, allow-death, arrow-consumed) that throws must
        // return the caller's fail-open fallback, never propagate.
        boolean failedOpen = EffectGuard.run("test_throwing", context, true, () -> {
            throw new IllegalStateException("deliberately misbehaving effect");
        });
        if (!failedOpen) {
            helper.fail("A throwing boolean effect must return the fail-open fallback (true)");
            return;
        }

        // A body that does not throw returns its own value, unchanged by the guard.
        boolean real = EffectGuard.run("test_ok", context, true, () -> false);
        if (real) {
            helper.fail("A non-throwing boolean effect must return its own value (false), not the fallback");
            return;
        }
        helper.succeed();
    }
}
