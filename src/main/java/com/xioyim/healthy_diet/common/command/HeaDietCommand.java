package com.xioyim.healthy_diet.common.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.xioyim.healthy_diet.common.capability.NutritionCapability;
import com.xioyim.healthy_diet.common.config.ConfigManager;
import com.xioyim.healthy_diet.common.network.HealthyDietNetwork;
import com.xioyim.healthy_diet.common.network.packet.SPacketOpenScreen;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class HeaDietCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("headiet")
                .executes(ctx -> sendUsage(ctx.getSource()))
                .then(Commands.literal("open")
                        .executes(ctx -> openSelf(ctx))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> openPlayer(ctx))))
                .then(Commands.literal("get")
                        .requires(src -> src.hasPermission(4))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("group", StringArgumentType.word())
                                        .suggests((ctx, b) -> { ConfigManager.getGroups().keySet().forEach(b::suggest); return b.buildFuture(); })
                                        .executes(ctx -> getValue(ctx)))))
                .then(Commands.literal("set")
                        .requires(src -> src.hasPermission(4))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("group", StringArgumentType.word())
                                        .suggests((ctx, b) -> { b.suggest("all"); ConfigManager.getGroups().keySet().forEach(b::suggest); return b.buildFuture(); })
                                        .then(Commands.argument("value", FloatArgumentType.floatArg(0, 100))
                                                .executes(ctx -> setValue(ctx))))))
                .then(Commands.literal("add")
                        .requires(src -> src.hasPermission(4))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("group", StringArgumentType.word())
                                        .suggests((ctx, b) -> { ConfigManager.getGroups().keySet().forEach(b::suggest); return b.buildFuture(); })
                                        .then(Commands.argument("value", FloatArgumentType.floatArg(0, 100))
                                                .executes(ctx -> addValue(ctx))))))
                .then(Commands.literal("subtract")
                        .requires(src -> src.hasPermission(4))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("group", StringArgumentType.word())
                                        .suggests((ctx, b) -> { ConfigManager.getGroups().keySet().forEach(b::suggest); return b.buildFuture(); })
                                        .then(Commands.argument("value", FloatArgumentType.floatArg(0, 100))
                                                .executes(ctx -> subtractValue(ctx))))))
                .then(Commands.literal("reset")
                        .requires(src -> src.hasPermission(4))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("group", StringArgumentType.word())
                                        .suggests((ctx, b) -> { ConfigManager.getGroups().keySet().forEach(b::suggest); return b.buildFuture(); })
                                        .executes(ctx -> resetValue(ctx)))))
                .then(Commands.literal("pause")
                        .requires(src -> src.hasPermission(4))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("group", StringArgumentType.word())
                                        .suggests((ctx, b) -> { b.suggest("all"); ConfigManager.getGroups().keySet().forEach(b::suggest); return b.buildFuture(); })
                                        .executes(ctx -> pauseGroup(ctx)))))
                .then(Commands.literal("resume")
                        .requires(src -> src.hasPermission(4))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("group", StringArgumentType.word())
                                        .suggests((ctx, b) -> { b.suggest("all"); ConfigManager.getGroups().keySet().forEach(b::suggest); return b.buildFuture(); })
                                        .executes(ctx -> resumeGroup(ctx)))))
                .then(Commands.literal("clear")
                        .requires(src -> src.hasPermission(4))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> clearAll(ctx))))
        );
    }

    private static int openSelf(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        HealthyDietNetwork.sendToPlayer(player, new SPacketOpenScreen());
        return 1;
    }

    private static int openPlayer(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        HealthyDietNetwork.sendToPlayer(target, new SPacketOpenScreen());
        return 1;
    }

    private static int getValue(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        String group = StringArgumentType.getString(ctx, "group");
        if (ConfigManager.getGroup(group) == null) {
            ctx.getSource().sendFailure(Component.translatable("command.healthy_diet.error.group_not_found", group));
            return 0;
        }
        float[] result = {0f};
        NutritionCapability.get(target).ifPresent(t -> result[0] = t.getValue(group));
        String displayName = ConfigManager.getGroup(group).displayName;
        ctx.getSource().sendSuccess(() -> Component.translatable("command.healthy_diet.get.result",
                target.getName(), displayName, (int) result[0] + "%"), false);
        return 1;
    }

    private static int setValue(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        String group = StringArgumentType.getString(ctx, "group");
        float value = FloatArgumentType.getFloat(ctx, "value");
        if (group.equals("all")) {
            NutritionCapability.get(target).ifPresent(t -> {
                ConfigManager.getGroups().keySet().forEach(g -> t.setValue(g, value));
                t.sync();
            });
            ctx.getSource().sendSuccess(() -> Component.translatable("command.healthy_diet.set.result",
                    target.getName(), "all", (int) value + "%"), true);
            return 1;
        }
        if (ConfigManager.getGroup(group) == null) {
            ctx.getSource().sendFailure(Component.translatable("command.healthy_diet.error.group_not_found", group));
            return 0;
        }
        NutritionCapability.get(target).ifPresent(t -> { t.setValue(group, value); t.sync(); });
        String displayName = ConfigManager.getGroup(group).displayName;
        ctx.getSource().sendSuccess(() -> Component.translatable("command.healthy_diet.set.result",
                target.getName(), displayName, (int) value + "%"), true);
        return 1;
    }

    private static int addValue(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        String group = StringArgumentType.getString(ctx, "group");
        float value = FloatArgumentType.getFloat(ctx, "value");
        if (ConfigManager.getGroup(group) == null) {
            ctx.getSource().sendFailure(Component.translatable("command.healthy_diet.error.group_not_found", group));
            return 0;
        }
        NutritionCapability.get(target).ifPresent(t -> { t.addValue(group, value); t.sync(); });
        String displayName = ConfigManager.getGroup(group).displayName;
        ctx.getSource().sendSuccess(() -> Component.translatable("command.healthy_diet.add.result",
                target.getName(), displayName, (int) value + "%"), true);
        return 1;
    }

    private static int subtractValue(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        String group = StringArgumentType.getString(ctx, "group");
        float value = FloatArgumentType.getFloat(ctx, "value");
        if (ConfigManager.getGroup(group) == null) {
            ctx.getSource().sendFailure(Component.translatable("command.healthy_diet.error.group_not_found", group));
            return 0;
        }
        NutritionCapability.get(target).ifPresent(t -> { t.subtractValue(group, value); t.sync(); });
        String displayName = ConfigManager.getGroup(group).displayName;
        ctx.getSource().sendSuccess(() -> Component.translatable("command.healthy_diet.subtract.result",
                target.getName(), displayName, (int) value + "%"), true);
        return 1;
    }

    private static int resetValue(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        String group = StringArgumentType.getString(ctx, "group");
        if (ConfigManager.getGroup(group) == null) {
            ctx.getSource().sendFailure(Component.translatable("command.healthy_diet.error.group_not_found", group));
            return 0;
        }
        NutritionCapability.get(target).ifPresent(t -> { t.resetValue(group); t.sync(); });
        String displayName = ConfigManager.getGroup(group).displayName;
        ctx.getSource().sendSuccess(() -> Component.translatable("command.healthy_diet.reset.result",
                target.getName(), displayName), true);
        return 1;
    }

    private static int pauseGroup(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        String group = StringArgumentType.getString(ctx, "group");
        if (group.equals("all")) {
            NutritionCapability.get(target).ifPresent(t -> {
                ConfigManager.getGroups().keySet().forEach(t::pauseGroup);
                t.sync();
            });
            ctx.getSource().sendSuccess(() -> Component.translatable("command.healthy_diet.pause.result",
                    target.getName(), "all"), true);
            return 1;
        }
        if (ConfigManager.getGroup(group) == null) {
            ctx.getSource().sendFailure(Component.translatable("command.healthy_diet.error.group_not_found", group));
            return 0;
        }
        NutritionCapability.get(target).ifPresent(t -> { t.pauseGroup(group); t.sync(); });
        String displayName = ConfigManager.getGroup(group).displayName;
        ctx.getSource().sendSuccess(() -> Component.translatable("command.healthy_diet.pause.result",
                target.getName(), displayName), true);
        return 1;
    }

    private static int resumeGroup(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        String group = StringArgumentType.getString(ctx, "group");
        if (group.equals("all")) {
            NutritionCapability.get(target).ifPresent(t -> {
                ConfigManager.getGroups().keySet().forEach(t::resumeGroup);
                t.sync();
            });
            ctx.getSource().sendSuccess(() -> Component.translatable("command.healthy_diet.resume.result",
                    target.getName(), "all"), true);
            return 1;
        }
        if (ConfigManager.getGroup(group) == null) {
            ctx.getSource().sendFailure(Component.translatable("command.healthy_diet.error.group_not_found", group));
            return 0;
        }
        NutritionCapability.get(target).ifPresent(t -> { t.resumeGroup(group); t.sync(); });
        String displayName = ConfigManager.getGroup(group).displayName;
        ctx.getSource().sendSuccess(() -> Component.translatable("command.healthy_diet.resume.result",
                target.getName(), displayName), true);
        return 1;
    }

    private static int clearAll(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        NutritionCapability.get(target).ifPresent(t -> { t.clearAll(); t.sync(); });
        ctx.getSource().sendSuccess(() -> Component.translatable("command.healthy_diet.clear.result",
                target.getName()), true);
        return 1;
    }

    private static int sendUsage(CommandSourceStack src) {
        src.sendSuccess(() -> Component.translatable("command.healthy_diet.usage.headiet.0"), false);
        src.sendSuccess(() -> Component.translatable("command.healthy_diet.usage.headiet.1"), false);
        src.sendSuccess(() -> Component.translatable("command.healthy_diet.usage.headiet.2"), false);
        src.sendSuccess(() -> Component.translatable("command.healthy_diet.usage.headiet.3"), false);
        src.sendSuccess(() -> Component.translatable("command.healthy_diet.usage.headiet.4"), false);
        src.sendSuccess(() -> Component.translatable("command.healthy_diet.usage.headiet.5"), false);
        src.sendSuccess(() -> Component.translatable("command.healthy_diet.usage.headiet.6"), false);
        src.sendSuccess(() -> Component.translatable("command.healthy_diet.usage.headiet.7"), false);
        src.sendSuccess(() -> Component.translatable("command.healthy_diet.usage.headiet.8"), false);
        src.sendSuccess(() -> Component.translatable("command.healthy_diet.usage.headiet.9"), false);
        return 0;
    }
}
