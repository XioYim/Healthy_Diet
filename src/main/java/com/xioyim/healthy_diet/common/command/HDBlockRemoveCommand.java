package com.xioyim.healthy_diet.common.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.xioyim.healthy_diet.common.config.BlockConfig;
import com.xioyim.healthy_diet.common.config.ConfigManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * /HDBlockRemove <blockId> effect|cmd|group <index>
 * OP 专用：删除方块右键配置中的指定条目（由 /HDBlockList 的 [X] 按钮调用）。
 */
public class HDBlockRemoveCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("HDBlockRemove")
                .requires(src -> src.hasPermission(4))
                .then(Commands.argument("blockId", ResourceLocationArgument.id())
                        .suggests((ctx, b) -> {
                            ConfigManager.getBlockConfigKeys().forEach(b::suggest);
                            return b.buildFuture();
                        })
                        // effect <index>
                        .then(Commands.literal("effect")
                                .then(Commands.argument("index", IntegerArgumentType.integer(0))
                                        .executes(ctx -> removeEffect(ctx))))
                        // cmd <index>
                        .then(Commands.literal("cmd")
                                .then(Commands.argument("index", IntegerArgumentType.integer(0))
                                        .executes(ctx -> removeCmd(ctx))))
                        // group <index>
                        .then(Commands.literal("group")
                                .then(Commands.argument("index", IntegerArgumentType.integer(0))
                                        .executes(ctx -> removeGroup(ctx)))))
        );
    }

    private static BlockConfig getConfig(CommandContext<CommandSourceStack> ctx)
            throws CommandSyntaxException {
        ResourceLocation blockRl = ResourceLocationArgument.getId(ctx, "blockId");
        return ConfigManager.getBlockConfig(blockRl.toString());
    }

    private static int removeEffect(CommandContext<CommandSourceStack> ctx)
            throws CommandSyntaxException {
        String blockId = ResourceLocationArgument.getId(ctx, "blockId").toString();
        int index = IntegerArgumentType.getInteger(ctx, "index");
        BlockConfig config = ConfigManager.getBlockConfig(blockId);
        if (config == null || index >= config.effects.size()) {
            ctx.getSource().sendFailure(Component.translatable("command.healthy_diet.remove.not_found"));
            return 0;
        }
        String removed = config.effects.get(index).effect;
        config.effects.remove(index);
        ConfigManager.saveBlockConfig(blockId, config);
        ctx.getSource().sendSuccess(() -> Component.translatable(
                "command.healthy_diet.hdblock.remove.effect", blockId, index, removed), true);
        return 1;
    }

    private static int removeCmd(CommandContext<CommandSourceStack> ctx)
            throws CommandSyntaxException {
        String blockId = ResourceLocationArgument.getId(ctx, "blockId").toString();
        int index = IntegerArgumentType.getInteger(ctx, "index");
        BlockConfig config = ConfigManager.getBlockConfig(blockId);
        if (config == null || index >= config.onClickCommands.size()) {
            ctx.getSource().sendFailure(Component.translatable("command.healthy_diet.remove.not_found"));
            return 0;
        }
        String removed = config.onClickCommands.get(index);
        config.onClickCommands.remove(index);
        ConfigManager.saveBlockConfig(blockId, config);
        ctx.getSource().sendSuccess(() -> Component.translatable(
                "command.healthy_diet.hdblock.remove.cmd", blockId, index, removed), true);
        return 1;
    }

    private static int removeGroup(CommandContext<CommandSourceStack> ctx)
            throws CommandSyntaxException {
        String blockId = ResourceLocationArgument.getId(ctx, "blockId").toString();
        int index = IntegerArgumentType.getInteger(ctx, "index");
        BlockConfig config = ConfigManager.getBlockConfig(blockId);
        if (config == null || index >= config.nutritionChanges.size()) {
            ctx.getSource().sendFailure(Component.translatable("command.healthy_diet.remove.not_found"));
            return 0;
        }
        String group = config.nutritionChanges.get(index).group;
        config.nutritionChanges.remove(index);
        ConfigManager.saveBlockConfig(blockId, config);
        ctx.getSource().sendSuccess(() -> Component.translatable(
                "command.healthy_diet.hdblock.remove.group", blockId, index, group), true);
        return 1;
    }
}
