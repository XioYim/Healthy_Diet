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
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;

/**
 * /HeaDietStage <group> list                 → 列出所有阶段（含 [X] 删除按钮）
 * /HeaDietStage <group> remove <stageIndex>  → 删除指定序号的阶段（[X] 调用）
 *
 * 描述行管理：/HeaDietStageDesc
 * 点击指令管理：/HeaDietStageCmd
 */
public class HeaDietStageCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("HeaDietStage")
                .requires(src -> src.hasPermission(4))
                .then(Commands.argument("group", StringArgumentType.word())
                        .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                                ConfigManager.getGroups().keySet(), b))

                        // /HeaDietStage <group> list
                        .then(Commands.literal("list")
                                .executes(ctx -> listStages(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "group"))))

                        // /HeaDietStage <group> remove <stageIndex>
                        .then(Commands.literal("remove")
                                .then(Commands.argument("stageIndex", IntegerArgumentType.integer(0))
                                        .executes(ctx -> removeStage(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "group"),
                                                IntegerArgumentType.getInteger(ctx, "stageIndex")))))
                ));
    }

    // ══════════════════════════════════════════════════════════════
    //  阶段列表 / 删除
    // ══════════════════════════════════════════════════════════════

    static int listStages(CommandSourceStack src, String groupId) {
        if (ConfigManager.getGroup(groupId) == null) {
            src.sendFailure(Component.translatable(
                    "command.healthy_diet.error.group_not_found", groupId));
            return 0;
        }
        StageConfig cfg = ConfigManager.getStageConfig(groupId);

        src.sendSuccess(() -> Component.translatable(
                "command.healthy_diet.stage.list_header", groupId)
                .withStyle(ChatFormatting.YELLOW), false);

        if (cfg == null || cfg.stages.isEmpty()) {
            src.sendSuccess(() -> Component.translatable("command.healthy_diet.stage.empty")
                    .withStyle(ChatFormatting.GRAY), false);
            return 1;
        }

        for (int i = 0; i < cfg.stages.size(); i++) {
            StageConfig.Stage s = cfg.stages.get(i);
            final int idx = i;
            // 格式：  [0] 阶段名 (0% ~ 30%)  [X]
            MutableComponent line = Component.translatable(
                    "command.healthy_diet.stage.list_entry",
                    idx, s.label, (int) s.min, (int) s.max);
            line.append(Component.literal(" "));
            line.append(xBtn(
                    "/HeaDietStage " + groupId + " remove " + idx,
                    Component.translatable("command.healthy_diet.stage.remove_stage")));
            src.sendSuccess(() -> line, false);
        }
        return 1;
    }

    private static int removeStage(CommandSourceStack src, String groupId, int idx) {
        StageConfig cfg = getValidConfig(src, groupId);
        if (cfg == null) return 0;
        StageConfig.Stage stage = getStage(src, cfg, idx);
        if (stage == null) return 0;

        String label = stage.label;
        cfg.stages.remove(idx);
        ConfigManager.saveStageConfig(groupId, cfg);
        src.sendSuccess(() -> Component.translatable(
                "command.healthy_diet.stage.remove", groupId, idx, label), true);
        return 1;
    }

    // ══════════════════════════════════════════════════════════════
    //  包级共享工具方法（供 HeaDietStageDesc / HeaDietStageCmd 复用）
    // ══════════════════════════════════════════════════════════════

    /** 获取 StageConfig，组不存在或无配置时发送失败并返回 null。 */
    static StageConfig getValidConfig(CommandSourceStack src, String groupId) {
        if (ConfigManager.getGroup(groupId) == null) {
            src.sendFailure(Component.translatable(
                    "command.healthy_diet.error.group_not_found", groupId));
            return null;
        }
        StageConfig cfg = ConfigManager.getStageConfig(groupId);
        if (cfg == null || cfg.stages.isEmpty()) {
            src.sendFailure(Component.translatable("command.healthy_diet.stage.empty"));
            return null;
        }
        return cfg;
    }

    /** 验证阶段序号合法，越界时发送失败并返回 null。 */
    static StageConfig.Stage getStage(CommandSourceStack src, StageConfig cfg, int idx) {
        if (idx < 0 || idx >= cfg.stages.size()) {
            src.sendFailure(Component.translatable("command.healthy_diet.remove.not_found"));
            return null;
        }
        return cfg.stages.get(idx);
    }

    /** 构造红色可点击 [X] 按钮。 */
    static MutableComponent xBtn(String command, Component hoverText) {
        return Component.translatable("command.healthy_diet.info.btn_remove")
                .withStyle(s -> s
                        .withColor(ChatFormatting.RED)
                        .withBold(false)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverText)));
    }
}
