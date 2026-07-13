package com.rfizzle.meridian.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.rfizzle.meridian.Meridian;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

/**
 * Custom render types for Meridian's in-world overlays. The Dowse reveal glow uses an xray type
 * (no depth test, so ore glows through walls) that writes colour only — never depth — so the
 * translucent cubes never occlude one another. Drawn in {@code WorldRenderEvents.LAST} per the
 * {@code mc-world-render} guidance, it touches no chunk/BE rendering and composes with Sodium/Iris.
 */
public final class MeridianRenderTypes {

    private static final RenderType DOWSE_GLOW = RenderType.create(
            Meridian.MOD_ID + ":dowse_glow",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS,
            256,
            RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer::getPositionColorShader))
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .createCompositeState(false));

    private MeridianRenderTypes() {}

    public static RenderType dowseGlow() {
        return DOWSE_GLOW;
    }
}
