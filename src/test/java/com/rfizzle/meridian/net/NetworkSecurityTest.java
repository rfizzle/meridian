// Tier: 2 (fabric-loader-junit)
package com.rfizzle.meridian.net;

import com.rfizzle.meridian.enchanting.PowerFunction;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Guards the bounds checks that stop a malicious or corrupted server from crashing the client
 * through the S2C codecs. Each test feeds a hand-built buffer whose declared element count or
 * discriminator byte is out of range and asserts the decode fails fast with a
 * {@link DecoderException} rather than allocating an oversized collection or indexing out of bounds.
 */
class NetworkSecurityTest {

    private static RegistryAccess.Frozen REGISTRIES;

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        REGISTRIES = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    }

    private static RegistryFriendlyByteBuf newBuf() {
        return new RegistryFriendlyByteBuf(Unpooled.buffer(), REGISTRIES);
    }

    // ---- EnchantmentInfoPayload (map capped at 1024) -----------------------

    @Test
    void enchantmentInfoPayload_rejectsOversizedMap() {
        RegistryFriendlyByteBuf buf = newBuf();
        ByteBufCodecs.VAR_INT.encode(buf, 2000); // > 1024
        assertThrows(DecoderException.class, () -> EnchantmentInfoPayload.CODEC.decode(buf));
    }

    @Test
    void enchantmentInfoPayload_rejectsNegativeSize() {
        RegistryFriendlyByteBuf buf = newBuf();
        ByteBufCodecs.VAR_INT.encode(buf, -1); // HashMap(int) would throw on negative capacity
        assertThrows(DecoderException.class, () -> EnchantmentInfoPayload.CODEC.decode(buf));
    }

    // ---- CluesPayload (clue list bounded at 256) ---------------------------

    @Test
    void cluesPayload_rejectsOversizedClueList() {
        RegistryFriendlyByteBuf buf = newBuf();
        ByteBufCodecs.VAR_INT.encode(buf, 0);   // slot
        ByteBufCodecs.VAR_INT.encode(buf, 300); // clue count > 256
        assertThrows(DecoderException.class, () -> CluesPayload.CODEC.decode(buf));
    }

    // ---- StatsPayload (blacklist bounded at 1024) --------------------------

    @Test
    void statsPayload_rejectsOversizedBlacklist() {
        RegistryFriendlyByteBuf buf = newBuf();
        buf.writeFloat(0F); // eterna
        buf.writeFloat(0F); // quanta
        buf.writeFloat(0F); // arcana
        buf.writeFloat(0F); // rectification
        ByteBufCodecs.VAR_INT.encode(buf, 0); // clues
        buf.writeFloat(0F); // maxEterna
        ByteBufCodecs.VAR_INT.encode(buf, 2000); // blacklist count > 1024
        assertThrows(DecoderException.class, () -> StatsPayload.CODEC.decode(buf));
    }

    // ---- EnchantingStatSyncPayload (blocks + tags each capped at 4096) ------

    @Test
    void enchantingStatSyncPayload_rejectsOversizedBlockMap() {
        RegistryFriendlyByteBuf buf = newBuf();
        ByteBufCodecs.VAR_INT.encode(buf, 5000); // block count > 4096
        assertThrows(DecoderException.class, () -> EnchantingStatSyncPayload.CODEC.decode(buf));
    }

    @Test
    void enchantingStatSyncPayload_rejectsNegativeBlockCount() {
        RegistryFriendlyByteBuf buf = newBuf();
        ByteBufCodecs.VAR_INT.encode(buf, -1); // LinkedHashMap(int) would throw on negative capacity
        assertThrows(DecoderException.class, () -> EnchantingStatSyncPayload.CODEC.decode(buf));
    }

    @Test
    void enchantingStatSyncPayload_rejectsOversizedTagList() {
        RegistryFriendlyByteBuf buf = newBuf();
        ByteBufCodecs.VAR_INT.encode(buf, 0);    // empty block map
        ByteBufCodecs.VAR_INT.encode(buf, 5000); // tag count > 4096
        assertThrows(DecoderException.class, () -> EnchantingStatSyncPayload.CODEC.decode(buf));
    }

    @Test
    void enchantingStatSyncPayload_rejectsNegativeTagCount() {
        RegistryFriendlyByteBuf buf = newBuf();
        ByteBufCodecs.VAR_INT.encode(buf, 0);  // empty block map
        ByteBufCodecs.VAR_INT.encode(buf, -1); // ArrayList(int) would throw on negative capacity
        assertThrows(DecoderException.class, () -> EnchantingStatSyncPayload.CODEC.decode(buf));
    }

    // ---- PowerFunction (discriminator byte bounded by Type enum) -----------

    @Test
    void powerFunction_rejectsUnknownDiscriminator() {
        RegistryFriendlyByteBuf buf = newBuf();
        buf.writeByte(100); // far past the enum range
        assertThrows(DecoderException.class, () -> PowerFunction.STREAM_CODEC.decode(buf));
    }

    @Test
    void powerFunction_rejectsBoundaryDiscriminator() {
        RegistryFriendlyByteBuf buf = newBuf();
        buf.writeByte(PowerFunction.Type.values().length); // first index past the array
        assertThrows(DecoderException.class, () -> PowerFunction.STREAM_CODEC.decode(buf));
    }
}
