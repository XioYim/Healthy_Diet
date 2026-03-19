package com.xioyim.healthy_diet.common.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.xioyim.healthy_diet.common.config.BlockConfig;
import com.xioyim.healthy_diet.common.config.ConfigManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * /HDBlockCmd <blockId> <command>
 * 为指定方块添加右键执行指令。
 */
public class HDBlockCmdCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("HDBlockCmd")
                .requires(src -> src.hasPermission(4))
                .then(Commands.argument("blockId", ResourceLocationArgument.id())
                        .suggests((ctx, b) -> {
                            ForgeRegistries.BLOCKS.getKeys().stream()
                                    .map(ResourceLocation::toString).forEach(b::suggest);
                            return b.buildFuture();
                        })
                        .then(Commands.argument("command", StringArgumentType.greedyString())
                                .executes(HDBlockCmdCommand::execute)))
        );
    }

    private static int execute(CommandContext<CommandSourceStack> ctx)
            throws CommandSyntaxException {
        ResourceLocation blockRl = ResourceLocationArgument.getId(ctx, "blockId");
        String command = StringArgumentType.getString(ctx, "command");

        if (ForgeRegistries.BLOCKS.getValue(blockRl) == null) {
            ctx.getSource().sendFailure(Component.translatable(
                    "command.healthy_diet.error.block_not_found", blockRl));
            return 0;
        }

        String blockIdStr = blockRl.toString();
        BlockConfig config = ConfigManager.getBlockConfig(blockIdStr);
        if (config == null) config = new BlockConfig();

        config.onClickCommands.add(command);
        ConfigManager.saveBlockConfig(blockIdStr, config);

        ctx.getSource().sendSuccess(() -> Component.translatable(
                "command.healthy_diet.hdblock.set_cmd.result", blockIdStr, command), true);
        return 1;
    }
}
