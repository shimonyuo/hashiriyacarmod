package com.hashiriyacarmod.cars;

import com.hashiriyacarmod.HitboxDefinition;
import java.util.List;
import java.util.ArrayList;

public class CarJsonResult {
    public final String displayName;
    public final float width;
    public final float height;
    public final String type;
    public final List<HitboxDefinition> hitboxes;
    public final List<String> allowedPartGroups;

    public CarJsonResult(String displayName, float width, float height, String type,
                         List<HitboxDefinition> hitboxes, List<String> allowedPartGroups) {
        this.displayName = displayName;
        this.width = width;
        this.height = height;
        this.type = type;
        this.hitboxes = hitboxes;
        this.allowedPartGroups = allowedPartGroups != null ? allowedPartGroups : new ArrayList<>();
    }
}