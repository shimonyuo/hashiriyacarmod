package com.hashiriyacarmod.cars;

import com.hashiriyacarmod.HitFaceDefinition;
import com.hashiriyacarmod.HitboxDefinition;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CarJsonResult {

    public final String displayName;
    public final Map<String, String> displayNames;
    public final float width;
    public final float height;
    public final String type;
    public final List<HitboxDefinition> hitboxes;
    public final List<HitFaceDefinition> hitFaces;
    public final List<String> allowedPartGroups;
    public final List<PartPlacement> partPlacements;

    /** 旧呼び出し互換（hitFaces 空） */
    public CarJsonResult(String displayName, float width, float height, String type,
                         List<HitboxDefinition> hitboxes, List<String> allowedPartGroups,
                         List<PartPlacement> partPlacements) {
        this(Map.of("default", displayName != null ? displayName : ""),
                width, height, type, hitboxes, allowedPartGroups, partPlacements, List.of());
    }

    public CarJsonResult(Map<String, String> displayNames, float width, float height, String type,
                         List<HitboxDefinition> hitboxes, List<String> allowedPartGroups,
                         List<PartPlacement> partPlacements) {
        this(displayNames, width, height, type, hitboxes, allowedPartGroups, partPlacements, List.of());
    }

    public CarJsonResult(Map<String, String> displayNames, float width, float height, String type,
                         List<HitboxDefinition> hitboxes, List<String> allowedPartGroups,
                         List<PartPlacement> partPlacements, List<HitFaceDefinition> hitFaces) {
        Map<String, String> copy = new LinkedHashMap<>();
        if (displayNames != null) {
            for (Map.Entry<String, String> e : displayNames.entrySet()) {
                if (e.getKey() == null || e.getValue() == null) continue;
                copy.put(e.getKey().toLowerCase(Locale.ROOT), e.getValue());
            }
        }
        this.displayNames = Map.copyOf(copy);
        String def = this.displayNames.get("default");
        if (def == null || def.isEmpty()) {
            def = this.displayNames.values().stream().findFirst().orElse("");
        }
        this.displayName = def;
        this.width = width;
        this.height = height;
        this.type = type;
        this.hitboxes = hitboxes != null ? hitboxes : new ArrayList<>();
        this.hitFaces = hitFaces != null ? hitFaces : new ArrayList<>();
        this.allowedPartGroups = allowedPartGroups != null ? allowedPartGroups : new ArrayList<>();
        this.partPlacements = partPlacements != null ? partPlacements : new ArrayList<>();
    }

    public String resolveDisplayName(String languageCode) {
        if (languageCode != null) {
            String key = languageCode.toLowerCase(Locale.ROOT);
            String specific = displayNames.get(key);
            if (specific != null && !specific.isEmpty()) {
                return specific;
            }
        }
        String def = displayNames.get("default");
        if (def != null && !def.isEmpty()) {
            return def;
        }
        return displayName != null ? displayName : "";
    }

    public static class PartPlacement {
        public final Vec3 position;
        public final float rotX;
        public final float rotY;
        public final float rotZ;
        public final List<String> groups;
        public final String defaultPart;

        public PartPlacement(Vec3 position, float rotX, float rotY, float rotZ, List<String> groups) {
            this(position, rotX, rotY, rotZ, groups, "");
        }

        public PartPlacement(Vec3 position, float rotX, float rotY, float rotZ,
                             List<String> groups, String defaultPart) {
            this.position = position;
            this.rotX = rotX;
            this.rotY = rotY;
            this.rotZ = rotZ;
            this.groups = groups != null ? groups : List.of();
            this.defaultPart = defaultPart != null ? defaultPart.trim() : "";
        }
    }
}