package com.rfizzle.meridian.shelf;

import com.rfizzle.meridian.MeridianRegistry;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * Central declaration of every shelf block shipped by Meridian. IDs are kept 1:1 with
 * Zenith so the ported datapack files under {@code data/meridian/enchanting_stats/}
 * resolve to the correct block on load — stat values themselves live in those JSON files, not
 * in this class.
 *
 * <p>Three physical tiers:
 * <ul>
 *   <li><b>Wood-tier</b> ({@link SoundType#WOOD}, strength {@code 0.75}) — soft organic shelves
 *       like {@code beeshelf} and {@code melonshelf}.</li>
 *   <li><b>Stone-tier</b> ({@link SoundType#STONE}, {@code requiresCorrectToolForDrops},
 *       strength {@code 1.5}–{@code 5.0}) — the themed biome roster plus the utility shelves.
 *       Utility shelves share stone properties because their distinction lives in stat JSON,
 *       not block behaviour.</li>
 *   <li><b>Sculk-tier</b> ({@link SoundType#STONE} + {@code randomTicks}, strength {@code 3.5})
 *       — the two reanimated sculk variants.</li>
 * </ul>
 *
 * <p>Every block here is constructed as a {@link EnchantingShelfBlock} carrying a
 * {@link ParticleTheme}; block entities for filtering/treasure shelves ship in a later story.
 *
 * <p>Registration is guarded inside {@link #register()} — callers must invoke it once during
 * {@code onInitialize} before the vanilla registries freeze. {@link MeridianRegistry}
 * already drives the call.
 */
public final class MeridianShelves {

    public static final EnchantingShelfBlock BEESHELF =
            woodShelf(MapColor.COLOR_YELLOW, 0.75F, ParticleTheme.ENCHANT);
    public static final EnchantingShelfBlock MELONSHELF =
            woodShelf(MapColor.COLOR_GREEN, 0.75F, ParticleTheme.ENCHANT);

    public static final EnchantingShelfBlock STONESHELF =
            stoneShelf(MapColor.STONE, 1.75F, ParticleTheme.ENCHANT);

    public static final EnchantingShelfBlock HELLSHELF =
            stoneShelf(MapColor.COLOR_BLACK, 1.5F, ParticleTheme.ENCHANT_FIRE);
    public static final EnchantingShelfBlock BLAZING_HELLSHELF =
            stoneShelf(MapColor.COLOR_BLACK, 1.5F, ParticleTheme.ENCHANT_FIRE);
    public static final EnchantingShelfBlock GLOWING_HELLSHELF =
            stoneShelf(MapColor.COLOR_BLACK, 1.5F, ParticleTheme.ENCHANT_FIRE);
    public static final EnchantingShelfBlock INFUSED_HELLSHELF =
            stoneShelf(MapColor.COLOR_BLACK, 1.5F, ParticleTheme.ENCHANT_FIRE);

    public static final EnchantingShelfBlock SEASHELF =
            stoneShelf(MapColor.COLOR_CYAN, 1.5F, ParticleTheme.ENCHANT_WATER);
    public static final EnchantingShelfBlock HEART_SEASHELF =
            stoneShelf(MapColor.COLOR_CYAN, 1.5F, ParticleTheme.ENCHANT_WATER);
    public static final EnchantingShelfBlock CRYSTAL_SEASHELF =
            stoneShelf(MapColor.COLOR_CYAN, 1.5F, ParticleTheme.ENCHANT_WATER);
    public static final EnchantingShelfBlock INFUSED_SEASHELF =
            stoneShelf(MapColor.COLOR_CYAN, 1.5F, ParticleTheme.ENCHANT_WATER);

    public static final EnchantingShelfBlock ENDSHELF =
            stoneShelf(MapColor.SAND, 4.5F, ParticleTheme.ENCHANT_END);
    public static final EnchantingShelfBlock PEARL_ENDSHELF =
            stoneShelf(MapColor.SAND, 4.5F, ParticleTheme.ENCHANT_END);
    public static final EnchantingShelfBlock DRACONIC_ENDSHELF =
            stoneShelf(MapColor.SAND, 5.0F, ParticleTheme.ENCHANT_END);

    public static final EnchantingShelfBlock DEEPSHELF =
            stoneShelf(MapColor.COLOR_BLACK, 2.5F, ParticleTheme.ENCHANT_SCULK);
    public static final EnchantingShelfBlock DORMANT_DEEPSHELF =
            stoneShelf(MapColor.COLOR_BLACK, 2.5F, ParticleTheme.ENCHANT_SCULK);
    public static final EnchantingShelfBlock ECHOING_DEEPSHELF =
            stoneShelf(MapColor.COLOR_BLACK, 2.5F, ParticleTheme.ENCHANT_SCULK);
    public static final EnchantingShelfBlock SOUL_TOUCHED_DEEPSHELF =
            stoneShelf(MapColor.COLOR_BLACK, 2.5F, ParticleTheme.ENCHANT_SCULK);

    public static final EnchantingShelfBlock ECHOING_SCULKSHELF =
            sculkShelf(ParticleTheme.ENCHANT_SCULK);
    public static final EnchantingShelfBlock SOUL_TOUCHED_SCULKSHELF =
            sculkShelf(ParticleTheme.ENCHANT_SCULK);

    public static final EnchantingShelfBlock SIGHTSHELF =
            stoneShelf(MapColor.COLOR_BLACK, 1.5F, ParticleTheme.ENCHANT_FIRE);
    public static final EnchantingShelfBlock SIGHTSHELF_T2 =
            stoneShelf(MapColor.COLOR_BLACK, 1.5F, ParticleTheme.ENCHANT_FIRE);

    public static final EnchantingShelfBlock RECTIFIER =
            stoneShelf(MapColor.COLOR_CYAN, 1.5F, ParticleTheme.ENCHANT_WATER);
    public static final EnchantingShelfBlock RECTIFIER_T2 =
            stoneShelf(MapColor.COLOR_BLACK, 1.5F, ParticleTheme.ENCHANT_FIRE);
    public static final EnchantingShelfBlock RECTIFIER_T3 =
            stoneShelf(MapColor.SAND, 1.5F, ParticleTheme.ENCHANT_END);

    private static boolean registered = false;

    private MeridianShelves() {
    }

    /**
     * Registers every shelf block and its companion {@link net.minecraft.world.item.BlockItem}
     * with the vanilla built-in registries. Safe to call more than once — the second call is a
     * no-op, so test bootstrapping can share state with the runtime initializer.
     */
    public static void register() {
        if (registered) return;
        registered = true;

        // Wood tier
        registerShelf("beeshelf", BEESHELF);
        registerShelf("melonshelf", MELONSHELF);

        // Stone tier — baseline
        registerShelf("stoneshelf", STONESHELF);

        // Stone tier — Nether (hellshelf family)
        registerShelf("hellshelf", HELLSHELF);
        registerShelf("blazing_hellshelf", BLAZING_HELLSHELF);
        registerShelf("glowing_hellshelf", GLOWING_HELLSHELF);
        registerShelf("infused_hellshelf", INFUSED_HELLSHELF);

        // Stone tier — Ocean (seashelf family)
        registerShelf("seashelf", SEASHELF);
        registerShelf("heart_seashelf", HEART_SEASHELF);
        registerShelf("crystal_seashelf", CRYSTAL_SEASHELF);
        registerShelf("infused_seashelf", INFUSED_SEASHELF);

        // Stone tier — End (endshelf family)
        registerShelf("endshelf", ENDSHELF);
        registerShelf("pearl_endshelf", PEARL_ENDSHELF);
        registerShelf("draconic_endshelf", DRACONIC_ENDSHELF);

        // Stone tier — Deep (deepshelf family)
        registerShelf("deepshelf", DEEPSHELF);
        registerShelf("dormant_deepshelf", DORMANT_DEEPSHELF);
        registerShelf("echoing_deepshelf", ECHOING_DEEPSHELF);
        registerShelf("soul_touched_deepshelf", SOUL_TOUCHED_DEEPSHELF);

        // Sculk tier
        registerShelf("echoing_sculkshelf", ECHOING_SCULKSHELF);
        registerShelf("soul_touched_sculkshelf", SOUL_TOUCHED_SCULKSHELF);

        // Utility tier — clue shelves
        registerShelf("sightshelf", SIGHTSHELF);
        registerShelf("sightshelf_t2", SIGHTSHELF_T2);

        // Utility tier — rectification shelves
        registerShelf("rectifier", RECTIFIER);
        registerShelf("rectifier_t2", RECTIFIER_T2);
        registerShelf("rectifier_t3", RECTIFIER_T3);
    }

    private static void registerShelf(String name, EnchantingShelfBlock block) {
        MeridianRegistry.registerBlock(name, block, new Item.Properties());
    }

    private static EnchantingShelfBlock woodShelf(MapColor color, float strength, ParticleTheme theme) {
        return new EnchantingShelfBlock(
                BlockBehaviour.Properties.of()
                        .mapColor(color)
                        .sound(SoundType.WOOD)
                        .strength(strength),
                theme);
    }

    private static EnchantingShelfBlock stoneShelf(MapColor color, float strength, ParticleTheme theme) {
        return new EnchantingShelfBlock(
                BlockBehaviour.Properties.of()
                        .mapColor(color)
                        .sound(SoundType.STONE)
                        .strength(strength)
                        .requiresCorrectToolForDrops(),
                theme);
    }

    private static SculkShelfBlock sculkShelf(ParticleTheme theme) {
        return new SculkShelfBlock(
                BlockBehaviour.Properties.of()
                        .mapColor(MapColor.COLOR_BLACK)
                        .sound(SoundType.STONE)
                        .strength(3.5F)
                        .randomTicks()
                        .requiresCorrectToolForDrops(),
                theme);
    }
}
