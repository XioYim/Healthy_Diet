package com.xioyim.healthy_diet.common.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.xioyim.healthy_diet.common.config.BlockConfig;
import com.xioyim.healthy_diet.common.config.ConfigManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * /HDBlockGroup <blockId> <group> <value>
 * 为指定方块添加右键营养值变化（正数增加，负数减少）。
 */
public class HDBlockGroupCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("hdblockgroup")
                .requires(src -> src.hasPermission(4))
                .executes(ctx -> sendUsage(ctx.getSource()))
                .then(Commands.argument("blockId", ResourceLocationArgument.id())
                        .suggests((ctx, b) -> {
                            ForgeRegistries.BLOCKS.getKeys().stream()
                                    .map(ResourceLocation::toString).forEach(b::suggest);
                            return b.buildFuture();
                        })
                        .then(Commands.argument("group", StringArgumentType.word())
                                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                                        ConfigManager.getGroups().keySet(), b))
                                .then(Commands.argument("value", FloatArgumentType.floatArg())
                                        .executes(HDBlockGroupCommand::execute))))
        );
    }

    private static int sendUsage(CommandSourceStack src) {
        src.sendSuccess(() -> Component.translatable("command.healthy_diet.usage.hdblockgroup.0"), false);
        src.sendSuccess(() -> Component.translatable("command.healthy_diet.usage.hdblockgroup.1"), false);
        return 0;
    }

    private static int execute(CommandContext<CommandSourceStack> ctx)
            throws CommandSyntaxException {
        ResourceLocation blockRl = ResourceLocationArgument.getId(ctx, "blockId");
        String group = StringArgumentType.getString(ctx, "group");
        float value  = FloatArgumentType.getFloat(ctx, "value");

        if (ForgeRegistries.BLOCKS.getValue(blockRl) == null) {
            ctx.getSource().sendFailure(Component.translatable(
                    "command.healthy_diet.error.block_not_found", blockRl));
            return 0;
        }
        if (ConfigManager.getGroup(group) == null) {
            ctx.getSource().sendFailure(Component.translatable(
                    "command.healthy_diet.error.group_not_found", group));
            return 0;
        }

        String blockIdStr = blockRl.toString();
        BlockConfig config = ConfigManager.getBlockConfig(blockIdStr);
        if (config == null) config = new BlockConfig();

        BlockConfig.NutritionChange nc = new BlockConfig.NutritionChange();
        nc.group = group;
        nc.value = value;
        config.nutritionChanges.add(nc);
        ConfigManager.saveBlockConfig(blockIdStr, config);

        String groupName = ConfigManager.getGroup(group).displayName;
        String sign   = value >= 0 ? "+" : "";
        String valStr = sign + (value == Math.floor(value)
                ? String.valueOf((int) value) : String.valueOf(value));
        ctx.getSource().sendSuccess(() -> Component.translatable(
                "command.healthy_diet.hdblock.set_group.result", blockIdStr, groupName, valStr), true);
        return 1;
    }
}
