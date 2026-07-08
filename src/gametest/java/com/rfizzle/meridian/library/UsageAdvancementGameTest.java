// Tier: 3 (Fabric Gametest)
package com.rfizzle.meridian.library;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.MeridianRegistry;
import com.rfizzle.meridian.gametest.MockPlayers;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Tier-3 coverage for the usage-triggered advancements added in #159: the mechanics grant their
 * advancement only when a real {@link ServerPlayer} drives them, and the automated
 * ({@code depositBookSilent}) library path — the one hoppers use — never touches player
 * advancements. Firing sits at the menu/mixin layer, so these paths cannot be reached from Tier-1.
 * Package-local to {@code com.rfizzle.meridian.library} so it can drive the package-private
 * {@code depositBookSilent} hopper path directly.
 */
public class UsageAdvancementGameTest implements FabricGameTest {

    private static final BlockPos LIB_POS = new BlockPos(2, 1, 2);
    private static final BlockPos ANVIL_POS = new BlockPos(1, 1, 1);

    @GameTest(template = "meridian:empty_5x5x5", timeoutTicks = 100, batch = "advDeposit")
    public void playerDepositGrantsAdvancement(GameTestHelper helper) {
        EnchantmentLibraryBlockEntity tile = placeLibrary(helper);
        if (tile == null) return;

        ServerPlayer player = MockPlayers.serverPlayerInLevel(helper);
        EnchantmentLibraryMenu menu = new EnchantmentLibraryMenu(1, player.getInventory(), tile);
        menu.getSlot(EnchantmentLibraryMenu.DEPOSIT_SLOT).set(sharpnessBook(helper));

        assertAdvancement(helper, player, "library_deposit", true,
                "player deposit should grant library_deposit");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_5x5x5", timeoutTicks = 100, batch = "advHopper")
    public void hopperDepositDoesNotGrantAdvancement(GameTestHelper helper) {
        EnchantmentLibraryBlockEntity tile = placeLibrary(helper);
        if (tile == null) return;

        ServerPlayer player = MockPlayers.serverPlayerInLevel(helper);
        // The automated storage path (LibraryStorageAdapter, hoppers) goes straight to the BE with
        // no player — this asserts that path grants nothing to a player standing by.
        tile.depositBookSilent(sharpnessBook(helper));

        assertAdvancement(helper, player, "library_deposit", false,
                "automated (hopper) deposit must not grant library_deposit");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_5x5x5", timeoutTicks = 100, batch = "advExtract")
    public void playerExtractGrantsAdvancement(GameTestHelper helper) {
        EnchantmentLibraryBlockEntity tile = placeLibrary(helper);
        if (tile == null) return;

        Registry<Enchantment> enchReg = helper.getLevel().registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT);
        // Seed the pool so the extract can succeed.
        for (int i = 0; i < 8; i++) {
            tile.depositBookSilent(sharpnessBook(helper));
        }

        ServerPlayer player = MockPlayers.serverPlayerInLevel(helper);
        EnchantmentLibraryMenu menu = new EnchantmentLibraryMenu(1, player.getInventory(), tile);
        Enchantment sharpness = enchReg.getOrThrow(Enchantments.SHARPNESS);
        int index = enchReg.getId(sharpness);

        boolean clicked = menu.clickMenuButton(player, index);
        if (!clicked) {
            helper.fail("Extraction click should succeed with a seeded pool");
            player.discard();
            return;
        }
        assertAdvancement(helper, player, "library_extract", true,
                "player extraction should grant library_extract");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_5x5x5", timeoutTicks = 100, batch = "advSalvage")
    public void takingScrapTomeOutputGrantsAdvancement(GameTestHelper helper) {
        helper.setBlock(ANVIL_POS, Blocks.ANVIL.defaultBlockState());
        ServerPlayer player = MockPlayers.serverPlayerInLevel(helper);
        player.experienceLevel = 30;
        AnvilMenu menu = new AnvilMenu(1, player.getInventory(),
                ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(ANVIL_POS)));

        Registry<Enchantment> reg = helper.getLevel().registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT);
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.enchant(reg.getHolderOrThrow(Enchantments.SHARPNESS), 5);

        menu.getSlot(0).set(sword);
        menu.getSlot(1).set(new ItemStack(MeridianRegistry.SCRAP_TOME));

        ItemStack output = menu.getSlot(2).getItem();
        if (output.isEmpty()) {
            helper.fail("Scrap tome should produce an output book to take");
            player.discard();
            return;
        }
        // Advancement fires from the onTake path, not the createResult preview.
        assertAdvancement(helper, player, "tome_salvage", false,
                "building the preview must not grant tome_salvage before the take");
        menu.getSlot(2).onTake(player, output);
        assertAdvancement(helper, player, "tome_salvage", true,
                "taking the salvage output should grant tome_salvage");
        player.discard();
        helper.succeed();
    }

    // --- Helpers ---

    private static ItemStack sharpnessBook(GameTestHelper helper) {
        Registry<Enchantment> enchReg = helper.getLevel().registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> sharpness = enchReg.getHolderOrThrow(Enchantments.SHARPNESS);
        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        mutable.set(sharpness, 1);
        book.set(DataComponents.STORED_ENCHANTMENTS, mutable.toImmutable());
        return book;
    }

    private static void assertAdvancement(GameTestHelper helper, ServerPlayer player,
                                          String path, boolean expectedDone, String message) {
        ResourceLocation id = Meridian.id(path);
        AdvancementHolder holder = helper.getLevel().getServer().getAdvancements().get(id);
        if (holder == null) {
            helper.fail("advancement " + id + " not loaded on the server");
            return;
        }
        boolean done = player.getAdvancements().getOrStartProgress(holder).isDone();
        if (done != expectedDone) {
            helper.fail(message + " (expected done=" + expectedDone + ", was " + done + ")");
        }
    }

    private EnchantmentLibraryBlockEntity placeLibrary(GameTestHelper helper) {
        Block libraryBlock = BuiltInRegistries.BLOCK.get(Meridian.id("library"));
        if (libraryBlock == Blocks.AIR) {
            helper.fail("library block not found in registry");
            return null;
        }
        helper.setBlock(LIB_POS, libraryBlock.defaultBlockState());
        BlockEntity be = helper.getLevel().getBlockEntity(helper.absolutePos(LIB_POS));
        if (!(be instanceof EnchantmentLibraryBlockEntity tile)) {
            helper.fail("library block entity missing after placement");
            return null;
        }
        return tile;
    }
}
