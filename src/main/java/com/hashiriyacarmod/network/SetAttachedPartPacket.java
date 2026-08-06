package com.hashiriyacarmod.network;

import com.hashiriyacarmod.CarEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/** クライアント → サーバー：指定 group にパーツを付ける / NONE で外す */
public class SetAttachedPartPacket {

    private final UUID carUUID;
    private final String groupName;
    /** NONE またはパーツ baseName */
    private final String partBaseName;

    public SetAttachedPartPacket(UUID carUUID, String groupName, String partBaseName) {
        this.carUUID = carUUID;
        this.groupName = groupName != null ? groupName : "";
        this.partBaseName = partBaseName != null ? partBaseName : "NONE";
    }

    public static void encode(SetAttachedPartPacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.carUUID);
        buf.writeUtf(msg.groupName);
        buf.writeUtf(msg.partBaseName);
    }

    public static SetAttachedPartPacket decode(FriendlyByteBuf buf) {
        return new SetAttachedPartPacket(buf.readUUID(), buf.readUtf(), buf.readUtf());
    }

    public static void handle(SetAttachedPartPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            Entity entity = player.serverLevel().getEntity(msg.carUUID);
            if (!(entity instanceof CarEntity car)) return;

            // 必要なら距離チェックを追加
            car.setAttachedPartForGroup(msg.groupName, msg.partBaseName);
        });
        ctx.get().setPacketHandled(true);
    }
}