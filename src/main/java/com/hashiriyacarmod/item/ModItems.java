package com.hashiriyacarmod.item;

import com.hashiriyacarmod.HashiriyaCarMod;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * このMod専用の固定アイテム(車パックから動的生成されるスポナーアイテムとは別枠)を登録します。
 */
public class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, HashiriyaCarMod.MOD_ID);

    /** 車のパーツ調整(オフセット・アニメーション設定など)に使うレンチです。 */
    public static final RegistryObject<Item> WRENCH = ITEMS.register(
            "wrench",
            () -> new WrenchItem(new Item.Properties().stacksTo(1))
    );

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}