package com.hashiriyacarmod;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

/**
 * 本物の VertexBuffer を1パーツ1個保持し、影mod互換を確保します。
 * 初回は BufferBuilder 経由で upload()（バニラ経路）。
 * 以後は upload() 直後にGLから読み取ったバッファIDに対して
 * glBufferSubData でライト・色のみピンポイント更新します。
 * Reflectionは一切使わないため、Forgeバージョン・OptiFine・Iris問わず動作します。
 *
 * NEW_ENTITY interleavedレイアウト（stride=36）:
 *   [0]  pos     float x3 = 12byte
 *   [12] color   ubyte x4 =  4byte
 *   [16] uv0     float x2 =  8byte
 *   [24] overlay short x2 =  4byte
 *   [28] light   short x2 =  4byte
 *   [32] normal  byte  x3 + pad1 = 4byte
 */
@OnlyIn(Dist.CLIENT)
public class StandardVboCache implements AutoCloseable {

    private static final int STRIDE    = 36;
    private static final int OFF_LIGHT = 28;
    private static final int OFF_COLOR = 12;

    private final VertexBuffer vertexBuffer;
    /** upload()直後にGLから読み取ったバッファID。Reflectionなし。 */
    private final int cachedVertexBufferId;
    /** 展開後の頂点数（mesh.indices.length）*/
    private final int expandedVertexCount;
    private final int actualStride;

    public StandardVboCache(ObjMesh mesh, int initialPackedLight) {
        this.expandedVertexCount = mesh.indices.length;

        // ---- 初回: BufferBuilder でインデックス展開しながら積む（バニラ経路） ----
        BufferBuilder builder = new BufferBuilder(expandedVertexCount * STRIDE);
        builder.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.NEW_ENTITY);

        for (int idx : mesh.indices) {
            builder.vertex(mesh.posX[idx], mesh.posY[idx], mesh.posZ[idx])
                    .color(255, 255, 255, 255)
                    .uv(mesh.u[idx], mesh.v[idx])
                    .overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(initialPackedLight)
                    .normal(mesh.nx[idx], mesh.ny[idx], mesh.nz[idx])
                    .endVertex();
        }

        vertexBuffer = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
        vertexBuffer.bind();
        vertexBuffer.upload(builder.end());

        // upload()がGL_ARRAY_BUFFERにバインドした状態のまま終わるので、
        // 今バインドされているバッファIDをGLに直接問い合わせてキャッシュする。
        // Reflectionを使わないため全環境で安定して動作する。
        IntBuffer idHolder = ByteBuffer.allocateDirect(4)
                .order(ByteOrder.nativeOrder()).asIntBuffer();
        GL11.glGetIntegerv(GL15.GL_ARRAY_BUFFER_BINDING, idHolder);
        this.cachedVertexBufferId = idHolder.get(0);
        this.actualStride = vertexBuffer.getFormat() != null
                ? vertexBuffer.getFormat().getVertexSize()
                : 36;

        VertexBuffer.unbind();
    }

    /** 描画。bind() → drawWithShader() → unbind() のバニラ経路を通る。 */
    public void drawWithShader(org.joml.Matrix4f modelView,
                               org.joml.Matrix4f projection,
                               net.minecraft.client.renderer.ShaderInstance shader) {
        vertexBuffer.bind();
        vertexBuffer.drawWithShader(modelView, projection, shader);
        VertexBuffer.unbind();
    }

    /**
     * ライトだけをピンポイントで書き換えます。
     * stride=36 の interleaved バッファに対して、
     * 頂点ごとにオフセット28から4バイト（short x2）だけ上書きします。
     */
    public void updateLight(int packedLight) {
        if (cachedVertexBufferId <= 0) return;

        short lu = (short) (packedLight & 0xFFFF);
        short lv = (short) (packedLight >> 16 & 0xFFFF);

        ByteBuffer tmp = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder());
        GlStateManager._glBindBuffer(GL15.GL_ARRAY_BUFFER, cachedVertexBufferId);
        for (int i = 0; i < expandedVertexCount; i++) {
            tmp.clear();
            tmp.putShort(lu);
            tmp.putShort(lv);
            tmp.rewind();
            GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER,
                    (long) i * actualStride + OFF_LIGHT, tmp);
        }
        GlStateManager._glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    }

    /**
     * 色だけをピンポイントで書き換えます（発光・ダメージ演出用。現状呼び出し元未実装）。
     */
    public void updateColor(int r, int g, int b, int a) {
        if (cachedVertexBufferId <= 0) return;

        ByteBuffer tmp = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder());
        GlStateManager._glBindBuffer(GL15.GL_ARRAY_BUFFER, cachedVertexBufferId);
        for (int i = 0; i < expandedVertexCount; i++) {
            tmp.clear();
            tmp.put((byte) r);
            tmp.put((byte) g);
            tmp.put((byte) b);
            tmp.put((byte) a);
            tmp.rewind();
            GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER,
                    (long) i * actualStride + OFF_COLOR, tmp);
        }
        GlStateManager._glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    }

    @Override
    public void close() {
        vertexBuffer.close();
    }
}