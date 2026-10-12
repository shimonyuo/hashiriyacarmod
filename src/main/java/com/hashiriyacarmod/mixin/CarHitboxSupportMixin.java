package com.hashiriyacarmod.mixin;

import com.hashiriyacarmod.CarCollisionUtil;
import com.hashiriyacarmod.CarEntity;
import com.hashiriyacarmod.CarEntityRegistry;
import com.hashiriyacarmod.CarGroundNormalHolder;
import com.hashiriyacarmod.HitFaceRayUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 車の OBB / hitfaces に対する接触・面沿い・端止めスキップ。
 * hitface は無限平面の裏ではなく、多角形面と AABB（または拡大 AABB）の重なりのみ。
 */
@Mixin(Entity.class)
public abstract class CarHitboxSupportMixin implements CarGroundNormalHolder {

    private static final double ON_GROUND_THRESHOLD = Math.cos(Math.toRadians(45.0));
    private static final double EPS = 1.0E-10;
    private static final double FACE_EPS = 1.0E-6;

    private static final int[][] AABB_EDGES = {
            {0, 1}, {1, 2}, {2, 3}, {3, 0},
            {4, 5}, {5, 6}, {6, 7}, {7, 4},
            {0, 4}, {1, 5}, {2, 6}, {3, 7}
    };

    @Unique
    private boolean hashiriyacarmod$carGroundThisMove = false;

    @Unique
    private boolean hashiriyacarmod$shouldSkipEdge = false;

    @Override
    public boolean hashiriyacarmod$shouldSkipEdge() {
        return this.hashiriyacarmod$shouldSkipEdge;
    }

    @Override
    public void hashiriyacarmod$setShouldSkipEdge(boolean skip) {
        this.hashiriyacarmod$shouldSkipEdge = skip;
    }

    @ModifyVariable(
            method = "move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private Vec3 hashiriyacarmod$slideAlongCarObb(Vec3 movement) {
        Entity self = (Entity) (Object) this;
        this.hashiriyacarmod$carGroundThisMove = false;
        this.hashiriyacarmod$setShouldSkipEdge(false);

        if (self instanceof CarEntity) return movement;
        if (self.level() == null) return movement;
        if (self.isSpectator()) return movement;

        AABB box = self.getBoundingBox();
        Vec3 result = movement;
        boolean hasMovement = movement.lengthSqr() >= EPS;

        if (hasMovement) {
            AABB expanded = box.expandTowards(movement.x, movement.y, movement.z);
            if (touchesAnyCarGeometry(self, expanded)) {
                this.hashiriyacarmod$setShouldSkipEdge(true);
            }
        }
        if (!this.hashiriyacarmod$shouldSkipEdge) {
            if (touchesAnyCarGeometry(self, box)) {
                this.hashiriyacarmod$setShouldSkipEdge(true);
            }
        }

        result = processHits(self, box, result, hasMovement, true);
        box = self.getBoundingBox();
        result = processHits(self, box, result, hasMovement, false);

        return result;
    }

    /**
     * @param groundPass true = 0〜45度 / false = 壁
     */
    @Unique
    private Vec3 processHits(Entity self, AABB box, Vec3 result, boolean hasMovement, boolean groundPass) {
        for (CarEntity car : CarEntityRegistry.getAllInLevel(self.level())) {
            if (car.isRemoved()) continue;

            for (Vec3[] obbVertices : car.getAllWorldHitboxVertices()) {
                result = processOneObb(self, box, result, hasMovement, groundPass, obbVertices);
                box = self.getBoundingBox();
            }

            List<Vec3[]> faces = car.getAllWorldHitFaceVertices();
            if (faces == null) continue;
            for (Vec3[] face : faces) {
                if (face == null || face.length < 3) continue;
                result = processOneFace(self, box, result, hasMovement, groundPass, face);
                box = self.getBoundingBox();
            }
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // OBB
    // -------------------------------------------------------------------------

    @Unique
    private Vec3 processOneObb(Entity self, AABB box, Vec3 result,
                               boolean hasMovement, boolean groundPass, Vec3[] obbVertices) {

        Vec3 mtvNow = CarCollisionUtil.computeMTV(obbVertices, aabbToVertices(box));
        if (mtvNow != null && mtvNow.lengthSqr() >= EPS) {
            Vec3 normal = mtvNow.normalize();
            boolean ground = normal.y >= ON_GROUND_THRESHOLD;
            if (ground != groundPass) return result;

            if (hasMovement) {
                result = respondToSurface(result, normal, ground);
            }
            applyVelocityResponse(self, normal, ground);
            self.setPos(self.getX() + mtvNow.x, self.getY() + mtvNow.y, self.getZ() + mtvNow.z);
            if (ground) this.hashiriyacarmod$carGroundThisMove = true;
            return result;
        }

        if (!hasMovement) return result;

        AABB swept = box.expandTowards(result.x, result.y, result.z);
        Vec3 mtvSwept = CarCollisionUtil.computeMTV(obbVertices, aabbToVertices(swept));
        if (mtvSwept == null || mtvSwept.lengthSqr() < EPS) return result;

        Vec3 normal = mtvSwept.normalize();
        boolean ground = normal.y >= ON_GROUND_THRESHOLD;
        if (ground != groundPass) return result;

        double toi = findTimeOfImpactObb(box, result, obbVertices);
        toi = Math.max(0.0, Math.min(1.0, toi));

        Vec3 toContact = result.scale(toi);
        Vec3 remaining = result.subtract(toContact);

        AABB atContact = box.move(toContact.x, toContact.y, toContact.z);
        Vec3 mtvContact = CarCollisionUtil.computeMTV(obbVertices, aabbToVertices(atContact));
        if (mtvContact != null && mtvContact.lengthSqr() >= EPS) {
            normal = mtvContact.normalize();
            ground = normal.y >= ON_GROUND_THRESHOLD;
            if (ground != groundPass) return result;
        }

        if (ground) this.hashiriyacarmod$carGroundThisMove = true;
        remaining = respondToSurface(remaining, normal, ground);
        result = toContact.add(remaining);
        applyVelocityResponse(self, normal, ground);
        return result;
    }

    // -------------------------------------------------------------------------
    // hitfaces（厳密：多角形面 × AABB の重なり）
    // -------------------------------------------------------------------------

    @Unique
    private Vec3 processOneFace(Entity self, AABB box, Vec3 result,
                                boolean hasMovement, boolean groundPass, Vec3[] face) {

        double[] depthBuf = new double[1];
        Vec3 normalNow = faceOverlap(box, face, depthBuf);

        // すでに面と重なり、裏に食い込み → 表へ setPos
        if (normalNow != null && depthBuf[0] > FACE_EPS) {
            boolean ground = normalNow.y >= ON_GROUND_THRESHOLD;
            if (ground != groundPass) return result;

            Vec3 push = normalNow.scale(depthBuf[0]);
            if (hasMovement) {
                result = respondToSurface(result, normalNow, ground);
            }
            applyVelocityResponse(self, normalNow, ground);
            self.setPos(self.getX() + push.x, self.getY() + push.y, self.getZ() + push.z);
            if (ground) this.hashiriyacarmod$carGroundThisMove = true;
            return result;
        }

        if (!hasMovement) return result;

        // 移動で拡大した箱が面と重なるか
        AABB swept = box.expandTowards(result.x, result.y, result.z);
        Vec3 normalSwept = faceOverlap(swept, face, depthBuf);
        if (normalSwept == null) return result;

        Vec3 normal = normalSwept;
        boolean ground = normal.y >= ON_GROUND_THRESHOLD;
        if (ground != groundPass) return result;

        double toi = findTimeOfImpactFace(box, result, face);
        toi = Math.max(0.0, Math.min(1.0, toi));

        Vec3 toContact = result.scale(toi);
        Vec3 remaining = result.subtract(toContact);

        AABB atContact = box.move(toContact.x, toContact.y, toContact.z);
        Vec3 normalContact = faceOverlap(atContact, face, depthBuf);
        if (normalContact != null) {
            normal = normalContact;
            ground = normal.y >= ON_GROUND_THRESHOLD;
            if (ground != groundPass) return result;

            if (depthBuf[0] > FACE_EPS) {
                Vec3 push = normal.scale(depthBuf[0]);
                self.setPos(
                        self.getX() + toContact.x + push.x,
                        self.getY() + toContact.y + push.y,
                        self.getZ() + toContact.z + push.z
                );
                remaining = respondToSurface(remaining, normal, ground);
                applyVelocityResponse(self, normal, ground);
                if (ground) this.hashiriyacarmod$carGroundThisMove = true;
                return remaining;
            }
        }

        if (ground) this.hashiriyacarmod$carGroundThisMove = true;
        remaining = respondToSurface(remaining, normal, ground);
        result = toContact.add(remaining);
        applyVelocityResponse(self, normal, ground);
        return result;
    }

    @Unique
    @Nullable
    private static Vec3 faceOverlap(AABB box, Vec3[] face, double[] depthOut) {
        depthOut[0] = 0.0;

        Vec3 n = HitFaceRayUtil.planeNormal(face);
        if (n.lengthSqr() < EPS) return null;
        n = n.normalize();

        Vec3[] corners = aabbToVertices(box);
        double minDot = Double.POSITIVE_INFINITY;
        double maxDot = Double.NEGATIVE_INFINITY;
        for (Vec3 c : corners) {
            double d = c.subtract(face[0]).dot(n);
            if (d < minDot) minDot = d;
            if (d > maxDot) maxDot = d;
        }

        // 平面が箱を切っていない → 面と重ならない
        if (maxDot < -FACE_EPS || minDot > FACE_EPS) {
            return null;
        }

        boolean inPoly = false;

        // ★ 角の投影は使わない。辺と平面の交点だけ見る
        for (int[] e : AABB_EDGES) {
            Vec3 a = corners[e[0]];
            Vec3 b = corners[e[1]];
            double da = a.subtract(face[0]).dot(n);
            double db = b.subtract(face[0]).dot(n);
            if (da * db > 0.0) continue;          // 同側
            if (Math.abs(da - db) < EPS) continue;
            double t = da / (da - db);
            if (t < -FACE_EPS || t > 1.0 + FACE_EPS) continue;
            Vec3 p = a.add(b.subtract(a).scale(t));
            if (HitFaceRayUtil.pointInPolygon(p, face)) {
                inPoly = true;
                break;
            }
        }

        // 多角形の辺が AABB を切るか
        if (!inPoly) {
            int m = face.length;
            for (int i = 0; i < m; i++) {
                Vec3 a = face[i];
                Vec3 b = face[(i + 1) % m];
                if (a == null || b == null) continue;
                if (segmentIntersectsAabb(a, b, box)) {
                    inPoly = true;
                    break;
                }
            }
        }

        if (!inPoly) {
            return null;
        }

        depthOut[0] = minDot < 0.0 ? -minDot : 0.0;
        return n;
    }

    @Unique
    private static boolean segmentIntersectsAabb(Vec3 a, Vec3 b, AABB box) {
        double dx = b.x - a.x;
        double dy = b.y - a.y;
        double dz = b.z - a.z;
        double[] t = new double[]{0.0, 1.0};
        if (!clipAxis(a.x, dx, box.minX, box.maxX, t)) return false;
        if (!clipAxis(a.y, dy, box.minY, box.maxY, t)) return false;
        if (!clipAxis(a.z, dz, box.minZ, box.maxZ, t)) return false;
        return t[0] <= t[1];
    }

    @Unique
    private static boolean clipAxis(double start, double dir, double min, double max, double[] t) {
        if (Math.abs(dir) < EPS) {
            return start >= min - FACE_EPS && start <= max + FACE_EPS;
        }
        double inv = 1.0 / dir;
        double tNear = (min - start) * inv;
        double tFar = (max - start) * inv;
        if (tNear > tFar) {
            double tmp = tNear;
            tNear = tFar;
            tFar = tmp;
        }
        if (tNear > t[0]) t[0] = tNear;
        if (tFar < t[1]) t[1] = tFar;
        return t[0] <= t[1];
    }

    @Unique
    private static double findTimeOfImpactFace(AABB box, Vec3 movement, Vec3[] face) {
        double[] depthBuf = new double[1];
        if (movement.lengthSqr() < EPS) return 1.0;
        if (faceOverlap(box, face, depthBuf) != null) return 0.0;

        AABB expanded = box.expandTowards(movement.x, movement.y, movement.z);
        if (faceOverlap(expanded, face, depthBuf) == null) return 1.0;

        double lo = 0.0;
        double hi = 1.0;
        for (int i = 0; i < 16; i++) {
            double mid = (lo + hi) * 0.5;
            AABB at = box.move(movement.x * mid, movement.y * mid, movement.z * mid);
            if (faceOverlap(at, face, depthBuf) != null) {
                hi = mid;
            } else {
                lo = mid;
            }
        }
        return hi;
    }

    // -------------------------------------------------------------------------
    // 端止め
    // -------------------------------------------------------------------------

    @Unique
    private static boolean touchesAnyCarGeometry(Entity self, AABB box) {
        Vec3[] boxVerts = aabbToVertices(box);
        double[] depthBuf = new double[1];

        for (CarEntity car : CarEntityRegistry.getAllInLevel(self.level())) {
            if (car.isRemoved()) continue;

            for (Vec3[] obb : car.getAllWorldHitboxVertices()) {
                Vec3 mtv = CarCollisionUtil.computeMTV(obb, boxVerts);
                if (mtv != null && mtv.lengthSqr() >= EPS) return true;
            }

            List<Vec3[]> faces = car.getAllWorldHitFaceVertices();
            if (faces == null) continue;
            for (Vec3[] face : faces) {
                if (face == null || face.length < 3) continue;
                if (faceOverlap(box, face, depthBuf) != null) return true;
            }
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // 共通
    // -------------------------------------------------------------------------

    @Inject(
            method = "move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V",
            at = @At("RETURN")
    )
    private void hashiriyacarmod$finalizeCarOnGround(MoverType type, Vec3 movement, CallbackInfo ci) {
        if (this.hashiriyacarmod$carGroundThisMove) {
            ((Entity) (Object) this).setOnGround(true);
        }
        this.hashiriyacarmod$carGroundThisMove = false;
    }

    @Unique
    private static Vec3 respondToSurface(Vec3 v, Vec3 normal, boolean ground) {
        if (ground && v.y < 0.0) {
            v = new Vec3(v.x, 0.0, v.z);
        }
        return projectAlongSurface(v, normal);
    }

    @Unique
    private static void applyVelocityResponse(Entity self, Vec3 normal, boolean ground) {
        Vec3 vel = self.getDeltaMovement();
        Vec3 adjusted = respondToSurface(vel, normal, ground);
        if (adjusted.x != vel.x || adjusted.y != vel.y || adjusted.z != vel.z) {
            self.setDeltaMovement(adjusted);
        }
    }

    @Unique
    private static Vec3 projectAlongSurface(Vec3 v, Vec3 normal) {
        double dot = v.dot(normal);
        if (dot < 0.0) {
            return v.subtract(normal.scale(dot));
        }
        return v;
    }

    @Unique
    private static double findTimeOfImpactObb(AABB box, Vec3 movement, Vec3[] obbVertices) {
        if (movement.lengthSqr() < EPS) return 1.0;
        if (CarCollisionUtil.computeMTV(obbVertices, aabbToVertices(box)) != null) return 0.0;

        AABB expanded = box.expandTowards(movement.x, movement.y, movement.z);
        if (CarCollisionUtil.computeMTV(obbVertices, aabbToVertices(expanded)) == null) return 1.0;

        double lo = 0.0;
        double hi = 1.0;
        for (int i = 0; i < 16; i++) {
            double mid = (lo + hi) * 0.5;
            AABB at = box.move(movement.x * mid, movement.y * mid, movement.z * mid);
            if (CarCollisionUtil.computeMTV(obbVertices, aabbToVertices(at)) != null) {
                hi = mid;
            } else {
                lo = mid;
            }
        }
        return hi;
    }

    @Unique
    private static Vec3[] aabbToVertices(AABB box) {
        return new Vec3[]{
                new Vec3(box.minX, box.minY, box.minZ),
                new Vec3(box.maxX, box.minY, box.minZ),
                new Vec3(box.maxX, box.minY, box.maxZ),
                new Vec3(box.minX, box.minY, box.maxZ),
                new Vec3(box.minX, box.maxY, box.minZ),
                new Vec3(box.maxX, box.maxY, box.minZ),
                new Vec3(box.maxX, box.maxY, box.maxZ),
                new Vec3(box.minX, box.maxY, box.maxZ)
        };
    }
}