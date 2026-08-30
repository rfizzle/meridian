package com.rfizzle.meridian.network;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.enchanting.EnchantingStats;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * S2C payload that snapshots {@link com.rfizzle.meridian.enchanting.EnchantingStatRegistry}
 * and ships it to clients. Sent on player join and after
 * {@code /reload} so the client's registry copy stays in sync with the server's datapack state.
 *
 * <p>Without this payload a dedicated-server client's registry is permanently empty,
 * silently breaking every client-side reader: Jade/WTHIT shelf stat lines, enchant particles,
 * and EMI/REI/JEI info panels.
 *
 * <p>The decode path caps both the block-entry and tag-entry counts at {@value #MAX_ENTRIES}
 * to guard against a malicious server sending an oversized payload.
 */
public record EnchantingStatSyncPayload(
        Map<ResourceLocation, EnchantingStats> blocks,
        List<TagEntry> tags
) implements CustomPacketPayload {

    static final int MAX_ENTRIES = 4096;

    public static final Type<EnchantingStatSyncPayload> TYPE =
            new Type<>(Meridian.id("enchanting_stat_sync"));

    public static final StreamCodec<ByteBuf, EnchantingStatSyncPayload> CODEC =
            StreamCodec.of(EnchantingStatSyncPayload::encode, EnchantingStatSyncPayload::decode);

    private static void encode(ByteBuf buf, EnchantingStatSyncPayload payload) {
        ByteBufCodecs.VAR_INT.encode(buf, payload.blocks().size());
        for (Map.Entry<ResourceLocation, EnchantingStats> entry : payload.blocks().entrySet()) {
            ResourceLocation.STREAM_CODEC.encode(buf, entry.getKey());
            EnchantingStats.STREAM_CODEC.encode(buf, entry.getValue());
        }
        ByteBufCodecs.VAR_INT.encode(buf, payload.tags().size());
        for (TagEntry tag : payload.tags()) {
            ResourceLocation.STREAM_CODEC.encode(buf, tag.tagId());
            EnchantingStats.STREAM_CODEC.encode(buf, tag.stats());
        }
    }

    private static EnchantingStatSyncPayload decode(ByteBuf buf) {
        int blockCount = ByteBufCodecs.VAR_INT.decode(buf);
        if (blockCount < 0 || blockCount > MAX_ENTRIES) {
            throw new DecoderException("EnchantingStatSyncPayload block count out of bounds: " + blockCount);
        }
        Map<ResourceLocation, EnchantingStats> blocks = new LinkedHashMap<>(blockCount);
        for (int i = 0; i < blockCount; i++) {
            ResourceLocation id = ResourceLocation.STREAM_CODEC.decode(buf);
            EnchantingStats stats = EnchantingStats.STREAM_CODEC.decode(buf);
            blocks.put(id, stats);
        }

        int tagCount = ByteBufCodecs.VAR_INT.decode(buf);
        if (tagCount < 0 || tagCount > MAX_ENTRIES) {
            throw new DecoderException("EnchantingStatSyncPayload tag count out of bounds: " + tagCount);
        }
        List<TagEntry> tags = new ArrayList<>(tagCount);
        for (int i = 0; i < tagCount; i++) {
            ResourceLocation tagId = ResourceLocation.STREAM_CODEC.decode(buf);
            EnchantingStats stats = EnchantingStats.STREAM_CODEC.decode(buf);
            tags.add(new TagEntry(tagId, stats));
        }

        return new EnchantingStatSyncPayload(blocks, tags);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * A single tag-keyed stat entry: the block tag's {@link ResourceLocation} paired with the
     * stats it contributes. Reconstructed into a {@code TagBinding} (with a proper
     * {@link net.minecraft.tags.TagKey}) by
     * {@link com.rfizzle.meridian.enchanting.EnchantingStatRegistry#applySync}.
     */
    public record TagEntry(ResourceLocation tagId, EnchantingStats stats) {
    }
}
