package com.hashiriyacarmod.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class CarWrenchDataPacket {

    private final UUID carUUID;
    private final CompoundTag nbtData;
    private final List<String> allowedGroups;

    public CarWrenchDataPacket(UUID carUUID, CompoundTag nbtData, List<String> allowedGroups) {
        this.carUUID = carUUID;
        this.nbtData = nbtData;
        this.allowedGroups = allowedGroups != null ? allowedGroups : List.of();
    }

    public static void handle(CarWrenchDataPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // クライアント側でのみ実行する
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleWrenchData(msg));
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

    // ゲッター（ClientPacketHandler から使う）
    public UUID getCarUUID() { return carUUID; }
    public CompoundTag getNbtData() { return nbtData; }
    public List<String> getAllowedGroups() { return allowedGroups; }
}