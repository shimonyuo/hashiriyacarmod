package com.hashiriyacarmod.parts;

import com.hashiriyacarmod.HashiriyaCarMod;
import com.hashiriyacarmod.ObjMesh;
import com.hashiriyacarmod.ObjLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * type:"parts" のJSONと同名OBJを紐づけて管理するローダーです。
 * エンティティ登録は行いません。
 * JSON要素の解釈は PartJsonParser、保持はここ、という分担です。
 */
public class PartRegistry {

    private static final Logger LOGGER = LogManager.getLogger(HashiriyaCarMod.MOD_ID);

    private static final Map<String, Map<String, ObjMesh>> partMeshMap = new LinkedHashMap<>();
    /** baseName → パーツJSONの group（静的） */
    private static final Map<String, List<String>> partGroupMap = new LinkedHashMap<>();

    private static final Map<String, String> partDisplayNameMap = new LinkedHashMap<>();

    /** 従来どおり（group なし） */
    public static void register(String baseName, File objFile) {
        register(baseName, objFile, List.of());
    }

    /** group 付き登録 */
    public static void register(String baseName, File objFile, List<String> groups) {
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
        partGroupMap.put(baseName, groups != null ? List.copyOf(groups) : List.of());
        LOGGER.info("[PartRegistry] パーツ登録完了: {} ({} メッシュ, groups={})",
                baseName, meshParts.size(), partGroupMap.get(baseName));
    }

    /**
     * JSONファイルも渡せる版。
     * PartJsonParser で group 等を読み、メッシュと一緒に登録する。
     */
    public static void register(String baseName, File objFile, File jsonFile) {
        PartJsonResult parsed = PartJsonParser.parse(jsonFile);
        register(baseName, objFile, parsed.groups);
        // ★ 追加：メニュー用表示名
        String name = parsed.displayName;
        if (name == null || name.isEmpty()) {
            name = baseName;
        }
        partDisplayNameMap.put(baseName, name);
    }

    public static Map<String, ObjMesh> getPartMeshes(String baseName) {
        return partMeshMap.get(baseName);
    }

    public static List<String> getPartGroups(String baseName) {
        List<String> g = partGroupMap.get(baseName);
        return g != null ? g : List.of();
    }

    public static String getDisplayName(String baseName) {
        String n = partDisplayNameMap.get(baseName);
        return n != null ? n : baseName;
    }

    public static List<String> getBaseNamesForGroup(String group) {
        List<String> out = new ArrayList<>();
        if (group == null || group.isEmpty()) return out;
        for (Map.Entry<String, List<String>> e : partGroupMap.entrySet()) {
            if (e.getValue().contains(group)) {
                out.add(e.getKey());
            }
        }
        return out;
    }
    public static boolean matchesCarGroups(String partBaseName, List<String> carAllowedGroups) {
        if (carAllowedGroups == null || carAllowedGroups.isEmpty()) return false;
        List<String> partGroups = getPartGroups(partBaseName);
        if (partGroups.isEmpty()) return false;
        for (String pg : partGroups) {
            if (carAllowedGroups.contains(pg)) return true;
        }
        return false;
    }
}