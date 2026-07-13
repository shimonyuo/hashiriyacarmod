package com.hashiriyacarmod;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.Function;

@OnlyIn(Dist.CLIENT)
public class CarRenderTypes extends RenderStateShard {

    private CarRenderTypes() {
        super("car_render_types", () -> {}, () -> {});
    }

    @javax.annotation.Nullable
    private static net.minecraft.client.renderer.ShaderInstance carEntityShader;

    public static void setCarEntityShader(net.minecraft.client.renderer.ShaderInstance shader) {
        carEntityShader = shader;
    }

    /** シェーダーmod（Oculus等）が有効かどうか判定 */
    private static boolean isShadersEnabled() {
        try {
            Class<?> irisApi = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Object instance = irisApi.getMethod("getInstance").invoke(null);
            return (boolean) instance.getClass().getMethod("isShaderPackInUse").invoke(instance);
        } catch (Exception e) {
            return false;
        }
    }

    private static final Function<ResourceLocation, RenderType> ENTITY_CUTOUT_TRIANGLES =
            Util.memoize((texture) -> {
                RenderType.CompositeState state = RenderType.CompositeState.builder()
                        .setShaderState(new RenderStateShard.ShaderStateShard(() ->
                                isShadersEnabled()
                                        ? net.minecraft.client.renderer.GameRenderer.getRendertypeEntityCutoutNoCullShader()
                                        : carEntityShader
                        ))
                        .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                        .setTransparencyState(NO_TRANSPARENCY)
                        .setLightmapState(LIGHTMAP)
                        .setOverlayState(OVERLAY)
                        .createCompositeState(true);

                return RenderType.create(
                        "car_entity_cutout_triangles_" + texture.getPath(),
                        DefaultVertexFormat.NEW_ENTITY,
                        VertexFormat.Mode.TRIANGLES,
                        65536,
                        true,
                        false,
                        state
                );
            });

    public static RenderType entityCutoutTriangles(ResourceLocation texture) {
        return ENTITY_CUTOUT_TRIANGLES.apply(texture);
    }
}