package com.hashiriyacarmod.cars;

import com.hashiriyacarmod.HitboxDefinition;
import java.util.List;

public class CarJsonResult {
    public final String displayName;
    public final float width;
    public final float height;
    public final String type;
    public final List<HitboxDefinition> hitboxes;

    public CarJsonResult(String displayName, float width, float height, String type, List<HitboxDefinition> hitboxes) {
        this.displayName = displayName;
        this.width = width;
        this.height = height;
        this.type = type;
        this.hitboxes = hitboxes;
    }
}