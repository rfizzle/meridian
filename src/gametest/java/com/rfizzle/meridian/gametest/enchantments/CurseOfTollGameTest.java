// Tier: 3 (Fabric Gametest)
package com.rfizzle.meridian.gametest.enchantments;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.enchanting.TollExperienceMath;
import com.rfizzle.meridian.gametest.util.MockPlayers;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * End-to-end coverage for {@code TollExperienceMixin} — the Curse of Toll redirect that skims
 * collected experience off the orb-pickup path. The per-level fraction is unit-tested in
 * {@code TollExperienceMathTest}; this drives the redirect through the real
 * {@code ExperienceOrb.playerTouch} → {@code Player.giveExperiencePoints} call so the wiring
 * (slot scan, orb pickup, XP award) is exercised, and confirms the tax is inert without the curse.
 */
public class CurseOfTollGameTest implements FabricGameTest {

    private static final int ORB_VALUE = 100;

    private Holder<Enchantment> curse(GameTestHelper helper, String id) {
        Registry<Enchantment> reg = helper.getLevel().registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT);
        return reg.getHolder(Meridian.id(id)).orElse(null);
    }

    /** Places the player at the structure origin, drops an orb worth {@link #ORB_VALUE} on them,
     * and returns the total experience they collected from it (they start at 0). */
    private int collectOrbXp(GameTestHelper helper, ServerPlayer player) {
        ServerLevel level = helper.getLevel();
        BlockPos abs = helper.absolutePos(new BlockPos(1, 2, 1));
        player.teleportTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5);
        player.takeXpDelay = 0;
        ExperienceOrb orb = new ExperienceOrb(level, player.getX(), player.getY(), player.getZ(), ORB_VALUE);
        orb.playerTouch(player);
        return player.totalExperience;
    }

    // Not worn: the wearer collects the full orb value; the redirect must leave it alone.
    @GameTest(template = "meridian:empty_3x3")
    public void unwornCollectsFullExperience(GameTestHelper helper) {
        ServerPlayer player = MockPlayers.serverPlayerInLevel(helper);
        try {
            int collected = collectOrbXp(helper, player);
            if (collected != ORB_VALUE) {
                helper.fail("Without Curse of Toll the wearer must keep the full orb value "
                        + ORB_VALUE + ", collected " + collected);
                return;
            }
            helper.succeed();
        } finally {
            MockPlayers.retire(player);
        }
    }

    // Worn at level II: the collected experience is reduced by the curse's per-level fraction.
    @GameTest(template = "meridian:empty_3x3")
    public void wornReducesCollectedExperience(GameTestHelper helper) {
        Holder<Enchantment> ench = curse(helper, "curse_of_toll");
        if (ench == null) { helper.fail("curse_of_toll not in registry"); return; }

        ServerPlayer player = MockPlayers.serverPlayerInLevel(helper);
        try {
            ItemStack chest = new ItemStack(Items.DIAMOND_CHESTPLATE);
            chest.enchant(ench, 2);
            player.setItemSlot(EquipmentSlot.CHEST, chest);

            int collected = collectOrbXp(helper, player);
            int expected = TollExperienceMath.reduce(ORB_VALUE, 2);
            if (collected != expected) {
                helper.fail("Curse of Toll II should reduce the collected " + ORB_VALUE
                        + " XP to " + expected + ", got " + collected);
                return;
            }
            if (collected >= ORB_VALUE) {
                helper.fail("Curse of Toll must reduce collected XP below the raw orb value");
                return;
            }
            helper.succeed();
        } finally {
            MockPlayers.retire(player);
        }
    }
}
