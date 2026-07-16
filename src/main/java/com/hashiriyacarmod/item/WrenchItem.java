package com.hashiriyacarmod.item;

import com.hashiriyacarmod.CarCollisionUtil;
import com.hashiriyacarmod.CarEntity;
import com.hashiriyacarmod.client.WrenchGuiScreen;
import com.hashiriyacarmod.network.CarWrenchDataPacket;   // ← 追加
import com.hashiriyacarmod.network.ModNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import java.util.List;

/**
 * 車のパーツ調整に使うレンチアイテムです。
 */
public class WrenchItem extends Item {

    public WrenchItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide()) {
            // クライアント側はサーバーに要求を送るだけ（GUIはパケット受信後に開く）
            return InteractionResultHolder.consume(stack);
        }

        // サーバー側
        sendCarDataToClient((ServerPlayer) player);

        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("車のパーツを調整するレンチ"));
    }

    private void sendCarDataToClient(ServerPlayer player) {
        Vec3 eyePos = player.getEyePosition(1.0f);
        Vec3 lookDir = player.getViewVector(1.0f);
        double reach = player.isCreative() ? 5.0 : 4.5;
        Vec3 lineEnd = eyePos.add(lookDir.scale(reach));

        AABB searchArea = new AABB(eyePos, lineEnd).inflate(reach);
        List<Entity> nearbyEntities = player.level().getEntities(player, searchArea, e -> e instanceof CarEntity);

        for (Entity entity : nearbyEntities) {
            CarEntity car = (CarEntity) entity;
            var defs = car.getHitboxDefinitions();
            if (defs.isEmpty()) continue;

            boolean hit = false;
            for (Vec3[] worldVertices : car.getAllWorldHitboxVertices()) {
                if (CarCollisionUtil.lineIntersectsBox(eyePos, lineEnd, worldVertices)) {
                    hit = true;
                    break;
                }
            }
            if (!hit) continue;

            // === 両方のデータを取得 ===
            CompoundTag savedNbt = car.getSaveData();
            List<String> groups = car.getAllowedPartGroups();   // JSON由来

            // パケット送信（NBT + グループ情報）
            CarWrenchDataPacket packet = new CarWrenchDataPacket(car.getUUID(), savedNbt, groups);
            ModNetworking.sendToClient(player, packet);

            player.sendSystemMessage(Component.literal("§6[Server] 車データ（NBT + Groups）を送信しました"));
            break;
        }
    }
}