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

    public AssetRegistry(String baseName, File objFile, File pngFile,
                         Map<String, ObjMesh> parts,
                         List<String> allowedPartGroups,
                         List<CarJsonResult.PartPlacement> partPlacements) {
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
    }
}