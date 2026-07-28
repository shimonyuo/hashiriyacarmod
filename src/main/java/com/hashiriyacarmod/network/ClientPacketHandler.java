package com.hashiriyacarmod.network;

import com.hashiriyacarmod.client.WrenchGuiScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ClientPacketHandler {

    public static void handleWrenchData(CarWrenchDataPacket msg) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        player.sendSystemMessage(Component.literal("§a[Client] Wrenchデータを受信しました！ UUID: " + msg.getCarUUID()));

        WrenchGuiScreen.lastReceivedNbt = msg.getNbtData();
        WrenchGuiScreen.lastReceivedCarUUID = msg.getCarUUID();
        WrenchGuiScreen.lastReceivedGroups = msg.getAllowedGroups();

        Minecraft.getInstance().setScreen(new WrenchGuiScreen());
    }
}