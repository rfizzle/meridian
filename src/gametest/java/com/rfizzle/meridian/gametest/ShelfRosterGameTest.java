// Tier: 3 (Fabric Gametest)
package com.rfizzle.meridian.gametest;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.shelf.EnchantingShelfBlock;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;

public class ShelfRosterGameTest implements FabricGameTest {

    private record ShelfSpec(String id, SoundType expectedSound, float expectedStrength) {}

    private static final ShelfSpec[] SHELF_SPECS = {
            // Wood tier (SoundType.WOOD, 0.75F)
            new ShelfSpec("beeshelf", SoundType.WOOD, 0.75F),
            new ShelfSpec("melonshelf", SoundType.WOOD, 0.75F),
            // Stone tier — baseline
            new ShelfSpec("stoneshelf", SoundType.STONE, 1.75F),
            // Stone tier — Nether
            new ShelfSpec("hellshelf", SoundType.STONE, 1.5F),
            new ShelfSpec("blazing_hellshelf", SoundType.STONE, 1.5F),
            new ShelfSpec("glowing_hellshelf", SoundType.STONE, 1.5F),
            new ShelfSpec("infused_hellshelf", SoundType.STONE, 1.5F),
            // Stone tier — Ocean
            new ShelfSpec("seashelf", SoundType.STONE, 1.5F),
            new ShelfSpec("heart_seashelf", SoundType.STONE, 1.5F),
            new ShelfSpec("crystal_seashelf", SoundType.STONE, 1.5F),
            new ShelfSpec("infused_seashelf", SoundType.STONE, 1.5F),
            // Stone tier — End
            new ShelfSpec("endshelf", SoundType.STONE, 4.5F),
            new ShelfSpec("pearl_endshelf", SoundType.STONE, 4.5F),
            new ShelfSpec("draconic_endshelf", SoundType.STONE, 5.0F),
            // Stone tier — Deep
            new ShelfSpec("deepshelf", SoundType.STONE, 2.5F),
            new ShelfSpec("dormant_deepshelf", SoundType.STONE, 2.5F),
            new ShelfSpec("echoing_deepshelf", SoundType.STONE, 2.5F),
            new ShelfSpec("soul_touched_deepshelf", SoundType.STONE, 2.5F),
            // Sculk tier
            new ShelfSpec("echoing_sculkshelf", SoundType.STONE, 3.5F),
            new ShelfSpec("soul_touched_sculkshelf", SoundType.STONE, 3.5F),
            // Utility — sight
            new ShelfSpec("sightshelf", SoundType.STONE, 1.5F),
            new ShelfSpec("sightshelf_t2", SoundType.STONE, 1.5F),
            // Utility — rectification
            new ShelfSpec("rectifier", SoundType.STONE, 1.5F),
            new ShelfSpec("rectifier_t2", SoundType.STONE, 1.5F),
            new ShelfSpec("rectifier_t3", SoundType.STONE, 1.5F),
    };

    @GameTest(template = "meridian:empty_3x3")
    public void rosterContainsExactly25Shelves(GameTestHelper helper) {
        if (SHELF_SPECS.length != 25) {
            helper.fail("Expected 25 shelf specs, got " + SHELF_SPECS.length);
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void everyShelfIsAnEnchantingShelfBlock(GameTestHelper helper) {
        for (ShelfSpec spec : SHELF_SPECS) {
            ResourceLocation loc = Meridian.id(spec.id);
            if (!BuiltInRegistries.BLOCK.containsKey(loc)) {
                helper.fail("Block not registered: " + loc);
                return;
            }
            Block block = BuiltInRegistries.BLOCK.get(loc);
            if (!(block instanceof EnchantingShelfBlock)) {
                helper.fail("Not an EnchantingShelfBlock: " + loc);
                return;
            }
        }
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void everyShelfSoundTypeMatchesDesign(GameTestHelper helper) {
        for (ShelfSpec spec : SHELF_SPECS) {
            ResourceLocation loc = Meridian.id(spec.id);
            Block block = BuiltInRegistries.BLOCK.get(loc);
            SoundType actual = block.defaultBlockState().getSoundType();
            if (actual != spec.expectedSound) {
                helper.fail(loc + " SoundType: expected "
                        + soundName(spec.expectedSound) + ", got " + soundName(actual));
                return;
            }
        }
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void everyShelfDestroySpeedMatchesDesign(GameTestHelper helper) {
        for (ShelfSpec spec : SHELF_SPECS) {
            ResourceLocation loc = Meridian.id(spec.id);
            Block block = BuiltInRegistries.BLOCK.get(loc);
            float actual = block.defaultBlockState().getDestroySpeed(helper.getLevel(), BlockPos.ZERO);
            if (Math.abs(actual - spec.expectedStrength) > 0.001F) {
                helper.fail(loc + " destroySpeed: expected " + spec.expectedStrength + ", got " + actual);
                return;
            }
        }
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void everyShelfExplosionResistanceMatchesDesign(GameTestHelper helper) {
        for (ShelfSpec spec : SHELF_SPECS) {
            ResourceLocation loc = Meridian.id(spec.id);
            Block block = BuiltInRegistries.BLOCK.get(loc);
            float actual = block.getExplosionResistance();
            if (Math.abs(actual - spec.expectedStrength) > 0.001F) {
                helper.fail(loc + " explosionResistance: expected " + spec.expectedStrength + ", got " + actual);
                return;
            }
        }
        helper.succeed();
    }

    private static String soundName(SoundType sound) {
        if (sound == SoundType.WOOD) return "WOOD";
        if (sound == SoundType.STONE) return "STONE";
        return sound.toString();
    }
}
