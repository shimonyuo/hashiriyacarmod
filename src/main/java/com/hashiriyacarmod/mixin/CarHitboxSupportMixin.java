package com.hashiriyacarmod.mixin;

import com.hashiriyacarmod.CarCollisionUtil;
import com.hashiriyacarmod.CarEntity;
import com.hashiriyacarmod.CarEntityRegistry;
import com.hashiriyacarmod.CarGroundNormalHolder;
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

@Mixin(Entity.class)
public abstract class CarHitboxSupportMixin implements CarGroundNormalHolder {

    private static final double ON_GROUND_THRESHOLD = Math.cos(Math.toRadians(45.0));
    private static final double EPS = 1.0E-10;

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

        // ★ 独自拡張ボックスが OBB と重なったら端止めスキップ用フラグを立てる
        if (hasMovement) {
            AABB expandedByMovement = box.expandTowards(movement.x, movement.y, movement.z);
            Vec3[] expandedVertices = aabbToVertices(expandedByMovement);
            for (CarEntity car : CarEntityRegistry.getAllInLevel(self.level())) {
                if (car.isRemoved()) continue;
                for (Vec3[] obbVertices : car.getAllWorldHitboxVertices()) {
                    Vec3 mtv = CarCollisionUtil.computeMTV(obbVertices, expandedVertices);
                    if (mtv != null && mtv.lengthSqr() >= EPS) {
                        this.hashiriyacarmod$setShouldSkipEdge(true);
                        break;
                    }
                }
                if (this.hashiriyacarmod$shouldSkipEdge) break;
            }
        }
        // すでに本体が重なっている場合も端止めオフ
        if (!this.hashiriyacarmod$shouldSkipEdge) {
            Vec3[] boxVertices = aabbToVertices(box);
            for (CarEntity car : CarEntityRegistry.getAllInLevel(self.level())) {
                if (car.isRemoved()) continue;
                for (Vec3[] obbVertices : car.getAllWorldHitboxVertices()) {
                    Vec3 mtv = CarCollisionUtil.computeMTV(obbVertices, boxVertices);
                    if (mtv != null && mtv.lengthSqr() >= EPS) {
                        this.hashiriyacarmod$setShouldSkipEdge(true);
                        break;
                    }
                }
                if (this.hashiriyacarmod$shouldSkipEdge) break;
            }
        }

        // 1回目：0〜45度面だけ
        result = processHits(self, box, result, hasMovement, true);
        box = self.getBoundingBox();

        // 2回目：壁だけ（地面のあと）
        result = processHits(self, box, result, hasMovement, false);

        return result;
    }

    /**
     * @param groundPass true = 0〜45度面だけ / false = 壁だけ
     */
    @Unique
    private Vec3 processHits(Entity self, AABB box, Vec3 result, boolean hasMovement, boolean groundPass) {
        for (CarEntity car : CarEntityRegistry.getAllInLevel(self.level())) {
            if (car.isRemoved()) continue;

            for (Vec3[] obbVertices : car.getAllWorldHitboxVertices()) {

                // ① すでに重なっている
                Vec3 mtvNow = CarCollisionUtil.computeMTV(obbVertices, aabbToVertices(box));
                if (mtvNow != null && mtvNow.lengthSqr() >= EPS) {
                    Vec3 normal = mtvNow.normalize();
                    boolean ground = normal.y >= ON_GROUND_THRESHOLD;

                    if (ground != groundPass) {
                        continue;
                    }

                    if (hasMovement) {
                        result = respondToSurface(result, normal, ground);
                    }
                    applyVelocityResponse(self, normal, ground);

                    self.setPos(
                            self.getX() + mtvNow.x,
                            self.getY() + mtvNow.y,
                            self.getZ() + mtvNow.z
                    );
                    if (ground) {
                        this.hashiriyacarmod$carGroundThisMove = true;
                    }
                    box = self.getBoundingBox();
                    continue;
                }

                if (!hasMovement) continue;

                // ② 移動量分だけ拡大した検知ボックス
                AABB swept = box.expandTowards(result.x, result.y, result.z);
                Vec3 mtvSwept = CarCollisionUtil.computeMTV(obbVertices, aabbToVertices(swept));
                if (mtvSwept == null || mtvSwept.lengthSqr() < EPS) {
                    continue;
                }

                Vec3 normal = mtvSwept.normalize();
                boolean ground = normal.y >= ON_GROUND_THRESHOLD;

                if (ground != groundPass) {
                    continue;
                }

                double toi = findTimeOfImpact(box, result, obbVertices);
                toi = Math.max(0.0, Math.min(1.0, toi));

                Vec3 toContact = result.scale(toi);
                Vec3 remaining = result.subtract(toContact);

                AABB atContact = box.move(toContact.x, toContact.y, toContact.z);
                Vec3 mtvContact = CarCollisionUtil.computeMTV(obbVertices, aabbToVertices(atContact));
                if (mtvContact != null && mtvContact.lengthSqr() >= EPS) {
                    normal = mtvContact.normalize();
                    ground = normal.y >= ON_GROUND_THRESHOLD;
                    if (ground != groundPass) {
                        continue;
                    }
                }

                if (ground) {
                    this.hashiriyacarmod$carGroundThisMove = true;
                }

                remaining = respondToSurface(remaining, normal, ground);
                result = toContact.add(remaining);

                applyVelocityResponse(self, normal, ground);
            }
        }
        return result;
    }

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
    private static double findTimeOfImpact(AABB box, Vec3 movement, Vec3[] obbVertices) {
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