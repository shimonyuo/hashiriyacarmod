package com.hashiriyacarmod.command;

import com.hashiriyacarmod.AssetRegistry;
import com.hashiriyacarmod.CarPackLoader;
import com.hashiriyacarmod.HashiriyaCarMod;
import com.hashiriyacarmod.parts.PartAttachmentIndex;
import com.hashiriyacarmod.parts.PartRegistry;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = HashiriyaCarMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class HashiriyaCommand {

    private HashiriyaCommand() {}

    /** Tab: registered part baseNames */
    private static final SuggestionProvider<CommandSourceStack> SUGGEST_PARTS =
            (ctx, builder) -> SharedSuggestionProvider.suggest(
                    PartRegistry.getAllBaseNames(), builder);

    /** Tab: registered car baseNames */
    private static final SuggestionProvider<CommandSourceStack> SUGGEST_CARS =
            (ctx, builder) -> {
                List<String> names = new ArrayList<>();
                for (AssetRegistry reg : CarPackLoader.getAllAssetRegistries()) {
                    if (reg != null && reg.baseName != null && !reg.baseName.isEmpty()) {
                        names.add(reg.baseName);
                    }
                }
                return SharedSuggestionProvider.suggest(names, builder);
            };

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();

        d.register(
                Commands.literal("hashiriyacommand")
                        .then(Commands.literal("search")
                                .then(Commands.argument("part", StringArgumentType.string())
                                        .suggests(SUGGEST_PARTS)
                                        .executes(ctx -> runSearch(ctx, false))
                                        .then(Commands.argument("car", StringArgumentType.string())
                                                .suggests(SUGGEST_CARS)
                                                .executes(ctx -> runSearch(ctx, true)))))
        );
    }

    private static int runSearch(CommandContext<CommandSourceStack> ctx, boolean hasCar) {
        String part = StringArgumentType.getString(ctx, "part");
        String car = hasCar ? StringArgumentType.getString(ctx, "car") : null;

        List<String> lines = PartAttachmentIndex.searchLines(part, car);
        CommandSourceStack source = ctx.getSource();
        for (String line : lines) {
            source.sendSuccess(() -> Component.literal(line), false);
        }
        return lines.size() == 1 && "nothing".equals(lines.get(0)) ? 0 : lines.size();
    }
}