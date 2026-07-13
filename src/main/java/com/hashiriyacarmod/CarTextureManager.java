package com.hashiriyacarmod;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.io.File;

/**
 * 外部パックのPNGをリソースパック経由でアトラスに乗せる。
 * DynamicTextureは使わない（影MODのgl_TextureMatrix[0]と非互換のため）。
 *
 * パス構造：
 * hashiriyacarpack/{パック名}/assets/hashiriyacarmod/textures/{任意のパス}/{baseName}.png
 * → ResourceLocation: hashiriyacarmod:textures/{任意のパス}/{baseName}.png
 */
@OnlyIn(Dist.CLIENT)
public class CarTextureManager {

    /**
     * pngFileのパスから hashiriyacarpack/{パック名}/assets/hashiriyacarmod/ 以降を抜き出し、
     * ResourceLocation に変換して返す。
     * DynamicTextureへの登録は一切行わない。
     */
    public static ResourceLocation getOrLoad(String baseName, File pngFile) {
        if (pngFile == null || !pngFile.exists()) return null;

        // pngFileの絶対パスから "assets/hashiriyacarmod/" 以降を抽出
        String absPath = pngFile.getAbsolutePath().replace('\\', '/');
        String marker = "assets/hashiriyacarmod/";
        int idx = absPath.indexOf(marker);
        if (idx < 0) return null;

        // "textures/xxx/yyy.png" の部分
        String relativePath = absPath.substring(idx + marker.length());

        // ResourceLocation のパス部分（拡張子を除く）
        // 例: textures/car_tab/test3.png → hashiriyacarmod:textures/car_tab/test3.png
        // ※ ResourceLocation にはテクスチャの場合拡張子込みで渡す
        return new ResourceLocation(HashiriyaCarMod.MOD_ID, relativePath);
    }
}