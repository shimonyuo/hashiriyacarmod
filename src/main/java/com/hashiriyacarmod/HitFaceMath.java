package com.hashiriyacarmod;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Locale;

/**
 * 任意頂点数の多角形平面に対するヒット計算（基本ユーティリティ）。
 * レイ／点判定は副作用なし。コリジョン等からも利用する想定。
 *
 * 【検査用・後で削除】
 * debugChatRayHitFacesFromPlayer はテスト用。本番前にメソッドごと消す。
 */
public final class HitFaceMath {

    private static final double EPS = 1.0E-8;
    private static final double PLANE_DIST_EPS = 1.0E-4;

    private HitFaceMath() {}

    // -------------------------------------------------------------------------
    // 基本計算（残す）
    // -------------------------------------------------------------------------

    /**
     * 線分 start→end が多角形（同一平面・3点以上）に当たるときの t（0=始点, 1=終点）。
     * 非ヒットは null。
     */
    public static Double lineHitPolygonT(Vec3 start, Vec3 end, Vec3[] poly) {
        if (start == null || end == null || poly == null || poly.length < 3) {
            return null;
        }

        Vec3 dir = end.subtract(start);
        if (dir.lengthSqr() < EPS) {
            return pointInPolygon(start, poly) ? 0.0 : null;
        }

        Vec3 n = planeNormal(poly);
        if (n.lengthSqr() < EPS) {
            return null;
        }
        n = n.normalize();

        double denom = dir.dot(n);
        if (Math.abs(denom) < EPS) {
            return null;
        }

        double t = poly[0].subtract(start).dot(n) / denom;
        if (t < 0.0 || t > 1.0) {
            return null;
        }

        Vec3 hit = start.add(dir.scale(t));
        if (!pointInPolygon(hit, poly)) {
            return null;
        }
        return t;
    }

    /**
     * 複数多角形のうち、最も手前（最小 t）のヒット。
     * @return t、無ければ null
     */
    public static Double firstHitT(Vec3 start, Vec3 end, Iterable<Vec3[]> faces) {
        Double best = null;
        if (faces == null) {
            return null;
        }
        for (Vec3[] face : faces) {
            Double t = lineHitPolygonT(start, end, face);
            if (t == null) {
                continue;
            }
            if (best == null || t < best) {
                best = t;
            }
        }
        return best;
    }

    /**
     * 同一平面上の点が多角形内か（非凸対応・2D 射影レイキャスト）。
     */
    public static boolean pointInPolygon(Vec3 point, Vec3[] poly) {
        if (point == null || poly == null || poly.length < 3) {
            return false;
        }

        Vec3 n = planeNormal(poly);
        if (n.lengthSqr() < EPS) {
            return false;
        }
        n = n.normalize();

        if (Math.abs(point.subtract(poly[0]).dot(n)) > PLANE_DIST_EPS) {
            return false;
        }

        int drop = dominantAxis(n);
        int crossings = 0;
        int m = poly.length;

        for (int i = 0; i < m; i++) {
            Vec3 a = poly[i];
            Vec3 b = poly[(i + 1) % m];
            if (a == null || b == null) {
                continue;
            }

            double ax = coord(a, drop, 0);
            double ay = coord(a, drop, 1);
            double bx = coord(b, drop, 0);
            double by = coord(b, drop, 1);
            double px = coord(point, drop, 0);
            double py = coord(point, drop, 1);

            if (Math.abs(ay - by) < EPS) {
                continue;
            }
            if (py < Math.min(ay, by) || py >= Math.max(ay, by)) {
                continue;
            }

            double s = (py - ay) / (by - ay);
            double xInt = ax + s * (bx - ax);
            if (xInt > px) {
                crossings++;
            }
        }
        return (crossings & 1) == 1;
    }

    public static Vec3 planeNormal(Vec3[] poly) {
        if (poly == null || poly.length < 3) {
            return Vec3.ZERO;
        }
        for (int i = 0; i + 2 < poly.length; i++) {
            Vec3 a = poly[i];
            Vec3 b = poly[i + 1];
            Vec3 c = poly[i + 2];
            if (a == null || b == null || c == null) {
                continue;
            }
            Vec3 n = b.subtract(a).cross(c.subtract(a));
            if (n.lengthSqr() > EPS) {
                return n;
            }
        }
        return Vec3.ZERO;
    }

    private static int dominantAxis(Vec3 n) {
        double ax = Math.abs(n.x);
        double ay = Math.abs(n.y);
        double az = Math.abs(n.z);
        if (ax >= ay && ax >= az) {
            return 0;
        }
        if (ay >= az) {
            return 1;
        }
        return 2;
    }

    private static double coord(Vec3 v, int dropAxis, int which) {
        return switch (dropAxis) {
            case 0 -> which == 0 ? v.y : v.z;
            case 1 -> which == 0 ? v.x : v.z;
            default -> which == 0 ? v.x : v.y;
        };
    }

    // -------------------------------------------------------------------------
    // 【検査用・後で削除】プレイヤー視線レイ → ヒットした CarEntity 情報を本人チャットへ
    // -------------------------------------------------------------------------

    /**
     * TODO: 検査用。後でこのメソッドごと削除する。
     * サーバー専用。player の目線レイが CarEntity の hitfaces に当たったら、
     * そのプレイヤーのチャットにエンティティ情報を出す（他プレイヤーには出さない）。
     */
    public static void debugChatRayHitFacesFromPlayer(ServerPlayer player) {
        if (player == null || player.level().isClientSide()) {
            return;
        }

        Vec3 eye = player.getEyePosition(1.0f);
        Vec3 look = player.getViewVector(1.0f);
        double reach = player.isCreative() ? 5.0 : 4.5;
        Vec3 end = eye.add(look.scale(reach));

        AABB area = new AABB(eye, end).inflate(reach);
        List<Entity> nearby = player.level().getEntities(
                player, area, e -> e instanceof CarEntity);

        CarEntity bestCar = null;
        double bestT = Double.POSITIVE_INFINITY;
        int bestFace = -1;

        for (Entity e : nearby) {
            CarEntity car = (CarEntity) e;
            List<Vec3[]> faces = car.getAllWorldHitFaceVertices();
            if (faces == null || faces.isEmpty()) {
                continue;
            }
            for (int i = 0; i < faces.size(); i++) {
                Double t = lineHitPolygonT(eye, end, faces.get(i));
                if (t == null) {
                    continue;
                }
                if (t < bestT) {
                    bestT = t;
                    bestCar = car;
                    bestFace = i;
                }
            }
        }

        if (bestCar == null) {
            player.sendSystemMessage(Component.literal("[HitFaceRayUtil DEBUG] ヒットなし"));
            return;
        }

        player.sendSystemMessage(Component.literal(String.format(
                Locale.ROOT,
                "[HitFaceRayUtil DEBUG] uuid=%s baseName=%s face=%d t=%.4f pos=%.2f,%.2f,%.2f",
                bestCar.getUUID(),
                bestCar.getBaseName(),
                bestFace,
                bestT,
                bestCar.getX(),
                bestCar.getY(),
                bestCar.getZ()
        )));
    }
}