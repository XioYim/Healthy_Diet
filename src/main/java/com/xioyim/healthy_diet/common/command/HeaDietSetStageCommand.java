package com.xioyim.healthy_diet.common.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.xioyim.healthy_diet.common.config.ConfigManager;
import com.xioyim.healthy_diet.common.config.StageConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

/**
 * /HeaDietSetStage <group> <min> <max> <stageName>
 *
 * 为指定营养组添加一个阶段定义。
 * 阶段数值范围为 0~100，对应营养百分比。
 * 阶段名字在 UI 进度条悬停时以 Tooltip 形式显示，
 * 并用纯白色边框高亮对应区间。
 *
 * 示例：
 *   /HeaDietSetStage vegetables 0 30 非健康阶段
 *   /HeaDietSetStage vegetables 31 100 健康阶段
 */
public class HeaDietSetStageCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("HeaDietSetStage")
                .requires(src -> src.hasPermission(4))
                .then(Commands.argument("group", StringArgumentType.string())
                        // 自动补全：已配置的营养组 ID
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                ConfigManager.getGroups().keySet(), builder))
                        .then(Commands.argument("min", FloatArgumentType.floatArg(0f, 100f))
                                .then(Commands.argument("max", FloatArgumentType.floatArg(0f, 100f))
                                        .then(Commands.argument("stageName", StringArgumentType.greedyString())
                                                .executes(ctx -> execute(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "group"),
                                                        FloatArgumentType.getFloat(ctx, "min"),
                                                        FloatArgumentType.getFloat(ctx, "max"),
                                                        StringArgumentType.getString(ctx, "stageName")
                                                )))))));
    }

    private static int execute(CommandSourceStack src,
                               String groupId, float min, float max, String stageName) {
        // 验证营养组存在
        if (ConfigManager.getGroup(groupId) == null) {
            src.sendFailure(Component.translatable(
                    "command.healthy_diet.error.group_not_found", groupId));
            return 0;
        }
        // 验证范围合法
        if (min >= max) {
            src.sendFailure(Component.translatable(
                    "command.healthy_diet.error.min_greater_max"));
            return 0;
        }

        // 加载已有配置（若无则新建）
        StageConfig cfg = ConfigManager.getStageConfig(groupId);
        if (cfg == null) cfg = new StageConfig();

        // 追加新阶段
        StageConfig.Stage stage = new StageConfig.Stage();
        stage.min   = min;
        stage.max   = max;
        stage.label = stageName;
        cfg.stages.add(stage);

        // 持久化到 stages/<groupId>.json
        ConfigManager.saveStageConfig(groupId, cfg);

        src.sendSuccess(() -> Component.translatable(
                "command.healthy_diet.set_stage.result",
                groupId, stageName, (int) min, (int) max), true);
        return 1;
    }
}
