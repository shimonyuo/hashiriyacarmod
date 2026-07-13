package com.hashiriyacarmod;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;

public class CarSpawnerItem extends Item {

    private final RegistryObject<EntityType<CarEntity>> entityTypeRef;
    private final String displayName;
    private final String baseName; // スポーン時にエンティティへ渡す

    public CarSpawnerItem(RegistryObject<EntityType<CarEntity>> entityTypeRef,
                          String displayName,
                          String baseName) {
        super(new Item.Properties());
        this.entityTypeRef = entityTypeRef;
        this.displayName   = displayName;
        this.baseName      = baseName;
    }

    @Override
    public net.minecraft.world.InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) return net.minecraft.world.InteractionResult.SUCCESS;

        // ① 「右クリックで実際にカーソルが当たった正確な座標」を取得します
        //    （ブロックの角ではなく、クリックした面の実際の位置）
        Vec3 hitPos = context.getClickLocation();

        // ② 右クリックしたプレイヤーを取得します
        Player player = context.getPlayer();

        // ③ プレイヤーが向いている角度（ヨー）を取得します
        //    プレイヤーがいない場合は保険として0（北向き）にします
        float playerYaw = (player != null) ? player.getYRot() : 0f;

        CarEntity entity = new CarEntity(entityTypeRef.get(), level);

        // ④ クリックした地点そのものに、プレイヤーの向きでスポーンさせます
        entity.moveTo(hitPos.x, hitPos.y, hitPos.z, playerYaw, 0f);

        entity.setYRot(entity.getYRot() + 0f);
        entity.setCarPitch(0f);
        entity.setCarRoll(0f);

        entity.setBaseName(baseName); // どの車かを記録
        level.addFreshEntity(entity);

        return net.minecraft.world.InteractionResult.SUCCESS;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal(displayName);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("右クリックでスポーン"));
    }
}