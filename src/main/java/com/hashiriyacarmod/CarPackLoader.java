package com.hashiriyacarmod;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hashiriyacarmod.parts.PartRegistry;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.world.item.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.google.gson.JsonArray;
import net.minecraft.world.phys.Vec3;

import java.io.File;
import java.io.FileReader;
import java.nio.file.Paths;
import java.util.*;

public class CarPackLoader {

    private static final Map<String, List<HitboxDefinition>> hitboxMap = new LinkedHashMap<>();

    /** その車が持つ、すべての当たり判定の箱を返します。定義がなければ空リストです。 */
    public static List<HitboxDefinition> getHitboxDefinitions(String baseName) {
        return hitboxMap.getOrDefault(baseName, List.of());
    }

    private static final Logger LOGGER = LogManager.getLogger(HashiriyaCarMod.MOD_ID);

    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, HashiriyaCarMod.MOD_ID);

    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, HashiriyaCarMod.MOD_ID);

    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, HashiriyaCarMod.MOD_ID);

    private static final Map<String, List<RegistryObject<Item>>> tabItems      = new LinkedHashMap<>();
    private static final Map<String, AssetRegistry>              assetRegistryMap = new LinkedHashMap<>();

    public static void initialize(FMLJavaModLoadingContext context) {

        File packRootDir = Paths.get("hashiriyacarpack").toFile();

        if (!packRootDir.exists() || !packRootDir.isDirectory()) {
            LOGGER.warn("[HashiriyaCarMod] hashiriyacarpack フォルダが見つかりませんでした: {}",
                    packRootDir.getAbsolutePath());
        } else {
            File[] packDirs = packRootDir.listFiles(File::isDirectory);
            if (packDirs != null) {
                for (File packDir : packDirs) {
                    loadPack(packDir);
                }
            }
        }

        logAssetBindings();
        registerTabs();

        ITEMS.register(context.getModEventBus());
        ENTITY_TYPES.register(context.getModEventBus());
        TABS.register(context.getModEventBus());

        context.getModEventBus().addListener(CarPackLoader::addItemsToTabs);
        context.getModEventBus().addListener(CarPackLoader::onClientSetup);
    }

    public static AssetRegistry getAssetRegistry(String baseName) {
        return assetRegistryMap.get(baseName);
    }

    private static void loadPack(File packDir) {
        String packId  = sanitizeId(packDir.getName());
        File modAssets = new File(packDir, "assets/" + HashiriyaCarMod.MOD_ID);
        File jsonsRoot = new File(modAssets, "jsons");
        File objsRoot  = new File(modAssets, "objs");
        File texRoot   = new File(modAssets, "textures");

        if (!jsonsRoot.exists()) return;

        File[] tabDirs = jsonsRoot.listFiles(File::isDirectory);
        if (tabDirs == null) return;

        for (File tabDir : tabDirs) {
            String tabId     = sanitizeId(tabDir.getName());
            String fullTabId = packId + "_" + tabId;
            tabItems.putIfAbsent(fullTabId, new ArrayList<>());

            List<File> jsonFiles = collectAllJsonFiles(tabDir);
            for (File jsonFile : jsonFiles) {
                String baseName = jsonFile.getName().replace(".json", "");
                File objFile = findFileRecursive(objsRoot, baseName, ".obj");
                // pngはCarPackFinderがリソースパック経由で登録済みなので
                // ここではファイル参照のみ（CarTextureManagerでResourceLocation化）
                File pngFile = findFileRecursive(texRoot, baseName, ".png");

                loadCarJson(jsonFile, objFile, pngFile, fullTabId, fullTabId, baseName);
            }
        }
    }

    /** jsonsフォルダの中を再帰的にすべて集める（直下 → サブフォルダの順） */
    private static List<File> collectAllJsonFiles(File dir) {
        List<File> result = new ArrayList<>();
        if (dir == null || !dir.exists()) return result;

        File[] children = dir.listFiles();
        if (children == null) return result;

        // 1. 直下の.jsonファイル
        for (File f : children) {
            if (f.isFile() && f.getName().toLowerCase(Locale.ROOT).endsWith(".json")) {
                result.add(f);
            }
        }

        // 2. サブフォルダの中を再帰的に探す
        for (File f : children) {
            if (f.isDirectory()) {
                result.addAll(collectAllJsonFiles(f));
            }
        }

        return result;
    }

    /** サブフォルダも含めて再帰的にファイルを探す */
    private static File findFileRecursive(File dir, String baseName, String extension) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) return null;

        File[] children = dir.listFiles();
        if (children == null) return null;

        for (File child : children) {
            if (child.isDirectory()) {
                File found = findFileRecursive(child, baseName, extension);
                if (found != null) return found;
            } else if (child.isFile()) {
                String name = child.getName();
                if (name.toLowerCase(Locale.ROOT).endsWith(extension.toLowerCase(Locale.ROOT))) {
                    String nameWithoutExt = name.substring(0, name.length() - extension.length());
                    if (nameWithoutExt.equalsIgnoreCase(baseName)) {
                        return child;
                    }
                }
            }
        }
        return null;
    }

    private static void loadCarJson(File jsonFile, File objFile, File pngFile, String tabId, String idPrefix, String baseName) {
        String rawId    = jsonFile.getName().replace(".json", "");
        String entityId = idPrefix + "_" + sanitizeId(rawId);

        String displayName = rawId;
        float  width       = 1.0f;   // ← デフォルト値
        float  height      = 1.0f;
        String type        = "cars";   // デフォルトはcars

        try (FileReader reader = new FileReader(jsonFile)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            if (root.has("basic")) {
                JsonObject basic = root.getAsJsonObject("basic");
                if (basic.has("name"))   displayName = basic.get("name").getAsString();
                if (basic.has("width"))  width       = basic.get("width").getAsFloat();   // ← 追加
                if (basic.has("height")) height      = basic.get("height").getAsFloat();
                if (basic.has("type"))   type        = basic.get("type").getAsString();
            }

            // ここから追加: hitboxの読み込みです
            if (root.has("hitbox")) {
                JsonObject hitboxObj = root.getAsJsonObject("hitbox");

                if (hitboxObj.has("obbList")) {
                    JsonArray obbList = hitboxObj.getAsJsonArray("obbList");
                    List<HitboxDefinition> defs = new ArrayList<>();

                    for (int obbIndex = 0; obbIndex < obbList.size(); obbIndex++) {
                        JsonObject obb = obbList.get(obbIndex).getAsJsonObject();

                        Vec3 positionOffset = Vec3.ZERO;
                        if (obb.has("po")) {
                            JsonArray po = obb.getAsJsonArray("po");
                            if (po.size() >= 3) {
                                positionOffset = new Vec3(
                                        po.get(0).getAsDouble(),
                                        po.get(1).getAsDouble(),
                                        po.get(2).getAsDouble());
                            }
                        }

                        float obbYaw = 0f, obbPitch = 0f, obbRoll = 0f;
                        if (obb.has("ro")) {
                            JsonArray ro = obb.getAsJsonArray("ro");
                            if (ro.size() >= 3) {
                                obbYaw   = ro.get(0).getAsFloat();
                                obbPitch = ro.get(1).getAsFloat();
                                obbRoll  = ro.get(2).getAsFloat();
                            }
                        }

                        if (obb.has("vertices")) {
                            JsonArray vertArray = obb.getAsJsonArray("vertices");
                            if (vertArray.size() == 8) {
                                Vec3[] verts = new Vec3[8];
                                for (int i = 0; i < 8; i++) {
                                    JsonArray p = vertArray.get(i).getAsJsonArray();
                                    verts[i] = new Vec3(
                                            p.get(0).getAsDouble(),
                                            p.get(1).getAsDouble(),
                                            p.get(2).getAsDouble());
                                }
                                defs.add(new HitboxDefinition(verts, positionOffset, obbYaw, obbPitch, obbRoll));
                            } else {
                                LOGGER.warn("[HashiriyaCarMod] obbList[{}]のverticesは8個必要です: {}",
                                        obbIndex, jsonFile.getName());
                            }
                        }
                    }

                    if (!defs.isEmpty()) {
                        hitboxMap.put(baseName, defs);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("[HashiriyaCarMod] JSON読み込み失敗: {} / {}", jsonFile.getName(), e.getMessage());
            return;
        }


        if ("cars".equalsIgnoreCase(type)) {
            Map<String, ObjMesh> meshParts = ObjLoader.loadWithParts(objFile);
            AssetRegistry entry = new AssetRegistry(baseName, objFile, pngFile, meshParts);
            assetRegistryMap.put(baseName, entry);
        }

        if (!"cars".equalsIgnoreCase(type)) {
            if ("parts".equalsIgnoreCase(type)) {
                PartRegistry.register(baseName, objFile);
            } else {
                LOGGER.debug("[HashiriyaCarMod] type='{}' のためスキップ: {}", type, rawId);
            }
            return;
        }

        // ここから先は今まで通り（carsの場合のみ登録）
        final String finalDisplayName = displayName;
        final float  finalWidth       = width;    // ← 追加
        final float  finalHeight      = height;
        final String finalBaseName    = baseName;

        RegistryObject<EntityType<CarEntity>> entityType = ENTITY_TYPES.register(
                entityId,
                () -> EntityType.Builder
                        .<CarEntity>of((typeObj, level) -> new CarEntity(typeObj, level), MobCategory.MISC)
                        .sized(finalWidth, finalHeight)
                        .clientTrackingRange(128)
                        .build(HashiriyaCarMod.MOD_ID + ":" + entityId)
        );

        RegistryObject<Item> spawnItem = ITEMS.register(
                entityId + "_spawner",
                () -> new CarSpawnerItem(entityType, finalDisplayName, finalBaseName)
        );

        tabItems.get(tabId).add(spawnItem);
        LOGGER.info("[HashiriyaCarMod] 登録完了: {} (表示名: {})",
                entityId, finalDisplayName);
    }

    private static void logAssetBindings() {
        LOGGER.debug("========== [HashiriyaCarMod] アセット紐づけ結果 ==========");
        if (assetRegistryMap.isEmpty()) {
            LOGGER.debug("  紐づけされたエントリがありません。");
            LOGGER.debug("==========================================================");
            return;
        }
        for (AssetRegistry entry : assetRegistryMap.values()) {
            String objStatus = (entry.objFile != null)
                    ? "OK -> " + entry.objFile.getName() + " (" + entry.parts.size() + " パーツ)"
                    : "なし（.obj が見つかりませんでした）";
            String pngStatus = (entry.pngFile != null)
                    ? "OK -> " + entry.pngFile.getName()
                    : "なし（.png が見つかりませんでした）";
            LOGGER.debug("  [{}]  OBJ: {}  |  PNG: {}", entry.baseName, objStatus, pngStatus);
        }
        LOGGER.debug("==========================================================");
    }

    private static void registerTabs() {
        for (Map.Entry<String, List<RegistryObject<Item>>> entry : tabItems.entrySet()) {
            String fullTabId = entry.getKey();
            TABS.register(fullTabId, () -> CreativeModeTab.builder()
                    .title(Component.literal(fullTabId))
                    .icon(() -> entry.getValue().isEmpty()
                            ? new ItemStack(Items.BARRIER)
                            : new ItemStack(entry.getValue().get(0).get()))
                    .displayItems((params, output) -> { })
                    .build()
            );
        }
    }

    private static void addItemsToTabs(BuildCreativeModeTabContentsEvent event) {
        for (Map.Entry<String, List<RegistryObject<Item>>> entry : tabItems.entrySet()) {
            if (event.getTab() == TABS.getEntries().stream()
                    .filter(ro -> ro.getId().getPath().equals(entry.getKey()))
                    .findFirst()
                    .map(RegistryObject::get)
                    .orElse(null)) {
                for (RegistryObject<Item> item : entry.getValue()) {
                    event.accept(item.get());
                }
            }
        }
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            for (var entry : ENTITY_TYPES.getEntries()) {
                //noinspection unchecked
                EntityRenderers.register(
                        (EntityType<CarEntity>) entry.get(),
                        CarEntityRenderer::new
                );
            }
        });
    }

    private static String sanitizeId(String input) {
        return input.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
    }
}