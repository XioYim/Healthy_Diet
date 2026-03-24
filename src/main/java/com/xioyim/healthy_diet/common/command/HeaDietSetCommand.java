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
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public class HeaDietSetCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("headietset")
                .requires(src -> src.hasPermission(4))
                .executes(ctx -> sendUsage(ctx.getSource()))
                // /HeaDietSet hand <group> <value>  (uses held item)
                .then(Commands.literal("hand")
                        .then(Commands.argument("group", StringArgumentType.word())
                                .suggests((ctx, b) -> { ConfigManager.getGroups().keySet().forEach(b::suggest); return b.buildFuture(); })
                                .then(Commands.argument("value", FloatArgumentType.floatArg(-100, 100))
                                        .executes(ctx -> execute(ctx, false)))))
                // /HeaDietSet <itemOrBlockId> <group> <value>  (explicit ID)
                .then(Commands.argument("targetId", ResourceLocationArgument.id())
                        .suggests((ctx, b) -> {
                            ForgeRegistries.ITEMS.getKeys().stream().map(ResourceLocation::toString).forEach(b::suggest);
                            ForgeRegistries.BLOCKS.getKeys().stream().map(ResourceLocation::toString).forEach(b::suggest);
                            return b.buildFuture();
                        })
                        .then(Commands.argument("group", StringArgumentType.word())
                                .suggests((ctx, b) -> { ConfigManager.getGroups().keySet().forEach(b::suggest); return b.buildFuture(); })
                                .then(Commands.argument("value", FloatArgumentType.floatArg(-100, 100))
                                        .executes(ctx -> execute(ctx, true)))))
        );
    }

    private static int execute(CommandContext<CommandSourceStack> ctx, boolean hasExplicitId) throws CommandSyntaxException {
        String group = StringArgumentType.getString(ctx, "group");
        float value = FloatArgumentType.getFloat(ctx, "value");

        if (ConfigManager.getGroup(group) == null) {
            ctx.getSource().sendFailure(Component.translatable("command.healthy_diet.error.group_not_found", group));
            return 0;
        }

        String itemIdStr = resolveTargetId(ctx, hasExplicitId);
        if (itemIdStr == null) return 0;

        ItemConfig config = ConfigManager.getItemConfig(itemIdStr);
        if (config == null) config = new ItemConfig();
        config.nutrition.put(group, value);
        ConfigManager.saveItemConfig(itemIdStr, config);

        String displayName = ConfigManager.getGroup(group).displayName;
        final String finalId = itemIdStr;
        // 保留小数显示，整数时不显示 .0
        String valStr = (value == Math.floor(value))
                ? (value >= 0 ? "+" : "") + (int) value
                : (value >= 0 ? "+" : "") + value;
        ctx.getSource().sendSuccess(() -> Component.translatable("command.healthy_diet.set_item.result",
                finalId, displayName, valStr), true);
        return 1;
    }

    private static int sendUsage(CommandSourceStack src) {
        src.sendSuccess(() -> Component.translatable("command.healthy_diet.usage.headietset.0"), false);
        src.sendSuccess(() -> Component.translatable("command.healthy_diet.usage.headietset.1"), false);
        src.sendSuccess(() -> Component.translatable("command.healthy_diet.usage.headietset.2"), false);
        src.sendSuccess(() -> Component.translatable("command.healthy_diet.usage.headietset.3"), false);
        return 0;
    }

    /** Resolves the config key: explicit ResourceLocation ID or held item's ID. Returns null on error. */
    static String resolveTargetId(CommandContext<CommandSourceStack> ctx, boolean hasExplicitId) throws CommandSyntaxException {
        if (hasExplicitId) {
            return ResourceLocationArgument.getId(ctx, "targetId").toString();
        }
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) {
            ctx.getSource().sendFailure(Component.translatable("command.healthy_diet.error.no_held_item"));
            return null;
        }
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(held.getItem());
        if (itemId == null) {
            ctx.getSource().sendFailure(Component.translatable("command.healthy_diet.error.unrecognized_item"));
            return null;
        }
        return itemId.toString();
    }
}
