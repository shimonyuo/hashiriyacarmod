package com.hashiriyacarmod.parts;

import com.hashiriyacarmod.AssetRegistry;
import com.hashiriyacarmod.CarPackLoader;
import com.hashiriyacarmod.cars.CarJsonResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * group -> parents (car or part). Used by /hashiriyacommand search.
 */
public final class PartAttachmentIndex {

    public enum OwnerKind {
        CAR,
        PART
    }

    public static final class SeatOwner {
        public final OwnerKind kind;
        public final String baseName;
        public final String seat;

        public SeatOwner(OwnerKind kind, String baseName, String seat) {
            this.kind = kind;
            this.baseName = baseName;
            this.seat = seat;
        }
    }

    private static final Map<String, List<SeatOwner>> BY_GROUP = new LinkedHashMap<>();
    private static boolean built = false;

    private PartAttachmentIndex() {}

    public static void rebuild() {
        BY_GROUP.clear();
        built = false;
        build();
    }

    public static void build() {
        if (built) return;

        for (AssetRegistry reg : CarPackLoader.getAllAssetRegistries()) {
            if (reg == null || reg.partPlacements == null) continue;
            for (CarJsonResult.PartPlacement p : reg.partPlacements) {
                if (p.groups == null) continue;
                for (String g : p.groups) {
                    add(g, new SeatOwner(OwnerKind.CAR, reg.baseName, g));
                }
            }
        }

        for (String partBase : PartRegistry.getAllBaseNames()) {
            for (CarJsonResult.PartPlacement p : PartRegistry.getPartPlacements(partBase)) {
                if (p.groups == null) continue;
                for (String g : p.groups) {
                    add(g, new SeatOwner(OwnerKind.PART, partBase, g));
                }
            }
        }

        built = true;
    }

    private static void add(String group, SeatOwner owner) {
        if (group == null || group.isBlank()) return;
        String key = group.trim();
        BY_GROUP.computeIfAbsent(key, k -> new ArrayList<>()).add(owner);
    }

    public static List<SeatOwner> findParentsByGroup(String group) {
        build();
        if (group == null || group.isBlank()) return List.of();
        List<SeatOwner> list = BY_GROUP.get(group.trim());
        return list != null ? List.copyOf(list) : List.of();
    }

    public static String normalizeQuery(String query) {
        if (query == null) return "";
        String q = query.trim();
        if (q.toLowerCase(Locale.ROOT).endsWith(".json")) {
            q = q.substring(0, q.length() - 5);
        }
        int slash = Math.max(q.lastIndexOf('/'), q.lastIndexOf('\\'));
        if (slash >= 0 && slash + 1 < q.length()) {
            q = q.substring(slash + 1);
        }
        return q.trim();
    }

    /**
     * @param partQuery part baseName (optional .json)
     * @param carFilter optional car baseName; null/blank = all cars
     * @return path lines only, or a single "nothing"
     */
    public static List<String> searchLines(String partQuery, String carFilter) {
        build();
        String partBase = normalizeQuery(partQuery);
        String car = carFilter == null ? "" : normalizeQuery(carFilter);

        if (partBase.isEmpty()) {
            return List.of("nothing");
        }
        if (!PartRegistry.getAllBaseNames().contains(partBase)) {
            return List.of("nothing");
        }

        List<String> groups = PartRegistry.getPartGroups(partBase);
        List<String> paths = new ArrayList<>();
        Set<String> visiting = new LinkedHashSet<>();
        collectCarPaths(partBase, groups, partBase, car, visiting, paths, 0, 8);

        // unique, stable order
        List<String> unique = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (String p : paths) {
            if (seen.add(p)) {
                unique.add(p);
            }
        }

        if (unique.isEmpty()) {
            return List.of("nothing");
        }
        return unique;
    }

    /**
     * suffix is the right-hand side already built (toward the leaf).
     * Example leaf start: "foglamp1"
     * After one parent part: "ae86frontaero1[shimofoglamp1]...foglamp1"
     * After car: "test3[ae86frontaero]...ae86frontaero1[shimofoglamp1]...foglamp1"
     */
    private static void collectCarPaths(String currentBase,
                                        List<String> currentGroups,
                                        String suffix,
                                        String carFilter,
                                        Set<String> visiting,
                                        List<String> out,
                                        int depth,
                                        int maxDepth) {
        if (depth >= maxDepth) return;
        if (currentBase == null || currentBase.isEmpty()) return;
        if (!visiting.add(currentBase)) return;

        for (String g : currentGroups) {
            for (SeatOwner o : findParentsByGroup(g)) {
                String nextSuffix = o.baseName + "[" + o.seat + "]..." + suffix;
                if (o.kind == OwnerKind.CAR) {
                    if (carFilter.isEmpty() || carFilter.equals(o.baseName)) {
                        out.add(nextSuffix);
                    }
                } else {
                    List<String> parentGroups = PartRegistry.getPartGroups(o.baseName);
                    collectCarPaths(o.baseName, parentGroups, nextSuffix,
                            carFilter, visiting, out, depth + 1, maxDepth);
                }
            }
        }

        visiting.remove(currentBase);
    }
}