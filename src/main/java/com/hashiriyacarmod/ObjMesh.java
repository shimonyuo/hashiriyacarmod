package com.hashiriyacarmod;

/**
 * OBJファイルを解析した結果を保持するデータクラス。
 * Index Buffer対応版。
 */
public class ObjMesh {
    /** 重複を排除した後の頂点数 */
    public final int vertexCount;

    public final float[] posX, posY, posZ;
    public final float[] u, v;
    public final float[] nx, ny, nz;

    /** Index Buffer（三角形の頂点番号リスト） */
    public final int[] indices;
    public final int indexCount;

    public ObjMesh(int vertexCount,
                   float[] posX, float[] posY, float[] posZ,
                   float[] u, float[] v,
                   float[] nx, float[] ny, float[] nz,
                   int[] indices) {
        this.vertexCount = vertexCount;
        this.posX = posX; this.posY = posY; this.posZ = posZ;
        this.u = u; this.v = v;
        this.nx = nx; this.ny = ny; this.nz = nz;
        this.indices = indices;
        this.indexCount = indices.length;
    }
}