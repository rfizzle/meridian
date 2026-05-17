package com.rfizzle.meridian.shelf;

import com.rfizzle.meridian.enchanting.IEnchantingStatProvider;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Tier 2 — constructs an {@link EnchantingShelfBlock} locally and verifies its
 * {@link ParticleTheme} round-trips and that it implements {@link IEnchantingStatProvider}.
 * Registry resolution is covered by the Tier 3 gametest {@code RegistryGameTest}.
 */
class EnchantingShelfBlockTest {

    private static EnchantingShelfBlock shelf;

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        shelf = new EnchantingShelfBlock(
                BlockBehaviour.Properties.of()
                        .mapColor(MapColor.WOOD)
                        .strength(0.75F)
                        .sound(SoundType.WOOD),
                ParticleTheme.ENCHANT_FIRE);
    }

    @Test
    void implementsIEnchantingStatProvider() {
        assertInstanceOf(IEnchantingStatProvider.class, shelf,
                "shelf must implement IEnchantingStatProvider so the stat registry picks it up");
    }

    @Test
    void getParticleTheme_returnsConstructorValue() {
        assertSame(ParticleTheme.ENCHANT_FIRE, shelf.getParticleTheme(),
                "the theme passed to the constructor must round-trip through getParticleTheme");
    }
}
