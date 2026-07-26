package com.hashiriyacarmod.mixin;

import com.hashiriyacarmod.CarCollisionUtil;
import com.hashiriyacarmod.CarEntity;
import com.hashiriyacarmod.CarEntityRegistry;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Entity.class)
public abstract class CarHitboxSupportMixin {

    @ModifyVariable(
            method = "setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V",
            at = @At("HEAD"),
            argsOnly = true
    )
    private Vec3 slideAlongObbSurface(Vec3 movement) {
        Entity self = (Entity) (Object) this;

        if (self instanceof CarEntity) {
            return movement;
        }

        if (movement.lengthSqr() < 1.0E-10) {
            return movement;
        }

        AABB box = self.getBoundingBox();
        Vec3[] playerVertices = aabbToVertices(box);

        Vec3 result = movement;
        boolean shouldBeOnGround = false;

        final double ON_GROUND_NORMAL_Y_THRESHOLD = Math.cos(Math.toRadians(45.0));

        for (CarEntity car : CarEntityRegistry.getAllInLevel(self.level())) {
            if (car.isRemoved()) continue;

            for (Vec3[] obbVertices : car.getAllWorldHitboxVertices()) {
                Vec3 mtv = CarCollisionUtil.computeMTV(obbVertices, playerVertices);
                if (mtv == null || mtv.lengthSqr() < 1.0E-10) continue;

                Vec3 normal = mtv.normalize();

                if (normal.y >= ON_GROUND_NORMAL_Y_THRESHOLD) {
                    shouldBeOnGround = true;
                }

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