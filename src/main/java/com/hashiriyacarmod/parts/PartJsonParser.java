package com.hashiriyacarmod.parts;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hashiriyacarmod.HashiriyaCarMod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * type:"parts" の JSON を読む。
 * 車本体用の CarJsonParser とは別系統。
 * CarPackLoader には依存しない（File を渡されて解析するだけ）。
 *
 * 静的: name / type / group
 * 動的: 将来 basic やルートに足した要素をここで読む
 */
public class PartJsonParser {

    private static final Logger LOGGER = LogManager.getLogger(HashiriyaCarMod.MOD_ID);

    public static PartJsonResult parse(File jsonFile) {
        String fallbackName = jsonFile != null
                ? jsonFile.getName().replace(".json", "")
                : "";
        String displayName = fallbackName;
        String type = "parts";
        List<String> groups = new ArrayList<>();
        String texturePath = "";

        if (jsonFile == null || !jsonFile.exists()) {
            return PartJsonResult.empty(fallbackName);
        }

        try (InputStreamReader reader = new InputStreamReader(
                new FileInputStream(jsonFile), StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();

            if (root.has("basic") && root.get("basic").isJsonObject()) {
                JsonObject basic = root.getAsJsonObject("basic");

                if (basic.has("name")) {
                    displayName = basic.get("name").getAsString();
                }
                if (basic.has("type")) {
                    type = basic.get("type").getAsString();
                }
                // 静的: group（配列でも単一文字列でも可）
                if (basic.has("group")) {
                    groups.addAll(readStringList(basic.get("group")));
                }

                // 将来の動的・追加フィールド例（まだ使わないが拡張点）:
                // if (basic.has("someDynamic")) { ... }
            }

            // ルート直下に group を書く形式にも一応対応
            if (groups.isEmpty() && root.has("group")) {
                groups.addAll(readStringList(root.get("group")));
            }

            if (root.has("texture_path") && root.get("texture_path").isJsonPrimitive()) {
                texturePath = root.get("texture_path").getAsString().trim();
            }

            // ===== 車と同じ parts[]（子パーツ用の座 po/ro/group）=====
            List<com.hashiriyacarmod.cars.CarJsonResult.PartPlacement> partPlacements =
                    new ArrayList<>();
            List<String> allowedChildGroups = new ArrayList<>();

            if (root.has("parts") && root.get("parts").isJsonArray()) {
                JsonArray partsArray = root.getAsJsonArray("parts");
                for (int i = 0; i < partsArray.size(); i++) {
                    if (!partsArray.get(i).isJsonObject()) continue;
                    JsonObject partObj = partsArray.get(i).getAsJsonObject();

                    net.minecraft.world.phys.Vec3 position =
                            net.minecraft.world.phys.Vec3.ZERO;
                    if (partObj.has("po") && partObj.get("po").isJsonArray()) {
                        JsonArray po = partObj.getAsJsonArray("po");
                        if (po.size() >= 3) {
                            position = new net.minecraft.world.phys.Vec3(
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

                    List<String> seatGroups = new ArrayList<>();
                    if (partObj.has("group")) {
                        seatGroups.addAll(readStringList(partObj.get("group")));
                        for (String g : seatGroups) {
                            if (!allowedChildGroups.contains(g)) {
                                allowedChildGroups.add(g);
                            }
                        }
                    }

                    partPlacements.add(
                            new com.hashiriyacarmod.cars.CarJsonResult.PartPlacement(
                                    position, rotX, rotY, rotZ, seatGroups));
                }
            }

            return new PartJsonResult(
                    displayName, type, groups, texturePath,
                    partPlacements, allowedChildGroups);

        } catch (Exception e) {
            LOGGER.warn("[PartJsonParser] 読み取り失敗: {} ({})",
                    jsonFile.getName(), e.toString());
            return PartJsonResult.empty(fallbackName);
        }
    }

    private static List<String> readStringList(JsonElement element) {
        List<String> out = new ArrayList<>();
        if (element == null || element.isJsonNull()) {
            return out;
        }
        if (element.isJsonArray()) {
            JsonArray arr = element.getAsJsonArray();
            for (JsonElement e : arr) {
                if (e.isJsonPrimitive()) {
                    String s = e.getAsString().trim();
                    if (!s.isEmpty() && !out.contains(s)) {
                        out.add(s);
                    }
                }
            }
        } else if (element.isJsonPrimitive()) {
            String s = element.getAsString().trim();
            if (!s.isEmpty()) {
                out.add(s);
            }
        }
        return out;
    }
}