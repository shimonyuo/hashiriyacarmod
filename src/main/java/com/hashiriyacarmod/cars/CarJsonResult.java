package com.hashiriyacarmod.cars;

import com.hashiriyacarmod.HitboxDefinition;
import net.minecraft.world.phys.Vec3;
import java.util.List;
import java.util.ArrayList;

public class CarJsonResult {
    public final String displayName;
    public final float width;
    public final float height;
    public final String type;
    public final List<HitboxDefinition> hitboxes;
    public final List<String> allowedPartGroups;
    public final List<PartPlacement> partPlacements;  // ★追加

    public CarJsonResult(String displayName, float width, float height, String type,
                         List<HitboxDefinition> hitboxes, List<String> allowedPartGroups,
                         List<PartPlacement> partPlacements) {  // ★引数追加
        this.displayName = displayName;
        this.width = width;
        this.height = height;
        this.type = type;
        this.hitboxes = hitboxes;
        this.allowedPartGroups = allowedPartGroups != null ? allowedPartGroups : new ArrayList<>();
        this.partPlacements = partPlacements != null ? partPlacements : new ArrayList<>();  // ★追加
    }

    /** パーツ1つ分の位置・回転・グループ */
    public static class PartPlacement {
        public final Vec3 position;   // po
        public final float rotX;      // ro[0]
        public final float rotY;      // ro[1]
        public final float rotZ;      // ro[2]
        public final List<String> groups;

        public PartPlacement(Vec3 position, float rotX, float rotY, float rotZ, List<String> groups) {
            this.position = position;
            this.rotX = rotX;
            this.rotY = rotY;
            this.rotZ = rotZ;
            this.groups = groups;
        }
    }
}