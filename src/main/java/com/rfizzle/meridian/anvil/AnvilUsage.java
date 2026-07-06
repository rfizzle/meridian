package com.rfizzle.meridian.anvil;

import java.util.function.Consumer;

import com.rfizzle.meridian.advancement.ModTriggers;
import net.minecraft.server.level.ServerPlayer;

/**
 * The Meridian anvil mechanic a claimed {@link AnvilResult} represents, used only to award the
 * matching usage advancement when the player actually <em>takes</em> the output. Handlers tag
 * their result via {@link AnvilResult#withUsage(AnvilUsage)}; the {@code onTake} hook in
 * {@link com.rfizzle.meridian.mixin.AnvilMenuMixin} calls {@link #award(ServerPlayer)} on the
 * pending result's usage. Tagging is orthogonal to the output itself — a preview built during
 * {@code createResult} never grants anything, because the trigger fires from the take path only.
 */
public enum AnvilUsage {

    /** Any of the three salvage tomes (Scrap, Improved Scrap, Extraction). */
    SALVAGE(ModTriggers.TOME_SALVAGE::trigger),
    /** Prismatic Web curse strip. */
    CURSE_STRIP(ModTriggers.CURSE_STRIP::trigger),
    /** Tempered Core unbreakable application. */
    TEMPERED_CORE(ModTriggers.TEMPERED_CORE::trigger);

    private final Consumer<ServerPlayer> trigger;

    AnvilUsage(Consumer<ServerPlayer> trigger) {
        this.trigger = trigger;
    }

    /** Fires this mechanic's usage advancement for {@code player}. */
    public void award(ServerPlayer player) {
        this.trigger.accept(player);
    }
}
