package com.hashiriyacarmod;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.HashMap;
import java.io.*;
import java.util.*;

public class ObjLoader {

    private static final Logger LOGGER = LogManager.getLogger(HashiriyaCarMod.MOD_ID);

    /** oオブジェクトごとに分割して返す（メイン） */
    public static Map<String, ObjMesh> loadWithParts(File file) {
        if (file == null || !file.exists()) return Map.of();

        Map<String, ObjMeshBuilder> builders = new LinkedHashMap<>();
        String currentPart = "default";

        List<float[]> positions = new ArrayList<>();
        List<float[]> uvs = new ArrayList<>();
        List<float[]> normals = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                String[] tokens = line.split("\\s+");
                switch (tokens[0]) {
                    case "o" -> currentPart = tokens.length > 1 ? tokens[1] : "default";
                    case "v" -> positions.add(parseFloat3(tokens));
                    case "vt" -> uvs.add(parseFloat2(tokens));
                    case "vn" -> normals.add(parseFloat3(tokens));
                    case "f" -> {
                        ObjMeshBuilder builder = builders.computeIfAbsent(currentPart, k -> new ObjMeshBuilder());
                        processFace(tokens, builder, positions, uvs, normals);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("[HashiriyaCarMod] OBJ読み込み失敗: {} / {}", file.getName(), e.getMessage());
            return Map.of();
        }

        Map<String, ObjMesh> result = new LinkedHashMap<>();
        for (var entry : builders.entrySet()) {
            result.put(entry.getKey(), entry.getValue().build());
        }

        LOGGER.debug("[HashiriyaCarMod] OBJ解析完了: {} ({} パーツ)", file.getName(), result.size());
        return result;
    }

    private static float[] parseFloat3(String[] tokens) {
        return new float[]{
                Float.parseFloat(tokens[1]),
                Float.parseFloat(tokens[2]),
                Float.parseFloat(tokens[3])
        };
    }

    private static float[] parseFloat2(String[] tokens) {
        return new float[]{
                Float.parseFloat(tokens[1]),
                Float.parseFloat(tokens[2])
        };
    }

    private static void processFace(String[] tokens, ObjMeshBuilder builder,
                                    List<float[]> positions, List<float[]> uvs, List<float[]> normals) {
        int count = tokens.length - 1;
        float[][] verts = new float[count][];
        for (int i = 0; i < count; i++) {
            verts[i] = parseVertex(tokens[i + 1], positions, uvs, normals);
        }
        for (int i = 1; i < count - 1; i++) {
            builder.addVertex(verts[0]);
            builder.addVertex(verts[i]);
            builder.addVertex(verts[i + 1]);
        }
    }

    private static float[] parseVertex(String token, List<float[]> positions, List<float[]> uvs, List<float[]> normals) {
        String[] idx = token.split("/", -1);
        int vi = Integer.parseInt(idx[0]) - 1;
        int vti = (idx.length > 1 && !idx[1].isEmpty()) ? Integer.parseInt(idx[1]) - 1 : -1;
        int vni = (idx.length > 2 && !idx[2].isEmpty()) ? Integer.parseInt(idx[2]) - 1 : -1;

        float[] pos = positions.get(vi);
        float[] uv = vti >= 0 ? uvs.get(vti) : new float[]{0f, 0f};
        float[] nrm = vni >= 0 ? normals.get(vni) : new float[]{0f, 1f, 0f};

        return new float[]{pos[0], pos[1], pos[2], uv[0], uv[1], nrm[0], nrm[1], nrm[2]};
    }

    // 後方互換
    public static ObjMesh load(File file) {
        Map<String, ObjMesh> parts = loadWithParts(file);
        return parts.isEmpty() ? null : parts.values().iterator().next();
    }
}

class ObjMeshBuilder {
    private final List<float[]> uniquePos = new ArrayList<>();
    private final List<float[]> uniqueUv = new ArrayList<>();
    private final List<float[]> uniqueNrm = new ArrayList<>();
    private final Map<String, Integer> vertexMap = new HashMap<>();
    private final List<Integer> indices = new ArrayList<>();

    void addVertex(float[] vert) {
        String key = makeKey(vert);
        Integer index = vertexMap.get(key);

        if (index != null) {
            indices.add(index);
            return;
        }

        int newIndex = uniquePos.size();
        uniquePos.add(new float[]{vert[0], vert[1], vert[2]});
        uniqueUv.add(new float[]{vert[3], vert[4]});
        uniqueNrm.add(new float[]{vert[5], vert[6], vert[7]});
        vertexMap.put(key, newIndex);
        indices.add(newIndex);
    }

    private String makeKey(float[] vert) {
        StringBuilder sb = new StringBuilder();
        for (float f : vert) {
            sb.append(f).append(',');
        }
        return sb.toString();
    }

    ObjMesh build() {
        int count = uniquePos.size();
        float[] px = new float[count], py = new float[count], pz = new float[count];
        float[] fu = new float[count], fv = new float[count];
        float[] fnx = new float[count], fny = new float[count], fnz = new float[count];

        for (int i = 0; i < count; i++) {
            px[i] = uniquePos.get(i)[0];
            py[i] = uniquePos.get(i)[1];
            pz[i] = uniquePos.get(i)[2];
            fu[i] = uniqueUv.get(i)[0];
            fv[i] = 1.0f - uniqueUv.get(i)[1];
            fnx[i] = uniqueNrm.get(i)[0];
            fny[i] = uniqueNrm.get(i)[1];
            fnz[i] = uniqueNrm.get(i)[2];
        }

        int[] indexArray = new int[indices.size()];
        for (int i = 0; i < indices.size(); i++) {
            indexArray[i] = indices.get(i);
        }

        return new ObjMesh(count, px, py, pz, fu, fv, fnx, fny, fnz, indexArray);
    }
}