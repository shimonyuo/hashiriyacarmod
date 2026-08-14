package com.hashiriyacarmod;

import net.minecraft.world.phys.Vec3;

/**
 * JSON hitbox の8頂点 + po/ro + skip_faces。
 * FACES index: 0下面 1上面 2前面 3右 4背面 5左
 */
public class HitboxDefinition {

    private final Vec3[] localVertices;
    private final Vec3 positionOffset;
    private final float offsetYaw;
    private final float offsetPitch;
    private final float offsetRoll;
    /** length 6。true の面はコリジョン計算から除外 */
    private final boolean[] skipFace;

    private static final int[][] FACES = {
            {0, 1, 2, 3},
            {7, 6, 5, 4},
            {0, 4, 5, 1},
            {1, 5, 6, 2},
            {2, 6, 7, 3},
            {3, 7, 4, 0}
    };

    public HitboxDefinition(Vec3[] localVertices) {
        this(localVertices, Vec3.ZERO, 0f, 0f, 0f, null);
    }

    public HitboxDefinition(Vec3[] localVertices, float offsetYaw, float offsetPitch, float offsetRoll) {
        this(localVertices, Vec3.ZERO, offsetYaw, offsetPitch, offsetRoll, null);
    }

    public HitboxDefinition(Vec3[] localVertices, Vec3 positionOffset,
                            float offsetYaw, float offsetPitch, float offsetRoll) {
        this(localVertices, positionOffset, offsetYaw, offsetPitch, offsetRoll, null);
    }

    public HitboxDefinition(Vec3[] localVertices, Vec3 positionOffset,
                            float offsetYaw, float offsetPitch, float offsetRoll,
                            boolean[] skipFace) {
        if (localVertices == null || localVertices.length != 8) {
            throw new IllegalArgumentException("hitboxのverticesは8個指定する必要があります");
        }
        this.localVertices = localVertices;
        this.positionOffset = positionOffset != null ? positionOffset : Vec3.ZERO;
        this.offsetYaw = offsetYaw;
        this.offsetPitch = offsetPitch;
        this.offsetRoll = offsetRoll;
        this.skipFace = normalizeSkip(skipFace);
    }

    private static boolean[] normalizeSkip(boolean[] src) {
        boolean[] s = new boolean[6];
        if (src != null) {
            for (int i = 0; i < Math.min(6, src.length); i++) {
                s[i] = src[i];
            }
        }
        return s;
    }

    public static int[][] faces() {
        return FACES;
    }

    /** コピーを返す（呼び出し側で書き換えない想定でも安全） */
    public boolean[] getSkipFaces() {
        return skipFace.clone();
    }

    public boolean isFaceSkipped(int faceIndex) {
        return faceIndex >= 0 && faceIndex < 6 && skipFace[faceIndex];
    }

    public Vec3[] toWorldVertices(double posX, double posY, double posZ,
                                  float yawDegrees, float pitchDegrees, float rollDegrees) {
        double roll = Math.toRadians(rollDegrees + offsetRoll);
        double pitch = Math.toRadians(pitchDegrees + offsetPitch);
        double yaw = Math.toRadians(yawDegrees + offsetYaw);

        Vec3 rotatedOffset = rotateVec(positionOffset, roll, pitch, yaw);
        double centerX = posX + rotatedOffset.x;
        double centerY = posY + rotatedOffset.y;
        double centerZ = posZ + rotatedOffset.z;

        Vec3[] result = new Vec3[8];
        for (int i = 0; i < 8; i++) {
            Vec3 rotated = rotateVec(localVertices[i], roll, pitch, yaw);
            result[i] = new Vec3(centerX + rotated.x, centerY + rotated.y, centerZ + rotated.z);
        }
        return result;
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