package com.rfizzle.meridian.event;

import com.rfizzle.meridian.attachment.MeridianAttachments;
import com.rfizzle.meridian.config.MeridianConfig;
import com.rfizzle.meridian.enchanting.GroomMath;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Groom — drives {@link GroomHandler#attemptGroom} and {@link GroomHandler#groomDropFor} directly
 * (the same core the {@code UseEntityCallback} shell calls) to verify the roster mapping, the
 * always-pays config path, per-animal cooldown gating, and the ineligible cases (babies, sheared
 * sheep).
 */
public class GroomGameTest implements FabricGameTest {

    private static final BlockPos SPOT = new BlockPos(1, 1, 1);

    /** A config whose Groom drop chance is 1.0 at both levels, so a roll is deterministic. */
    private static MeridianConfig alwaysDropConfig() {
        MeridianConfig config = new MeridianConfig();
        config.groom.chanceLevel1 = 1.0;
        config.groom.chanceLevel2 = 1.0;
        return config;
    }

    @GameTest(template = "meridian:empty_3x3")
    public void groomRosterMapsEachAnimalToItsDrop(GameTestHelper helper) {
        Chicken chicken = helper.spawn(EntityType.CHICKEN, SPOT);
        Cow cow = helper.spawn(EntityType.COW, SPOT);
        MushroomCow mooshroom = helper.spawn(EntityType.MOOSHROOM, SPOT);
        Rabbit rabbit = helper.spawn(EntityType.RABBIT, SPOT);
        Sheep sheep = helper.spawn(EntityType.SHEEP, SPOT);
        sheep.setSheared(false);

        assertDrop(helper, "chicken", GroomHandler.groomDropFor(chicken), Items.FEATHER);
        assertDrop(helper, "cow", GroomHandler.groomDropFor(cow), Items.LEATHER);
        assertDrop(helper, "mooshroom", GroomHandler.groomDropFor(mooshroom), Items.LEATHER);
        assertDrop(helper, "rabbit", GroomHandler.groomDropFor(rabbit), Items.RABBIT_HIDE);
        // Sheep drop is a wool of some color; assert it is a wool item.
        ItemStack woolDrop = GroomHandler.groomDropFor(sheep);
        if (woolDrop == null || !woolDrop.getItem().toString().endsWith("wool")) {
            helper.fail("Groom on an unsheared sheep should drop wool, got " + woolDrop);
            return;
        }
        helper.succeed();
    }

    private void assertDrop(GameTestHelper helper, String who, ItemStack actual, net.minecraft.world.item.Item expected) {
        if (actual == null || !actual.is(expected)) {
            helper.fail("Groom on " + who + " should drop " + expected + ", got " + actual);
        }
    }

    @GameTest(template = "meridian:empty_3x3")
    public void groomYieldsDropAndStampsCooldown(GameTestHelper helper) {
        Chicken chicken = helper.spawn(EntityType.CHICKEN, SPOT);
        RandomSource random = RandomSource.create(1L);

        boolean attempted = GroomHandler.attemptGroom(helper.getLevel(), chicken, 1, alwaysDropConfig(), random);
        if (!attempted) {
            helper.fail("Grooming an off-cooldown chicken should make an attempt");
            return;
        }
        long stamped = chicken.getAttachedOrElse(MeridianAttachments.GROOM_LAST_BRUSHED, GroomMath.NEVER_BRUSHED);
        if (stamped == GroomMath.NEVER_BRUSHED) {
            helper.fail("A groom attempt should stamp the per-animal cooldown");
            return;
        }
        helper.runAfterDelay(2, () -> {
            helper.assertItemEntityPresent(Items.FEATHER, SPOT, 3.0);
            helper.succeed();
        });
    }

    @GameTest(template = "meridian:empty_3x3")
    public void groomCooldownBlocksImmediateRebrush(GameTestHelper helper) {
        Chicken chicken = helper.spawn(EntityType.CHICKEN, SPOT);
        MeridianConfig config = alwaysDropConfig();
        RandomSource random = RandomSource.create(1L);

        boolean first = GroomHandler.attemptGroom(helper.getLevel(), chicken, 1, config, random);
        boolean second = GroomHandler.attemptGroom(helper.getLevel(), chicken, 1, config, random);
        if (!first) {
            helper.fail("First groom attempt should succeed");
        } else if (second) {
            helper.fail("A same-tick re-brush should be blocked by the per-animal cooldown");
        } else {
            helper.succeed();
        }
    }

    @GameTest(template = "meridian:empty_3x3")
    public void groomIgnoresBabies(GameTestHelper helper) {
        Chicken chick = helper.spawn(EntityType.CHICKEN, SPOT);
        chick.setBaby(true);

        boolean attempted = GroomHandler.attemptGroom(helper.getLevel(), chick, 2, alwaysDropConfig(), RandomSource.create(1L));
        if (attempted) {
            helper.fail("Groom should not affect baby animals");
        } else {
            helper.succeed();
        }
    }

    @GameTest(template = "meridian:empty_3x3")
    public void groomIgnoresShearedSheep(GameTestHelper helper) {
        Sheep sheep = helper.spawn(EntityType.SHEEP, SPOT);
        sheep.setSheared(true);

        if (GroomHandler.groomDropFor(sheep) != null) {
            helper.fail("A sheared sheep has no loose fleece to gather");
            return;
        }
        boolean attempted = GroomHandler.attemptGroom(helper.getLevel(), sheep, 1, alwaysDropConfig(), RandomSource.create(1L));
        if (attempted) {
            helper.fail("Groom should make no attempt on a sheared sheep");
        } else {
            helper.succeed();
        }
    }
}
