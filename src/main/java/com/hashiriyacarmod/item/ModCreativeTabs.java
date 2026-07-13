package com.hashiriyacarmod.item;

import com.hashiriyacarmod.HashiriyaCarMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * このMod固定のアイテム(レンチなど)専用のクリエイティブタブです。
 * 車パックが動的に作るタブとは別に、常に存在する固定タブとして用意します。
 */
@Mod.EventBusSubscriber(modid = HashiriyaCarMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, HashiriyaCarMod.MOD_ID);

    public static final RegistryObject<CreativeModeTab> TOOLS_TAB = TABS.register(
            "hashiriyacarmod_tools",
            () -> CreativeModeTab.builder()
                    .title(Component.literal("Hashiriya Car Tools"))
                    .icon(() -> new ItemStack(ModItems.WRENCH.get()))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.WRENCH.get());
                    })
                    .build()
    );

    public static void register(IEventBus eventBus) {
        TABS.register(eventBus);
    }

    // BuildCreativeModeTabContentsEventでの追加登録が必要な場合はここに実装します。
    // 現状はdisplayItemsで直接指定しているため不要です。
    @SubscribeEvent
    public static void onBuildContents(BuildCreativeModeTabContentsEvent event) {
        // 予備:今後動的にアイテムを追加したくなった場合、ここで
        // event.getTab() == TOOLS_TAB.get() を判定して追加できます。
    }
}