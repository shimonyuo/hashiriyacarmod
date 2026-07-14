package com.hashiriyacarmod;

import com.hashiriyacarmod.parts.PartRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import java.util.List;
import java.util.Map;

public class CarEntity extends Entity {

    private static final EntityDataAccessor<String> BASE_NAME =
            SynchedEntityData.defineId(CarEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float> CAR_PITCH =
            SynchedEntityData.defineId(CarEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> CAR_ROLL =
            SynchedEntityData.defineId(CarEntity.class, EntityDataSerializers.FLOAT);

    private static final double MAX_PUSH_PER_TICK = 0.5;

    public CarEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noCulling = true;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(BASE_NAME, "");
        this.entityData.define(CAR_PITCH, 0.0f);
        this.entityData.define(CAR_ROLL, 0.0f);
    }

    public String getBaseName() {
        return this.entityData.get(BASE_NAME);
    }

    public void setBaseName(String name) {
        this.entityData.set(BASE_NAME, name);
        if (this.level() != null && this.level().isClientSide()) {
            invalidateRenderCache();
        }
    }

    public float getCarPitch() {
        return this.entityData.get(CAR_PITCH);
    }

    public void setCarPitch(float pitch) {
        this.entityData.set(CAR_PITCH, pitch);
    }

    public float getCarRoll() {
        return this.entityData.get(CAR_ROLL);
    }

    public void setCarRoll(float roll) {
        this.entityData.set(CAR_ROLL, roll);
    }

    @OnlyIn(Dist.CLIENT)
    public Map<String, ObjMesh> getPartMeshes() {
        return PartRegistry.getPartMeshes(getBaseName());
    }

    // ==================== 検知ボックス（hitbox）関連 ====================

    public List<HitboxDefinition> getHitboxDefinitions() {
        return CarPackLoader.getHitboxDefinitions(getBaseName());
    }

    /**
     * すべての箱を、ワールド座標の8頂点の配列（複数個）として返します。
     * 定義が1つも無ければ、空のリストです。
     */
    public List<Vec3[]> getAllWorldHitboxVertices() {
        return getAllWorldHitboxVertices(this.getYRot());
    }

    public List<Vec3[]> getAllWorldHitboxVertices(float yawDegrees) {
        List<HitboxDefinition> defs = getHitboxDefinitions();
        List<Vec3[]> result = new java.util.ArrayList<>(defs.size());
        for (HitboxDefinition def : defs) {
            result.add(def.toWorldVertices(getX(), getY(), getZ(), yawDegrees, getCarPitch(), getCarRoll()));
        }
        return result;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    // ==================== 押し出しのみ（サーバー側のみ） ====================

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) return;

        List<Vec3[]> myBoxesList = getAllWorldHitboxVertices();
        if (myBoxesList.isEmpty()) return;

        for (Vec3[] myVertices : myBoxesList) {
            pushOutOverlaps(myVertices);
        }
    }

    private void pushOutOverlaps(Vec3[] myVertices) {
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
        for (Vec3 v : myVertices) {
            minX = Math.min(minX, v.x); maxX = Math.max(maxX, v.x);
            minY = Math.min(minY, v.y); maxY = Math.max(maxY, v.y);
            minZ = Math.min(minZ, v.z); maxZ = Math.max(maxZ, v.z);
        }
        AABB broadPhase = new AABB(minX, minY, minZ, maxX, maxY, maxZ);

        for (Entity other : this.level().getEntities(this, broadPhase, e -> true)) {
            if (other instanceof CarEntity otherCar) {
                for (Vec3[] otherVertices : otherCar.getAllWorldHitboxVertices()) {
                    Vec3 mtv = CarCollisionUtil.computeMTV(myVertices, otherVertices);
                    if (mtv != null) {
                        pushEntity(otherCar, clampPush(mtv.scale(0.5)));
                        pushEntity(this, clampPush(mtv.scale(-0.5)));
                    }
                }
            } else {
                Vec3 mtv = CarCollisionUtil.computeMTV(myVertices, other.getBoundingBox());
                if (mtv != null) {
                    pushEntity(other, clampPush(mtv));
                }
            }
        }
    }
    private Vec3 clampPush(Vec3 push) {
        double length = push.length();
        if (length > MAX_PUSH_PER_TICK) {
            return push.scale(MAX_PUSH_PER_TICK / length);
        }
        return push;
    }

    private void pushEntity(Entity target, Vec3 push) {
        if (push.lengthSqr() < 1.0E-9) return;
        target.setPos(target.getX() + push.x, target.getY() + push.y, target.getZ() + push.z);
        Vec3 currentMotion = target.getDeltaMovement();
        target.setDeltaMovement(currentMotion.add(push.scale(0.3)));
        target.hurtMarked = true;
    }

    // ==================== クライアント側専用の「引き出し」 ====================

    @OnlyIn(Dist.CLIENT)
    private ClientRenderData clientData;

    @OnlyIn(Dist.CLIENT)
    private static class ClientRenderData {
        Map<String, ObjMesh> parts = Map.of();
        ResourceLocation textureLocation = null;
        boolean resolved = false;

        // パーツごとの標準VBOキャッシュ。影mod互換のため本物のVertexBufferを使用。
        Map<String, StandardVboCache> partBuffers = new java.util.HashMap<>();

        // 各パーツの「直前に送った明るさ」。これと違う時だけ明るさを更新します。
        Map<String, Integer> lastLight = new java.util.HashMap<>();

        // 各パーツの「直前に送った色」。これと違う時だけ色を更新します。
        Map<String, int[]> lastColor = new java.util.HashMap<>();
    }
    @OnlyIn(Dist.CLIENT)
    public boolean resolveRenderCache() {
        if (clientData == null) {
            clientData = new ClientRenderData();
        }
        if (clientData.resolved) return !clientData.parts.isEmpty();

        String baseName = getBaseName();
        if (baseName == null || baseName.isEmpty()) return false;

        AssetRegistry registry = CarPackLoader.getAssetRegistry(baseName);
        if (registry == null || registry.parts.isEmpty()) {
            clientData.resolved = true;
            return false;
        }

        clientData.parts = registry.parts;
        if (registry.pngFile != null) {
            clientData.textureLocation = CarTextureManager.getOrLoad(baseName, registry.pngFile);
        }
        clientData.resolved = true;
        return true;
    }

    @OnlyIn(Dist.CLIENT)
    public Map<String, ObjMesh> getCachedParts() {
        return clientData != null ? clientData.parts : Map.of();
    }

    @OnlyIn(Dist.CLIENT)
    public StandardVboCache getOrCreatePartBuffer(String partName, ObjMesh mesh, int initialPackedLight) {
        if (clientData == null) return null;

        StandardVboCache existing = clientData.partBuffers.get(partName);
        if (existing != null) return existing;

        existing = new StandardVboCache(mesh, initialPackedLight);
        clientData.partBuffers.put(partName, existing);
        clientData.lastLight.put(partName, initialPackedLight);
        clientData.lastColor.put(partName, new int[]{255, 255, 255, 255});
        return existing;
    }

    @OnlyIn(Dist.CLIENT)
    public void updatePartLightIfChanged(String partName, StandardVboCache buffer, int packedLight) {
        if (clientData == null) return;
        Integer prev = clientData.lastLight.get(partName);
        if (prev != null && prev == packedLight) return;

        buffer.updateLight(packedLight);
        clientData.lastLight.put(partName, packedLight);
    }
    @OnlyIn(Dist.CLIENT)
    public void updatePartColorIfChanged(String partName, StandardVboCache buffer, int r, int g, int b, int a) {
        if (clientData == null) return;
        int[] prev = clientData.lastColor.get(partName);
        if (prev != null && prev[0] == r && prev[1] == g && prev[2] == b && prev[3] == a) return;

        buffer.updateColor(r, g, b, a);
        clientData.lastColor.put(partName, new int[]{r, g, b, a});
    }
    @OnlyIn(Dist.CLIENT)
    public ResourceLocation getCachedTextureLocation() {
        return clientData != null ? clientData.textureLocation : null;
    }
    @OnlyIn(Dist.CLIENT)
    private void invalidateRenderCache() {
        if (clientData != null) {
            for (StandardVboCache buffer : clientData.partBuffers.values()) {
                buffer.close();
            }
        }
        clientData = null;
    }

    // ==================== データ保存 ====================

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("BaseName")) {
            setBaseName(tag.getString("BaseName"));
        }
        if (tag.contains("CarPitch")) {
            setCarPitch(tag.getFloat("CarPitch"));
        }
        if (tag.contains("CarRoll")) {
            setCarRoll(tag.getFloat("CarRoll"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putString("BaseName", getBaseName());
        tag.putFloat("CarPitch", getCarPitch());
        tag.putFloat("CarRoll", getCarRoll());
    }
}