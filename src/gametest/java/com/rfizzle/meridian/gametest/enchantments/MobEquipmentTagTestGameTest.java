package com.rfizzle.meridian.gametest.enchantments;

import com.rfizzle.meridian.Meridian;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Verifies the {@code meridian:mob_equipment} provider contract — a curated pool of combat/armor
 * enchants for sibling mods scaling hostile-mob gear. Asserts the tag exists, carries the expected
 * inclusions, and keeps the documented exclusions out.
 */
public class MobEquipmentTagTestGameTest implements FabricGameTest {

    private static final TagKey<Enchantment> MOB_EQUIPMENT = TagKey.create(
            Registries.ENCHANTMENT, Meridian.id("mob_equipment"));

    private static final List<String> EXPECTED_PRESENT = List.of(
            "keen_edge",   // melee bonus damage
            "shackle",     // on-hit debuff
            "bulwark",     // armor protection
            "frostguard",  // defensive retaliation
            "gale_shot");  // ranged combat

    private static final List<String> EXPECTED_ABSENT = List.of(
            "excavate",     // mining
            "true_flight",  // ranged utility, not combat
            "bloodrage",    // treasure-tier swing
            "ricochet",     // ranged utility
            "curse_of_decay"); // curse

    @GameTest(template = "meridian:empty_3x3")
    public void mobEquipmentTagIsCuratedAsDocumented(GameTestHelper helper) {
        Registry<Enchantment> reg = helper.getLevel().registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT);

        Optional<HolderSet.Named<Enchantment>> tag = reg.getTag(MOB_EQUIPMENT);
        if (tag.isEmpty() || tag.get().size() == 0) {
            helper.fail("meridian:mob_equipment tag is missing or empty");
            return;
        }

        Set<ResourceLocation> members = tag.get().stream()
                .map(Holder::unwrapKey)
                .filter(Optional::isPresent)
                .map(k -> k.get().location())
                .collect(Collectors.toSet());

        for (String id : EXPECTED_PRESENT) {
            if (!members.contains(Meridian.id(id))) {
                helper.fail("meridian:mob_equipment should contain " + id + " but does not");
                return;
            }
        }
        for (String id : EXPECTED_ABSENT) {
            if (members.contains(Meridian.id(id))) {
                helper.fail("meridian:mob_equipment should not contain " + id + " but does");
                return;
            }
        }
        helper.succeed();
    }
}
