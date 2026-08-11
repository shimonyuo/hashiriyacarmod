package com.hashiriyacarmod.parts;

import com.hashiriyacarmod.AssetRegistry;
import com.hashiriyacarmod.CarEntity;
import com.hashiriyacarmod.CarPackLoader;
import com.hashiriyacarmod.cars.CarJsonResult;
import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.List;

/**
 * 車 / パーツ JSON の parts[].default を、
 * AttachedParts が空のときだけ初期装着する。
 *
 * CarEntity は setAttachedPartInTree を呼ぶだけ（ロジックはここに集約）。
 */
public final class DefaultAttachedPartsApplier {

    private DefaultAttachedPartsApplier() {}

    /**
     * ルートの装着キーが1つも無いときだけ、
     * 車の AssetRegistry.partPlacements の default を適用する。
     * 子パーツ JSON の default も再帰で付ける。
     */
    public static void applyDefaultsIfEmpty(CarEntity car) {
        if (car == null) return;
        if (car.getBaseName() == null || car.getBaseName().isEmpty()) return;

        // 既に何か付いている（NBT /summon / メニュー操作後）→ 触らない
        if (!car.getAttachedParts().isEmpty()) return;

        AssetRegistry registry = CarPackLoader.getAssetRegistry(car.getBaseName());
        if (registry == null || registry.partPlacements == null
                || registry.partPlacements.isEmpty()) {
            return;
        }

        applyPlacements(car, List.of(), registry.partPlacements);
    }

    /**
     * pathTokens = 親ノードまでの装着トークン列（空なら車直下）。
     * placements の default を、各 seat（group）に付ける。
     */
    public static void applyPlacements(CarEntity car,
                                       List<String> pathTokens,
                                       List<CarJsonResult.PartPlacement> placements) {
        if (car == null || placements == null || placements.isEmpty()) return;

        List<String> path = pathTokens != null ? pathTokens : List.of();

        for (CarJsonResult.PartPlacement p : placements) {
            if (p.defaultPart == null || p.defaultPart.isEmpty()) continue;
            if (p.groups == null || p.groups.isEmpty()) continue;

            for (String seat : p.groups) {
                if (seat == null || seat.isBlank()) continue;
                String seatTrim = seat.trim();

                // 既存の setAttachedPartInTree（filter 込み）
                car.setAttachedPartInTree(path, seatTrim, p.defaultPart);

                // 実際に付いたトークンを探し、そのパーツの parts[].default を再帰
                String token = findTokenForSeat(car, path, seatTrim, p.defaultPart);
                if (token == null || token.isEmpty()) continue;

                List<String> childPath = new ArrayList<>(path);
                childPath.add(token);

                List<CarJsonResult.PartPlacement> childPlacements =
                        PartRegistry.getPartPlacements(p.defaultPart);
                if (!childPlacements.isEmpty()) {
                    applyPlacements(car, childPath, childPlacements);
                }
            }
        }
    }

    /**
     * path 先の Compound から、expectedBase が付き seat に属するキーを探す。
     */
    private static String findTokenForSeat(CarEntity car,
                                           List<String> pathTokens,
                                           String seat,
                                           String expectedBase) {
        CompoundTag node = car.getAttachedPartsTree();
        if (node == null) return null;

        if (pathTokens != null) {
            for (String t : pathTokens) {
                if (t == null || t.isBlank()) continue;
                if (!node.contains(t, CompoundTag.TAG_COMPOUND)) {
                    return null;
                }
                node = node.getCompound(t);
            }
        }

        String expected = expectedBase != null ? expectedBase.trim() : "";
        if (expected.isEmpty()) return null;

        // 優先: base>seat
        String prefer = expected + ">" + seat;
        if (node.contains(prefer)) {
            return prefer;
        }

        for (String key : node.getAllKeys()) {
            if (key == null || key.isBlank()) continue;
            if (!CarEntity.attachedBaseName(key).equals(expected)) continue;

            String keySeat = CarEntity.attachedSeatGroup(key);
            if (keySeat.isEmpty() || keySeat.equals(seat)) {
                return key;
            }
        }
        return null;
    }
}