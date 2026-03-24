package com.xioyim.healthy_diet.common.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.xioyim.healthy_diet.common.config.ConfigManager;
import com.xioyim.healthy_diet.common.config.StageConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.List;

/**
 * 管理阶段的点击触发指令。
 *
 *   /HeaDietStageCmd <group> <stageIndex> add <命令>     → 追加一条点击指令
 *   /HeaDietStageCmd <group> <stageIndex> list           → 列出所有指令（含 [X]）
 *   /HeaDietStageCmd <group> <stageIndex> remove <cmdIndex> → 删除指定指令
 */
public class HeaDietStageCmdCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("headietstagecmd")
                .requires(src -> src.hasPermission(4))
                .executes(ctx -> sendUsage(ctx.getSource()))
                .then(Commands.argument("group", StringArgumentType.word())
                        .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                                ConfigManager.getGroups().keySet(), b))
                        .then(Commands.argument("stageIndex", IntegerArgumentType.integer(0))

                                // add
                                .then(Commands.literal("add")
                                        .then(Commands.argument("command", StringArgumentType.greedyString())
                                                .executes(ctx -> addCmd(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "group"),
                                                        IntegerArgumentType.getInteger(ctx, "stageIndex"),
                                                        StringArgumentType.getString(ctx, "command")))))

                                // list
                                .then(Commands.literal("list")
                                        .executes(ctx -> listCmd(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "group"),
                                                IntegerArgumentType.getInteger(ctx, "stageIndex"))))

                                // remove
                                .then(Commands.literal("remove")
                                        .then(Commands.argument("cmdIndex", IntegerArgumentType.integer(0))
                                                .executes(ctx -> removeCmd(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "group"),
                                                        IntegerArgumentType.getInteger(ctx, "stageIndex"),
                                                        IntegerArgumentType.getInteger(ctx, "cmdIndex")))))
                        )));
    }

    private static int sendUsage(CommandSourceStack src) {
        src.sendSuccess(() -> Component.translatable("command.healthy_diet.usage.headietstagecmd.0"), false);
        src.sendSuccess(() -> Component.translatable("command.healthy_diet.usage.headietstagecmd.1"), false);
        src.sendSuccess(() -> Component.translatable("command.healthy_diet.usage.headietstagecmd.2"), false);
        src.sendSuccess(() -> Component.translatable("command.healthy_diet.usage.headietstagecmd.3"), false);
        return 0;
    }

    private static int addCmd(CommandSourceStack src,
                               String groupId, int stageIdx, String command) {
        StageConfig cfg = HeaDietStageCommand.getValidConfig(src, groupId);
        if (cfg == null) return 0;
        StageConfig.Stage stage = HeaDietStageCommand.getStage(src, cfg, stageIdx);
        if (stage == null) return 0;

        stage.commands.add(command);
        ConfigManager.saveStageConfig(groupId, cfg);
        src.sendSuccess(() -> Component.translatable(
                "command.healthy_diet.stage.cmd_add",
                groupId, stage.label, command), true);
        return 1;
    }

    private static int listCmd(CommandSourceStack src, String groupId, int stageIdx) {
        StageConfig cfg = HeaDietStageCommand.getValidConfig(src, groupId);
        if (cfg == null) return 0;
        StageConfig.Stage stage = HeaDietStageCommand.getStage(src, cfg, stageIdx);
        if (stage == null) return 0;

        src.sendSuccess(() -> Component.translatable(
                "command.healthy_diet.stage.cmd_header", stage.label)
                .withStyle(ChatFormatting.YELLOW), false);

        if (stage.commands.isEmpty()) {
            src.sendSuccess(() -> Component.translatable("command.healthy_diet.stage.empty")
                    .withStyle(ChatFormatting.GRAY), false);
            return 1;
        }

        List<String> cmds = stage.commands;
        for (int i = 0; i < cmds.size(); i++) {
            final String cmd = cmds.get(i);
            final int ci = i;
            MutableComponent entry = Component.translatable(
                    "command.healthy_diet.stage.cmd_entry", cmd);
            entry.append(Component.literal(" "));
            entry.append(HeaDietStageCommand.xBtn(
                    "/HeaDietStageCmd " + groupId + " " + stageIdx + " remove " + ci,
                    Component.translatable("command.healthy_diet.stage.remove_cmd")));
            src.sendSuccess(() -> entry, false);
        }
        return 1;
    }

    private static int removeCmd(CommandSourceStack src,
                                  String groupId, int stageIdx, int cmdIdx) {
        StageConfig cfg = HeaDietStageCommand.getValidConfig(src, groupId);
        if (cfg == null) return 0;
        StageConfig.Stage stage = HeaDietStageCommand.getStage(src, cfg, stageIdx);
        if (stage == null) return 0;

        if (cmdIdx < 0 || cmdIdx >= stage.commands.size()) {
            src.sendFailure(Component.translatable("command.healthy_diet.remove.not_found"));
            return 0;
        }
        String removed = stage.commands.remove(cmdIdx);
        ConfigManager.saveStageConfig(groupId, cfg);
        src.sendSuccess(() -> Component.translatable(
                "command.healthy_diet.stage.cmd_remove",
                groupId, stage.label, cmdIdx, removed), true);
        return 1;
    }
}
