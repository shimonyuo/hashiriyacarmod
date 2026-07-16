package com.hashiriyacarmod;

import com.hashiriyacarmod.item.ModCreativeTabs;
import com.hashiriyacarmod.item.ModItems;
import com.hashiriyacarmod.network.ModNetworking;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(HashiriyaCarMod.MOD_ID)
public class HashiriyaCarMod {

    public static final String MOD_ID = "hashiriyacarmod";

    public HashiriyaCarMod() {
        var eventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // ★ 追加：専用シェーダーを登録します
        eventBus.addListener((net.minecraftforge.client.event.RegisterShadersEvent event) -> {
            try {
                event.registerShader(
                        new net.minecraft.client.renderer.ShaderInstance(
                                event.getResourceProvider(),
                                new ResourceLocation(MOD_ID, "car_entity"),
                                DefaultVertexFormat.NEW_ENTITY
                        ),
                        shader -> CarRenderTypes.setCarEntityShader(shader)
                );
            } catch (Exception e) {
                throw new RuntimeException("[HashiriyaCarMod] car_entityシェーダーの登録に失敗しました", e);
            }
        });

        ModItems.register(eventBus);
        ModCreativeTabs.register(eventBus);
        CarPackLoader.initialize(FMLJavaModLoadingContext.get());
        ModNetworking.register();
    }
}
