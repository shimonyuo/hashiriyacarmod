package com.hashiriyacarmod;

import com.hashiriyacarmod.cars.CarJsonResult;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

public class AssetRegistry {

    public final String baseName;
    public final File objFile;
    public final File pngFile;

    /** oオブジェクトごとのメッシュ */
    public final Map<String, ObjMesh> parts;
    public final List<String> allowedPartGroups;

    /** 車JSON parts[] の po / ro / group */
    public final List<CarJsonResult.PartPlacement> partPlacements;

    /** meshName -> テクスチャパス。車本体のメッシュごとに個別テクスチャを持てる */
    public final Map<String, String> meshTexturePath;

    // 既存の呼び出しを壊さないための互換用
    public AssetRegistry(String baseName, File objFile, File pngFile,
                         Map<String, ObjMesh> parts,
                         List<String> allowedPartGroups,
                         List<CarJsonResult.PartPlacement> partPlacements) {
        this(baseName, objFile, pngFile, parts, allowedPartGroups, partPlacements, Map.of());
    }

    public AssetRegistry(String baseName, File objFile, File pngFile,
                         Map<String, ObjMesh> parts,
                         List<String> allowedPartGroups,
                         List<CarJsonResult.PartPlacement> partPlacements,
                         Map<String, String> meshTexturePath) {
        this.baseName = baseName;
        this.objFile = objFile;
        this.pngFile = pngFile;
        this.parts = parts != null ? Map.copyOf(parts) : Map.of();
        this.allowedPartGroups = allowedPartGroups != null
                ? new ArrayList<>(allowedPartGroups)
                : new ArrayList<>();
        this.partPlacements = partPlacements != null
                ? new ArrayList<>(partPlacements)
                : new ArrayList<>();
        this.meshTexturePath = meshTexturePath != null ? Map.copyOf(meshTexturePath) : Map.of();
    }
}