package com.hashiriyacarmod;

import com.hashiriyacarmod.cars.CarJsonResult;
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
    /** 装着ツリー（/summon の AttachedParts と同型） */
    private CompoundTag attachedPartsTree = new CompoundTag();

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

    public static String[] splitAttachedToken(String token) {
        if (token == null) return new String[]{"", ""};
        String t = token.trim();
        int i = t.indexOf('>');
        if (i < 0) return new String[]{t, ""};
        return new String[]{t.substring(0, i).trim(), t.substring(i + 1).trim()};
    }

    public static String attachedBaseName(String token) {
        return splitAttachedToken(token)[0];
    }

    public static String attachedSeatGroup(String token) {
        return splitAttachedToken(token)[1];
    }

    public List<String> getAttachedParts() {
        return new java.util.ArrayList<>(attachedPartsTree.getAllKeys());
    }

    public CompoundTag getAttachedPartsTree() {
        return attachedPartsTree;
    }

    public void setAttachedParts(List<String> parts) {
        CompoundTag tree = new CompoundTag();
        for (String t : filterAttachedPartsForThisCar(parts)) {
            tree.put(t, new CompoundTag());
        }
        applyAttachedTree(tree);
    }

    private void applyAttachedTree(CompoundTag tree) {
        this.attachedPartsTree = tree != null ? tree.copy() : new CompoundTag();
        String sync = this.attachedPartsTree.isEmpty()
                ? ""
                : this.attachedPartsTree.toString();
        this.entityData.set(ATTACHED_PARTS, sync);
    }

    private static CompoundTag parseAttachedTreeSnbt(String raw) {
        if (raw == null || raw.isBlank()) return new CompoundTag();
        try {
            net.minecraft.nbt.Tag tag = net.minecraft.nbt.TagParser.parseTag(raw);
            if (tag instanceof CompoundTag c) return c;
        } catch (Exception ignored) {}
        CompoundTag flat = new CompoundTag();
        for (String p : raw.split(",")) {
            if (p != null && !p.isBlank()) flat.put(p.trim(), new CompoundTag());
        }
        return flat;
    }

    public CompoundTag filterAttachedTree(CompoundTag node, String parentPartBase) {
        CompoundTag out = new CompoundTag();
        if (node == null || node.isEmpty()) return out;

        List<String> allowed = (parentPartBase == null || parentPartBase.isEmpty())
                ? getAllowedPartGroups()
                : PartRegistry.getAllowedChildGroups(parentPartBase);

        for (String token : node.getAllKeys()) {
            if (token == null || token.isBlank()) continue;
            String t = token.trim();
            String base = attachedBaseName(t);
            String seat = attachedSeatGroup(t);
            if (base.isEmpty()) continue;

            List<String> partGroups = PartRegistry.getPartGroups(base);
            if (partGroups.isEmpty()) continue;

            boolean ok;
            if (!seat.isEmpty()) {
                ok = partGroups.contains(seat) && allowed.contains(seat);
            } else {
                ok = false;
                for (String g : partGroups) {
                    if (allowed.contains(g)) { ok = true; break; }
                }
            }
            if (!ok) continue;

            out.put(t, filterAttachedTree(node.getCompound(t), base));
        }
        return out;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (ATTACHED_PARTS.equals(key)) {
            this.attachedPartsTree = parseAttachedTreeSnbt(this.entityData.get(ATTACHED_PARTS));
            if (this.level() != null && this.level().isClientSide()) {
                invalidateAttachedPartBuffersOnly();
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("BaseName")) {
            setBaseName(tag.getString("BaseName"));
        }
        if (tag.contains("CarPitch")) setCarPitch(tag.getFloat("CarPitch"));
        if (tag.contains("CarRoll")) setCarRoll(tag.getFloat("CarRoll"));

        if (tag.contains("AttachedParts", CompoundTag.TAG_COMPOUND)) {
            applyAttachedTree(filterAttachedTree(tag.getCompound("AttachedParts"), null));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putString("BaseName", getBaseName());
        tag.putFloat("CarPitch", getCarPitch());
        tag.putFloat("CarRoll", getCarRoll());
        tag.put("AttachedParts", attachedPartsTree.copy());
    }

    public void setAttachedPartForGroup(String groupName, String partBaseName) {
        setAttachedPartInTree(List.of(), groupName, partBaseName);
    }

    /**
     * pathTokens = 親ノードまでのキー列（空なら AttachedParts 直下）
     * その親の下で groupName に属する装着を付け替える。
     */
    public void setAttachedPartInTree(List<String> pathTokens, String groupName, String partBaseName) {
        if (groupName == null || groupName.isBlank()) return;

        CompoundTag root = this.attachedPartsTree.copy();
        CompoundTag parent = root;

        if (pathTokens != null) {
            for (String pathToken : pathTokens) {
                if (pathToken == null || pathToken.isBlank()) continue;
                if (!parent.contains(pathToken, CompoundTag.TAG_COMPOUND)) {
                    parent.put(pathToken, new CompoundTag());
                }
                parent = parent.getCompound(pathToken);
            }
        }

        // この seat の既存トークンを削除（子ツリーごと消える）
        java.util.List<String> remove = new java.util.ArrayList<>();
        for (String token : parent.getAllKeys()) {
            if (tokenBelongsToSeat(token, groupName)) {
                remove.add(token);
            }
        }
        for (String t : remove) {
            parent.remove(t);
        }

        if (partBaseName != null && !partBaseName.isBlank()
                && !"NONE".equalsIgnoreCase(partBaseName)) {
            String base = partBaseName.trim();
            List<String> partGroups = PartRegistry.getPartGroups(base);
            String token = (partGroups.size() <= 1)
                    ? base
                    : base + ">" + groupName.trim();
            parent.put(token, new CompoundTag());
        }
        applyAttachedTree(filterAttachedTree(root, null));
    }

    private static boolean tokenBelongsToSeat(String token, String groupName) {
        String seat = attachedSeatGroup(token);
        if (!seat.isEmpty()) {
            return seat.equals(groupName);
        }
        return PartRegistry.getPartGroups(attachedBaseName(token)).contains(groupName);
    }

    /** 車本体VBOは残し、装着パーツのバッファだけ捨てる（OBJ再読込なし） */
    @OnlyIn(Dist.CLIENT)
    private void invalidateAttachedPartBuffersOnly() {
        if (clientData == null) return;
        java.util.Iterator<Map.Entry<String, StandardVboCache>> it =
                clientData.partBuffers.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, StandardVboCache> e = it.next();
            // 装着パーツは "baseName/objPart" 形式でキーにしている
            if (e.getKey().contains("/")) {
                e.getValue().close();
                it.remove();
                clientData.lastLight.remove(e.getKey());
                clientData.lastColor.remove(e.getKey());
            }
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

        for (String raw : parts) {
            if (raw == null || raw.isBlank()) continue;
            String token = raw.trim();
            String base = attachedBaseName(token);
            String seat = attachedSeatGroup(token);
            if (base.isEmpty()) continue;

            List<String> partGroups = PartRegistry.getPartGroups(base);
            if (partGroups.isEmpty()) continue;

            if (!seat.isEmpty()) {
                // base>group : パーツがその group を持ち、車も許可している
                if (partGroups.contains(seat) && allowed.contains(seat)) {
                    out.add(base + ">" + seat);
                }
            } else {
                // 旧形式 base のみ
                if (PartRegistry.matchesCarGroups(base, allowed)) {
                    out.add(base);
                }
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

    @OnlyIn(Dist.CLIENT)
    public void reloadObjDrawing() {
        invalidateRenderCache();
    }



    public CompoundTag getSaveData() {
        CompoundTag tag = new CompoundTag();
        this.addAdditionalSaveData(tag);   // protectedメソッドは同じクラス内なので呼べる
        return tag;
    }

    public Vec3 getPartOffset(String attachedToken) {
        String base = attachedBaseName(attachedToken);
        String seat = attachedSeatGroup(attachedToken);

        AssetRegistry registry = CarPackLoader.getAssetRegistry(getBaseName());
        if (registry == null || registry.partPlacements == null) return Vec3.ZERO;

        if (!seat.isEmpty()) {
            for (CarJsonResult.PartPlacement p : registry.partPlacements) {
                if (p.groups.contains(seat)) {
                    return p.position;
                }
            }
            return Vec3.ZERO;
        }

        List<String> partGroups = PartRegistry.getPartGroups(base);
        for (CarJsonResult.PartPlacement p : registry.partPlacements) {
            for (String g : partGroups) {
                if (p.groups.contains(g)) {
                    return p.position;
                }
            }
        }
        return Vec3.ZERO;
    }

    /** 車の parts[] から ro（ルート装着） */
    public float[] getPartRotation(String attachedToken) {
        String base = attachedBaseName(attachedToken);
        String seat = attachedSeatGroup(attachedToken);

        AssetRegistry registry = CarPackLoader.getAssetRegistry(getBaseName());
        if (registry == null || registry.partPlacements == null) return new float[]{0, 0, 0};

        if (!seat.isEmpty()) {
            for (CarJsonResult.PartPlacement p : registry.partPlacements) {
                if (p.groups.contains(seat)) {
                    return new float[]{p.rotX, p.rotY, p.rotZ};
                }
            }
            return new float[]{0, 0, 0};
        }

        List<String> partGroups = PartRegistry.getPartGroups(base);
        for (CarJsonResult.PartPlacement p : registry.partPlacements) {
            for (String g : partGroups) {
                if (p.groups.contains(g)) {
                    return new float[]{p.rotX, p.rotY, p.rotZ};
                }
            }
        }
        return new float[]{0, 0, 0};
    }

    public Vec3 getPartOffset(String parentPartBase, String attachedToken) {
        if (parentPartBase == null || parentPartBase.isEmpty()) {
            return getPartOffset(attachedToken);
        }
        String base = attachedBaseName(attachedToken);
        String seat = attachedSeatGroup(attachedToken);
        if (!seat.isEmpty()) {
            CarJsonResult.PartPlacement p = PartRegistry.findPlacement(parentPartBase, seat);
            return p != null ? p.position : Vec3.ZERO;
        }
        for (String g : PartRegistry.getPartGroups(base)) {
            CarJsonResult.PartPlacement p = PartRegistry.findPlacement(parentPartBase, g);
            if (p != null) return p.position;
        }
        return Vec3.ZERO;
    }

    public float[] getPartRotation(String parentPartBase, String attachedToken) {
        if (parentPartBase == null || parentPartBase.isEmpty()) {
            return getPartRotation(attachedToken);
        }
        String base = attachedBaseName(attachedToken);
        String seat = attachedSeatGroup(attachedToken);
        if (!seat.isEmpty()) {
            CarJsonResult.PartPlacement p = PartRegistry.findPlacement(parentPartBase, seat);
            if (p != null) return new float[]{p.rotX, p.rotY, p.rotZ};
            return new float[]{0, 0, 0};
        }
        for (String g : PartRegistry.getPartGroups(base)) {
            CarJsonResult.PartPlacement p = PartRegistry.findPlacement(parentPartBase, g);
            if (p != null) return new float[]{p.rotX, p.rotY, p.rotZ};
        }
        return new float[]{0, 0, 0};
    }
}