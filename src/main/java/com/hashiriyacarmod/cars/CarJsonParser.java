package com.hashiriyacarmod.cars;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hashiriyacarmod.HashiriyaCarMod;
import com.hashiriyacarmod.HitFaceDefinition;
import com.hashiriyacarmod.HitboxDefinition;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CarJsonParser {

    private static final Logger LOGGER = LogManager.getLogger(HashiriyaCarMod.MOD_ID);

    public static CarJsonResult parse(File jsonFile) {
        String fallbackName = jsonFile != null
                ? jsonFile.getName().replace(".json", "")
                : "";
        Map<String, String> displayNames = new LinkedHashMap<>();
        float width = 1.0f;
        float height = 1.0f;
        String type = "cars";
        List<HitboxDefinition> hitboxes = new ArrayList<>();
        List<HitFaceDefinition> hitFaces = new ArrayList<>();
        List<String> allowedPartGroups = new ArrayList<>();
        List<CarJsonResult.PartPlacement> partPlacements = new ArrayList<>();

        if (jsonFile == null || !jsonFile.exists()) {
            displayNames.put("default", fallbackName);
            return new CarJsonResult(displayNames, width, height, type,
                    hitboxes, allowedPartGroups, partPlacements, hitFaces);
        }

        try (InputStreamReader reader = new InputStreamReader(
                new FileInputStream(jsonFile), StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();

            if (root.has("basic") && root.get("basic").isJsonObject()) {
                JsonObject basic = root.getAsJsonObject("basic");

                if (basic.has("name")) {
                    parseDisplayNames(basic.get("name"), displayNames);
                }
                if (basic.has("width")) {
                    width = basic.get("width").getAsFloat();
                }
                if (basic.has("height")) {
                    height = basic.get("height").getAsFloat();
                }
                if (basic.has("type")) {
                    type = basic.get("type").getAsString();
                }
            }

            if (displayNames.isEmpty()
                    || !displayNames.containsKey("default")
                    || displayNames.get("default") == null
                    || displayNames.get("default").isEmpty()) {
                displayNames.put("default", fallbackName);
            }

            if (root.has("parts") && root.get("parts").isJsonArray()) {
                JsonArray partsArray = root.getAsJsonArray("parts");
                for (int i = 0; i < partsArray.size(); i++) {
                    if (!partsArray.get(i).isJsonObject()) continue;
                    JsonObject partObj = partsArray.get(i).getAsJsonObject();

                    Vec3 position = Vec3.ZERO;
                    if (partObj.has("po") && partObj.get("po").isJsonArray()) {
                        JsonArray po = partObj.getAsJsonArray("po");
                        if (po.size() >= 3) {
                            position = new Vec3(
                                    po.get(0).getAsDouble(),
                                    po.get(1).getAsDouble(),
                                    po.get(2).getAsDouble()
                            );
                        }
                    }

                    float rotX = 0f, rotY = 0f, rotZ = 0f;
                    if (partObj.has("ro") && partObj.get("ro").isJsonArray()) {
                        JsonArray ro = partObj.getAsJsonArray("ro");
                        if (ro.size() >= 3) {
                            rotX = ro.get(0).getAsFloat();
                            rotY = ro.get(1).getAsFloat();
                            rotZ = ro.get(2).getAsFloat();
                        }
                    }

                    List<String> groups = new ArrayList<>();
                    if (partObj.has("group") && partObj.get("group").isJsonArray()) {
                        JsonArray groupArray = partObj.getAsJsonArray("group");
                        for (int j = 0; j < groupArray.size(); j++) {
                            String groupName = groupArray.get(j).getAsString();
                            groups.add(groupName);
                            if (!allowedPartGroups.contains(groupName)) {
                                allowedPartGroups.add(groupName);
                            }
                        }
                    }

                    String defaultPart = "";
                    if (partObj.has("default") && partObj.get("default").isJsonPrimitive()) {
                        defaultPart = partObj.get("default").getAsString().trim();
                    }

                    partPlacements.add(new CarJsonResult.PartPlacement(
                            position, rotX, rotY, rotZ, groups, defaultPart));
                }
            }

            if (root.has("hitbox") && root.get("hitbox").isJsonObject()) {
                JsonObject hitboxObj = root.getAsJsonObject("hitbox");

                if (hitboxObj.has("obbList") && hitboxObj.get("obbList").isJsonArray()) {
                    JsonArray obbList = hitboxObj.getAsJsonArray("obbList");
                    List<HitboxDefinition> defs = new ArrayList<>();

                    for (int obbIndex = 0; obbIndex < obbList.size(); obbIndex++) {
                        if (!obbList.get(obbIndex).isJsonObject()) continue;
                        JsonObject obb = obbList.get(obbIndex).getAsJsonObject();

                        Vec3 positionOffset = Vec3.ZERO;
                        if (obb.has("po") && obb.get("po").isJsonArray()) {
                            JsonArray po = obb.getAsJsonArray("po");
                            if (po.size() >= 3) {
                                positionOffset = new Vec3(
                                        po.get(0).getAsDouble(),
                                        po.get(1).getAsDouble(),
                                        po.get(2).getAsDouble());
                            }
                        }

                        float obbYaw = 0f, obbPitch = 0f, obbRoll = 0f;
                        if (obb.has("ro") && obb.get("ro").isJsonArray()) {
                            JsonArray ro = obb.getAsJsonArray("ro");
                            if (ro.size() >= 3) {
                                obbYaw = ro.get(0).getAsFloat();
                                obbPitch = ro.get(1).getAsFloat();
                                obbRoll = ro.get(2).getAsFloat();
                            }
                        }

                        if (obb.has("vertices") && obb.get("vertices").isJsonArray()) {
                            JsonArray vertArray = obb.getAsJsonArray("vertices");
                            if (vertArray.size() == 8) {
                                Vec3[] verts = new Vec3[8];
                                for (int i = 0; i < 8; i++) {
                                    JsonArray p = vertArray.get(i).getAsJsonArray();
                                    verts[i] = new Vec3(
                                            p.get(0).getAsDouble(),
                                            p.get(1).getAsDouble(),
                                            p.get(2).getAsDouble());
                                }

                                boolean[] skipFace = new boolean[6];
                                if (obb.has("skip_faces") && obb.get("skip_faces").isJsonArray()) {
                                    JsonArray skipArr = obb.getAsJsonArray("skip_faces");
                                    for (int s = 0; s < skipArr.size(); s++) {
                                        if (!skipArr.get(s).isJsonPrimitive()) continue;
                                        int idx = skipArr.get(s).getAsInt();
                                        if (idx >= 0 && idx < 6) {
                                            skipFace[idx] = true;
                                        }
                                    }
                                }

                                defs.add(new HitboxDefinition(
                                        verts, positionOffset,
                                        obbYaw, obbPitch, obbRoll,
                                        skipFace));
                            } else {
                                LOGGER.warn("[HashiriyaCarMod] obbList[{}]のverticesは8個必要です: {}",
                                        obbIndex, jsonFile.getName());
                            }
                        }
                    }

                    if (!defs.isEmpty()) {
                        hitboxes.addAll(defs);
                    }
                }
            }

            // hitfaces（任意頂点数の平面ポリゴン）
            if (root.has("hitfaces") && root.get("hitfaces").isJsonObject()) {
                JsonObject hfRoot = root.getAsJsonObject("hitfaces");
                if (hfRoot.has("facesList") && hfRoot.get("facesList").isJsonArray()) {
                    JsonArray list = hfRoot.getAsJsonArray("facesList");
                    for (int i = 0; i < list.size(); i++) {
                        if (!list.get(i).isJsonObject()) continue;
                        JsonObject faceObj = list.get(i).getAsJsonObject();

                        Vec3 po = Vec3.ZERO;
                        if (faceObj.has("po") && faceObj.get("po").isJsonArray()) {
                            JsonArray a = faceObj.getAsJsonArray("po");
                            if (a.size() >= 3) {
                                po = new Vec3(
                                        a.get(0).getAsDouble(),
                                        a.get(1).getAsDouble(),
                                        a.get(2).getAsDouble());
                            }
                        }

                        float yaw = 0f, pitch = 0f, roll = 0f;
                        if (faceObj.has("ro") && faceObj.get("ro").isJsonArray()) {
                            JsonArray a = faceObj.getAsJsonArray("ro");
                            if (a.size() >= 3) {
                                yaw = a.get(0).getAsFloat();
                                pitch = a.get(1).getAsFloat();
                                roll = a.get(2).getAsFloat();
                            }
                        }

                        if (!faceObj.has("faceVertices") || !faceObj.get("faceVertices").isJsonArray()) {
                            continue;
                        }
                        JsonArray va = faceObj.getAsJsonArray("faceVertices");
                        if (va.size() < 3) {
                            LOGGER.warn("[CarJsonParser] faceVertices は3点以上: {}", jsonFile.getName());
                            continue;
                        }

                        Vec3[] verts = new Vec3[va.size()];
                        boolean ok = true;
                        for (int v = 0; v < va.size(); v++) {
                            if (!va.get(v).isJsonArray() || va.get(v).getAsJsonArray().size() < 3) {
                                ok = false;
                                break;
                            }
                            JsonArray p = va.get(v).getAsJsonArray();
                            verts[v] = new Vec3(
                                    p.get(0).getAsDouble(),
                                    p.get(1).getAsDouble(),
                                    p.get(2).getAsDouble());
                        }
                        if (!ok) continue;

                        hitFaces.add(new HitFaceDefinition(verts, po, yaw, pitch, roll));
                    }
                }
            }

        } catch (Exception e) {
            LOGGER.warn("[CarJsonParser] 読み取り失敗: {} ({})",
                    jsonFile.getName(), e.toString());
            if (!displayNames.containsKey("default")) {
                displayNames.put("default", fallbackName);
            }
        }

        return new CarJsonResult(displayNames, width, height, type,
                hitboxes, allowedPartGroups, partPlacements, hitFaces);
    }

    private static void parseDisplayNames(JsonElement nameEl, Map<String, String> out) {
        if (nameEl == null || nameEl.isJsonNull()) {
            return;
        }
        if (nameEl.isJsonPrimitive()) {
            String s = nameEl.getAsString();
            if (s != null && !s.isEmpty()) {
                out.put("default", s);
            }
            return;
        }
        if (nameEl.isJsonObject()) {
            JsonObject nameObj = nameEl.getAsJsonObject();
            for (Map.Entry<String, JsonElement> e : nameObj.entrySet()) {
                if (e.getValue() != null && e.getValue().isJsonPrimitive()) {
                    String key = e.getKey().toLowerCase(Locale.ROOT);
                    String val = e.getValue().getAsString();
                    if (val != null && !val.isEmpty()) {
                        out.put(key, val);
                    }
                }
            }
        }
    }
}