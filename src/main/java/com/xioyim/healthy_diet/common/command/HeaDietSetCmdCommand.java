package com.xioyim.healthy_diet.common.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.xioyim.healthy_diet.common.config.ConfigManager;
import com.xioyim.healthy_diet.common.config.ItemConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

public class HeaDietSetCmdCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("HeaDietSetCommand")
                .requires(src -> src.hasPermission(4))
                // ── Forms with explicit block ID ─────────────────────────────────
                .then(Commands.argument("targetId", ResourceLocationArgument.id())
                        .suggests((ctx, b) -> {
                            ForgeRegistries.BLOCKS.getKeys().stream().map(ResourceLocation::toString).forEach(b::suggest);
                            return b.buildFuture();
                        })
                        .then(Commands.literal("ClearCommand").executes(ctx -> clearCommands(ctx, true)))
                        .then(Commands.argument("command", StringArgumentType.greedyString())
                                .executes(ctx -> addCommand(ctx, true))))
                // ── hand keyword forms ────────────────────────────────────────────
                .then(Commands.literal("hand")
                        .then(Commands.literal("ClearCommand").executes(ctx -> clearCommands(ctx, false)))
                        .then(Commands.argument("command", StringArgumentType.greedyString())
                                .executes(ctx -> addCommand(ctx, false))))
                // ── Fallback: held item (no keyword) ─────────────────────────────
                .then(Commands.literal("ClearCommand").executes(ctx -> clearCommands(ctx, false)))
                .then(Commands.argument("command", StringArgumentType.greedyString())
                        .executes(ctx -> addCommand(ctx, false)))
        );
    }

    private static int addCommand(CommandContext<CommandSourceStack> ctx, boolean hasExplicitId) throws CommandSyntaxException {
        String command = StringArgumentType.getString(ctx, "command");
        String itemIdStr = HeaDietSetCommand.resolveTargetId(ctx, hasExplicitId);
        if (itemIdStr == null) return 0;

        ItemConfig config = ConfigManager.getItemConfig(itemIdStr);
        if (config == null) config = new ItemConfig();
        config.onEatCommands.add(command);
        ConfigManager.saveItemConfig(itemIdStr, config);

        ctx.getSource().sendSuccess(() -> Component.translatable("command.healthy_diet.set_command.result",
                itemIdStr, command), true);
        return 1;
    }

    private static int clearCommands(CommandContext<CommandSourceStack> ctx, boolean hasExplicitId) throws CommandSyntaxException {
        String itemIdStr = HeaDietSetCommand.resolveTargetId(ctx, hasExplicitId);
        if (itemIdStr == null) return 0;

        ItemConfig config = ConfigManager.getItemConfig(itemIdStr);
        if (config != null) {
            config.onEatCommands.clear();
            ConfigManager.saveItemConfig(itemIdStr, config);
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("command.healthy_diet.set_command.cleared", itemIdStr), true);
        return 1;
    }
}
