package com.rfizzle.meridian.event;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.attachment.MeridianAttachments;
import com.rfizzle.meridian.config.MeridianConfig;
import com.rfizzle.meridian.enchanting.EnchantmentEffects;
import com.rfizzle.meridian.enchanting.GroomMath;
import net.minecraft.util.RandomSource;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BrushItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Groom — brushing a farm animal with a Groom-enchanted brush has a per-level chance to gather the
 * animal's renewable drop (feather, wool, leather, rabbit hide), extending vanilla's armadillo
 * brushing to the rest of the pen. A per-animal cooldown, persisted on the animal
 * ({@link MeridianAttachments#GROOM_LAST_BRUSHED}), stops a pen from being farmed continuously:
 * each groom attempt off cooldown stamps it, whether or not the roll pays out. Armadillos are left
 * to vanilla (they already brush for scute).
 *
 * <p>Runs on {@link UseEntityCallback} — the interaction analogue of the block-use hooks in
 * {@link ToolEnchantmentHandler}. The shell here handles the item/entity plumbing and the brush
 * feedback (sound, durability, arm swing); the roster resolution, cooldown gating, and roll live in
 * {@link #attemptGroom} so they can be driven directly by tests.
 */
public final class GroomHandler {

    private GroomHandler() {}

    public static void register() {
        UseEntityCallback.EVENT.register(GroomHandler::onUseEntity);
    }

    private static InteractionResult onUseEntity(Player player, Level world, InteractionHand hand,
                                                 Entity entity, @Nullable EntityHitResult hitResult) {
        if (world.isClientSide()) return InteractionResult.PASS;
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;
        if (!(world instanceof ServerLevel serverLevel)) return InteractionResult.PASS;
        if (!(entity instanceof Animal animal)) return InteractionResult.PASS;

        ItemStack tool = player.getItemInHand(hand);
        if (!(tool.getItem() instanceof BrushItem)) return InteractionResult.PASS;

        int level = EnchantmentEffects.getEnchantmentLevel(tool, EnchantmentEffects.GROOM);
        if (level <= 0) return InteractionResult.PASS;

        boolean attempted = attemptGroom(serverLevel, animal, level, Meridian.getConfig(), serverLevel.getRandom());
        if (!attempted) {
            // Ineligible animal or still on cooldown — no feedback, fall through to vanilla.
            return InteractionResult.PASS;
        }

        serverLevel.playSound(null, animal.getX(), animal.getY(), animal.getZ(),
                SoundEvents.BRUSH_GENERIC, SoundSource.PLAYERS, 1.0F, 1.0F);
        tool.hurtAndBreak(1, serverPlayer, EquipmentSlot.MAINHAND);
        serverPlayer.swing(hand, true);
        return InteractionResult.CONSUME;
    }

    /**
     * Rolls a groom attempt on {@code animal} with a level-{@code enchantLevel} brush. Returns
     * {@code true} if an attempt was actually made — the animal is an eligible adult roster member
     * and was off cooldown — in which case the cooldown is stamped and, on a successful roll, the
     * drop is spawned at the animal. Returns {@code false} for an ineligible animal or one still on
     * cooldown, leaving no side effects.
     */
    static boolean attemptGroom(ServerLevel level, Animal animal, int enchantLevel,
                                @Nullable MeridianConfig config, RandomSource random) {
        if (enchantLevel <= 0 || animal.isBaby()) return false;

        ItemStack drop = groomDropFor(animal);
        if (drop == null) return false;

        int cooldownTicks = config != null ? config.groom.cooldownTicks : GroomMath.DEFAULT_COOLDOWN_TICKS;
        long now = level.getGameTime();
        long last = animal.getAttachedOrElse(MeridianAttachments.GROOM_LAST_BRUSHED, GroomMath.NEVER_BRUSHED);
        if (!GroomMath.cooldownElapsed(last, now, cooldownTicks)) return false;

        animal.setAttached(MeridianAttachments.GROOM_LAST_BRUSHED, now);

        double chanceLevel1 = config != null ? config.groom.chanceLevel1 : GroomMath.DEFAULT_CHANCE_LEVEL_1;
        double chanceLevel2 = config != null ? config.groom.chanceLevel2 : GroomMath.DEFAULT_CHANCE_LEVEL_2;
        if (random.nextDouble() < GroomMath.groomChance(enchantLevel, chanceLevel1, chanceLevel2)) {
            animal.spawnAtLocation(drop);
        }
        return true;
    }

    /**
     * The renewable drop for an eligible adult farm animal, or {@code null} if the animal is not in
     * the roster (or a sheared sheep, which has no loose fleece to gather). Cow covers mooshroom
     * ({@code MushroomCow extends Cow}).
     */
    @Nullable
    static ItemStack groomDropFor(Animal animal) {
        if (animal instanceof Chicken) {
            return new ItemStack(Items.FEATHER);
        }
        if (animal instanceof Cow) {
            return new ItemStack(Items.LEATHER);
        }
        if (animal instanceof Rabbit) {
            return new ItemStack(Items.RABBIT_HIDE);
        }
        if (animal instanceof Sheep sheep) {
            if (sheep.isSheared()) return null;
            return new ItemStack(woolByColor(sheep.getColor()));
        }
        return null;
    }

    private static Item woolByColor(DyeColor color) {
        return switch (color) {
            case WHITE -> Items.WHITE_WOOL;
            case ORANGE -> Items.ORANGE_WOOL;
            case MAGENTA -> Items.MAGENTA_WOOL;
            case LIGHT_BLUE -> Items.LIGHT_BLUE_WOOL;
            case YELLOW -> Items.YELLOW_WOOL;
            case LIME -> Items.LIME_WOOL;
            case PINK -> Items.PINK_WOOL;
            case GRAY -> Items.GRAY_WOOL;
            case LIGHT_GRAY -> Items.LIGHT_GRAY_WOOL;
            case CYAN -> Items.CYAN_WOOL;
            case PURPLE -> Items.PURPLE_WOOL;
            case BLUE -> Items.BLUE_WOOL;
            case BROWN -> Items.BROWN_WOOL;
            case GREEN -> Items.GREEN_WOOL;
            case RED -> Items.RED_WOOL;
            case BLACK -> Items.BLACK_WOOL;
        };
    }
}
