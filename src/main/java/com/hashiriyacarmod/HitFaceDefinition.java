package com.hashiriyacarmod;

import net.minecraft.world.phys.Vec3;

/**
 * hitfaces.facesList の1面。
 * faceVertices は時計回り（外側から見て）で法線を決める。
 * 頂点3以上。平面・平行前提は JSON 側の約束。
 */
public class HitFaceDefinition {

    private final Vec3[] localVertices;
    private final Vec3 positionOffset;
    private final float offsetYaw;
    private final float offsetPitch;
    private final float offsetRoll;

    public HitFaceDefinition(Vec3[] localVertices, Vec3 positionOffset,
                             float offsetYaw, float offsetPitch, float offsetRoll) {
        if (localVertices == null || localVertices.length < 3) {
            throw new IllegalArgumentException("faceVertices は3点以上必要です");
        }
        this.localVertices = localVertices;
        this.positionOffset = positionOffset != null ? positionOffset : Vec3.ZERO;
        this.offsetYaw = offsetYaw;
        this.offsetPitch = offsetPitch;
        this.offsetRoll = offsetRoll;
    }

    public int vertexCount() {
        return localVertices.length;
    }

    public Vec3[] toWorldVertices(double posX, double posY, double posZ,
                                  float yawDegrees, float pitchDegrees, float rollDegrees) {
        double roll = Math.toRadians(rollDegrees + offsetRoll);
        double pitch = Math.toRadians(pitchDegrees + offsetPitch);
        double yaw = Math.toRadians(yawDegrees + offsetYaw);

        Vec3 rotatedOffset = rotateVec(positionOffset, roll, pitch, yaw);
        double cx = posX + rotatedOffset.x;
        double cy = posY + rotatedOffset.y;
        double cz = posZ + rotatedOffset.z;

        Vec3[] out = new Vec3[localVertices.length];
        for (int i = 0; i < localVertices.length; i++) {
            Vec3 r = rotateVec(localVertices[i], roll, pitch, yaw);
            out[i] = new Vec3(cx + r.x, cy + r.y, cz + r.z);
        }
        return out;
    }

    /**
     * ワールド頂点から外向き法線（正規化）。
     * 時計回り: (p1-p0)×(p2-p0) が外側になる前提。
     */
    public static Vec3 outwardNormal(Vec3[] worldVerts) {
        if (worldVerts == null || worldVerts.length < 3) return Vec3.ZERO;
        Vec3 e1 = worldVerts[1].subtract(worldVerts[0]);
        Vec3 e2 = worldVerts[2].subtract(worldVerts[0]);
        Vec3 n = e1.cross(e2);
        double len = n.length();
        if (len < 1.0E-10) return Vec3.ZERO;
        return n.scale(1.0 / len);
    }

    private static Vec3 rotateVec(Vec3 v, double roll, double pitch, double yaw) {
        double x1 = v.x * Math.cos(roll) - v.y * Math.sin(roll);
        double y1 = v.x * Math.sin(roll) + v.y * Math.cos(roll);
        double z1 = v.z;

        double y2 = y1 * Math.cos(pitch) - z1 * Math.sin(pitch);
        double z2 = y1 * Math.sin(pitch) + z1 * Math.cos(pitch);
        double x2 = x1;

        double x3 = x2 * Math.cos(yaw) - z2 * Math.sin(yaw);
        double z3 = x2 * Math.sin(yaw) + z2 * Math.cos(yaw);
        double y3 = y2;

        return new Vec3(x3, y3, z3);
    }
}