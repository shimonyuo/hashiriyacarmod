package com.hashiriyacarmod;

import net.minecraft.world.phys.Vec3;

/**
 * .jsonのhitboxで指定された8頂点をもとに、箱（台形も含む）の形を保持するクラスです。
 * ローカル座標（エンティティの原点を基準にした、回転前の座標）を持ち、
 * エンティティの位置・ヨー・ピッチ・ロールを反映したワールド座標へ変換する機能を持ちます。
 *
 * 「po」（位置オフセット）と「ro」（回転オフセット）を持てるようにしてあります。
 * これにより、objモデルの見た目の向きとは完全に独立して、当たり判定の箱だけを
 * 好きな位置・角度に固定でずらしておくことができます。
 * この2つは、エンティティ本体の位置・回転が動くたびに、常に一緒に追従します。
 */
public class HitboxDefinition {

    private final Vec3[] localVertices;

    // ========== hitbox専用の固定オフセット ==========
    // これはエンティティ本体の位置・yaw/pitch/rollとは別に、常にこの分だけ
    // 追加でずらす・回転させるための値です。objモデル側には一切影響しません。
    private final Vec3 positionOffset;   // "po" : 中心位置のずらし
    private final float offsetYaw;
    private final float offsetPitch;
    private final float offsetRoll;

    private static final int[][] FACES = {
            {0, 1, 2, 3}, // 下面
            {7, 6, 5, 4}, // 上面
            {0, 4, 5, 1}, // 前面
            {1, 5, 6, 2}, // 右面
            {2, 6, 7, 3}, // 背面
            {3, 7, 4, 0}  // 左面
    };

    public HitboxDefinition(Vec3[] localVertices) {
        this(localVertices, Vec3.ZERO, 0f, 0f, 0f);
    }

    public HitboxDefinition(Vec3[] localVertices, float offsetYaw, float offsetPitch, float offsetRoll) {
        this(localVertices, Vec3.ZERO, offsetYaw, offsetPitch, offsetRoll);
    }

    public HitboxDefinition(Vec3[] localVertices, Vec3 positionOffset,
                            float offsetYaw, float offsetPitch, float offsetRoll) {
        if (localVertices == null || localVertices.length != 8) {
            throw new IllegalArgumentException("hitboxのverticesは8個指定する必要があります");
        }
        this.localVertices = localVertices;
        this.positionOffset = positionOffset != null ? positionOffset : Vec3.ZERO;
        this.offsetYaw = offsetYaw;
        this.offsetPitch = offsetPitch;
        this.offsetRoll = offsetRoll;
    }

    public static int[][] faces() {
        return FACES;
    }

    /**
     * エンティティの位置・ヨー・ピッチ・ロールに、hitbox専用の po/ro を
     * 加算した上で、ワールド座標の8頂点を計算します。
     *
     * 順番：まず頂点をロール→ピッチ→ヨーで回転させ、その後、
     * 「回転済みのpositionOffset」をエンティティの位置に足します。
     * こうすることで、po自体もエンティティの向きに合わせて一緒に回るようになります。
     */
    public Vec3[] toWorldVertices(double posX, double posY, double posZ,
                                  float yawDegrees, float pitchDegrees, float rollDegrees) {

        double roll  = Math.toRadians(rollDegrees + offsetRoll);
        double pitch = Math.toRadians(pitchDegrees + offsetPitch);
        double yaw   = Math.toRadians(yawDegrees + offsetYaw);

        // ── positionOffset自体も、エンティティの向きに合わせて回転させます ──
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

    /** ロール→ピッチ→ヨーの順で、1つのベクトルを回転させます。 */
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