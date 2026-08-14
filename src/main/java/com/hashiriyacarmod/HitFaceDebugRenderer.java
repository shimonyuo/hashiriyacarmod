package com.hashiriyacarmod.client;

import com.hashiriyacarmod.CarEntity;
import com.hashiriyacarmod.CarEntityRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.hashiriyacarmod.HashiriyaCarMod;

@Mod.EventBusSubscriber(modid = HashiriyaCarMod.MOD_ID, value = Dist.CLIENT)
public final class HitFaceDebugRenderer {

    private HitFaceDebugRenderer() {}

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.gameRenderer == null) return;
        if (!mc.getEntityRenderDispatcher().shouldRenderHitBoxes()) return;

        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer line = buffers.getBuffer(RenderType.lines());

        Vec3 cam = event.getCamera().getPosition();
        float r = 0f, g = 1f, b = 0f, a = 1f; // 緑

        for (CarEntity car : CarEntityRegistry.getAllInLevel(mc.level)) {
            if (car.isRemoved()) continue;
            for (CarEntity.WorldHitFace face : car.getAllWorldHitFaces()) {
                Vec3[] v = face.vertices;
                if (v == null || v.length < 2) continue;
                for (int i = 0; i < v.length; i++) {
                    Vec3 a0 = v[i];
                    Vec3 a1 = v[(i + 1) % v.length];
                    drawLine(pose, line, cam, a0, a1, r, g, b, a);
                }
            }
        }

        buffers.endBatch(RenderType.lines());
    }

    private static void drawLine(PoseStack pose, VertexConsumer vc, Vec3 cam,
                                 Vec3 p0, Vec3 p1, float r, float g, float b, float a) {
        float x0 = (float) (p0.x - cam.x);
        float y0 = (float) (p0.y - cam.y);
        float z0 = (float) (p0.z - cam.z);
        float x1 = (float) (p1.x - cam.x);
        float y1 = (float) (p1.y - cam.y);
        float z1 = (float) (p1.z - cam.z);
        // 法線ダミー（線用）
        float nx = 0f, ny = 1f, nz = 0f;
        vc.vertex(pose.last().pose(), x0, y0, z0)
                .color(r, g, b, a).normal(pose.last().normal(), nx, ny, nz).endVertex();
        vc.vertex(pose.last().pose(), x1, y1, z1)
                .color(r, g, b, a).normal(pose.last().normal(), nx, ny, nz).endVertex();
    }
}