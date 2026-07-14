package com.rfizzle.meridian.gametest.enchantments;

import com.rfizzle.meridian.Meridian;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/**
 * Alchemist's Draw (bow / crossbow): a pure-data enchantment that gives a chance per level for a
 * fired tipped or spectral arrow not to be consumed, and never affects plain arrows (Infinity's
 * domain). The effect is the stock {@code minecraft:ammo_use} + {@code minecraft:remove_binomial}
 * gated to the two special arrow items, so these tests drive the shipped JSON through vanilla's own
 * {@link EnchantmentHelper#processAmmoUse} evaluator — the exact call {@code ProjectileWeaponItem}
 * makes on every bow/crossbow shot. A return of 0 means the arrow was refunded; 1 means it was
 * spent.
 */
public class AlchemistsDrawGameTest implements FabricGameTest {

    private static final int TRIALS = 256;

    private static Holder<Enchantment> meridian(GameTestHelper helper, String id) {
        return holder(helper, Meridian.id(id));
    }

    private static Holder<Enchantment> vanilla(GameTestHelper helper, String id) {
        return holder(helper, ResourceLocation.withDefaultNamespace(id));
    }

    private static Holder<Enchantment> holder(GameTestHelper helper, ResourceLocation id) {
        Registry<Enchantment> reg = helper.getLevel().registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT);
        return reg.getHolder(id).orElse(null);
    }

    private static ItemStack bow(Holder<Enchantment>... enchants) {
        ItemStack bow = new ItemStack(Items.BOW);
        for (Holder<Enchantment> e : enchants) {
            bow.enchant(e, 2);
        }
        return bow;
    }

    /** How many of {@link #TRIALS} shots leave the ammo unspent (processAmmoUse returns 0). */
    private static int refunds(ServerLevel level, ItemStack weapon, ItemStack ammo) {
        int refunded = 0;
        for (int i = 0; i < TRIALS; i++) {
            if (EnchantmentHelper.processAmmoUse(level, weapon, ammo, 1) == 0) {
                refunded++;
            }
        }
        return refunded;
    }

    // --- Alchemist's Draw refunds tipped and spectral arrows sometimes, plain arrows never ---

    @GameTest(template = "meridian:empty_3x3")
    public void alchemistsDrawRefundsSpecialArrowsNotPlain(GameTestHelper helper) {
        Holder<Enchantment> draw = meridian(helper, "alchemists_draw");
        if (draw == null) { helper.fail("alchemists_draw not in registry"); return; }
        ServerLevel level = helper.getLevel();
        ItemStack weapon = bow(draw);

        int tipped = refunds(level, weapon, new ItemStack(Items.TIPPED_ARROW));
        int spectral = refunds(level, weapon, new ItemStack(Items.SPECTRAL_ARROW));
        int plain = refunds(level, weapon, new ItemStack(Items.ARROW));

        // Level II ≈ 40% refund. Over 256 trials the odds of zero or of all-256 refunds are
        // vanishingly small, so ">0 and <TRIALS" is a safe, non-flaky bound — never free, never nothing.
        if (tipped <= 0 || tipped >= TRIALS) {
            helper.fail("Alchemist's Draw should sometimes (but not always) refund tipped arrows, got "
                    + tipped + "/" + TRIALS);
            return;
        }
        if (spectral <= 0 || spectral >= TRIALS) {
            helper.fail("Alchemist's Draw should sometimes (but not always) refund spectral arrows, got "
                    + spectral + "/" + TRIALS);
            return;
        }
        if (plain != 0) {
            helper.fail("Alchemist's Draw must never refund plain arrows, got " + plain + "/" + TRIALS);
            return;
        }
        helper.succeed();
    }

    // --- Alchemist's Draw and Infinity coexist: Infinity owns plain arrows, Draw owns the rest ---

    @GameTest(template = "meridian:empty_3x3")
    public void alchemistsDrawCoexistsWithInfinity(GameTestHelper helper) {
        Holder<Enchantment> draw = meridian(helper, "alchemists_draw");
        Holder<Enchantment> infinity = vanilla(helper, "infinity");
        if (draw == null) { helper.fail("alchemists_draw not in registry"); return; }
        if (infinity == null) { helper.fail("infinity not in registry"); return; }
        ServerLevel level = helper.getLevel();
        ItemStack weapon = bow(draw);
        weapon.enchant(infinity, 1); // Infinity is max level 1

        int plain = refunds(level, weapon, new ItemStack(Items.ARROW));
        int tipped = refunds(level, weapon, new ItemStack(Items.TIPPED_ARROW));

        // Infinity sets plain-arrow ammo use to 0 unconditionally, so every plain shot is free — and
        // Alchemist's Draw's predicate never matches a plain arrow, so it changes nothing there.
        if (plain != TRIALS) {
            helper.fail("Infinity should make every plain arrow free even alongside Alchemist's Draw, got "
                    + plain + "/" + TRIALS);
            return;
        }
        // Tipped arrows fall outside Infinity's predicate, so only Alchemist's Draw's binomial applies —
        // still sometimes-but-not-always, exactly as without Infinity present.
        if (tipped <= 0 || tipped >= TRIALS) {
            helper.fail("Tipped arrows should stay binomial under Alchemist's Draw with Infinity present, got "
                    + tipped + "/" + TRIALS);
            return;
        }
        helper.succeed();
    }
}
