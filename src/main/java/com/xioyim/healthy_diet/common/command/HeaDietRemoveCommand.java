package com.xioyim.healthy_diet.common.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.xioyim.healthy_diet.common.config.ConfigManager;
import com.xioyim.healthy_diet.common.config.ItemConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;

public class HeaDietRemoveCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("HeaDietRemove")
                .requires(src -> src.hasPermission(4))
                .then(Commands.argument("targetId", ResourceLocationArgument.id())
                        .suggests((ctx, b) -> {
                            ConfigManager.getItemConfigKeys().forEach(b::suggest);
                            return b.buildFuture();
                        })
                        // /HeaDietRemove <id> nutrition <group>
                        .then(Commands.literal("nutrition")
                                .then(Commands.argument("group", StringArgumentType.word())
                                        .suggests((ctx, b) -> {
                                            ConfigManager.getGroups().keySet().forEach(b::suggest);
                                            return b.buildFuture();
                                        })
                                        .executes(ctx -> removeNutrition(ctx))))
                        // /HeaDietRemove <id> effect <index>
                        .then(Commands.literal("effect")
                                .then(Commands.argument("index", IntegerArgumentType.integer(0))
                                        .executes(ctx -> removeEffect(ctx))))
                        // /HeaDietRemove <id> command <index>
                        .then(Commands.literal("command")
                                .then(Commands.argument("index", IntegerArgumentType.integer(0))
                                        .executes(ctx -> removeCommand(ctx))))
                        // /HeaDietRemove <id> food
                        .then(Commands.literal("food")
                                .executes(ctx -> removeFood(ctx)))
                        // /HeaDietRemove <id> condcmd <index>
                        .then(Commands.literal("condcmd")
                                .then(Commands.argument("index", IntegerArgumentType.integer(0))
                                        .executes(ctx -> removeCondCmd(ctx)))))
        );
    }

    private static int removeNutrition(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String id = ResourceLocationArgument.getId(ctx, "targetId").toString();
        String group = StringArgumentType.getString(ctx, "group");

        ItemConfig config = ConfigManager.getItemConfig(id);
        if (config == null || !config.nutrition.containsKey(group)) {
            ctx.getSource().sendFailure(Component.translatable("command.healthy_diet.remove.not_found"));
            return 0;
        }
        config.nutrition.remove(group);
        ConfigManager.saveItemConfig(id, config);
        ctx.getSource().sendSuccess(() -> Component.translatable("command.healthy_diet.remove.nutrition", id, group), true);
        return 1;
    }

    private static int removeEffect(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String id = ResourceLocationArgument.getId(ctx, "targetId").toString();
        int index = IntegerArgumentType.getInteger(ctx, "index");

        ItemConfig config = ConfigManager.getItemConfig(id);
        if (config == null || index < 0 || index >= config.effects.size()) {
            ctx.getSource().sendFailure(Component.translatable("command.healthy_diet.remove.not_found"));
            return 0;
        }
        String removed = config.effects.get(index).effect;
        config.effects.remove(index);
        ConfigManager.saveItemConfig(id, config);
        ctx.getSource().sendSuccess(() -> Component.translatable("command.healthy_diet.remove.effect", id, removed), true);
        return 1;
    }

    private static int removeCommand(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String id = ResourceLocationArgument.getId(ctx, "targetId").toString();
        int index = IntegerArgumentType.getInteger(ctx, "index");

        ItemConfig config = ConfigManager.getItemConfig(id);
        if (config == null || index < 0 || index >= config.onEatCommands.size()) {
            ctx.getSource().sendFailure(Component.translatable("command.healthy_diet.remove.not_found"));
            return 0;
        }
        String removed = config.onEatCommands.get(index);
        config.onEatCommands.remove(index);
        ConfigManager.saveItemConfig(id, config);
        ctx.getSource().sendSuccess(() -> Component.translatable("command.healthy_diet.remove.command", id, removed), true);
        return 1;
    }

    private static int removeFood(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String id = ResourceLocationArgument.getId(ctx, "targetId").toString();

        ItemConfig config = ConfigManager.getItemConfig(id);
        if (config == null || config.foodCooldown == null) {
            ctx.getSource().sendFailure(Component.translatable("command.healthy_diet.remove.not_found"));
            return 0;
        }
        config.foodCooldown = null;
        ConfigManager.saveItemConfig(id, config);
        ctx.getSource().sendSuccess(() -> Component.translatable("command.healthy_diet.remove.food", id), true);
        return 1;
    }

    private static int removeCondCmd(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String id = ResourceLocationArgument.getId(ctx, "targetId").toString();
        int index = IntegerArgumentType.getInteger(ctx, "index");

        ItemConfig config = ConfigManager.getItemConfig(id);
        if (config == null || index < 0 || index >= config.conditionalCommands.size()) {
            ctx.getSource().sendFailure(Component.translatable("command.healthy_diet.remove.not_found"));
            return 0;
        }
        String removed = config.conditionalCommands.get(index).command;
        config.conditionalCommands.remove(index);
        ConfigManager.saveItemConfig(id, config);
        final int finalIndex = index;
        ctx.getSource().sendSuccess(() -> Component.translatable(
                "command.healthy_diet.remove.condcmd", id, finalIndex, removed), true);
        return 1;
    }
}
