package com.hashiriyacarmod.mixin;

import com.hashiriyacarmod.CarEntity;
import com.hashiriyacarmod.CarEntityRegistry;
import com.hashiriyacarmod.HitFaceMath;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * hitfaces（多角形平面）に Entity の AABB がめり込んだら、
 * 法線方向に setPos して押し出す（第1段）。
 */
@Mixin(Entity.class)
public abstract class HitFacesCollisionMixin {

    private static final double EPS = 1.0E-8;
    private static final double PENETRATE_EPS = 1.0E-4;

    @Inject(
            method = "move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V",
            at = @At("HEAD")
    )
    private void hashiriyacarmod$pushOutFromHitFaces(MoverType type, Vec3 movement, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;

        if (self instanceof CarEntity) return;
        if (self.level() == null) return;
        if (self.isSpectator()) return;

        AABB box = self.getBoundingBox();

        for (CarEntity car : CarEntityRegistry.getAllInLevel(self.level())) {
            if (car.isRemoved()) continue;

            List<Vec3[]> faces = car.getAllWorldHitFaceVertices();
            if (faces == null || faces.isEmpty()) continue;

            for (Vec3[] face : faces) {
                if (face == null || face.length < 3) continue;

                Vec3 push = computePushOut(box, face);
                if (push == null || push.lengthSqr() < EPS) continue;

                self.setPos(
                        self.getX() + push.x,
                        self.getY() + push.y,
                        self.getZ() + push.z
                );
                box = self.getBoundingBox();
            }
        }
    }

    /**
     * AABB が面の裏側に食い込み、多角形と関係するとき
     * 法線方向の押し出しベクトル。不要なら null。
     */
    @Unique
    private static Vec3 computePushOut(AABB box, Vec3[] face) {
        Vec3 n = HitFaceMath.planeNormal(face);
        if (n.lengthSqr() < EPS) return null;
        n = n.normalize();

        double minDot = Double.POSITIVE_INFINITY;
        boolean anyInPoly = false;

        for (Vec3 c : cornersOf(box)) {
            double d = c.subtract(face[0]).dot(n);
            if (d < minDot) {
                minDot = d;
            }
            if (Math.abs(d) < 0.5) {
                Vec3 onPlane = c.subtract(n.scale(d));
                if (HitFaceMath.pointInPolygon(onPlane, face)) {
                    anyInPoly = true;
                }
            }
        }

        if (minDot >= -PENETRATE_EPS) {
            return null;
        }

        if (!anyInPoly) {
            Vec3 center = new Vec3(
                    (box.minX + box.maxX) * 0.5,
                    (box.minY + box.maxY) * 0.5,
                    (box.minZ + box.maxZ) * 0.5
            );
            double cd = center.subtract(face[0]).dot(n);
            Vec3 centerOnPlane = center.subtract(n.scale(cd));
            if (!HitFaceMath.pointInPolygon(centerOnPlane, face)) {
                return null;
            }
        }

        double depth = -minDot;
        return n.scale(depth);
    }

    @Unique
    private static Vec3[] cornersOf(AABB box) {
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