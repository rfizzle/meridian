// Tier: 3 (Fabric Gametest)
package com.rfizzle.meridian.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.data.MeridianEnchantmentTagProvider;
import com.rfizzle.meridian.enchanting.RealEnchantmentHelper;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.RegistryOps;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctions;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

import java.util.List;
import java.util.Optional;

/**
 * In-game coverage for the published {@code meridian:rarity/*} enchantment tags (issue #140):
 * the four tags resolve in a live registry, partition the non-curse catalog by weight bucket,
 * and a vanilla {@code minecraft:enchant_randomly} loot function can roll from them by tag
 * reference — the exact consumption path sibling mods and datapacks use.
 */
public class RarityTagGameTest implements FabricGameTest {

    private static final List<TagKey<Enchantment>> RARITY_TAGS =
            MeridianEnchantmentTagProvider.RARITY_TAGS;

    @GameTest(template = "meridian:empty_3x3")
    public void rarityTagsResolveAndMatchWeightBuckets(GameTestHelper helper) {
        Registry<Enchantment> registry =
                helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT);

        long tagged = 0;
        for (int bucket = 0; bucket < RARITY_TAGS.size(); bucket++) {
            TagKey<Enchantment> tag = RARITY_TAGS.get(bucket);
            Optional<HolderSet.Named<Enchantment>> members = registry.getTag(tag);
            if (members.isEmpty() || members.get().size() == 0) {
                helper.fail(tag.location() + " did not resolve to a non-empty tag");
                return;
            }
            for (Holder<Enchantment> holder : members.get()) {
                int actual = RealEnchantmentHelper.rarityBucket(holder.value().getWeight());
                if (actual != bucket) {
                    helper.fail(holder.getRegisteredName() + " is in " + tag.location()
                            + " but its weight buckets to " + RARITY_TAGS.get(actual).location());
                    return;
                }
                tagged++;
            }
        }

        long expected = registry.holders()
                .filter(holder -> holder.key().location().getNamespace().equals(Meridian.MOD_ID))
                .filter(holder -> !holder.is(EnchantmentTags.CURSE))
                .count();
        if (tagged != expected) {
            helper.fail("rarity tags cover " + tagged + " enchantments, expected " + expected);
            return;
        }

        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void enchantRandomlyRollsFromRarityTag(GameTestHelper helper) {
        TagKey<Enchantment> rare = RARITY_TAGS.get(2);
        JsonElement json = JsonParser.parseString(
                "{\"function\": \"minecraft:enchant_randomly\", \"options\": \"#" + rare.location() + "\"}");
        RegistryOps<JsonElement> ops =
                RegistryOps.create(JsonOps.INSTANCE, helper.getLevel().registryAccess());
        LootItemFunction function = LootItemFunctions.ROOT_CODEC.parse(ops, json)
                .getOrThrow(message -> new IllegalStateException(
                        "enchant_randomly with a rarity-tag options field failed to parse: " + message));

        LootParams params = new LootParams.Builder(helper.getLevel())
                .create(LootContextParamSets.EMPTY);
        LootContext context = new LootContext.Builder(params).create(Optional.empty());
        ItemStack book = function.apply(new ItemStack(Items.BOOK), context);

        ItemEnchantments enchantments = EnchantmentHelper.getEnchantmentsForCrafting(book);
        if (enchantments.size() != 1) {
            helper.fail("expected exactly one rolled enchantment, got " + enchantments.size());
            return;
        }
        Holder<Enchantment> rolled = enchantments.keySet().iterator().next();
        if (!rolled.is(rare)) {
            helper.fail("rolled " + rolled.getRegisteredName() + " which is not in #" + rare.location());
            return;
        }

        helper.succeed();
    }
}
