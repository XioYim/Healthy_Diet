package com.xioyim.healthy_diet.common.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.xioyim.healthy_diet.common.config.ConfigManager;
import com.xioyim.healthy_diet.common.config.ItemConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

public class HeaDietSetEffectCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("HeaDietSetEffect")
                .requires(src -> src.hasPermission(4))
                // /HeaDietSetEffect hand <effect> <level> <durationSecs> [showParticles]
                .then(Commands.literal("hand")
                        .then(Commands.argument("effect", ResourceLocationArgument.id())
                                .suggests((ctx, b) -> {
                                    ForgeRegistries.MOB_EFFECTS.getKeys().stream().map(ResourceLocation::toString).forEach(b::suggest);
                                    return b.buildFuture();
                                })
                                .then(Commands.argument("level", IntegerArgumentType.integer(0, 255))
                                        .then(Commands.argument("durationSecs", IntegerArgumentType.integer(1))
                                                .executes(ctx -> execute(ctx, false, false))
                                                .then(Commands.argument("showParticles", BoolArgumentType.bool())
                                                        .executes(ctx -> execute(ctx, false, true)))))))
                // /HeaDietSetEffect <itemOrBlockId> <effect> <level> <durationSecs> [showParticles]
                .then(Commands.argument("targetId", ResourceLocationArgument.id())
                        .suggests((ctx, b) -> {
                            ForgeRegistries.ITEMS.getKeys().stream().map(ResourceLocation::toString).forEach(b::suggest);
                            ForgeRegistries.BLOCKS.getKeys().stream().map(ResourceLocation::toString).forEach(b::suggest);
                            return b.buildFuture();
                        })
                        .then(Commands.argument("effect", ResourceLocationArgument.id())
                                .suggests((ctx, b) -> {
                                    ForgeRegistries.MOB_EFFECTS.getKeys().stream().map(ResourceLocation::toString).forEach(b::suggest);
                                    return b.buildFuture();
                                })
                                .then(Commands.argument("level", IntegerArgumentType.integer(0, 255))
                                        .then(Commands.argument("durationSecs", IntegerArgumentType.integer(1))
                                                .executes(ctx -> execute(ctx, true, false))
                                                .then(Commands.argument("showParticles", BoolArgumentType.bool())
                                                        .executes(ctx -> execute(ctx, true, true)))))))
        );
    }

    private static int execute(CommandContext<CommandSourceStack> ctx, boolean hasExplicitId, boolean hasParticles) throws CommandSyntaxException {
        ResourceLocation effectRl = ResourceLocationArgument.getId(ctx, "effect");
        // 用户直接输入游戏内部等级（0 = Lv.1，1 = Lv.2…），不再偏移
        int level = IntegerArgumentType.getInteger(ctx, "level");
        int durationSecs = IntegerArgumentType.getInteger(ctx, "durationSecs");
        boolean showParticles = hasParticles && BoolArgumentType.getBool(ctx, "showParticles");

        if (ForgeRegistries.MOB_EFFECTS.getValue(effectRl) == null) {
            ctx.getSource().sendFailure(Component.translatable("command.healthy_diet.error.effect_not_found", effectRl.toString()));
            return 0;
        }

        String itemIdStr = HeaDietSetCommand.resolveTargetId(ctx, hasExplicitId);
        if (itemIdStr == null) return 0;

        ItemConfig config = ConfigManager.getItemConfig(itemIdStr);
        if (config == null) config = new ItemConfig();

        ItemConfig.EffectEntry entry = new ItemConfig.EffectEntry();
        entry.effect = effectRl.toString();
        entry.level = level;
        entry.duration = durationSecs * 20;
        entry.showParticles = showParticles;
        config.effects.add(entry);
        ConfigManager.saveItemConfig(itemIdStr, config);

        final String finalId = itemIdStr;
        ctx.getSource().sendSuccess(() -> Component.translatable("command.healthy_diet.set_effect.result",
                finalId, effectRl.toString(), level + 1), true);
        return 1;
    }
}
