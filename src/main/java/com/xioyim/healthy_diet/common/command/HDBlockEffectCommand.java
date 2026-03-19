package com.xioyim.healthy_diet.common.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.xioyim.healthy_diet.common.config.BlockConfig;
import com.xioyim.healthy_diet.common.config.ConfigManager;
import com.xioyim.healthy_diet.common.config.ItemConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * /HDBlockEffect <blockId> <effect> <level> <durationSecs> [showParticles]
 * 为指定方块添加右键药水效果（level 0-based：0=Lv.1）。
 */
public class HDBlockEffectCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("HDBlockEffect")
                .requires(src -> src.hasPermission(4))
                .then(Commands.argument("blockId", ResourceLocationArgument.id())
                        .suggests((ctx, b) -> {
                            ForgeRegistries.BLOCKS.getKeys().stream()
                                    .map(ResourceLocation::toString).forEach(b::suggest);
                            return b.buildFuture();
                        })
                        .then(Commands.argument("effect", ResourceLocationArgument.id())
                                .suggests((ctx, b) -> {
                                    ForgeRegistries.MOB_EFFECTS.getKeys().stream()
                                            .map(ResourceLocation::toString).forEach(b::suggest);
                                    return b.buildFuture();
                                })
                                .then(Commands.argument("level", IntegerArgumentType.integer(0, 255))
                                        .then(Commands.argument("durationSecs", IntegerArgumentType.integer(1))
                                                .executes(ctx -> execute(ctx, false))
                                                .then(Commands.argument("showParticles", BoolArgumentType.bool())
                                                        .executes(ctx -> execute(ctx, true)))))))
        );
    }

    private static int execute(CommandContext<CommandSourceStack> ctx, boolean hasParticles)
            throws CommandSyntaxException {
        ResourceLocation blockRl  = ResourceLocationArgument.getId(ctx, "blockId");
        ResourceLocation effectRl = ResourceLocationArgument.getId(ctx, "effect");
        int level       = IntegerArgumentType.getInteger(ctx, "level");
        int durationSecs = IntegerArgumentType.getInteger(ctx, "durationSecs");
        boolean showParticles = hasParticles && BoolArgumentType.getBool(ctx, "showParticles");

        if (ForgeRegistries.BLOCKS.getValue(blockRl) == null) {
            ctx.getSource().sendFailure(Component.translatable(
                    "command.healthy_diet.error.block_not_found", blockRl));
            return 0;
        }
        if (ForgeRegistries.MOB_EFFECTS.getValue(effectRl) == null) {
            ctx.getSource().sendFailure(Component.translatable(
                    "command.healthy_diet.error.effect_not_found", effectRl));
            return 0;
        }

        String blockIdStr = blockRl.toString();
        BlockConfig config = ConfigManager.getBlockConfig(blockIdStr);
        if (config == null) config = new BlockConfig();

        ItemConfig.EffectEntry entry = new ItemConfig.EffectEntry();
        entry.effect = effectRl.toString();
        entry.level = level;
        entry.duration = durationSecs * 20;
        entry.showParticles = showParticles;
        config.effects.add(entry);
        ConfigManager.saveBlockConfig(blockIdStr, config);

        ctx.getSource().sendSuccess(() -> Component.translatable(
                "command.healthy_diet.hdblock.set_effect.result",
                blockIdStr, effectRl.toString(), level + 1, durationSecs), true);
        return 1;
    }
}
