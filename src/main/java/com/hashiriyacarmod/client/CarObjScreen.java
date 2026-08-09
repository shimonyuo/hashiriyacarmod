package com.hashiriyacarmod.client;

import com.hashiriyacarmod.AssetRegistry;
import com.hashiriyacarmod.CarEntity;
import com.hashiriyacarmod.CarPackLoader;
import com.hashiriyacarmod.CarTextureManager;
import com.hashiriyacarmod.HashiriyaCarMod;
import com.hashiriyacarmod.ObjMesh;
import com.hashiriyacarmod.cars.CarJsonResult;
import com.hashiriyacarmod.parts.PartRegistry;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import java.util.List;
import java.util.Map;

/**
 * レンチメニューが開いている間、画面右側に叩いた車のOBJを3D表示する。
 * 右クリック長押し＋ドラッグで向きを変えられる。
 * 縦回転は ±90° まで、横回転は制限なし。
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CarObjScreen {

    /** 車の中心：画面右端から左へ何ピクセル（縦は画面の中央） */
    private static final int FROM_RIGHT_PX = 170;
    /** 車のおおよその表示サイズ（ピクセル） */
    private static final int MODEL_SIZE_PX = 240;

    /** 現状の固定向きを初期値とする */
    private static final float INITIAL_YAW = -45.0f;
    private static final float INITIAL_PITCH = 10.0f;
    private static final float PITCH_MIN = -90.0f;
    private static final float PITCH_MAX = 90.0f;
    /** 画面ピクセル1つあたりの回転量（度） */
    private static final float DRAG_SENSITIVITY = 0.3f;

    private static float viewYaw = INITIAL_YAW;
    private static float viewPitch = INITIAL_PITCH;

    private static boolean wasRightDragging = false;
    private static double lastCursorX;
    private static double lastCursorY;
    private static float viewSize = MODEL_SIZE_PX;
    private static final float ZOOM_PER_NOTCH = 12.0f;
    private static float panX = 0.0f;
    private static float panY = 0.0f;

    private static boolean wasLeftDragging = false;

    @SubscribeEvent
    public static void onScreenRenderPost(ScreenEvent.Render.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Screen current = event.getScreen();

        if (!(current instanceof WrenchGuiScreen)) {
            return;
        }

        CompoundTag nbt = WrenchGuiScreen.lastReceivedNbt;
        if (nbt == null || !nbt.contains("BaseName")) {
            return;
        }

        String baseName = nbt.getString("BaseName");
        if (baseName == null || baseName.isEmpty()) {
            return;
        }

        AssetRegistry registry = CarPackLoader.getAssetRegistry(baseName);
        if (registry == null || registry.parts.isEmpty()) {
            return;
        }

        GuiGraphics g = event.getGuiGraphics();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        // メニュー黒い四角の右端
        int menuRight = WrenchGuiScreen.WINDOW_X + WrenchGuiScreen.IMAGE_WIDTH;

        int clipLeft = menuRight + 10;
        int clipTop = 10;
        int clipRight = screenWidth - 10;
        int clipBottom = screenHeight - 10;

        if (clipRight <= clipLeft || clipBottom <= clipTop) {
            return;
        }

        updateViewFromRightDrag(mc, clipLeft, clipTop, clipRight, clipBottom);
        updateViewFromLeftDrag(mc, clipLeft, clipTop, clipRight, clipBottom);

        int centerX = (clipLeft + clipRight) / 2 + Math.round(panX);
        int centerY = (clipTop + clipBottom) / 2 + Math.round(panY);
        int size = Math.max(0, (int) viewSize);

        g.enableScissor(clipLeft, clipTop, clipRight, clipBottom);
        try {
            renderCarModel(g, registry, nbt, centerX, centerY, size);
        } finally {
            g.disableScissor();
        }
    }

    private static void updateViewFromLeftDrag(Minecraft mc,
                                               int clipLeft, int clipTop,
                                               int clipRight, int clipBottom) {
        if (wasRightDragging) {
            return; // 右ドラッグ優先
        }

        long window = mc.getWindow().getWindow();
        boolean leftDown = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;

        double[] xpos = new double[1];
        double[] ypos = new double[1];
        GLFW.glfwGetCursorPos(window, xpos, ypos);

        // GLFW座標 → GUIスケール座標
        double scaleX = (double) mc.getWindow().getGuiScaledWidth() / (double) mc.getWindow().getWidth();
        double scaleY = (double) mc.getWindow().getGuiScaledHeight() / (double) mc.getWindow().getHeight();
        double guiX = xpos[0] * scaleX;
        double guiY = ypos[0] * scaleY;

        boolean inside = guiX >= clipLeft && guiX < clipRight
                && guiY >= clipTop && guiY < clipBottom;

        if (leftDown && (wasLeftDragging || inside)) {
            if (wasLeftDragging) {
                // 前回も左ドラッグ中 → 差分で平行移動
                double dx = (xpos[0] - lastCursorX) * scaleX;
                double dy = (ypos[0] - lastCursorY) * scaleY;
                panX += (float) dx;
                panY += (float) dy;
            }
            wasLeftDragging = true;
            lastCursorX = xpos[0];
            lastCursorY = ypos[0];
        } else if (!leftDown) {
            wasLeftDragging = false;
        }
    }

    /**
     * 右クリック押下中のカーソル移動量で viewYaw / viewPitch を更新する。
     * yaw は無制限、pitch は [-90, 90]。
     */
    private static void updateViewFromRightDrag(Minecraft mc,
                                                int clipLeft, int clipTop,
                                                int clipRight, int clipBottom) {
        long window = mc.getWindow().getWindow();
        boolean rightDown = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;

        double[] xpos = new double[1];
        double[] ypos = new double[1];
        GLFW.glfwGetCursorPos(window, xpos, ypos);

        double scaleX = (double) mc.getWindow().getGuiScaledWidth() / (double) mc.getWindow().getWidth();
        double scaleY = (double) mc.getWindow().getGuiScaledHeight() / (double) mc.getWindow().getHeight();
        double guiX = xpos[0] * scaleX;
        double guiY = ypos[0] * scaleY;

        boolean inside = guiX >= clipLeft && guiX < clipRight
                && guiY >= clipTop && guiY < clipBottom;

        if (rightDown && (wasRightDragging || inside)) {
            if (wasRightDragging) {
                double dx = xpos[0] - lastCursorX;
                double dy = ypos[0] - lastCursorY;

                viewYaw += (float) (dx * DRAG_SENSITIVITY);
                viewPitch += (float) (dy * DRAG_SENSITIVITY);
                viewPitch = Mth.clamp(viewPitch, PITCH_MIN, PITCH_MAX);
            }
            wasRightDragging = true;
            lastCursorX = xpos[0];
            lastCursorY = ypos[0];
        } else if (!rightDown) {
            wasRightDragging = false;
        }
    }

    @SubscribeEvent
    public static void onMouseScroll(ScreenEvent.MouseScrolled.Pre event) {
        if (!(event.getScreen() instanceof WrenchGuiScreen)) {
            return;
        }

        // 上スクロールで拡大、下で縮小（逆なら scroll の符号を反転）
        double scroll = event.getScrollDelta();
        viewSize += (float) (scroll * ZOOM_PER_NOTCH);
        if (viewSize < 0.0f) {
            viewSize = 0.0f;
        }
        // メニュー上でもズームを優先したい場合
        event.setCanceled(true);
    }

    private static void renderCarModel(GuiGraphics g, AssetRegistry registry,
                                       CompoundTag nbt, int centerX, int centerY, int size) {

        ResourceLocation texLoc = null;
        if (registry.pngFile != null) {
            texLoc = CarTextureManager.getOrLoad(registry.baseName, registry.pngFile);
        }
        if (texLoc == null) {
            texLoc = new ResourceLocation(HashiriyaCarMod.MOD_ID, "textures/entity/placeholder.png");
        }

        float[] bounds = computeBounds(registry.parts);
        float modelSize = Math.max(bounds[3] - bounds[0],
                Math.max(bounds[4] - bounds[1], bounds[5] - bounds[2]));
        if (modelSize < 0.001f) modelSize = 1.0f;

        float scale = size / modelSize;
        float cx = (bounds[0] + bounds[3]) * 0.5f;
        float cy = (bounds[1] + bounds[4]) * 0.5f;
        float cz = (bounds[2] + bounds[5]) * 0.5f;

        PoseStack poseStack = g.pose();
        poseStack.pushPose();

        poseStack.translate(centerX, centerY, 100.0D);
        // XY だけズーム。Z はモデル実寸のまま → GUI の near/far で欠けない
        poseStack.scale(scale, -scale, 1.0f);
        poseStack.mulPose(Axis.XP.rotationDegrees(viewPitch));
        poseStack.mulPose(Axis.YP.rotationDegrees(viewYaw));
        poseStack.translate(-cx, -cy, -cz);

        Lighting.setupForEntityInInventory();
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();

        try {
            drawParts(poseStack, registry.parts, texLoc);

            if (nbt.contains("AttachedParts", Tag.TAG_LIST)) {
                ListTag list = nbt.getList("AttachedParts", Tag.TAG_STRING);
                for (int i = 0; i < list.size(); i++) {
                    String token = list.getString(i);
                    if (token == null || token.isBlank()) continue;

                    String attachedBase = CarEntity.attachedBaseName(token);
                    String seatGroup = CarEntity.attachedSeatGroup(token);
                    if (attachedBase.isEmpty()) continue;

                    Map<String, ObjMesh> attachedMeshes = PartRegistry.getPartMeshes(attachedBase);
                    if (attachedMeshes == null || attachedMeshes.isEmpty()) continue;

                    ResourceLocation partTex = resolvePartTexture(attachedBase, texLoc);

                    Vec3 po = resolvePartOffset(registry, attachedBase, seatGroup);
                    float[] ro = resolvePartRotation(registry, attachedBase, seatGroup);

                    poseStack.pushPose();
                    poseStack.translate(po.x, po.y, po.z);
                    poseStack.mulPose(Axis.XP.rotationDegrees(ro[0]));
                    poseStack.mulPose(Axis.YP.rotationDegrees(ro[1]));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(ro[2]));

                    drawParts(poseStack, attachedMeshes, partTex);
                    poseStack.popPose();
                }
            }
        } finally {
            RenderSystem.disableDepthTest();
            Lighting.setupFor3DItems();
            poseStack.popPose();
        }
    }

    private static Vec3 resolvePartOffset(AssetRegistry registry, String attachedBase, String seatGroup) {
        if (registry.partPlacements == null || registry.partPlacements.isEmpty()) {
            return Vec3.ZERO;
        }

        if (!seatGroup.isEmpty()) {
            for (CarJsonResult.PartPlacement p : registry.partPlacements) {
                if (p.groups != null && p.groups.contains(seatGroup)) {
                    return p.position;
                }
            }
            return Vec3.ZERO;
        }

        List<String> partGroups = PartRegistry.getPartGroups(attachedBase);
        for (CarJsonResult.PartPlacement p : registry.partPlacements) {
            if (p.groups == null) continue;
            for (String g : partGroups) {
                if (p.groups.contains(g)) {
                    return p.position;
                }
            }
        }
        return Vec3.ZERO;
    }

    private static float[] resolvePartRotation(AssetRegistry registry, String attachedBase, String seatGroup) {
        if (registry.partPlacements == null || registry.partPlacements.isEmpty()) {
            return new float[]{0, 0, 0};
        }

        if (!seatGroup.isEmpty()) {
            for (CarJsonResult.PartPlacement p : registry.partPlacements) {
                if (p.groups != null && p.groups.contains(seatGroup)) {
                    return new float[]{p.rotX, p.rotY, p.rotZ};
                }
            }
            return new float[]{0, 0, 0};
        }

        List<String> partGroups = PartRegistry.getPartGroups(attachedBase);
        for (CarJsonResult.PartPlacement p : registry.partPlacements) {
            if (p.groups == null) continue;
            for (String g : partGroups) {
                if (p.groups.contains(g)) {
                    return new float[]{p.rotX, p.rotY, p.rotZ};
                }
            }
        }
        return new float[]{0, 0, 0};
    }

    private static void drawParts(PoseStack poseStack, Map<String, ObjMesh> parts, ResourceLocation texLoc) {
        if (parts == null || parts.isEmpty()) return;

        Matrix4f matrix = poseStack.last().pose();
        Matrix3f normalMatrix = poseStack.last().normal();
        int light = 15728880;
        int overlay = OverlayTexture.NO_OVERLAY;

        RenderType renderType = RenderType.entityCutout(texLoc);
        renderType.setupRenderState();
        RenderSystem.enableCull();

        try {
            Tesselator tesselator = Tesselator.getInstance();
            BufferBuilder builder = tesselator.getBuilder();
            builder.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.NEW_ENTITY);

            for (ObjMesh mesh : parts.values()) {
                for (int index : mesh.indices) {
                    builder.vertex(matrix, mesh.posX[index], mesh.posY[index], mesh.posZ[index])
                            .color(255, 255, 255, 255)
                            .uv(mesh.u[index], mesh.v[index])
                            .overlayCoords(overlay)
                            .uv2(light)
                            .normal(normalMatrix, mesh.nx[index], mesh.ny[index], mesh.nz[index])
                            .endVertex();
                }
            }

            BufferUploader.drawWithShader(builder.end());
        } finally {
            renderType.clearRenderState();
        }
    }

    private static ResourceLocation resolvePartTexture(String partBaseName, ResourceLocation fallback) {
        String path = PartRegistry.getTexturePath(partBaseName);
        if (path == null || path.isEmpty()) {
            return fallback;
        }
        if (path.contains(":")) {
            return new ResourceLocation(path);
        }
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        return new ResourceLocation(HashiriyaCarMod.MOD_ID, path);
    }

    private static float[] computeBounds(Map<String, ObjMesh> parts) {
        float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY, minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY, maxZ = Float.NEGATIVE_INFINITY;

        for (ObjMesh mesh : parts.values()) {
            for (int i = 0; i < mesh.vertexCount; i++) {
                minX = Math.min(minX, mesh.posX[i]);
                minY = Math.min(minY, mesh.posY[i]);
                minZ = Math.min(minZ, mesh.posZ[i]);
                maxX = Math.max(maxX, mesh.posX[i]);
                maxY = Math.max(maxY, mesh.posY[i]);
                maxZ = Math.max(maxZ, mesh.posZ[i]);
            }
        }

        if (minX == Float.POSITIVE_INFINITY) {
            return new float[]{-1, -1, -1, 1, 1, 1};
        }
        return new float[]{minX, minY, minZ, maxX, maxY, maxZ};
    }

    public static void resetView() {
        wasRightDragging = false;
        wasLeftDragging = false;
        viewYaw = INITIAL_YAW;
        viewPitch = INITIAL_PITCH;
        viewSize = MODEL_SIZE_PX;
        panX = 0.0f;
        panY = 0.0f;
    }
}