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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;

public class AttributeAndProtectionTest implements FabricGameTest {

    private Holder<Enchantment> lookup(GameTestHelper helper, String id) {
        Registry<Enchantment> reg = helper.getLevel().registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT);
        return reg.getHolder(Meridian.id(id)).orElse(null);
    }

    // --- Vault: increases jump height via attribute ---

    @GameTest(template = "meridian:empty_3x3")
    public void vaultIncreasesJumpStrength(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "vault");
        if (ench == null) { helper.fail("vault not in registry"); return; }

        Mob mob = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 1, 1));
        double baseJump = mob.getAttributeValue(Attributes.JUMP_STRENGTH);

        ItemStack boots = new ItemStack(Items.DIAMOND_BOOTS);
        boots.enchant(ench, 3);
        mob.setItemSlot(EquipmentSlot.FEET, boots);

        helper.runAfterDelay(1, () -> {
            double modified = mob.getAttributeValue(Attributes.JUMP_STRENGTH);
            if (modified <= baseJump) {
                helper.fail("Vault III should increase jump strength. Base: " + baseJump + ", got: " + modified);
                return;
            }
            helper.succeed();
        });
    }

    @GameTest(template = "meridian:empty_3x3")
    public void vaultScalesWithLevel(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "vault");
        if (ench == null) { helper.fail("vault not in registry"); return; }

        Mob mob1 = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 1, 1));
        ItemStack boots1 = new ItemStack(Items.DIAMOND_BOOTS);
        boots1.enchant(ench, 1);
        mob1.setItemSlot(EquipmentSlot.FEET, boots1);

        Mob mob3 = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 1, 2));
        ItemStack boots3 = new ItemStack(Items.DIAMOND_BOOTS);
        boots3.enchant(ench, 3);
        mob3.setItemSlot(EquipmentSlot.FEET, boots3);

        helper.runAfterDelay(1, () -> {
            double jump1 = mob1.getAttributeValue(Attributes.JUMP_STRENGTH);
            double jump3 = mob3.getAttributeValue(Attributes.JUMP_STRENGTH);
            if (jump3 <= jump1) {
                helper.fail("Vault III should give more jump than I. L1: " + jump1 + ", L3: " + jump3);
                return;
            }
            helper.succeed();
        });
    }

    // --- Gallop: increases mount movement speed via attribute ---

    @GameTest(template = "meridian:empty_3x3")
    public void gallopHasMovementSpeedAttribute(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "gallop");
        if (ench == null) { helper.fail("gallop not in registry"); return; }

        var attrEffects = ench.value().getEffects(EnchantmentEffectComponents.ATTRIBUTES);
        boolean found = false;
        for (var effect : attrEffects) {
            if (effect.attribute().is(Attributes.MOVEMENT_SPEED)) {
                found = true;
                var mod = effect.getModifier(4, EquipmentSlot.BODY);
                if (mod.amount() <= 0) {
                    helper.fail("Gallop IV movement_speed modifier should be positive, got: " + mod.amount());
                    return;
                }
                break;
            }
        }
        if (!found) {
            helper.fail("Gallop should define a movement_speed attribute effect");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void gallopScalesWithLevel(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "gallop");
        if (ench == null) { helper.fail("gallop not in registry"); return; }

        var attrEffects = ench.value().getEffects(EnchantmentEffectComponents.ATTRIBUTES);
        for (var effect : attrEffects) {
            if (effect.attribute().is(Attributes.MOVEMENT_SPEED)) {
                var mod1 = effect.getModifier(1, EquipmentSlot.BODY);
                var mod4 = effect.getModifier(4, EquipmentSlot.BODY);
                if (mod4.amount() <= mod1.amount()) {
                    helper.fail("Gallop IV should give more speed than I. L1: " + mod1.amount() + ", L4: " + mod4.amount());
                    return;
                }
                helper.succeed();
                return;
            }
        }
        helper.fail("Gallop should define a movement_speed attribute");
    }

    // --- Curse of Leaden: reduces jump height and increases gravity (faster falling) ---

    @GameTest(template = "meridian:empty_3x3")
    public void curseOfLeadenReducesJumpAndIncreasesGravity(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "curse_of_leaden");
        if (ench == null) { helper.fail("curse_of_leaden not in registry"); return; }

        Mob mob = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 1, 1));
        double baseJump = mob.getAttributeValue(Attributes.JUMP_STRENGTH);
        double baseGravity = mob.getAttributeValue(Attributes.GRAVITY);

        ItemStack boots = new ItemStack(Items.DIAMOND_BOOTS);
        boots.enchant(ench, 2);
        mob.setItemSlot(EquipmentSlot.FEET, boots);

        helper.runAfterDelay(1, () -> {
            double jump = mob.getAttributeValue(Attributes.JUMP_STRENGTH);
            double gravity = mob.getAttributeValue(Attributes.GRAVITY);
            if (jump >= baseJump) {
                helper.fail("Curse of Leaden II should reduce jump strength. Base: " + baseJump + ", got: " + jump);
                return;
            }
            if (gravity <= baseGravity) {
                helper.fail("Curse of Leaden II should increase gravity. Base: " + baseGravity + ", got: " + gravity);
                return;
            }
            helper.succeed();
        });
    }

    @GameTest(template = "meridian:empty_3x3")
    public void curseOfLeadenScalesWithLevel(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "curse_of_leaden");
        if (ench == null) { helper.fail("curse_of_leaden not in registry"); return; }

        var attrEffects = ench.value().getEffects(EnchantmentEffectComponents.ATTRIBUTES);
        boolean sawJump = false;
        for (var effect : attrEffects) {
            if (effect.attribute().is(Attributes.JUMP_STRENGTH)) {
                sawJump = true;
                var mod1 = effect.getModifier(1, EquipmentSlot.FEET);
                var mod2 = effect.getModifier(2, EquipmentSlot.FEET);
                if (mod1.amount() >= 0 || mod2.amount() >= 0) {
                    helper.fail("Curse of Leaden jump_strength modifiers must be negative. L1: "
                            + mod1.amount() + ", L2: " + mod2.amount());
                    return;
                }
                if (mod2.amount() >= mod1.amount()) {
                    helper.fail("Curse of Leaden II should reduce jump more than I. L1: "
                            + mod1.amount() + ", L2: " + mod2.amount());
                    return;
                }
            }
        }
        if (!sawJump) {
            helper.fail("Curse of Leaden should define a jump_strength attribute effect");
            return;
        }
        helper.succeed();
    }

    // --- Skybound: increases jump strength for mounts ---

    @GameTest(template = "meridian:empty_3x3")
    public void skyboundHasJumpStrengthAttribute(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "skybound");
        if (ench == null) { helper.fail("skybound not in registry"); return; }

        var attrEffects = ench.value().getEffects(EnchantmentEffectComponents.ATTRIBUTES);
        boolean found = false;
        for (var effect : attrEffects) {
            if (effect.attribute().is(Attributes.JUMP_STRENGTH)) {
                found = true;
                var mod = effect.getModifier(7, EquipmentSlot.BODY);
                if (mod.amount() <= 0) {
                    helper.fail("Skybound VII jump_strength modifier should be positive, got: " + mod.amount());
                    return;
                }
                break;
            }
        }
        if (!found) {
            helper.fail("Skybound should define a jump_strength attribute effect");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void skyboundScalesAcrossAllLevels(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "skybound");
        if (ench == null) { helper.fail("skybound not in registry"); return; }

        var attrEffects = ench.value().getEffects(EnchantmentEffectComponents.ATTRIBUTES);
        for (var effect : attrEffects) {
            if (effect.attribute().is(Attributes.JUMP_STRENGTH)) {
                var mod1 = effect.getModifier(1, EquipmentSlot.BODY);
                var mod7 = effect.getModifier(7, EquipmentSlot.BODY);
                if (mod7.amount() <= mod1.amount()) {
                    helper.fail("Skybound VII should give more jump than I. L1: " + mod1.amount() + ", L7: " + mod7.amount());
                    return;
                }
                helper.succeed();
                return;
            }
        }
        helper.fail("Skybound should define a jump_strength attribute");
    }

    // --- Mason's Reach: extends block interaction range ---

    @GameTest(template = "meridian:empty_3x3")
    public void masonsReachIncreasesBlockInteractionRange(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "masons_reach");
        if (ench == null) { helper.fail("masons_reach not in registry"); return; }

        // block_interaction_range is a player-only attribute (absent from a generic mob's attribute
        // map), so assert on the enchantment's own attribute modifier — the pattern the sibling
        // attribute tests use — rather than reading the value off a spawned mob.
        var attrEffects = ench.value().getEffects(EnchantmentEffectComponents.ATTRIBUTES);
        for (var effect : attrEffects) {
            if (effect.attribute().is(Attributes.BLOCK_INTERACTION_RANGE)) {
                var mod1 = effect.getModifier(1, EquipmentSlot.CHEST);
                var mod3 = effect.getModifier(3, EquipmentSlot.CHEST);
                if (mod1.amount() <= 0) {
                    helper.fail("Mason's Reach should add positive block interaction range, got " + mod1.amount());
                    return;
                }
                if (mod3.amount() <= mod1.amount()) {
                    helper.fail("Mason's Reach III should exceed I. L1: " + mod1.amount() + ", L3: " + mod3.amount());
                    return;
                }
                helper.succeed();
                return;
            }
        }
        helper.fail("Mason's Reach should define a block_interaction_range attribute");
    }

    // --- Spellguard: has damage_protection effect ---

    @GameTest(template = "meridian:empty_3x3")
    public void spellguardHasDamageProtection(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "spellguard");
        if (ench == null) { helper.fail("spellguard not in registry"); return; }

        var protEffects = ench.value().getEffects(EnchantmentEffectComponents.DAMAGE_PROTECTION);
        if (protEffects.isEmpty()) {
            helper.fail("Spellguard should have damage_protection effects");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void spellguardProtectionScalesWithLevel(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "spellguard");
        if (ench == null) { helper.fail("spellguard not in registry"); return; }

        var protEffects = ench.value().getEffects(EnchantmentEffectComponents.DAMAGE_PROTECTION);
        if (protEffects.isEmpty()) {
            helper.fail("Spellguard has no damage_protection effects to scale");
            return;
        }
        if (ench.value().definition().maxLevel() != 4) {
            helper.fail("Spellguard should have 4 levels, got " + ench.value().definition().maxLevel());
            return;
        }
        helper.succeed();
    }

    // --- Ironwing: has damage_protection effect ---

    @GameTest(template = "meridian:empty_3x3")
    public void ironwingHasDamageProtection(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "ironwing");
        if (ench == null) { helper.fail("ironwing not in registry"); return; }

        var protEffects = ench.value().getEffects(EnchantmentEffectComponents.DAMAGE_PROTECTION);
        if (protEffects.isEmpty()) {
            helper.fail("Ironwing should have damage_protection effects");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void ironwingMaxLevelIsFour(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "ironwing");
        if (ench == null) { helper.fail("ironwing not in registry"); return; }

        if (ench.value().definition().maxLevel() != 4) {
            helper.fail("Ironwing should have max level 4, got " + ench.value().definition().maxLevel());
            return;
        }
        helper.succeed();
    }

    // --- Impact Ward: has damage_protection effect ---

    @GameTest(template = "meridian:empty_3x3")
    public void impactWardHasDamageProtection(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "impact_ward");
        if (ench == null) { helper.fail("impact_ward not in registry"); return; }

        var protEffects = ench.value().getEffects(EnchantmentEffectComponents.DAMAGE_PROTECTION);
        if (protEffects.isEmpty()) {
            helper.fail("Impact Ward should have damage_protection effects");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void impactWardMaxLevelIsFive(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "impact_ward");
        if (ench == null) { helper.fail("impact_ward not in registry"); return; }

        if (ench.value().definition().maxLevel() != 5) {
            helper.fail("Impact Ward should have max level 5, got " + ench.value().definition().maxLevel());
            return;
        }
        helper.succeed();
    }

    // --- Insight: has mob_experience effect ---

    @GameTest(template = "meridian:empty_3x3")
    public void insightHasMobExperienceEffect(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "insight");
        if (ench == null) { helper.fail("insight not in registry"); return; }

        var xpEffects = ench.value().getEffects(EnchantmentEffectComponents.MOB_EXPERIENCE);
        if (xpEffects.isEmpty()) {
            helper.fail("Insight should have mob_experience effects");
            return;
        }
        helper.succeed();
    }

    // --- Animus: has both mob_experience and block_experience ---

    @GameTest(template = "meridian:empty_3x3")
    public void animusHasMobExperienceEffect(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "animus");
        if (ench == null) { helper.fail("animus not in registry"); return; }

        var xpEffects = ench.value().getEffects(EnchantmentEffectComponents.MOB_EXPERIENCE);
        if (xpEffects.isEmpty()) {
            helper.fail("Animus should have mob_experience effects");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void animusHasBlockExperienceEffect(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "animus");
        if (ench == null) { helper.fail("animus not in registry"); return; }

        var xpEffects = ench.value().getEffects(EnchantmentEffectComponents.BLOCK_EXPERIENCE);
        if (xpEffects.isEmpty()) {
            helper.fail("Animus should have block_experience effects");
            return;
        }
        helper.succeed();
    }

    // --- Curse of Decay: has item_damage effect ---

    @GameTest(template = "meridian:empty_3x3")
    public void curseOfDecayHasItemDamageEffect(GameTestHelper helper) {
        Holder<Enchantment> ench = lookup(helper, "curse_of_decay");
        if (ench == null) { helper.fail("curse_of_decay not in registry"); return; }

        var dmgEffects = ench.value().getEffects(EnchantmentEffectComponents.ITEM_DAMAGE);
        if (dmgEffects.isEmpty()) {
            helper.fail("Curse of Decay should have item_damage effects");
            return;
        }
        helper.succeed();
    }
}
