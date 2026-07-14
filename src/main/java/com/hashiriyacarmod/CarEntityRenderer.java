package com.hashiriyacarmod;

import com.hashiriyacarmod.parts.PartRegistry;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class CarEntityRenderer extends EntityRenderer<CarEntity> {

    public CarEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(CarEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        renderHitboxLines(entity, entityYaw, poseStack, bufferSource);

        if (!entity.resolveRenderCache()) return;

        var parts = entity.getCachedParts();
        if (parts.isEmpty()) {
            parts = entity.getPartMeshes();
        }
        if (parts.isEmpty()) return;

        ResourceLocation texLoc = entity.getCachedTextureLocation();
        if (texLoc == null) return;

        float carPitch = entity.getCarPitch();
        float carRoll = entity.getCarRoll();

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(carPitch));
        poseStack.mulPose(Axis.ZP.rotationDegrees(carRoll));

        for (var entry : parts.entrySet()) {
            String partName = entry.getKey();
            ObjMesh mesh = entry.getValue();

            poseStack.pushPose();
            Matrix4f partMatrix = new Matrix4f(poseStack.last().pose());
            drawMeshOnGpu(entity, partName, mesh, partMatrix, texLoc, packedLight);
            poseStack.popPose();
        }

        poseStack.popPose();
    }
    private void drawMeshOnGpu(CarEntity entity, String partName, ObjMesh mesh,
                               Matrix4f partMatrix, ResourceLocation texLoc, int packedLight) {

        StandardVboCache buffer = entity.getOrCreatePartBuffer(partName, mesh, packedLight);
        if (buffer == null) return;

        entity.updatePartLightIfChanged(partName, buffer, packedLight);

        RenderType renderType = CarRenderTypes.entityCutoutTriangles(texLoc);
        renderType.setupRenderState();

        try {
            buffer.drawWithShader(partMatrix, RenderSystem.getProjectionMatrix(),
                    RenderSystem.getShader());
        } finally {
            renderType.clearRenderState();
        }
    }

    private void renderHitboxLines(CarEntity entity, float entityYaw,
                                   PoseStack poseStack, MultiBufferSource bufferSource) {

        if (!Minecraft.getInstance().getEntityRenderDispatcher().shouldRenderHitBoxes()) return;

        var allBoxes = entity.getAllWorldHitboxVertices(entityYaw);
        if (allBoxes.isEmpty()) return;

        VertexConsumer lineConsumer = bufferSource.getBuffer(RenderType.lines());
        PoseStack.Pose pose = poseStack.last();

        int[][] edges = {
                {0, 1}, {1, 2}, {2, 3}, {3, 0},
                {4, 5}, {5, 6}, {6, 7}, {7, 4},
                {0, 4}, {1, 5}, {2, 6}, {3, 7}
        };

        // 箱ごとに、線を描画します
        for (Vec3[] worldVertices : allBoxes) {
            float[] relX = new float[8], relY = new float[8], relZ = new float[8];
            for (int i = 0; i < 8; i++) {
                relX[i] = (float) (worldVertices[i].x - entity.getX());
                relY[i] = (float) (worldVertices[i].y - entity.getY());
                relZ[i] = (float) (worldVertices[i].z - entity.getZ());
            }

            for (int[] edge : edges) {
                int a = edge[0], b = edge[1];
                lineConsumer.vertex(pose.pose(), relX[a], relY[a], relZ[a])
                        .color(0, 0, 255, 255)
                        .normal(pose.normal(), 0, 1, 0)
                        .endVertex();
                lineConsumer.vertex(pose.pose(), relX[b], relY[b], relZ[b])
                        .color(0, 0, 255, 255)
                        .normal(pose.normal(), 0, 1, 0)
                        .endVertex();
            }
        }
    }

    @Override
    public ResourceLocation getTextureLocation(CarEntity entity) {
        return new ResourceLocation(HashiriyaCarMod.MOD_ID, "textures/entity/placeholder.png");
    }
}