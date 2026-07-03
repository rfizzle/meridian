package com.rfizzle.meridian.item;

import java.util.List;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.config.MeridianConfig;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

/**
 * Everfeast ration — an infused food that feeds many times from one inventory slot. Eating
 * restores the base food's normal nutrition/saturation (carried by the stack's {@code FOOD}
 * component, copied from the base item at registration) and consumes one "bite"; the item is
 * consumed on the final bite.
 *
 * <p>Bites live in the stack's {@code CUSTOM_DATA} component ({@value #TAG_MAX_BITES} /
 * {@value #TAG_BITES_USED}), stamped per-stack from {@code everfeast.bites} at infusion time by
 * {@code MeridianEnchantmentMenu#applyCraftingRecipe}. Because the count lives in the stack,
 * a ration keeps the bite total it was created with even if the config changes later, and the
 * client tooltip always agrees with the server without a config sync ({@code CUSTOM_DATA} is
 * persistent and network-synchronized). A stack that never got stamped (creative tab,
 * recipe-viewer ghost) falls back to the live config value and is stamped on its first bite.
 *
 * <p>Deliberately <em>not</em> the vanilla damage components: a {@code MAX_DAMAGE}-carrying
 * stack is damageable, which would open every durability channel — Curse of Decay on the
 * spend side, and grindstone merges, anvil combines, and the mod's own Extraction Tome /
 * Tempered Core repair paths on the restore side. With bites in custom data the ration is
 * not damageable and not enchantable, so none of those mechanics can touch the count.
 */
public class EverfeastRationItem extends Item {

    public static final String TAG_MAX_BITES = "everfeast_max_bites";
    public static final String TAG_BITES_USED = "everfeast_bites_used";

    public EverfeastRationItem(Properties properties) {
        super(properties);
    }

    /** The bite count newly-infused rations are created with, from live config. */
    public static int configuredBites(MeridianConfig config) {
        MeridianConfig effective = config != null ? config : new MeridianConfig();
        return Math.max(1, effective.everfeast.bites);
    }

    /** Sizes the stack's bite pool from live config. Called when the infusion creates the stack. */
    public static void stampBites(ItemStack stack) {
        stampBites(stack, configuredBites(Meridian.getConfig()));
    }

    public static void stampBites(ItemStack stack, int bites) {
        int max = Math.max(1, bites);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putInt(TAG_MAX_BITES, max);
            tag.putInt(TAG_BITES_USED, 0);
        });
    }

    /** The bite total this stack was created with, or the live config value if never stamped. */
    public static int maxBites(ItemStack stack) {
        int stamped = customTag(stack, TAG_MAX_BITES);
        return stamped > 0 ? stamped : configuredBites(Meridian.getConfig());
    }

    public static int remainingBites(ItemStack stack) {
        return Math.max(0, maxBites(stack) - customTag(stack, TAG_BITES_USED));
    }

    /**
     * One bite off {@code stack}: returns the stack that should remain in hand — a copy with one
     * more bite spent, or {@link ItemStack#EMPTY} when this was the final bite. Also stamps the
     * pool on a never-stamped stack, freezing its total against later config changes.
     */
    public static ItemStack consumeBite(ItemStack stack) {
        int max = maxBites(stack);
        int used = customTag(stack, TAG_BITES_USED) + 1;
        if (used >= max) {
            return ItemStack.EMPTY;
        }
        ItemStack next = stack.copy();
        CustomData.update(DataComponents.CUSTOM_DATA, next, tag -> {
            tag.putInt(TAG_MAX_BITES, max);
            tag.putInt(TAG_BITES_USED, used);
        });
        return next;
    }

    private static int customTag(ItemStack stack, String key) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt(key);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        // Super applies the FOOD component (nutrition, saturation, sounds, stats) and shrinks the
        // stack; the pre-eat copy is what the bite math runs on, and its result replaces vanilla's.
        ItemStack before = stack.copy();
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (entity instanceof Player player && player.getAbilities().instabuild) {
            return result; // creative eating doesn't consume vanilla food; bites don't tick either
        }
        return consumeBite(before);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("info.meridian.everfeast.bites",
                remainingBites(stack), maxBites(stack)).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("info.meridian.everfeast").withStyle(ChatFormatting.GRAY));
    }
}
