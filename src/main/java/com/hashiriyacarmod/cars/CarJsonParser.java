package com.hashiriyacarmod.cars;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hashiriyacarmod.HitboxDefinition;
import net.minecraft.world.phys.Vec3;
import com.hashiriyacarmod.CarPackLoader; // HitboxDefinitionを使うため
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class CarJsonParser {

    public static CarJsonResult parse(File jsonFile) {
        String displayName = jsonFile.getName().replace(".json", "");
        float width = 1.0f;
        float height = 1.0f;
        String type = "cars";
        List<HitboxDefinition> hitboxes = new ArrayList<>();

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
                    for (int i = 0; i < obbList.size(); i++) {
                        // ここに既存のhitbox解析ロジックを全部移動
                        JsonObject obb = obbList.get(i).getAsJsonObject();
                        // ... (元のloadCarJson内のhitbox処理をここに移動)
                        // 最終的にHitboxDefinitionを作成して hitboxes.add(...)
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