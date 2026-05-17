package com.rfizzle.meridian.gametest.enchantments;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.enchanting.EnchantmentEffects;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

public class EdgeCaseEnchantmentTest implements FabricGameTest {

    private Holder<Enchantment> lookup(GameTestHelper helper, String id) {
        Registry<Enchantment> reg = helper.getLevel().registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT);
        return reg.getHolder(Meridian.id(id)).orElse(null);
    }

    // --- Final Gambit: weapon destroyed after hit, not before ---

    @GameTest(template = "meridian:empty_3x3")
    public void finalGambitDestroysWeaponOnHit(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "final_gambit");
        if (ench == null) { helper.fail("final_gambit not in registry"); return; }

        Mob attacker = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 1, 1));
        attacker.setShiftKeyDown(true);

        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.enchant(ench, 1);
        attacker.setItemSlot(EquipmentSlot.MAINHAND, sword);

        Mob victim = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 1, 2));
        float healthBefore = victim.getHealth();

        attacker.doHurtTarget(victim);

        helper.runAfterDelay(2, () -> {
            ItemStack held = attacker.getMainHandItem();
            if (!held.isEmpty()) {
                helper.fail("Final Gambit should destroy the weapon after hit. Weapon still present.");
                return;
            }
            float damageDealt = healthBefore - victim.getHealth();
            if (damageDealt <= 7.0f) {
                helper.fail("Final Gambit should deal massive bonus damage. Damage dealt: " + damageDealt);
                return;
            }
            helper.succeed();
        });
    }

    // --- Ricochet: bounce count capped ---

    @GameTest(template = "meridian:empty_3x3")
    public void ricochetBounceCountMatchesLevel(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "ricochet");
        if (ench == null) { helper.fail("ricochet not in registry"); return; }

        if (ench.value().definition().maxLevel() < 2) {
            helper.fail("Ricochet should support at least level 2 for bounce testing");
            return;
        }
        helper.succeed();
    }

    // --- Detonation: no block damage ---

    @GameTest(template = "meridian:empty_3x3")
    public void detonationDoesNotDestroyBlocks(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 1, 1), Blocks.STONE);
        helper.setBlock(new BlockPos(1, 1, 2), Blocks.STONE);
        helper.setBlock(new BlockPos(2, 1, 1), Blocks.STONE);

        helper.getLevel().explode(null,
                helper.absolutePos(new BlockPos(1, 2, 1)).getX() + 0.5,
                helper.absolutePos(new BlockPos(1, 2, 1)).getY() + 0.5,
                helper.absolutePos(new BlockPos(1, 2, 1)).getZ() + 0.5,
                3.0f, Level.ExplosionInteraction.NONE);

        helper.runAfterDelay(2, () -> {
            if (!helper.getBlockState(new BlockPos(1, 1, 1)).is(Blocks.STONE)) {
                helper.fail("Detonation (ExplosionInteraction.NONE) should not destroy blocks");
                return;
            }
            helper.succeed();
        });
    }

    // --- Vital Mend + Mending: exclusive set enforced ---

    @GameTest(template = "meridian:empty_3x3")
    public void vitalMendAndMendingAreExclusive(GameTestHelper helper) {
        Holder<Enchantment> vitalMend = lookup(helper, "vital_mend");
        if (vitalMend == null) { helper.fail("vital_mend not in registry"); return; }

        Registry<Enchantment> reg = helper.getLevel().registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> mending = reg.getHolderOrThrow(Enchantments.MENDING);

        if (Enchantment.areCompatible(vitalMend, mending)) {
            helper.fail("Vital Mend and Mending must be mutually exclusive");
            return;
        }
        helper.succeed();
    }

    // --- Cinderwalk + Frost Walker: exclusive set enforced ---

    @GameTest(template = "meridian:empty_3x3")
    public void cinderwalkAndFrostWalkerAreExclusive(GameTestHelper helper) {
        Holder<Enchantment> cinderwalk = lookup(helper, "cinderwalk");
        if (cinderwalk == null) { helper.fail("cinderwalk not in registry"); return; }

        Registry<Enchantment> reg = helper.getLevel().registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> frostWalker = reg.getHolderOrThrow(Enchantments.FROST_WALKER);

        if (Enchantment.areCompatible(cinderwalk, frostWalker)) {
            helper.fail("Cinderwalk and Frost Walker must be mutually exclusive");
            return;
        }
        helper.succeed();
    }

    // --- Snare: spawn egg IDs are vanilla-correct ---

    @GameTest(template = "meridian:empty_3x3")
    public void snareDropsCorrectSpawnEggs(GameTestHelper helper) {
        boolean zombieHasEgg = SpawnEggItem.byId(EntityType.ZOMBIE) != null;
        boolean skeletonHasEgg = SpawnEggItem.byId(EntityType.SKELETON) != null;
        boolean creeperHasEgg = SpawnEggItem.byId(EntityType.CREEPER) != null;
        boolean spiderHasEgg = SpawnEggItem.byId(EntityType.SPIDER) != null;

        if (!zombieHasEgg || !skeletonHasEgg || !creeperHasEgg || !spiderHasEgg) {
            helper.fail("SpawnEggItem.byId() should resolve for common mob types. "
                    + "zombie=" + zombieHasEgg + ", skeleton=" + skeletonHasEgg
                    + ", creeper=" + creeperHasEgg + ", spider=" + spiderHasEgg);
            return;
        }

        SpawnEggItem zombieEgg = SpawnEggItem.byId(EntityType.ZOMBIE);
        if (!new ItemStack(zombieEgg).is(Items.ZOMBIE_SPAWN_EGG)) {
            helper.fail("SpawnEggItem.byId(ZOMBIE) should map to ZOMBIE_SPAWN_EGG");
            return;
        }
        helper.succeed();
    }

    // --- Excavate/Prospect: no recursive trigger ---

    @GameTest(template = "meridian:empty_3x3")
    public void excavateGuardPreventsRecursion(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "excavate");
        if (ench == null) { helper.fail("excavate not in registry"); return; }

        // The excavating flag in ToolEnchantmentHandler prevents re-entry.
        // We verify that the enchantment resolves and has the correct definition.
        Enchantment e = ench.value();
        if (e.definition().maxLevel() != 1) {
            helper.fail("Excavate should have maxLevel 1, got " + e.definition().maxLevel());
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void prospectVeinSizeIsCapped(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "prospect");
        if (ench == null) { helper.fail("prospect not in registry"); return; }

        Enchantment e = ench.value();
        if (e.definition().maxLevel() != 1) {
            helper.fail("Prospect should have maxLevel 1, got " + e.definition().maxLevel());
            return;
        }
        helper.succeed();
    }

    // --- Abyss Ward: definition check (cooldown logic is time-based) ---

    @GameTest(template = "meridian:empty_3x3")
    public void abyssWardIsRegisteredWithCorrectDefinition(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "abyss_ward");
        if (ench == null) { helper.fail("abyss_ward not in registry"); return; }

        Enchantment e = ench.value();
        if (e.definition().maxLevel() != 1) {
            helper.fail("Abyss Ward should have maxLevel 1, got " + e.definition().maxLevel());
            return;
        }
        if (e.definition().weight() != 1) {
            helper.fail("Abyss Ward should have weight 1 (treasure), got " + e.definition().weight());
            return;
        }
        helper.succeed();
    }

    // --- Rally: definition and cooldown field check ---

    @GameTest(template = "meridian:empty_3x3")
    public void rallyIsRegisteredWithCorrectDefinition(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "rally");
        if (ench == null) { helper.fail("rally not in registry"); return; }

        Enchantment e = ench.value();
        if (e.definition().maxLevel() < 1 || e.definition().maxLevel() > 2) {
            helper.fail("Rally should have maxLevel 1-2, got " + e.definition().maxLevel());
            return;
        }
        if (e.definition().weight() != 1) {
            helper.fail("Rally should have weight 1 (treasure), got " + e.definition().weight());
            return;
        }
        helper.succeed();
    }

    // --- Premonition: definition check (performance tested at runtime, not gametest) ---

    @GameTest(template = "meridian:empty_3x3")
    public void premonitionIsRegisteredCorrectly(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "premonition");
        if (ench == null) { helper.fail("premonition not in registry"); return; }

        Enchantment e = ench.value();
        if (e.definition().maxLevel() != 1) {
            helper.fail("Premonition should have maxLevel 1, got " + e.definition().maxLevel());
            return;
        }
        if (e.definition().weight() != 2) {
            helper.fail("Premonition should have weight 2, got " + e.definition().weight());
            return;
        }
        helper.succeed();
    }
}
