package com.hashiriyacarmod.network;

import com.hashiriyacarmod.CarEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class CarWrenchDataPacket {

    private final UUID carUUID;
    private final CompoundTag nbtData;

    public CarWrenchDataPacket(UUID carUUID, CompoundTag nbtData) {
        this.carUUID = carUUID;
        this.nbtData = nbtData;
    }

    // クライアント側で受信したときの処理
    public static void handle(CarWrenchDataPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Player player = ctx.get().getSender() == null ? net.minecraft.client.Minecraft.getInstance().player : null;
            if (player == null) return;

            // 受信完了のテスト用チャット
            player.sendSystemMessage(Component.literal("§a[Client] Wrenchデータを受信しました！ UUID: " + msg.carUUID));

            // 将来的にここでGUIにデータを渡す（WrenchGuiScreenを修正予定）
            com.hashiriyacarmod.client.WrenchGuiScreen.lastReceivedNbt = msg.nbtData;
            com.hashiriyacarmod.client.WrenchGuiScreen.lastReceivedCarUUID = msg.carUUID;

            // GUIを開く
            net.minecraft.client.Minecraft.getInstance().setScreen(new com.hashiriyacarmod.client.WrenchGuiScreen());
        });
        ctx.get().setPacketHandled(true);
    }

    // パケット書き込み
    public static void encode(CarWrenchDataPacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.carUUID);
        buf.writeNbt(msg.nbtData);
    }

    // パケット読み込み
    public static CarWrenchDataPacket decode(FriendlyByteBuf buf) {
        UUID uuid = buf.readUUID();
        CompoundTag nbt = buf.readNbt();
        return new CarWrenchDataPacket(uuid, nbt);
    }
}