package com.hashiriyacarmod;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * 8頂点OBB同士 / OBBとAABBの交差・MTV・線分判定。
 * skipFace[i]==true の面(0〜5)は法線・点内判定から除外する。
 */
public class CarCollisionUtil {

    public CarCollisionUtil() {}

    private static final double EPS = 1.0E-10;

    public static boolean intersects(Vec3[] verticesA, Vec3[] verticesB) {
        return computeMTV(verticesA, verticesB, null, null) != null;
    }

    public static boolean intersects(Vec3[] verticesA, AABB aabb) {
        return computeMTV(verticesA, aabbVertices(aabb), null, null) != null;
    }

    public static boolean lineIntersectsBox(Vec3 start, Vec3 end, Vec3[] boxVertices) {
        return lineIntersectsBox(start, end, boxVertices, null);
    }

    public static boolean lineIntersectsBox(Vec3 start, Vec3 end, Vec3[] boxVertices, boolean[] skipFace) {
        Vec3 diff = end.subtract(start);
        double length = diff.length();
        if (length < 1.0E-8) {
            return containsPoint(start, boxVertices, skipFace);
        }
        int steps = Math.max(64, (int) Math.ceil(length * 32));
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            if (containsPoint(start.add(diff.scale(t)), boxVertices, skipFace)) {
                return true;
            }
        }
        return false;
    }

    public static boolean containsPoint(Vec3 point, Vec3[] boxVertices) {
        return containsPoint(point, boxVertices, null);
    }

    public static boolean containsPoint(Vec3 point, Vec3[] boxVertices, boolean[] skipFace) {
        int[][] faces = HitboxDefinition.faces();
        Vec3 center = centerOf(boxVertices);

        for (int i = 0; i < faces.length; i++) {
            if (skipFace != null && i < skipFace.length && skipFace[i]) continue;

            Vec3 p0 = boxVertices[faces[i][0]];
            Vec3 p1 = boxVertices[faces[i][1]];
            Vec3 p2 = boxVertices[faces[i][2]];
            Vec3 normal = p1.subtract(p0).cross(p2.subtract(p0));
            if (normal.dot(center.subtract(p0)) > 0) {
                normal = normal.scale(-1);
            }
            if (point.subtract(p0).dot(normal) > 1.0E-6) {
                return false;
            }
        }
        return true;
    }

    public static Vec3 computeMTV(Vec3[] verticesA, Vec3[] verticesB) {
        return computeMTV(verticesA, verticesB, null, null);
    }

    /**
     * skipA / skipB: 各箱の面スキップ（null = 全面使用）。長さ6想定。
     * A=車OBB, B=プレイヤー等のとき skipA に JSON の skip_faces を渡す。
     */
    public static Vec3 computeMTV(Vec3[] verticesA, Vec3[] verticesB,
                                  boolean[] skipA, boolean[] skipB) {
        Vec3[] normalsA = faceNormals(verticesA, skipA);
        Vec3[] normalsB = faceNormals(verticesB, skipB);
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
                return null;
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

        Vec3 centerA = centerOf(verticesA);
        Vec3 centerB = centerOf(verticesB);
        Vec3 toB = centerB.subtract(centerA);
        if (minOverlapAxis.dot(toB) < 0) {
            minOverlapAxis = minOverlapAxis.scale(-1);
        }

        // MTV がスキップ面の外向きとほぼ一致するなら、その面からの押し出しは無効
        if (isAlignedWithSkippedFace(verticesA, skipA, minOverlapAxis)) {
            return null;
        }

        return minOverlapAxis.scale(minOverlap);
    }

    public static Vec3 computeMTV(Vec3[] verticesA, AABB aabb) {
        return computeMTV(verticesA, aabbVertices(aabb), null, null);
    }

    public static Vec3 computeMTV(Vec3[] verticesA, AABB aabb, boolean[] skipA) {
        return computeMTV(verticesA, aabbVertices(aabb), skipA, null);
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
            x += v.x;
            y += v.y;
            z += v.z;
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
        return faceNormals(vertices, null);
    }

    public static Vec3[] faceNormals(Vec3[] vertices, boolean[] skipFace) {
        Vec3 center = centerOf(vertices);
        int[][] faces = HitboxDefinition.faces();
        List<Vec3> out = new ArrayList<>();

        for (int i = 0; i < faces.length; i++) {
            if (skipFace != null && i < skipFace.length && skipFace[i]) continue;

            Vec3 p0 = vertices[faces[i][0]];
            Vec3 p1 = vertices[faces[i][1]];
            Vec3 p2 = vertices[faces[i][2]];
            Vec3 normal = p1.subtract(p0).cross(p2.subtract(p0));
            if (normal.dot(center.subtract(p0)) > 0) {
                normal = normal.scale(-1);
            }
            if (normal.lengthSqr() > 1.0E-8) {
                out.add(normal.normalize());
            }
        }
        return out.toArray(new Vec3[0]);
    }

    /** MTV軸がスキップ面の外向き法線にほぼ平行なら true */
    private static boolean isAlignedWithSkippedFace(Vec3[] vertices, boolean[] skipFace, Vec3 axis) {
        if (skipFace == null || axis == null) return false;
        Vec3 nAxis = axis.normalize();
        int[][] faces = HitboxDefinition.faces();
        Vec3 center = centerOf(vertices);

        for (int i = 0; i < faces.length; i++) {
            if (i >= skipFace.length || !skipFace[i]) continue;

            Vec3 p0 = vertices[faces[i][0]];
            Vec3 p1 = vertices[faces[i][1]];
            Vec3 p2 = vertices[faces[i][2]];
            Vec3 normal = p1.subtract(p0).cross(p2.subtract(p0));
            if (normal.lengthSqr() < 1.0E-8) continue;
            if (normal.dot(center.subtract(p0)) > 0) {
                normal = normal.scale(-1);
            }
            normal = normal.normalize();
            if (nAxis.dot(normal) > 0.99) {
                return true;
            }
        }
        return false;
    }

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

    public static Vec3 computePolygonAabbMTV(Vec3[] poly, Vec3 normal, AABB aabb) {
        if (poly == null || poly.length < 3 || normal == null) return null;
        double nLen = normal.length();
        if (nLen < 1.0E-10) return null;
        Vec3 n = normal.scale(1.0 / nLen);

        double plane = poly[0].dot(n);
        Vec3[] corners = aabbVertices(aabb);
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (Vec3 c : corners) {
            double d = c.dot(n);
            min = Math.min(min, d);
            max = Math.max(max, d);
        }

        // 法線正側に完全にいる／負側に完全にいる（平面を跨がない）→ 非接触
        // 食い込み: AABB の一部が平面の「裏側」(d < plane)
        if (min >= plane - 1.0E-8) return null; // 完全に表側
        double penetration = plane - min;
        if (penetration <= 1.0E-8) return null;

        // 平面上の代表点（AABB中心を平面へ投影）が多角形内か
        Vec3 center = new Vec3(
                (aabb.minX + aabb.maxX) * 0.5,
                (aabb.minY + aabb.maxY) * 0.5,
                (aabb.minZ + aabb.maxZ) * 0.5);
        double dist = center.dot(n) - plane;
        Vec3 onPlane = center.subtract(n.scale(dist));
        if (!pointInPolygon3D(onPlane, poly, n)) {
            // 中心が外でも、角が載っている場合用に最深角を試す
            Vec3 deepest = null;
            double best = Double.POSITIVE_INFINITY;
            for (Vec3 c : corners) {
                double d = c.dot(n);
                if (d < best) {
                    best = d;
                    deepest = c;
                }
            }
            if (deepest == null) return null;
            double dd = deepest.dot(n) - plane;
            onPlane = deepest.subtract(n.scale(dd));
            if (!pointInPolygon3D(onPlane, poly, n)) return null;
        }

        return n.scale(penetration);
    }

    /** 平面上の点-in-多角形（同一平面・凸を想定。扇状三角形） */
    public static boolean pointInPolygon3D(Vec3 point, Vec3[] poly, Vec3 normal) {
        if (poly.length < 3) return false;
        Vec3 n = normal.normalize();
        // 平面基底
        Vec3 t = Math.abs(n.y) < 0.9 ? new Vec3(0, 1, 0).cross(n) : new Vec3(1, 0, 0).cross(n);
        if (t.lengthSqr() < 1.0E-12) return false;
        t = t.normalize();
        Vec3 b = n.cross(t).normalize();

        double px = point.subtract(poly[0]).dot(t);
        double py = point.subtract(poly[0]).dot(b);

        // 2D winding
        boolean inside = false;
        for (int i = 0, j = poly.length - 1; i < poly.length; j = i++) {
            double ix = poly[i].subtract(poly[0]).dot(t);
            double iy = poly[i].subtract(poly[0]).dot(b);
            double jx = poly[j].subtract(poly[0]).dot(t);
            double jy = poly[j].subtract(poly[0]).dot(b);
            if (((iy > py) != (jy > py))
                    && (px < (jx - ix) * (py - iy) / (jy - iy + 1.0E-30) + ix)) {
                inside = !inside;
            }
        }
        return inside;
    }

    public static double findTimeOfImpactPolygon(AABB box, Vec3 movement,
                                                 Vec3[] poly, Vec3 normal) {
        if (movement.lengthSqr() < EPS) return 1.0;
        if (computePolygonAabbMTV(poly, normal, box) != null) return 0.0;

        AABB expanded = box.expandTowards(movement.x, movement.y, movement.z);
        if (computePolygonAabbMTV(poly, normal, expanded) == null) return 1.0;

        double lo = 0.0, hi = 1.0;
        for (int i = 0; i < 16; i++) {
            double mid = (lo + hi) * 0.5;
            AABB at = box.move(movement.x * mid, movement.y * mid, movement.z * mid);
            if (computePolygonAabbMTV(poly, normal, at) != null) hi = mid;
            else lo = mid;
        }
        return hi;
    }
}