package com.hashiriyacarmod.network;

import com.hashiriyacarmod.CarEntity;
import com.hashiriyacarmod.parts.DefaultAttachedPartsApplier;
import com.hashiriyacarmod.parts.PartRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/** クライアント → サーバー：指定 path + group にパーツを付ける / NONE で外す */
public class SetAttachedPartPacket {

    private final UUID carUUID;
    private final List<String> pathTokens;
    private final String groupName;
    /** NONE またはパーツ baseName */
    private final String partBaseName;

    public SetAttachedPartPacket(UUID carUUID, List<String> pathTokens,
                                 String groupName, String partBaseName) {
        this.carUUID = carUUID;
        this.pathTokens = pathTokens != null ? List.copyOf(pathTokens) : List.of();
        this.groupName = groupName != null ? groupName : "";
        this.partBaseName = partBaseName != null ? partBaseName : "NONE";
    }

    /** ルート専用の旧呼び出し用 */
    public SetAttachedPartPacket(UUID carUUID, String groupName, String partBaseName) {
        this(carUUID, List.of(), groupName, partBaseName);
    }

    public static void encode(SetAttachedPartPacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.carUUID);
        buf.writeCollection(msg.pathTokens, FriendlyByteBuf::writeUtf);
        buf.writeUtf(msg.groupName);
        buf.writeUtf(msg.partBaseName);
    }

    public static SetAttachedPartPacket decode(FriendlyByteBuf buf) {
        UUID uuid = buf.readUUID();
        List<String> path = buf.readList(FriendlyByteBuf::readUtf);
        String group = buf.readUtf();
        String part = buf.readUtf();
        return new SetAttachedPartPacket(uuid, path, group, part);
    }

    public static void handle(SetAttachedPartPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            Entity entity = player.serverLevel().getEntity(msg.carUUID);
            if (!(entity instanceof CarEntity car)) return;

            car.setAttachedPartInTree(msg.pathTokens, msg.groupName, msg.partBaseName);

            // NONE で外したときは子 default 不要
            if (msg.partBaseName == null
                    || msg.partBaseName.isBlank()
                    || "NONE".equalsIgnoreCase(msg.partBaseName)) {
                return;
            }

            // 今付けたノードのトークンを path 先から探す
            String token = findAttachedToken(car, msg.pathTokens, msg.groupName, msg.partBaseName);
            if (token == null) return;

            List<String> childPath = new ArrayList<>(msg.pathTokens);
            childPath.add(token);

            // そのパーツ JSON の parts[].default を再帰適用
            DefaultAttachedPartsApplier.applyPlacements(
                    car,
                    childPath,
                    PartRegistry.getPartPlacements(msg.partBaseName.trim())
            );
        });
        ctx.get().setPacketHandled(true);
    }

    private static String findAttachedToken(CarEntity car, List<String> path,
                                            String group, String expectedBase) {
        CompoundTag node = car.getAttachedPartsTree();
        if (path != null) {
            for (String t : path) {
                if (t == null || t.isBlank()) continue;
                if (!node.contains(t, CompoundTag.TAG_COMPOUND)) return null;
                node = node.getCompound(t);
            }
        }
        String prefer = expectedBase + ">" + group;
        if (node.contains(prefer)) return prefer;
        for (String key : node.getAllKeys()) {
            if (!CarEntity.attachedBaseName(key).equals(expectedBase)) continue;
            String seat = CarEntity.attachedSeatGroup(key);
            if (seat.isEmpty() || seat.equals(group)) return key;
        }
        return null;
    }
}