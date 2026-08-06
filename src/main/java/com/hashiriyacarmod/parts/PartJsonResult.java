package com.hashiriyacarmod.parts;

import java.util.ArrayList;
import java.util.List;

/**
 * パーツJSON（type:"parts"）を読んだ結果。
 * group など静的な定義を保持する。
 * 将来の動的パラメータも、ここにフィールドを足していく想定。
 */
public class PartJsonResult {

    public final String displayName;
    public final String type;
    /** basic.group（静的） */
    public final List<String> groups;

    public PartJsonResult(String displayName, String type, List<String> groups) {
        this.displayName = displayName != null ? displayName : "";
        this.type = type != null ? type : "parts";
        this.groups = groups != null ? List.copyOf(groups) : List.of();
    }

    public static PartJsonResult empty(String fallbackName) {
        return new PartJsonResult(fallbackName, "parts", new ArrayList<>());
    }
}