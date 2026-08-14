package com.hashiriyacarmod;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * 8頂点からなる箱どうし、または箱とMinecraft標準のAABB
 * （ブロックや、回転しない通常のエンティティが持つ直方体の判定範囲）が
 * 重なっているかどうかを判定します。
 *
 * さらに、重なっている場合に「どちらの方向に・どれだけ動かせば
 * 重なりが解消するか」という押し出しベクトル（MTV）も計算できます。
 */
public class CarCollisionUtil {

    public CarCollisionUtil() {}

    /** カスタム箱どうしが重なっているか判定します。 */
    public static boolean intersects(Vec3[] verticesA, Vec3[] verticesB) {
        return computeMTV(verticesA, verticesB) != null;
    }

    /** カスタム箱と、Minecraft標準のAABBが重なっているか判定します。 */
    public static boolean intersects(Vec3[] verticesA, AABB aabb) {
        return intersects(verticesA, aabbVertices(aabb));
    }

    public static boolean lineIntersectsBox(Vec3 start, Vec3 end, Vec3[] boxVertices) {

        Vec3 diff = end.subtract(start);
        double length = diff.length();
        if (length < 1.0E-8) {
            return containsPoint(start, boxVertices);
        }

        // 線分を細かく分割して、各点が箱の内側にあるかを調べます。
        // 分割数は「線の長さ」に応じて増やし、細長い箱でも見逃さないようにします。
        int steps = Math.max(64, (int) Math.ceil(length * 32));

        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            Vec3 point = start.add(diff.scale(t));
            if (containsPoint(point, boxVertices)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 1つの点が、8頂点で定義された凸多面体(箱)の内側にあるかを判定します。
     *
     * 考え方:箱の6面すべてについて、その面の法線と、面から点への向きの
     * 内積を調べます。すべての面で「内側向き」であれば、点は箱の中にあります。
     */
    public static boolean containsPoint(Vec3 point, Vec3[] boxVertices) {
        int[][] faces = HitboxDefinition.faces();

        for (int[] face : faces) {
            Vec3 p0 = boxVertices[face[0]];
            Vec3 p1 = boxVertices[face[1]];
            Vec3 p2 = boxVertices[face[2]];

            Vec3 normal = p1.subtract(p0).cross(p2.subtract(p0));

            // ★法線を外向きに統一（上記修正と同じ処理）
            Vec3 center = centerOf(boxVertices);
            Vec3 toCenter = center.subtract(p0);
            if (normal.dot(toCenter) > 0) {
                normal = normal.scale(-1);
            }

            Vec3 toPoint = point.subtract(p0);

            // 外向き法線なら、点が「外側（面から遠い側）」にあったら内側ではない
            if (toPoint.dot(normal) > 1.0E-6) {
                return false;
            }
        }

        return true;
    }
    /**
     * カスタム箱どうしの「押し出しベクトル」を計算します。
     *
     * 重なっていない場合は null を返します。
     * 重なっている場合は、「Aから見てBをこれだけ動かせば重ならなくなる」という
     * ベクトル（方向＋重なっている深さ）を返します。
     * （Aを反対方向に動かしたい場合は、このベクトルを反転させて使ってください）
     */
    public static Vec3 computeMTV(Vec3[] verticesA, Vec3[] verticesB) {
        // SAT（分離軸定理）で全軸をチェック
        Vec3[] normalsA = faceNormals(verticesA);
        Vec3[] normalsB = faceNormals(verticesB);
        Vec3[] edgesA = edgeDirections(verticesA);
        Vec3[] edgesB = edgeDirections(verticesB);

        List<Vec3> axes = new ArrayList<>();
        addAll(axes, normalsA);
        addAll(axes, normalsB);
        for (Vec3 ea : edgesA) {
            for (Vec3 eb : edgesB) {
                Vec3 cross = ea.cross(eb);
                if (cross.lengthSqr() > 1.0E-8) {
                    axes.add(cross.normalize());
                }
            }
        }

        Vec3 minOverlapAxis = null;
        double minOverlap = Double.POSITIVE_INFINITY;

        for (Vec3 axis : axes) {
            double[] rangeA = projectRange(axis, verticesA);
            double[] rangeB = projectRange(axis, verticesB);

            if (rangeA[1] < rangeB[0] || rangeB[1] < rangeA[0]) {
                return null; // 分離軸発見 → 重なっていない
            }

            double overlap = Math.min(rangeA[1], rangeB[1]) - Math.max(rangeA[0], rangeB[0]);
            if (overlap < minOverlap) {
                minOverlap = overlap;
                minOverlapAxis = axis;
            }
        }

        if (minOverlapAxis == null || minOverlap <= 0) {
            return Vec3.ZERO;
        }

        // 押し出す方向を決める（Aから見てBを押し出す）
        Vec3 centerA = centerOf(verticesA);
        Vec3 centerB = centerOf(verticesB);
        Vec3 toB = centerB.subtract(centerA);

        // 法線がBの方向を向いていない場合は反転
        if (minOverlapAxis.dot(toB) < 0) {
            minOverlapAxis = minOverlapAxis.scale(-1);
        }

        return minOverlapAxis.scale(minOverlap);
    }
    /**
     * カスタム箱とAABBの「押し出しベクトル」を計算します。
     * 重なっていなければ null です。
     */
    public static Vec3 computeMTV(Vec3[] verticesA, AABB aabb) {
        Vec3[] bVertices = aabbVertices(aabb);
        Vec3 mtv = computeMTV(verticesA, bVertices);

        // デバッグログ（一時的に追加）
        if (mtv != null && mtv.lengthSqr() > 0) {
            System.out.println("[MTV AABB] 押し出し量: " + mtv + " | 長さ: " + mtv.length());
        }

        return mtv;
    }

    public static double[] projectRange(Vec3 axis, Vec3[] vertices) {
        double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
        for (Vec3 v : vertices) {
            double d = v.dot(axis);
            min = Math.min(min, d);
            max = Math.max(max, d);
        }
        return new double[]{min, max};
    }

    public static Vec3 centerOf(Vec3[] vertices) {
        double x = 0, y = 0, z = 0;
        for (Vec3 v : vertices) {
            x += v.x; y += v.y; z += v.z;
        }
        int n = vertices.length;
        return new Vec3(x / n, y / n, z / n);
    }

    public static void addAll(List<Vec3> list, Vec3[] arr) {
        for (Vec3 v : arr) {
            if (v.lengthSqr() > 1.0E-8) {
                list.add(v.normalize());
            }
        }
    }

    public static Vec3[] faceNormals(Vec3[] vertices) {
        Vec3 center = centerOf(vertices);
        int[][] faces = HitboxDefinition.faces();
        Vec3[] normals = new Vec3[faces.length];

        for (int i = 0; i < faces.length; i++) {
            Vec3 p0 = vertices[faces[i][0]];
            Vec3 p1 = vertices[faces[i][1]];
            Vec3 p2 = vertices[faces[i][2]];

            Vec3 normal = p1.subtract(p0).cross(p2.subtract(p0));

            // ★重要：法線がボックスの中心から外向き（遠ざかる方向）であることを保証
            Vec3 toCenter = center.subtract(p0);
            if (normal.dot(toCenter) > 0) {
                // 内向きなら反転させ、外向きに統一
                normal = normal.scale(-1);
            }

            normals[i] = normal;
        }
        return normals;
    }

    /** 代表的な3方向の辺（0→1, 1→2, 0→4）を使います。 */
    private static Vec3[] edgeDirections(Vec3[] vertices) {
        return new Vec3[]{
                vertices[1].subtract(vertices[0]),
                vertices[2].subtract(vertices[1]),
                vertices[4].subtract(vertices[0])
        };
    }

    private static Vec3[] aabbVertices(AABB aabb) {
        return new Vec3[]{
                new Vec3(aabb.minX, aabb.minY, aabb.minZ),
                new Vec3(aabb.maxX, aabb.minY, aabb.minZ),
                new Vec3(aabb.maxX, aabb.minY, aabb.maxZ),
                new Vec3(aabb.minX, aabb.minY, aabb.maxZ),
                new Vec3(aabb.minX, aabb.maxY, aabb.minZ),
                new Vec3(aabb.maxX, aabb.maxY, aabb.minZ),
                new Vec3(aabb.maxX, aabb.maxY, aabb.maxZ),
                new Vec3(aabb.minX, aabb.maxY, aabb.maxZ)
        };
    }
}