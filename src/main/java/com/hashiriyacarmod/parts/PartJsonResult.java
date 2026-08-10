package com.hashiriyacarmod.parts;

import com.hashiriyacarmod.cars.CarJsonResult;
import java.util.ArrayList;
import java.util.List;

public class PartJsonResult {

    public final String displayName;
    public final String type;
    public final List<String> groups;
    public final String texturePath;

    /** このパーツが子パーツ用に公開する座（parts[] の po/ro/group） */
    public final List<CarJsonResult.PartPlacement> partPlacements;
    /** parts[].group から集めた、子に付けられる group 名 */
    public final List<String> allowedChildGroups;

    public PartJsonResult(String displayName, String type, List<String> groups,
                          String texturePath,
                          List<CarJsonResult.PartPlacement> partPlacements,
                          List<String> allowedChildGroups) {
        this.displayName = displayName != null ? displayName : "";
        this.type = type != null ? type : "parts";
        this.groups = groups != null ? List.copyOf(groups) : List.of();
        this.texturePath = texturePath != null ? texturePath.trim() : "";
        this.partPlacements = partPlacements != null
                ? List.copyOf(partPlacements) : List.of();
        this.allowedChildGroups = allowedChildGroups != null
                ? List.copyOf(allowedChildGroups) : List.of();
    }

    public static PartJsonResult empty(String fallbackName) {
        return new PartJsonResult(fallbackName, "parts", new ArrayList<>(), "",
                List.of(), List.of());
    }
}