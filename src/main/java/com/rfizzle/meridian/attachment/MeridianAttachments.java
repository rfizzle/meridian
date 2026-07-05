package com.rfizzle.meridian.attachment;

import com.mojang.serialization.Codec;
import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.enchanting.DefenseEnchantMath;
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

    /**
     * Game time when Blink last fired for this entity, {@link DefenseEnchantMath#BLINK_NEVER_USED}
     * if never. {@code copyOnDeath()} because the lockout is a once-per-N-game-days budget, not
     * once-per-life: dying (and respawning) must not hand the wearer a fresh Blink early.
     */
    public static final AttachmentType<Long> BLINK_LAST_USED = AttachmentRegistry.<Long>builder()
            .persistent(Codec.LONG)
            .copyOnDeath()
            .initializer(() -> DefenseEnchantMath.BLINK_NEVER_USED)
            .buildAndRegister(Meridian.id("blink_last_used"));

    private MeridianAttachments() {}

    /** Forces class-load so the attachment types register during mod init. */
    public static void init() {}
}
