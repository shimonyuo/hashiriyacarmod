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
    private static final EntityDataAccessor<String> ATTACHED_PARTS =
            SynchedEntityData.defineId(CarEntity.class, EntityDataSerializers.STRING);


    public CarEntity(EntityType<?> type, Level level) {
        super(type, level);
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
        this.entityData.define(ATTACHED_PARTS, "");

        // ★ 新規追加：パーツグループ（クライアントでもすぐ取れるように）
        // 将来的にSynchedEntityDataで同期したい場合はここに追加
    }

    // ★ 新規メソッド追加（任意の場所、例えば getHitboxDefinitions の近く）
    public List<String> getAllowedPartGroups() {
        // 現在はAssetRegistryやCarPackLoaderから取得する想定
        AssetRegistry registry = CarPackLoader.getAssetRegistry(getBaseName());
        // 簡易的にresultから持ってくる実装に後で拡張
        return registry != null ? registry.allowedPartGroups : List.of();   // 後でAssetRegistryにも追加
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

    private AABB computeAabbFromAllObbs() {
        List<Vec3[]> all = getAllWorldHitboxVertices();
        if (all.isEmpty()) {
            double x = this.getX();
            double y = this.getY();
            double z = this.getZ();
            return new AABB(x, y, z, x, y, z);
        }

        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;

        for (Vec3[] verts : all) {
            for (Vec3 v : verts) {
                minX = Math.min(minX, v.x);
                minY = Math.min(minY, v.y);
                minZ = Math.min(minZ, v.z);
                maxX = Math.max(maxX, v.x);
                maxY = Math.max(maxY, v.y);
                maxZ = Math.max(maxZ, v.z);
            }
        }
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /** 姿勢が変わったあとに呼ぶ。本体 AABB を OBB 囲いに合わせる。 */
    public void refreshObbBoundingBox() {
        this.setBoundingBox(computeAabbFromAllObbs());
    }

    /**
     * バニラが位置から箱を作るときの入口。
     * 寸法(width/height)ではなく、常に OBB 囲いを返す。
     *
     * ※ mappings によっては makeBoundingBox(Vec3) の方だけがある。
     *    その場合は下の「Vec3版」を使う。
     */
    @Override
    protected AABB makeBoundingBox() {
        return computeAabbFromAllObbs();
    }

    // Vec3 版がある環境用（どちらか一方でコンパイルが通る方を残す）
    // @Override
    // protected AABB makeBoundingBox(Vec3 pos) {
    //     // pos 基準で OBB を再計算したい場合は getAllWorldHitboxVertices を pos 対応にする
    //     return computeAabbFromAllObbs();
    // }

    @Override
    public void tick() {
        super.tick();
        // 毎tick：yaw / pitch / roll 反映後の OBB で本体 AABB を更新
        refreshObbBoundingBox();
    }

    @Override
    public void setYRot(float yRot) {
        super.setYRot(yRot);
        if (this.level() != null) {
            refreshObbBoundingBox();
        }
    }

    public float getCarPitch() {
        return this.entityData.get(CAR_PITCH);
    }

    public void setCarPitch(float pitch) {
        this.entityData.set(CAR_PITCH, pitch);
        if (this.level() != null) {
            refreshObbBoundingBox();
        }
    }

    public float getCarRoll() {
        return this.entityData.get(CAR_ROLL);
    }

    public void setCarRoll(float roll) {
        this.entityData.set(CAR_ROLL, roll);
        if (this.level() != null) {
            refreshObbBoundingBox();
        }
    }

    public List<String> getAttachedParts() {
        String raw = this.entityData.get(ATTACHED_PARTS);
        if (raw == null || raw.isEmpty()) return List.of();
        return List.of(raw.split(","));
    }

    public void setAttachedParts(List<String> parts) {
        List<String> filtered = filterAttachedPartsForThisCar(parts);
        String joined = filtered.isEmpty() ? "" : String.join(",", filtered);
        this.entityData.set(ATTACHED_PARTS, joined);
        if (this.level() != null && this.level().isClientSide()) {
            invalidateRenderCache();
        }
    }

    /**
     * パーツの group が、この車の allowedPartGroups に含まれるものだけ残す。
     * NBT /summon で書かれても、合わない名前は同期データに載せない。
     */
    public List<String> filterAttachedPartsForThisCar(List<String> parts) {
        if (parts == null || parts.isEmpty()) return List.of();
        List<String> allowed = getAllowedPartGroups();
        List<String> out = new java.util.ArrayList<>();
        for (String name : parts) {
            if (name == null || name.isBlank()) continue;
            String trimmed = name.trim();
            if (com.hashiriyacarmod.parts.PartRegistry.matchesCarGroups(trimmed, allowed)) {
                out.add(trimmed);
            }
        }
        return out;
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

    @Override
    public void onAddedToWorld() {
        super.onAddedToWorld();
        CarEntityRegistry.register(this);
    }

    @Override
    public void onRemovedFromWorld() {
        super.onRemovedFromWorld();
        CarEntityRegistry.unregister(this);
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

        // carsの場合（従来通り）
        if (registry != null && !registry.parts.isEmpty()) {
            clientData.parts = registry.parts;
            if (registry.pngFile != null) {
                clientData.textureLocation = CarTextureManager.getOrLoad(baseName, registry.pngFile);
            }
            clientData.resolved = true;
            return true;
        }

        // partsの場合：親のテクスチャを確保（送り付け）
        clientData.parts = getPartMeshes();  // PartRegistryから取得
        if (clientData.parts.isEmpty()) {
            clientData.resolved = true;
            return false;
        }

        // 親のテクスチャを送り付ける（最重要変更点）
        clientData.textureLocation = getCachedTextureLocationFromParent();

        clientData.resolved = true;
        return true;
    }

    @OnlyIn(Dist.CLIENT)
    public ResourceLocation getCachedTextureLocationFromParent() {
        // まず通常のキャッシュを確認
        if (clientData != null && clientData.textureLocation != null) {
            return clientData.textureLocation;
        }

        // AssetRegistryから取得を試みる（carsのフォールバック）
        AssetRegistry registry = CarPackLoader.getAssetRegistry(getBaseName());
        if (registry != null && registry.pngFile != null) {
            return CarTextureManager.getOrLoad(getBaseName(), registry.pngFile);
        }

        // 将来：parts個別テクスチャ拒否ロジックをここに追加可能
        return null;
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
        if (tag.contains("AttachedParts", CompoundTag.TAG_LIST)) {
            List<String> list = new java.util.ArrayList<>();
            var nbtList = tag.getList("AttachedParts", CompoundTag.TAG_STRING);
            for (int i = 0; i < nbtList.size(); i++) {
                list.add(nbtList.getString(i));
            }
            setAttachedParts(list);
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putString("BaseName", getBaseName());
        tag.putFloat("CarPitch", getCarPitch());
        tag.putFloat("CarRoll", getCarRoll());
        var list = new net.minecraft.nbt.ListTag();
        for (String p : getAttachedParts()) {
            list.add(net.minecraft.nbt.StringTag.valueOf(p));
        }
        tag.put("AttachedParts", list);
    }

    public CompoundTag getSaveData() {
        CompoundTag tag = new CompoundTag();
        this.addAdditionalSaveData(tag);   // protectedメソッドは同じクラス内なので呼べる
        return tag;
    }
}