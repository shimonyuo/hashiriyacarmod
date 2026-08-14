package com.hashiriyacarmod.parts;

import com.hashiriyacarmod.HashiriyaCarMod;
import com.hashiriyacarmod.ObjMesh;
import com.hashiriyacarmod.ObjLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.Collections;
import java.util.Set;
import java.io.File;
import java.util.*;

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
    /** baseName → 全体テクスチャパス（空 = 未指定） */
    private static final Map<String, String> partTexturePathMap = new LinkedHashMap<>();

    /** baseName → このパーツの parts[]（子用 po/ro/group） */
    private static final Map<String, List<com.hashiriyacarmod.cars.CarJsonResult.PartPlacement>> partPlacementMap =
            new LinkedHashMap<>();
    /** baseName → 子に付けられる group 名 */
    private static final Map<String, List<String>> partAllowedChildGroupMap =
            new LinkedHashMap<>();

    /** 従来どおり（group なし） */
    public static void register(String baseName, File objFile) {
        register(baseName, objFile, List.of(), "");
    }

    /** group 付き登録（テクスチャパスなし） */
    public static void register(String baseName, File objFile, List<String> groups) {
        register(baseName, objFile, groups, "");
    }

    /** group + 全体テクスチャパス付き登録 */
    public static void register(String baseName, File objFile, List<String> groups, String texturePath) {
        register(baseName, objFile, groups, texturePath, List.of(), List.of());
    }

    /** group + テクスチャ + 子用 parts[] */
    public static void register(String baseName, File objFile, List<String> groups, String texturePath,
                                List<com.hashiriyacarmod.cars.CarJsonResult.PartPlacement> placements,
                                List<String> allowedChildGroups) {
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
        partTexturePathMap.put(baseName, texturePath != null ? texturePath.trim() : "");
        partPlacementMap.put(baseName,
                placements != null ? List.copyOf(placements) : List.of());
        partAllowedChildGroupMap.put(baseName,
                allowedChildGroups != null ? List.copyOf(allowedChildGroups) : List.of());

        LOGGER.info("[PartRegistry] パーツ登録完了: {} ({} メッシュ, groups={}, childSeats={}, texture_path={})",
                baseName, meshParts.size(), partGroupMap.get(baseName),
                partAllowedChildGroupMap.get(baseName),
                partTexturePathMap.get(baseName));
    }

    public static void register(String baseName, File objFile, File jsonFile) {
        PartJsonResult parsed = PartJsonParser.parse(jsonFile);
        register(baseName, objFile, parsed.groups, parsed.texturePath,
                parsed.partPlacements, parsed.allowedChildGroups);
        String name = parsed.displayName;
        if (name == null || name.isEmpty()) {
            name = baseName;
        }
        partDisplayNameMap.put(baseName, name);
    }

    /** パーツ全体用テクスチャパス。未指定なら空文字 */
    public static String getTexturePath(String baseName) {
        String p = partTexturePathMap.get(baseName);
        return p != null ? p : "";
    }

    public static Map<String, ObjMesh> getPartMeshes(String baseName) {
        return partMeshMap.get(baseName);
    }

    public static List<String> getPartGroups(String baseName) {
        List<String> g = partGroupMap.get(baseName);
        return g != null ? g : List.of();
    }

    public static Set<String> getAllBaseNames() {
        return Collections.unmodifiableSet(partGroupMap.keySet());
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
    public static List<com.hashiriyacarmod.cars.CarJsonResult.PartPlacement> getPartPlacements(String baseName) {
        List<com.hashiriyacarmod.cars.CarJsonResult.PartPlacement> p = partPlacementMap.get(baseName);
        return p != null ? p : List.of();
    }

    public static List<String> getAllowedChildGroups(String baseName) {
        List<String> g = partAllowedChildGroupMap.get(baseName);
        return g != null ? g : List.of();
    }

    public static com.hashiriyacarmod.cars.CarJsonResult.PartPlacement findPlacement(
            String parentPartBase, String seatGroup) {
        if (parentPartBase == null || seatGroup == null || seatGroup.isEmpty()) {
            return null;
        }
        for (com.hashiriyacarmod.cars.CarJsonResult.PartPlacement p : getPartPlacements(parentPartBase)) {
            if (p.groups != null && p.groups.contains(seatGroup)) {
                return p;
            }
        }
        return null;
    }
}