package com.rfizzle.meridian.attachment;

import com.rfizzle.meridian.Meridian;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Fabric attachment types for per-player state that must survive a world reload.
 */
public final class MeridianAttachments {

    /**
     * Items pulled off a dying player by the Tether enchantment, stashed until respawn. Persisting
     * this on the player entity is what lets the items survive a disconnect on the death screen: the
     * attachment is written to the player's data on disconnect and restored on reconnect, so a
     * later respawn still returns them. Not {@code copyOnDeath()} — the respawn hook moves the items
     * into the new player's inventory itself.
     */
    public static final AttachmentType<List<ItemStack>> TETHERED_ITEMS =
            AttachmentRegistry.createPersistent(Meridian.id("tethered_items"), ItemStack.OPTIONAL_CODEC.listOf());

    private MeridianAttachments() {}

    /** Forces class-load so the attachment types register during mod init. */
    public static void init() {}
}
