package com.hashiriyacarmod.network;

import com.hashiriyacarmod.HashiriyaCarMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModNetworking {

    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(HashiriyaCarMod.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void register() {
        int id = 0;

        INSTANCE.registerMessage(
                id++,
                CarWrenchDataPacket.class,
                CarWrenchDataPacket::encode,
                CarWrenchDataPacket::decode,
                CarWrenchDataPacket::handle
                // NetworkDirectionは第6引数でOptionalで渡す必要があるバージョンもあるため省略
        );
    }

    public static void sendToClient(ServerPlayer player, Object packet) {
        INSTANCE.sendTo(packet, player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }
}