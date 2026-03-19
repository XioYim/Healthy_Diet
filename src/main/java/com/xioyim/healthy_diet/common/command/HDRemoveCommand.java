package com.xioyim.healthy_diet.common.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.xioyim.healthy_diet.common.config.ConfigManager;
import com.xioyim.healthy_diet.common.config.GroupBonusConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

/**
 * /HDRemove <group> <index>
 * OP 专用：按索引删除营养组的某条 BonusEntry（由 /HDInfo 显示的条目编号）。
 */
public class HDRemoveCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("HDRemove")
                .requires(src -> src.hasPermission(4))
                .then(Commands.argument("group", StringArgumentType.word())
                        .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                                ConfigManager.getGroups().keySet(), b))
                        .then(Commands.argument("index", IntegerArgumentType.integer(0))
                                .executes(HDRemoveCommand::execute)))
        );
    }

    private static int execute(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String groupId = StringArgumentType.getString(ctx, "group");
        int index = IntegerArgumentType.getInteger(ctx, "index");

        GroupBonusConfig bc = ConfigManager.getGroupBonusConfig(groupId);
        if (bc == null || index >= bc.bonuses.size()) {
            ctx.getSource().sendFailure(Component.translatable("command.healthy_diet.remove.not_found"));
            return 0;
        }

        bc.bonuses.remove(index);
        ConfigManager.saveGroupBonusConfig(groupId, bc);

        final int finalIdx = index;
        ctx.getSource().sendSuccess(() -> Component.translatable(
                "command.healthy_diet.hdinfo.remove_result", groupId, finalIdx), true);
        return 1;
    }
}
