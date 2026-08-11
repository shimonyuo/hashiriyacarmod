package com.hashiriyacarmod;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.RegistryObject;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CarSpawnerItem extends Item {

    private final RegistryObject<EntityType<CarEntity>> entityTypeRef;
    private final Map<String, String> displayNames;
    private final String baseName;

    public CarSpawnerItem(RegistryObject<EntityType<CarEntity>> entityTypeRef,
                          Map<String, String> displayNames,
                          String baseName) {
        super(new Item.Properties());
        this.entityTypeRef = entityTypeRef;
        this.displayNames = displayNames != null
                ? Map.copyOf(displayNames)
                : Map.of("default", baseName != null ? baseName : "");
        this.baseName = baseName;
    }

    /** 旧コンストラクタ互換（文字列1本＝default） */
    public CarSpawnerItem(RegistryObject<EntityType<CarEntity>> entityTypeRef,
                          String displayName,
                          String baseName) {
        this(entityTypeRef, singleDefault(displayName, baseName), baseName);
    }

    private static Map<String, String> singleDefault(String displayName, String baseName) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("default", displayName != null && !displayName.isEmpty()
                ? displayName
                : (baseName != null ? baseName : ""));
        return m;
    }

    @Override
    public net.minecraft.world.InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) return net.minecraft.world.InteractionResult.SUCCESS;

        Vec3 hitPos = context.getClickLocation();
        Player player = context.getPlayer();
        float playerYaw = (player != null) ? player.getYRot() : 0f;

        CarEntity entity = new CarEntity(entityTypeRef.get(), level);
        entity.moveTo(hitPos.x, hitPos.y, hitPos.z, playerYaw, 0f);
        entity.setYRot(entity.getYRot() + 0f);
        entity.setCarPitch(0f);
        entity.setCarRoll(0f);
        entity.setBaseName(baseName);
        com.hashiriyacarmod.parts.DefaultAttachedPartsApplier.applyDefaultsIfEmpty(entity);
        level.addFreshEntity(entity);

        return net.minecraft.world.InteractionResult.SUCCESS;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal(resolveDisplayName());
    }

    private String resolveDisplayName() {
        String lang = "default";
        if (FMLEnvironment.dist == Dist.CLIENT) {
            try {
                lang = Minecraft.getInstance().getLanguageManager().getSelected();
            } catch (Exception ignored) {
                lang = "default";
            }
        }
        if (lang != null) {
            String key = lang.toLowerCase(Locale.ROOT);
            String specific = displayNames.get(key);
            if (specific != null && !specific.isEmpty()) {
                return specific;
            }
        }
        String def = displayNames.get("default");
        if (def != null && !def.isEmpty()) {
            return def;
        }
        return baseName != null ? baseName : "";
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("右クリックでスポーン"));
    }
}