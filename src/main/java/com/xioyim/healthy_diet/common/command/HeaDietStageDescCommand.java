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
 * 管理阶段的悬浮描述行（hover tooltip）。
 *
 *   /HeaDietStageDesc <group> <stageIndex> add <文字>    → 追加一行描述
 *   /HeaDietStageDesc <group> <stageIndex> list          → 列出所有描述行（含 [X]）
 *   /HeaDietStageDesc <group> <stageIndex> remove <lineIndex> → 删除指定行
 */
public class HeaDietStageDescCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("headietstagedesc")
                .requires(src -> src.hasPermission(4))
                .executes(ctx -> sendUsage(ctx.getSource()))
                .then(Commands.argument("group", StringArgumentType.word())
                        .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                                ConfigManager.getGroups().keySet(), b))
                        .then(Commands.argument("stageIndex", IntegerArgumentType.integer(0))

                                // add
                                .then(Commands.literal("add")
                                        .then(Commands.argument("text", StringArgumentType.greedyString())
                                                .executes(ctx -> addDesc(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "group"),
                                                        IntegerArgumentType.getInteger(ctx, "stageIndex"),
                                                        StringArgumentType.getString(ctx, "text")))))

                                // list
                                .then(Commands.literal("list")
                                        .executes(ctx -> listDesc(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "group"),
                                                IntegerArgumentType.getInteger(ctx, "stageIndex"))))

                                // remove
                                .then(Commands.literal("remove")
                                        .then(Commands.argument("lineIndex", IntegerArgumentType.integer(0))
                                                .executes(ctx -> removeDesc(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "group"),
                                                        IntegerArgumentType.getInteger(ctx, "stageIndex"),
                                                        IntegerArgumentType.getInteger(ctx, "lineIndex")))))
                        )));
    }

    private static int sendUsage(CommandSourceStack src) {
        src.sendSuccess(() -> Component.translatable("command.healthy_diet.usage.headietstagedesc.0"), false);
        src.sendSuccess(() -> Component.translatable("command.healthy_diet.usage.headietstagedesc.1"), false);
        src.sendSuccess(() -> Component.translatable("command.healthy_diet.usage.headietstagedesc.2"), false);
        src.sendSuccess(() -> Component.translatable("command.healthy_diet.usage.headietstagedesc.3"), false);
        return 0;
    }

    private static int addDesc(CommandSourceStack src,
                                String groupId, int stageIdx, String text) {
        StageConfig cfg = HeaDietStageCommand.getValidConfig(src, groupId);
        if (cfg == null) return 0;
        StageConfig.Stage stage = HeaDietStageCommand.getStage(src, cfg, stageIdx);
        if (stage == null) return 0;

        stage.tooltip.add(text);
        ConfigManager.saveStageConfig(groupId, cfg);
        src.sendSuccess(() -> Component.translatable(
                "command.healthy_diet.stage.desc_add",
                groupId, stage.label, text), true);
        return 1;
    }

    private static int listDesc(CommandSourceStack src, String groupId, int stageIdx) {
        StageConfig cfg = HeaDietStageCommand.getValidConfig(src, groupId);
        if (cfg == null) return 0;
        StageConfig.Stage stage = HeaDietStageCommand.getStage(src, cfg, stageIdx);
        if (stage == null) return 0;

        src.sendSuccess(() -> Component.translatable(
                "command.healthy_diet.stage.desc_header", stage.label)
                .withStyle(ChatFormatting.YELLOW), false);

        if (stage.tooltip.isEmpty()) {
            src.sendSuccess(() -> Component.translatable("command.healthy_diet.stage.empty")
                    .withStyle(ChatFormatting.GRAY), false);
            return 1;
        }

        List<String> lines = stage.tooltip;
        for (int i = 0; i < lines.size(); i++) {
            final String text = lines.get(i);
            final int li = i;
            MutableComponent entry = Component.translatable(
                    "command.healthy_diet.stage.desc_entry", text);
            entry.append(Component.literal(" "));
            entry.append(HeaDietStageCommand.xBtn(
                    "/HeaDietStageDesc " + groupId + " " + stageIdx + " remove " + li,
                    Component.translatable("command.healthy_diet.stage.remove_desc")));
            src.sendSuccess(() -> entry, false);
        }
        return 1;
    }

    private static int removeDesc(CommandSourceStack src,
                                   String groupId, int stageIdx, int lineIdx) {
        StageConfig cfg = HeaDietStageCommand.getValidConfig(src, groupId);
        if (cfg == null) return 0;
        StageConfig.Stage stage = HeaDietStageCommand.getStage(src, cfg, stageIdx);
        if (stage == null) return 0;

        if (lineIdx < 0 || lineIdx >= stage.tooltip.size()) {
            src.sendFailure(Component.translatable("command.healthy_diet.remove.not_found"));
            return 0;
        }
        String removed = stage.tooltip.remove(lineIdx);
        ConfigManager.saveStageConfig(groupId, cfg);
        src.sendSuccess(() -> Component.translatable(
                "command.healthy_diet.stage.desc_remove",
                groupId, stage.label, lineIdx, removed), true);
        return 1;
    }
}
