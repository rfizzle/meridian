package com.rfizzle.meridian.net;

import com.rfizzle.meridian.Meridian;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

/**
 * S2C per-slot clue list for hover tooltips on the three enchant preview slots. Sent three
 * times per {@code slotsChanged} when the input is enchantable; the first clue per slot is the
 * exact enchant that slot rolls, and remaining clues backfill from the selection pool up to
 * {@code stats.clues()}. An empty list with {@link #exhaustedList()} true signals that the pool
 * couldn't satisfy the requested clue count.
 */
public record CluesPayload(int slot, List<EnchantmentClue> clues, boolean exhaustedList)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<CluesPayload> TYPE =
            new CustomPacketPayload.Type<>(Meridian.id("clues"));

    private static final StreamCodec<ByteBuf, List<EnchantmentClue>> CLUES_CODEC =
            EnchantmentClue.STREAM_CODEC.apply(ByteBufCodecs.list());

    public static final StreamCodec<RegistryFriendlyByteBuf, CluesPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, CluesPayload::slot,
                    CLUES_CODEC, CluesPayload::clues,
                    ByteBufCodecs.BOOL, CluesPayload::exhaustedList,
                    CluesPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
