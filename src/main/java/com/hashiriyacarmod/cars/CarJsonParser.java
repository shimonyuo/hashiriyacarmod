package com.hashiriyacarmod.cars;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hashiriyacarmod.HashiriyaCarMod;
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
        List<String> allowedPartGroups = new ArrayList<>();
        List<CarJsonResult.PartPlacement> partPlacements = new ArrayList<>();

        if (jsonFile == null || !jsonFile.exists()) {
            displayNames.put("default", fallbackName);
            return new CarJsonResult(displayNames, width, height, type,
                    hitboxes, allowedPartGroups, partPlacements);
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
                                defs.add(new HitboxDefinition(
                                        verts, positionOffset, obbYaw, obbPitch, obbRoll));
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

        } catch (Exception e) {
            LOGGER.warn("[CarJsonParser] 読み取り失敗: {} ({})",
                    jsonFile.getName(), e.toString());
            if (!displayNames.containsKey("default")) {
                displayNames.put("default", fallbackName);
            }
        }

        return new CarJsonResult(displayNames, width, height, type,
                hitboxes, allowedPartGroups, partPlacements);
    }

    /**
     * name が文字列 → default のみ
     * name がオブジェクト → default / ja_jp / en_us など
     */
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