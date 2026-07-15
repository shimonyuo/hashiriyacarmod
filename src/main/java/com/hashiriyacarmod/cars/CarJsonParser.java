package com.hashiriyacarmod.cars;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hashiriyacarmod.HashiriyaCarMod;
import com.hashiriyacarmod.HitboxDefinition;
import net.minecraft.world.phys.Vec3;
import com.hashiriyacarmod.CarPackLoader; // HitboxDefinitionを使うため
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class CarJsonParser {

    private static final Logger LOGGER = LogManager.getLogger(HashiriyaCarMod.MOD_ID);

    public static CarJsonResult parse(File jsonFile) {
        String displayName = jsonFile.getName().replace(".json", "");
        float width = 1.0f;
        float height = 1.0f;
        String type = "cars";
        List<HitboxDefinition> hitboxes = new ArrayList<>();
        List<String> partGroups = new ArrayList<>();

        try (FileReader reader = new FileReader(jsonFile)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();

            if (root.has("basic")) {
                JsonObject basic = root.getAsJsonObject("basic");
                if (basic.has("name")) displayName = basic.get("name").getAsString();
                if (basic.has("width")) width = basic.get("width").getAsFloat();
                if (basic.has("height")) height = basic.get("height").getAsFloat();
                if (basic.has("type")) type = basic.get("type").getAsString();
            }

            // hitbox解析（今後増える部分）
            if (root.has("hitbox")) {
                JsonObject hitboxObj = root.getAsJsonObject("hitbox");

                if (hitboxObj.has("obbList")) {
                    JsonArray obbList = hitboxObj.getAsJsonArray("obbList");
                    List<HitboxDefinition> defs = new ArrayList<>();

                    for (int obbIndex = 0; obbIndex < obbList.size(); obbIndex++) {
                        JsonObject obb = obbList.get(obbIndex).getAsJsonObject();

                        Vec3 positionOffset = Vec3.ZERO;
                        if (obb.has("po")) {
                            JsonArray po = obb.getAsJsonArray("po");
                            if (po.size() >= 3) {
                                positionOffset = new Vec3(
                                        po.get(0).getAsDouble(),
                                        po.get(1).getAsDouble(),
                                        po.get(2).getAsDouble());
                            }
                        }

                        float obbYaw = 0f, obbPitch = 0f, obbRoll = 0f;
                        if (obb.has("ro")) {
                            JsonArray ro = obb.getAsJsonArray("ro");
                            if (ro.size() >= 3) {
                                obbYaw   = ro.get(0).getAsFloat();
                                obbPitch = ro.get(1).getAsFloat();
                                obbRoll  = ro.get(2).getAsFloat();
                            }
                        }

                        if (obb.has("vertices")) {
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
                                defs.add(new HitboxDefinition(verts, positionOffset, obbYaw, obbPitch, obbRoll));
                            } else {
                                LOGGER.warn("[HashiriyaCarMod] obbList[{}]のverticesは8個必要です: {}",
                                        obbIndex, jsonFile.getName());  // LOGGERはstatic importかCarPackLoaderから呼ぶ
                            }
                        }
                    }

                    if (!defs.isEmpty()) {
                        hitboxes.addAll(defs);   // ここで追加
                    }
                }
            }

            if (root.has("parts")) {
                JsonArray partsArray = root.getAsJsonArray("parts");
                for (int i = 0; i < partsArray.size(); i++) {
                    JsonObject partObj = partsArray.get(i).getAsJsonObject();
                    if (partObj.has("group") && partObj.get("group").isJsonArray()) {
                        JsonArray groupArray = partObj.getAsJsonArray("group");
                        for (int j = 0; j < groupArray.size(); j++) {
                            String group = groupArray.get(j).getAsString();
                            if (group != null && !group.trim().isEmpty()) {
                                partGroups.add(group.trim());
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            // エラーログはPackLoader側に任せるか、ここで出す
            e.printStackTrace();
        }

        return new CarJsonResult(displayName, width, height, type, hitboxes);
    }
}