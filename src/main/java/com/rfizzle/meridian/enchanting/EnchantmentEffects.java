package com.rfizzle.meridian.enchanting;

import com.rfizzle.meridian.Meridian;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

public final class EnchantmentEffects {

    public static final ResourceKey<Enchantment> PRISMATIC = key("prismatic");
    public static final ResourceKey<Enchantment> RENEWAL = key("renewal");
    public static final ResourceKey<Enchantment> VITAL_MEND = key("vital_mend");
    public static final ResourceKey<Enchantment> PLUNDER = key("plunder");
    public static final ResourceKey<Enchantment> TETHER = key("tether");
    public static final ResourceKey<Enchantment> CURSE_OF_SEALING = key("curse_of_sealing");
    public static final ResourceKey<Enchantment> AURIFY = key("aurify");
    public static final ResourceKey<Enchantment> QUELL = key("quell");
    public static final ResourceKey<Enchantment> FINAL_GAMBIT = key("final_gambit");
    public static final ResourceKey<Enchantment> SIPHON = key("siphon");
    public static final ResourceKey<Enchantment> SNARE = key("snare");
    public static final ResourceKey<Enchantment> SOUL_TAX = key("soul_tax");
    public static final ResourceKey<Enchantment> CLEAVE = key("cleave");
    public static final ResourceKey<Enchantment> TRUE_FLIGHT = key("true_flight");
    public static final ResourceKey<Enchantment> GALE_SHOT = key("gale_shot");
    public static final ResourceKey<Enchantment> RESONANCE = key("resonance");
    public static final ResourceKey<Enchantment> PERMAFROST = key("permafrost");
    public static final ResourceKey<Enchantment> DETONATION = key("detonation");
    public static final ResourceKey<Enchantment> RICOCHET = key("ricochet");
    public static final ResourceKey<Enchantment> STORMCALL = key("stormcall");
    public static final ResourceKey<Enchantment> GLACIAL_LANCE = key("glacial_lance");
    public static final ResourceKey<Enchantment> TEMPEST = key("tempest");
    public static final ResourceKey<Enchantment> SEISMIC_SLAM = key("seismic_slam");
    public static final ResourceKey<Enchantment> UPDRAFT = key("updraft");
    public static final ResourceKey<Enchantment> LUMINANCE = key("luminance");
    public static final ResourceKey<Enchantment> ABYSS_WARD = key("abyss_ward");
    public static final ResourceKey<Enchantment> PREMONITION = key("premonition");
    public static final ResourceKey<Enchantment> REPULSE = key("repulse");
    public static final ResourceKey<Enchantment> FROSTGUARD = key("frostguard");
    public static final ResourceKey<Enchantment> RALLY = key("rally");
    public static final ResourceKey<Enchantment> BLOODRAGE = key("bloodrage");
    public static final ResourceKey<Enchantment> ANTIDOTE = key("antidote");
    public static final ResourceKey<Enchantment> GRAVITAS = key("gravitas");
    public static final ResourceKey<Enchantment> SLIPSTREAM = key("slipstream");
    public static final ResourceKey<Enchantment> CINDERWALK = key("cinderwalk");
    public static final ResourceKey<Enchantment> STEADFAST = key("steadfast");
    public static final ResourceKey<Enchantment> EXCAVATE = key("excavate");
    public static final ResourceKey<Enchantment> PROSPECT = key("prospect");
    public static final ResourceKey<Enchantment> BOUNTY = key("bounty");
    public static final ResourceKey<Enchantment> FURROW = key("furrow");
    public static final ResourceKey<Enchantment> BECKON = key("beckon");
    public static final ResourceKey<Enchantment> TERRASCULPT = key("terrasculpt");
    public static final ResourceKey<Enchantment> GALLOP = key("gallop");
    public static final ResourceKey<Enchantment> TRAMPLE = key("trample");
    public static final ResourceKey<Enchantment> SKYBOUND = key("skybound");
    public static final ResourceKey<Enchantment> SADDLEGUARD = key("saddleguard");
    public static final ResourceKey<Enchantment> RETRIBUTION = key("retribution");
    public static final ResourceKey<Enchantment> PUMMEL = key("pummel");
    public static final ResourceKey<Enchantment> FORTIFY = key("fortify");

    private EnchantmentEffects() {}

    public static int getEnchantmentLevel(ItemStack stack, ResourceKey<Enchantment> key) {
        if (stack == null || stack.isEmpty()) return 0;
        for (var entry : stack.getEnchantments().entrySet()) {
            if (entry.getKey().is(key)) {
                return entry.getIntValue();
            }
        }
        return 0;
    }

    public static int getEquippedLevel(LivingEntity entity, ResourceKey<Enchantment> key, EquipmentSlot... slots) {
        int maxLevel = 0;
        for (EquipmentSlot slot : slots) {
            int level = getEnchantmentLevel(entity.getItemBySlot(slot), key);
            if (level > maxLevel) maxLevel = level;
        }
        return maxLevel;
    }

    private static ResourceKey<Enchantment> key(String path) {
        return ResourceKey.create(Registries.ENCHANTMENT, Meridian.id(path));
    }
}
