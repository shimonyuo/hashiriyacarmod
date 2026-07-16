package com.hashiriyacarmod.network;

import com.hashiriyacarmod.client.WrenchGuiScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class CarWrenchDataPacket {

    private final UUID carUUID;
    private final CompoundTag nbtData;
    private final List<String> allowedGroups;

    // 3引数コンストラクタ（これを使う）
    public CarWrenchDataPacket(UUID carUUID, CompoundTag nbtData, List<String> allowedGroups) {
        this.carUUID = carUUID;
        this.nbtData = nbtData;
        this.allowedGroups = allowedGroups != null ? allowedGroups : List.of();
    }

    public static void handle(CarWrenchDataPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Player player = Minecraft.getInstance().player;
            if (player == null) return;

            player.sendSystemMessage(Component.literal("§a[Client] Wrenchデータを受信しました！ UUID: " + msg.carUUID));

            WrenchGuiScreen.lastReceivedNbt = msg.nbtData;
            WrenchGuiScreen.lastReceivedCarUUID = msg.carUUID;
            WrenchGuiScreen.lastReceivedGroups = msg.allowedGroups;

            Minecraft.getInstance().setScreen(new WrenchGuiScreen());
        });
        ctx.get().setPacketHandled(true);
    }

    public static void encode(CarWrenchDataPacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.carUUID);
        buf.writeNbt(msg.nbtData);
        buf.writeCollection(msg.allowedGroups, FriendlyByteBuf::writeUtf);
    }

    public static CarWrenchDataPacket decode(FriendlyByteBuf buf) {
        UUID uuid = buf.readUUID();
        CompoundTag nbt = buf.readNbt();
        List<String> groups = buf.readList(FriendlyByteBuf::readUtf);
        return new CarWrenchDataPacket(uuid, nbt, groups);
    }
}