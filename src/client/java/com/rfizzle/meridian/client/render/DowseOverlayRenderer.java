package com.rfizzle.meridian.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.rfizzle.meridian.enchanting.MiningEnchantMath;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.List;

/**
 * Draws the Dowse reveal glow: a translucent amber cube on each ore block of the revealed vein,
 * visible through walls via the xray render type, fading out as the pulse expires. Runs in
 * {@code WorldRenderEvents.LAST} — a post-scene pass that never touches chunk/block-entity
 * rendering, so it composes with Sodium/Iris. State comes entirely from {@link ClientDowseState},
 * which the server-authoritative {@link com.rfizzle.meridian.network.DowseGlowPayload} fills.
 */
public final class DowseOverlayRenderer {

    private static final float EPS = 0.01f;
    private static final int GLOW_RED = 255;
    private static final int GLOW_GREEN = 205;
    private static final int GLOW_BLUE = 90;
    /** Beyond this distance a revealed block isn't drawn — cheap cull if the player walks off. */
    private static final double MAX_RENDER_DIST_SQ = 64.0 * 64.0;

    private DowseOverlayRenderer() {}

    public static void register() {
        WorldRenderEvents.LAST.register(DowseOverlayRenderer::render);
    }

    private static void render(WorldRenderContext context) {
        if (context.world() == null) return;
        long now = context.world().getGameTime();
        if (!ClientDowseState.isActive(now, context.world().dimension())) return;
        if (!(context.consumers() instanceof MultiBufferSource.BufferSource bufferSource)) return;

        int alpha = Math.round(MiningEnchantMath.glowAlpha(ClientDowseState.ticksRemaining(now)) * 255f);
        if (alpha <= 0) return;

        Camera camera = context.camera();
        Vec3 cam = camera.getPosition();
        PoseStack pose = context.matrixStack();
        Matrix4f matrix = pose.last().pose();
        VertexConsumer buffer = bufferSource.getBuffer(MeridianRenderTypes.dowseGlow());

        List<BlockPos> positions = ClientDowseState.positions();
        boolean drewAny = false;
        for (BlockPos pos : positions) {
            double cx = pos.getX() + 0.5 - cam.x;
            double cy = pos.getY() + 0.5 - cam.y;
            double cz = pos.getZ() + 0.5 - cam.z;
            if (cx * cx + cy * cy + cz * cz > MAX_RENDER_DIST_SQ) continue;
            drawCube(matrix, buffer, pos, cam, alpha);
            drewAny = true;
        }

        if (drewAny) {
            bufferSource.endBatch(MeridianRenderTypes.dowseGlow());
        }
    }

    private static void drawCube(Matrix4f m, VertexConsumer buf, BlockPos pos, Vec3 cam, int alpha) {
        float x0 = (float) (pos.getX() - cam.x) - EPS;
        float y0 = (float) (pos.getY() - cam.y) - EPS;
        float z0 = (float) (pos.getZ() - cam.z) - EPS;
        float x1 = (float) (pos.getX() + 1 - cam.x) + EPS;
        float y1 = (float) (pos.getY() + 1 - cam.y) + EPS;
        float z1 = (float) (pos.getZ() + 1 - cam.z) + EPS;

        // down / up
        quad(m, buf, alpha, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
        quad(m, buf, alpha, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0);
        // north / south
        quad(m, buf, alpha, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0);
        quad(m, buf, alpha, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
        // west / east
        quad(m, buf, alpha, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
        quad(m, buf, alpha, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1);
    }

    private static void quad(Matrix4f m, VertexConsumer buf, int alpha,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz) {
        buf.addVertex(m, ax, ay, az).setColor(GLOW_RED, GLOW_GREEN, GLOW_BLUE, alpha);
        buf.addVertex(m, bx, by, bz).setColor(GLOW_RED, GLOW_GREEN, GLOW_BLUE, alpha);
        buf.addVertex(m, cx, cy, cz).setColor(GLOW_RED, GLOW_GREEN, GLOW_BLUE, alpha);
        buf.addVertex(m, dx, dy, dz).setColor(GLOW_RED, GLOW_GREEN, GLOW_BLUE, alpha);
    }
}
