package com.hashiriyacarmod.parts;

import com.hashiriyacarmod.HashiriyaCarMod;
import com.hashiriyacarmod.ObjMesh;
import com.hashiriyacarmod.ObjLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * type:"parts" のJSONと同名OBJを紐づけて管理するローダーです。
 * エンティティ登録は行いません。
 * CarPackLoaderがJSONを発見した際に、このクラスのregister()を呼び出します。
 */
public class PartRegistry {

    private static final Logger LOGGER = LogManager.getLogger(HashiriyaCarMod.MOD_ID);

    // baseName → ObjMesh の対応を保持します
    private static final Map<String, Map<String, ObjMesh>> partMeshMap = new LinkedHashMap<>();

    /**
     * CarPackLoaderから呼ばれます。
     * objFileが見つかっていればOBJを読み込み、ログを出します。
     */
    public static void register(String baseName, File objFile) {
        if (objFile == null || !objFile.exists()) {
            LOGGER.warn("[PartRegistry] OBJが見つかりません: {}", baseName);
            return;
        }

        Map<String, ObjMesh> meshParts = ObjLoader.loadWithParts(objFile);
        if (meshParts.isEmpty()) {
            LOGGER.warn("[PartRegistry] OBJのパーツが空です: {}", baseName);
            return;
        }

        partMeshMap.put(baseName, meshParts);
        LOGGER.info("[PartRegistry] パーツ登録完了: {} ({} パーツ, OBJ: {})",
                baseName, meshParts.size(), objFile.getName());
    }

    /**
     * baseNameに対応するパーツのOBJメッシュを返します。
     * 登録されていない場合はnullです。
     */
    public static Map<String, ObjMesh> getPartMeshes(String baseName) {
        return partMeshMap.get(baseName);
    }
}