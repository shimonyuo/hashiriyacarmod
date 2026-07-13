package com.hashiriyacarmod.item;

import com.hashiriyacarmod.CarCollisionUtil;
import com.hashiriyacarmod.CarEntity;
import com.hashiriyacarmod.client.WrenchGuiScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
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
 * 右クリックすると、プレイヤーの視線方向にリーチ距離分の線を伸ばし、
 * 既存の台形型OBB(HitboxDefinition)と交差しているかを調べます。
 *
 * 判定はクライアント側だけで行い、サーバーとのパケット通信は行いません。
 * これにより、シングルプレイ・マルチプレイ両方で動作し、
 * リプレイ再生中のようなサーバーが存在しない環境でも正しく動作します。
 */
public class WrenchItem extends Item {

    public WrenchItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // ── クライアント側だけで判定・GUIを開きます ──
        if (level.isClientSide()) {
            checkAndOpenGui(player);
        }

        return InteractionResultHolder.consume(stack);
    }

    /**
     * クライアント側でOBB判定を行い、検知したらGUIを開きます。
     */
    @OnlyIn(Dist.CLIENT)
    private static void checkAndOpenGui(Player player) {

        // ── ①視線の始点・終点を計算します ──
        Vec3 eyePos = player.getEyePosition(1.0f);
        Vec3 lookDir = player.getViewVector(1.0f);

        // Minecraft標準のリーチ距離を使います
        // (クリエイティブなら5.0、それ以外は4.5が標準です)
        double reach = player.isCreative() ? 5.0 : 4.5;

        Vec3 lineEnd = eyePos.add(lookDir.scale(reach));

        // ── ②周囲の車エンティティを探索します ──
        AABB searchArea = new AABB(eyePos, lineEnd).inflate(reach);
        List<Entity> nearbyEntities = player.level().getEntities(player, searchArea, e -> e instanceof CarEntity);

        boolean detected = false;

        for (Entity entity : nearbyEntities) {
            CarEntity car = (CarEntity) entity;
            var defs = car.getHitboxDefinitions();
            if (defs.isEmpty()) continue;

            for (Vec3[] worldVertices : car.getAllWorldHitboxVertices()) {
                if (CarCollisionUtil.lineIntersectsBox(eyePos, lineEnd, worldVertices)) {
                    detected = true;
                    break;
                }
            }
            if (detected) break;
        }

        // ── ③検知したらGUIを開きます ──
        if (detected) {
            Minecraft.getInstance().setScreen(new WrenchGuiScreen());
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("車のパーツを調整するレンチ"));
    }
}