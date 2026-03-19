package com.xioyim.healthy_diet.common.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.xioyim.healthy_diet.common.config.ConfigManager;
import com.xioyim.healthy_diet.common.config.ItemConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.resources.ResourceLocation;

/**
 * 为食物物品添加条件指令：仅当玩家指定营养组的值在 [min, max] 范围内时才执行。
 *
 * 用法：
 *   /HeaDietSetCondCmd hand   <group> <min> <max> <command>
 *   /HeaDietSetCondCmd <id>   <group> <min> <max> <command>
 *
 * 示例（消化道具：谷物 > 75 时才降低谷物）：
 *   /HeaDietSetCondCmd hand grains 75 100 /HeaDiet subtract @s grains 20
 *
 * min/max 范围含两端，使用 0~100 表示营养百分比。
 * 等于某个值：min=max=该值。
 * 大于 X：min=X，max=100。
 * 小于 X：min=0，max=X。
 */
public class HeaDietSetCondCmdCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("HeaDietSetCondCmd")
                .requires(src -> src.hasPermission(4))

                // /HeaDietSetCondCmd hand <group> <min> <max> <command>
                .then(Commands.literal("hand")
                        .then(Commands.argument("group", StringArgumentType.word())
                                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                                        ConfigManager.getGroups().keySet(), b))
                                .then(Commands.argument("min", FloatArgumentType.floatArg(0f, 100f))
                                        .then(Commands.argument("max", FloatArgumentType.floatArg(0f, 100f))
                                                .then(Commands.argument("command", StringArgumentType.greedyString())
                                                        .executes(ctx -> execute(ctx, false)))))))

                // /HeaDietSetCondCmd <id> <group> <min> <max> <command>
                .then(Commands.argument("targetId", ResourceLocationArgument.id())
                        .suggests((ctx, b) -> {
                            ForgeRegistries.ITEMS.getKeys().stream()
                                    .map(ResourceLocation::toString).forEach(b::suggest);
                            return b.buildFuture();
                        })
                        .then(Commands.argument("group", StringArgumentType.word())
                                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                                        ConfigManager.getGroups().keySet(), b))
                                .then(Commands.argument("min", FloatArgumentType.floatArg(0f, 100f))
                                        .then(Commands.argument("max", FloatArgumentType.floatArg(0f, 100f))
                                                .then(Commands.argument("command", StringArgumentType.greedyString())
                                                        .executes(ctx -> execute(ctx, true)))))))
        );
    }

    private static int execute(CommandContext<CommandSourceStack> ctx,
                                boolean hasExplicitId) throws CommandSyntaxException {
        String group   = StringArgumentType.getString(ctx, "group");
        float  min     = FloatArgumentType.getFloat(ctx, "min");
        float  max     = FloatArgumentType.getFloat(ctx, "max");
        String command = StringArgumentType.getString(ctx, "command");

        if (ConfigManager.getGroup(group) == null) {
            ctx.getSource().sendFailure(Component.translatable(
                    "command.healthy_diet.error.group_not_found", group));
            return 0;
        }
        if (min > max) {
            ctx.getSource().sendFailure(Component.translatable(
                    "command.healthy_diet.error.min_greater_max"));
            return 0;
        }

        String itemIdStr = HeaDietSetCommand.resolveTargetId(ctx, hasExplicitId);
        if (itemIdStr == null) return 0;

        ItemConfig config = ConfigManager.getItemConfig(itemIdStr);
        if (config == null) config = new ItemConfig();

        ItemConfig.ConditionalCommand cc = new ItemConfig.ConditionalCommand();
        cc.group   = group;
        cc.min     = min;
        cc.max     = max;
        cc.command = command;
        config.conditionalCommands.add(cc);
        ConfigManager.saveItemConfig(itemIdStr, config);

        final String finalId   = itemIdStr;
        final String groupName = ConfigManager.getGroup(group).displayName;
        ctx.getSource().sendSuccess(() -> Component.translatable(
                "command.healthy_diet.set_condcmd.result",
                finalId, groupName, (int) min, (int) max, command), true);
        return 1;
    }
}
