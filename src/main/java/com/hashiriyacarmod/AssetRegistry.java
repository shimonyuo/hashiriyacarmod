package com.hashiriyacarmod;

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

    public AssetRegistry(String baseName, File objFile, File pngFile, Map<String, ObjMesh> parts, List<String> allowedPartGroups) {
        this.baseName = baseName;
        this.objFile = objFile;
        this.pngFile = pngFile;
        this.parts = parts != null ? Map.copyOf(parts) : Map.of();
        this.allowedPartGroups = allowedPartGroups != null ? new ArrayList<>(allowedPartGroups) : new ArrayList<>();
    }
}