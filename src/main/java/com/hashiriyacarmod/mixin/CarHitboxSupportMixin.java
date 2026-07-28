package com.hashiriyacarmod.mixin;

import com.hashiriyacarmod.CarCollisionUtil;
import com.hashiriyacarmod.CarEntity;
import com.hashiriyacarmod.CarEntityRegistry;
import com.hashiriyacarmod.CarGroundNormalHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

@Mixin(Entity.class)
public abstract class CarHitboxSupportMixin implements CarGroundNormalHolder {

    private static final double ON_GROUND_THRESHOLD = Math.cos(Math.toRadians(45.0));

    @Unique
    @Nullable
    private Vec3 hashiriyacarmod$groundNormal = null;

    @Unique
    private boolean hashiriyacarmod$shouldSkipEdge = false;

    @Override
    public Vec3 hashiriyacarmod$getGroundNormal() {
        return this.hashiriyacarmod$groundNormal;
    }

    @Override
    public void hashiriyacarmod$setGroundNormal(@Nullable Vec3 normal) {
        this.hashiriyacarmod$groundNormal = normal;
    }

    @Override
    public boolean hashiriyacarmod$shouldSkipEdge() {
        return this.hashiriyacarmod$shouldSkipEdge;
    }

    @Override
    public void hashiriyacarmod$setShouldSkipEdge(boolean skip) {
        this.hashiriyacarmod$shouldSkipEdge = skip;
    }

    /**
     * バニラ掃引（collide）への条件追加。
     * getBoundingBox().expandTowards(movement) はバニラ本体と同じ伸ばし方です。
     */
    @Inject(
            method = "collide",
            at = @At("HEAD")
    )
    private void hashiriyacarmod$onVanillaSweep(Vec3 movement, CallbackInfoReturnable<Vec3> cir) {
        Entity self = (Entity) (Object) this;
        this.hashiriyacarmod$setShouldSkipEdge(false);

        if (self instanceof CarEntity) return;
        if (movement.lengthSqr() < 1.0E-10) return;

        // バニラ collide 内と同じ：移動方向に AABB を伸ばす
        AABB sweptBox = self.getBoundingBox().expandTowards(movement.x, movement.y, movement.z);
        Vec3[] sweptVertices = aabbToVertices(sweptBox);

        for (CarEntity car : CarEntityRegistry.getAllInLevel(self.level())) {
            if (car.isRemoved()) continue;
            for (Vec3[] obbVertices : car.getAllWorldHitboxVertices()) {
                Vec3 mtv = CarCollisionUtil.computeMTV(obbVertices, sweptVertices);
                if (mtv != null && mtv.lengthSqr() >= 1.0E-10) {
                    this.hashiriyacarmod$setShouldSkipEdge(true);
                    return;
                }
            }
        }
    }

    @ModifyVariable(
            method = "setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V",
            at = @At("HEAD"),
            argsOnly = true
    )
    private Vec3 slideAlongObbSurface(Vec3 movement) {
        Entity self = (Entity) (Object) this;

        if (self instanceof CarEntity) return movement;
        if (movement.lengthSqr() < 1.0E-10) return movement;

        AABB box = self.getBoundingBox();
        Vec3[] playerVertices = aabbToVertices(box);

        Vec3 result = movement;
        boolean shouldBeOnGround = false;

        java.util.List<Vec3> groundNormals = new java.util.ArrayList<>();
        java.util.List<Double> groundMtvLengths = new java.util.ArrayList<>();
        java.util.List<Vec3> wallNormals = new java.util.ArrayList<>();
        java.util.List<Vec3> wallMtvs = new java.util.ArrayList<>();

        for (CarEntity car : CarEntityRegistry.getAllInLevel(self.level())) {
            if (car.isRemoved()) continue;

            for (Vec3[] obbVertices : car.getAllWorldHitboxVertices()) {
                Vec3 mtv = CarCollisionUtil.computeMTV(obbVertices, playerVertices);
                if (mtv == null || mtv.lengthSqr() < 1.0E-10) continue;

                Vec3 normal = mtv.normalize();
                if (normal.y >= ON_GROUND_THRESHOLD) {
                    groundNormals.add(normal);
                    groundMtvLengths.add(mtv.length());
                    shouldBeOnGround = true;
                } else {
                    wallNormals.add(normal);
                    wallMtvs.add(mtv);
                }
            }
        }

        if (shouldBeOnGround) {
            for (int i = 0; i < groundNormals.size(); i++) {
                Vec3 normal = groundNormals.get(i);

                double dot = result.dot(normal);
                Vec3 slideResult = dot < 0
                        ? result.subtract(normal.scale(dot))
                        : result;

                if (result.y < 0) {
                    Vec3 gravityOnly = new Vec3(0, result.y, 0);
                    double gravDot = gravityOnly.dot(normal);
                    Vec3 gravParallel = gravityOnly.subtract(normal.scale(gravDot));
                    Vec3 gravCancel = gravParallel.scale(-1.0);
                    slideResult = slideResult.add(gravCancel);
                }

                result = slideResult;
            }

            double maxMtvLength = groundMtvLengths.stream()
                    .mapToDouble(Double::doubleValue).max().orElse(0);
            self.setPos(self.getX(), self.getY() + maxMtvLength, self.getZ());

        } else {
            for (int i = 0; i < wallNormals.size(); i++) {
                Vec3 normal = wallNormals.get(i);
                Vec3 mtv = wallMtvs.get(i);

                double dot = result.dot(normal);
                if (dot < 0) {
                    result = result.subtract(normal.scale(dot));
                }

                self.setPos(
                        self.getX() + mtv.x,
                        self.getY() + mtv.y,
                        self.getZ() + mtv.z
                );
            }
        }

        if (shouldBeOnGround) {
            self.setOnGround(true);
            this.hashiriyacarmod$setGroundNormal(groundNormals.get(0));
        } else {
            this.hashiriyacarmod$setGroundNormal(null);
        }

        return result;
    }

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