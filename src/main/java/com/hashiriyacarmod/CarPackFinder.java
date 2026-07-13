package com.hashiriyacarmod;

import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.File;
import java.nio.file.Paths;
import java.util.Optional;

@Mod.EventBusSubscriber(modid = HashiriyaCarMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class CarPackFinder {

    @SubscribeEvent
    public static void onAddPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.CLIENT_RESOURCES) return;

        File packRootDir = Paths.get("hashiriyacarpack").toFile();
        if (!packRootDir.exists() || !packRootDir.isDirectory()) return;

        File[] packDirs = packRootDir.listFiles(File::isDirectory);
        if (packDirs == null) return;

        for (File packDir : packDirs) {
            registerPack(event, packDir);
        }
    }

    private static void registerPack(AddPackFindersEvent event, File packDir) {
        String packId = "hashiriyacarmod_" + packDir.getName().toLowerCase();

        event.addRepositorySource((infoConsumer) -> {
            Pack pack = Pack.readMetaAndCreate(
                    packId,
                    net.minecraft.network.chat.Component.literal(packDir.getName()),
                    true,  // required=true で自動有効化
                    (name) -> new PathPackResources(name, packDir.toPath(), false),
                    PackType.CLIENT_RESOURCES,
                    Pack.Position.TOP,
                    PackSource.DEFAULT
            );
            if (pack != null) {
                infoConsumer.accept(pack);
            }
        });
    }
}