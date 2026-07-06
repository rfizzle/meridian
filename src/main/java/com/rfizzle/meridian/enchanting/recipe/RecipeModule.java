package com.rfizzle.meridian.enchanting.recipe;

import com.mojang.serialization.Codec;
import com.rfizzle.meridian.config.MeridianConfig;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;

import io.netty.buffer.ByteBuf;

/**
 * Content group a table-crafting recipe belongs to, read from the optional {@code "module"} field
 * on the {@code meridian:enchanting} / {@code meridian:keep_nbt_enchanting} codecs (#163). Untagged
 * recipes are {@link #CORE} and can never be switched off; the other groups are gated behind a
 * config toggle so server operators can opt out of them without a datapack.
 *
 * <p>The codec is strict: an unknown {@code "module"} string fails that recipe at load with
 * vanilla's recipe-parse error rather than silently falling back to {@link #CORE} — a typo must
 * not quietly un-gate a recipe.
 */
public enum RecipeModule implements StringRepresentable {
    /** Always-on table crafting — shelf upgrades, tomes, XP conversions. */
    CORE("core"),
    /** Vanilla-item duplication (totem, echo shard, golden apples, …); {@code tableCrafting.allowDuplication}. */
    DUPLICATION("duplication"),
    /** Everfeast rations and the Everfull Flask; {@code everfeast.enabled}. */
    EVERFEAST("everfeast");

    public static final Codec<RecipeModule> CODEC = StringRepresentable.fromEnum(RecipeModule::values);

    // ByIdMap clamps an out-of-range index (corrupt buffer, mismatched build) to CORE instead of
    // throwing a raw ArrayIndexOutOfBoundsException — the vanilla idiom for enum stream codecs.
    public static final StreamCodec<ByteBuf, RecipeModule> STREAM_CODEC = ByteBufCodecs.idMapper(
            ByIdMap.continuous(RecipeModule::ordinal, RecipeModule.values(), ByIdMap.OutOfBoundsStrategy.ZERO),
            RecipeModule::ordinal);

    private final String serializedName;

    RecipeModule(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }

    /**
     * Whether recipes in this module are active under {@code config}. Callers must supply the
     * side-correct config: {@code Meridian.getConfig()} on the server, the synced
     * {@code ClientMeridianConfig.effective()} on the client, so a dedicated server's toggles
     * govern its clients.
     */
    public boolean isEnabled(MeridianConfig config) {
        return switch (this) {
            case CORE -> true;
            case DUPLICATION -> config.tableCrafting.allowDuplication;
            case EVERFEAST -> config.everfeast.enabled;
        };
    }
}
