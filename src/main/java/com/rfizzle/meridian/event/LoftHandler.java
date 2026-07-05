package com.rfizzle.meridian.event;

import com.rfizzle.meridian.enchanting.DefenseEnchantMath;
import com.rfizzle.meridian.enchanting.EnchantmentEffects;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server side of Loft's mid-air jump. The client detects the airborne jump-key press and
 * sends {@link com.rfizzle.meridian.net.LoftJumpPayload}; everything that matters is
 * validated and applied here, so a spoofed packet can't jump without the enchant, in the
 * wrong state, or more than once per airtime. (The safe-fall half of Loft lives in
 * {@code LoftMixin}.)
 */
public final class LoftHandler {

    /** Players whose one mid-air jump is spent; cleared the next tick they touch ground. */
    private static final Set<UUID> airJumpSpent = ConcurrentHashMap.newKeySet();

    /** Game time of each player's last air jump, for the anti-spam minimum interval. */
    private static final Map<UUID, Long> lastAirJump = new ConcurrentHashMap<>();

    private LoftHandler() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(LoftHandler::onServerTick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            airJumpSpent.remove(handler.player.getUUID());
            lastAirJump.remove(handler.player.getUUID());
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            airJumpSpent.clear();
            lastAirJump.clear();
        });
    }

    private static void onServerTick(MinecraftServer server) {
        if (airJumpSpent.isEmpty()) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            resetOnGround(player);
        }
    }

    /** Re-arms the mid-air jump once the player is grounded. Public for gametests. */
    public static void resetOnGround(ServerPlayer player) {
        if (isGrounded(player)) {
            airJumpSpent.remove(player.getUUID());
        }
    }

    /**
     * Server-derived groundedness: real block collision in a thin slab just below the
     * feet (only the slab — a whole-box test would misread body overlap, e.g. a ceiling
     * at head height, as ground). Deliberately NOT {@code onGround()} — that flag is
     * copied verbatim from the client's move packets (the classic NoFall vector), and
     * trusting it for the re-arm would let a hacked client re-arm every tick mid-air,
     * turning Loft into a fly hack.
     */
    private static boolean isGrounded(ServerPlayer player) {
        AABB box = player.getBoundingBox();
        AABB underFeet = new AABB(box.minX, box.minY - 1.0E-3, box.minZ, box.maxX, box.minY, box.maxZ);
        return !player.level().noCollision(player, underFeet);
    }

    // Test support: mirror internal state for gametest diagnostics.
    public static boolean isGroundedForTest(ServerPlayer player) {
        return isGrounded(player);
    }

    public static boolean isAirJumpSpentForTest(UUID id) {
        return airJumpSpent.contains(id);
    }

    /**
     * Test teardown: drop a player's tracking from both sets. Gametests dispose their mock players
     * with {@code player.discard()}, which never fires the {@code DISCONNECT} listener that cleans up
     * real players — so without this a mock UUID would linger in {@code airJumpSpent}/{@code lastAirJump}
     * for the life of the test JVM.
     */
    public static void clearPlayerForTest(UUID id) {
        airJumpSpent.remove(id);
        lastAirJump.remove(id);
    }

    /**
     * Validates and performs the mid-air jump. Returns whether it fired. Every check the
     * client makes before sending is repeated here — the packet is a request, not a fact.
     */
    public static boolean tryAirJump(ServerPlayer player) {
        int level = EnchantmentEffects.getEquippedLevel(player, EnchantmentEffects.LOFT, EquipmentSlot.FEET);
        if (level <= 0) return false;
        if (player.onGround() || isGrounded(player) || player.isFallFlying() || player.isPassenger()
                || player.isInWaterOrBubble() || player.isInLava()
                || player.isSpectator() || player.getAbilities().flying) {
            return false;
        }

        long now = player.serverLevel().getGameTime();
        Long last = lastAirJump.get(player.getUUID());
        if (last != null && now - last < DefenseEnchantMath.LOFT_AIR_JUMP_MIN_INTERVAL_TICKS) return false;

        if (!airJumpSpent.add(player.getUUID())) return false;
        lastAirJump.put(player.getUUID(), now);

        Vec3 movement = player.getDeltaMovement();
        player.setDeltaMovement(movement.x, DefenseEnchantMath.LOFT_JUMP_VELOCITY, movement.z);
        player.hurtMarked = true;
        player.resetFallDistance();
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.BREEZE_JUMP,
                player.getSoundSource(), 0.5f, 1.2f);
        return true;
    }
}
