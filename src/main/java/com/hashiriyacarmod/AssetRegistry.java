package com.hashiriyacarmod;

import java.io.File;
import java.util.Map;

public class AssetRegistry {

    public final String baseName;
    public final File objFile;
    public final File pngFile;

    /** oオブジェクトごとのメッシュ */
    public final Map<String, ObjMesh> parts;

    public AssetRegistry(String baseName, File objFile, File pngFile, Map<String, ObjMesh> parts) {
        this.baseName = baseName;
        this.objFile = objFile;
        this.pngFile = pngFile;
        this.parts = parts != null ? Map.copyOf(parts) : Map.of();

    }
}