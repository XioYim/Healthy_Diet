package com.xioyim.healthy_diet.common.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.xioyim.healthy_diet.common.config.BlockConfig;
import com.xioyim.healthy_diet.common.config.ConfigManager;
import com.xioyim.healthy_diet.common.config.GroupDefinition;
import com.xioyim.healthy_diet.common.config.ItemConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * /HDBlockList <blockId>
 * 查看方块的所有右键交互配置，OP 可点击 [X] 删除条目。
 */
public class HDBlockListCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("HDBlockList")
                .requires(src -> src.hasPermission(4))
                .then(Commands.argument("blockId", ResourceLocationArgument.id())
                        .suggests((ctx, b) -> {
                            ConfigManager.getBlockConfigKeys().forEach(b::suggest);
                            return b.buildFuture();
                        })
                        .executes(HDBlockListCommand::execute))
        );
    }

    private static int execute(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ResourceLocation blockRl = ResourceLocationArgument.getId(ctx, "blockId");
        String blockIdStr = blockRl.toString();

        BlockConfig config = ConfigManager.getBlockConfig(blockIdStr);
        if (config == null) {
            ctx.getSource().sendFailure(Component.translatable(
                    "command.healthy_diet.error.block_not_found", blockIdStr));
            return 0;
        }

        // ── 标题 ──
        ctx.getSource().sendSuccess(() -> Component.translatable(
                "command.healthy_diet.hdblock.list.header", blockIdStr)
                .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD), false);

        boolean any = false;

        // ── 药水效果 ──
        if (!config.effects.isEmpty()) {
            any = true;
            ctx.getSource().sendSuccess(() -> Component.translatable(
                    "command.healthy_diet.hdblock.list.header_effect"), false);
            for (int i = 0; i < config.effects.size(); i++) {
                ItemConfig.EffectEntry ef = config.effects.get(i);
                MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(
                        ResourceLocation.tryParse(ef.effect));
                String eName = effect != null
                        ? Component.translatable(effect.getDescriptionId()).getString()
                        : ef.effect;
                final int idx = i;
                MutableComponent line = Component.translatable(
                        "command.healthy_diet.hdblock.list.effect_entry",
                        eName, ef.level + 1, ef.duration / 20);
                line.append(Component.literal(" "));
                line.append(xBtn("/HDBlockRemove " + blockIdStr + " effect " + idx,
                        Component.translatable("command.healthy_diet.hdblock.remove_entry")));
                ctx.getSource().sendSuccess(() -> line, false);
            }
        }

        // ── 执行指令 ──
        if (!config.onClickCommands.isEmpty()) {
            any = true;
            ctx.getSource().sendSuccess(() -> Component.translatable(
                    "command.healthy_diet.hdblock.list.header_cmd"), false);
            for (int i = 0; i < config.onClickCommands.size(); i++) {
                final String cmd = config.onClickCommands.get(i);
                final int idx = i;
                MutableComponent line = Component.translatable(
                        "command.healthy_diet.hdblock.list.cmd_entry", cmd);
                line.append(Component.literal(" "));
                line.append(xBtn("/HDBlockRemove " + blockIdStr + " cmd " + idx,
                        Component.translatable("command.healthy_diet.hdblock.remove_entry")));
                ctx.getSource().sendSuccess(() -> line, false);
            }
        }

        // ── 营养值变化 ──
        if (!config.nutritionChanges.isEmpty()) {
            any = true;
            ctx.getSource().sendSuccess(() -> Component.translatable(
                    "command.healthy_diet.hdblock.list.header_group"), false);
            for (int i = 0; i < config.nutritionChanges.size(); i++) {
                BlockConfig.NutritionChange nc = config.nutritionChanges.get(i);
                GroupDefinition gd = ConfigManager.getGroup(nc.group);
                String gName  = gd != null ? gd.displayName : nc.group;
                String sign   = nc.value >= 0 ? "+" : "";
                String valStr = sign + (nc.value == Math.floor(nc.value)
                        ? String.valueOf((int) nc.value) : String.valueOf(nc.value));
                final int idx = i;
                MutableComponent line = Component.translatable(
                        "command.healthy_diet.hdblock.list.group_entry", gName, valStr);
                line.append(Component.literal(" "));
                line.append(xBtn("/HDBlockRemove " + blockIdStr + " group " + idx,
                        Component.translatable("command.healthy_diet.hdblock.remove_entry")));
                ctx.getSource().sendSuccess(() -> line, false);
            }
        }

        if (!any) {
            ctx.getSource().sendSuccess(() -> Component.translatable(
                    "command.healthy_diet.hdblock.list.empty"), false);
        }

        return 1;
    }

    private static MutableComponent xBtn(String command, Component hover) {
        return Component.translatable("command.healthy_diet.info.btn_remove")
                .withStyle(style -> style
                        .withColor(ChatFormatting.RED)
                        .withBold(false)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover)));
    }
}
