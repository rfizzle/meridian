package com.rfizzle.meridian.net;

import com.rfizzle.meridian.Meridian;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.List;
import java.util.Optional;

/**
 * S2C stats bundle — carries the five enchanting stats, the aggregated blacklist, the treasure
 * flag, the optional fourth-row crafting result for the custom enchanting menu, and the player's
 * combined XP-tome balance (levels the table may draw on to cover a cost deficit). Re-sent on
 * every relevant {@code slotsChanged}; no incremental sync.
 */
public record StatsPayload(
        float eterna,
        float quanta,
        float arcana,
        float rectification,
        int clues,
        float maxEterna,
        List<ResourceKey<Enchantment>> blacklist,
        boolean treasure,
        Optional<CraftingResultEntry> craftingResult,
        int tomeBalance
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<StatsPayload> TYPE =
            new CustomPacketPayload.Type<>(Meridian.id("stats"));

    private static final StreamCodec<ByteBuf, List<ResourceKey<Enchantment>>> BLACKLIST_CODEC =
            ResourceKey.<Enchantment>streamCodec(Registries.ENCHANTMENT).apply(ByteBufCodecs.list(1024));

    private static final StreamCodec<RegistryFriendlyByteBuf, Optional<CraftingResultEntry>> CRAFTING_CODEC =
            ByteBufCodecs.optional(CraftingResultEntry.STREAM_CODEC);

    // StreamCodec.composite supports at most 6 fields in 1.21.1; this payload has 10, so encode
    // and decode are expanded by hand to preserve the DESIGN-mandated flat record shape.
    public static final StreamCodec<RegistryFriendlyByteBuf, StatsPayload> CODEC =
            StreamCodec.of(StatsPayload::encode, StatsPayload::decode);

    private static void encode(RegistryFriendlyByteBuf buf, StatsPayload payload) {
        buf.writeFloat(payload.eterna);
        buf.writeFloat(payload.quanta);
        buf.writeFloat(payload.arcana);
        buf.writeFloat(payload.rectification);
        ByteBufCodecs.VAR_INT.encode(buf, payload.clues);
        buf.writeFloat(payload.maxEterna);
        BLACKLIST_CODEC.encode(buf, payload.blacklist);
        buf.writeBoolean(payload.treasure);
        CRAFTING_CODEC.encode(buf, payload.craftingResult);
        ByteBufCodecs.VAR_INT.encode(buf, payload.tomeBalance);
    }

    private static StatsPayload decode(RegistryFriendlyByteBuf buf) {
        float eterna = buf.readFloat();
        float quanta = buf.readFloat();
        float arcana = buf.readFloat();
        float rectification = buf.readFloat();
        int clues = ByteBufCodecs.VAR_INT.decode(buf);
        float maxEterna = buf.readFloat();
        List<ResourceKey<Enchantment>> blacklist = BLACKLIST_CODEC.decode(buf);
        boolean treasure = buf.readBoolean();
        Optional<CraftingResultEntry> craftingResult = CRAFTING_CODEC.decode(buf);
        int tomeBalance = ByteBufCodecs.VAR_INT.decode(buf);
        return new StatsPayload(eterna, quanta, arcana, rectification, clues,
                maxEterna, blacklist, treasure, craftingResult, tomeBalance);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
