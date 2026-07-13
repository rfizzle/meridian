package com.rfizzle.meridian.gametest.enchantments;

import com.rfizzle.meridian.Meridian;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * Abyssal's depth-scaled damage reduction, exercised through the real {@code LivingEntity#hurt} path
 * that {@code AbyssalDamageMixin} injects into. The gametest structure sits well below sea level, so
 * a wearer standing in it is genuinely deep. Magic damage is used so armor never confounds the
 * measurement — the only reduction in play is Abyssal's own. Wearers are mobs (not creative mock
 * players), so the damage actually lands. The "no effect at or above sea level" boundary is covered
 * by {@code DefenseEnchantMathTest}.
 */
public class AbyssalGameTest implements FabricGameTest {

    private static final float DAMAGE = 10.0f;

    private Holder<Enchantment> lookup(GameTestHelper helper, String id) {
        Registry<Enchantment> reg = helper.getLevel().registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT);
        return reg.getHolder(Meridian.id(id)).orElse(null);
    }

    private float magicDamageLoss(LivingEntity entity) {
        float before = entity.getHealth();
        entity.hurt(entity.damageSources().magic(), DAMAGE);
        return before - entity.getHealth();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void abyssalReducesDamageWithDepth(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "abyssal");
        if (ench == null) { helper.fail("abyssal not in registry"); return; }

        // Sanity: the test structure must actually sit below sea level for Abyssal to engage.
        double depth = helper.getLevel().getSeaLevel() - helper.absolutePos(new BlockPos(1, 1, 1)).getY();
        if (depth <= 0) {
            helper.fail("test setup: structure is not below sea level (depth " + depth + ")");
            return;
        }

        Mob worn = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(0, 1, 1));
        Mob unworn = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(2, 1, 1));

        ItemStack abyssalChest = new ItemStack(Items.DIAMOND_CHESTPLATE);
        abyssalChest.enchant(ench, 3);
        worn.setItemSlot(EquipmentSlot.CHEST, abyssalChest);

        float wornLoss = magicDamageLoss(worn);
        float unwornLoss = magicDamageLoss(unworn);

        // Abyssal at depth soaks a real slice of the hit compared to no enchant at the same depth.
        if (wornLoss >= unwornLoss - 1.0f) {
            helper.fail("Abyssal at depth should reduce the hit: worn=" + wornLoss
                    + " vs unworn=" + unwornLoss);
            return;
        }
        helper.succeed();
    }
}
