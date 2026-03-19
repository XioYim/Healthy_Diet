package com.xioyim.healthy_diet.common.capability;

import com.xioyim.healthy_diet.HealthyDietConstants;
import com.xioyim.healthy_diet.common.config.BlockConfig;
import com.xioyim.healthy_diet.common.config.ConfigManager;
import com.xioyim.healthy_diet.common.config.ItemConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = HealthyDietConstants.MOD_ID)
public class BlockInteractEvents {

    /**
     * 玩家右键方块：仅服务端，仅主手，触发该方块的配置效果。
     */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock evt) {
        if (evt.getLevel().isClientSide()) return;
        if (evt.getHand() != InteractionHand.MAIN_HAND) return;
        if (!(evt.getEntity() instanceof ServerPlayer player)) return;

        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(
                evt.getLevel().getBlockState(evt.getPos()).getBlock());
        if (blockId == null) return;

        BlockConfig config = ConfigManager.getBlockConfig(blockId.toString());
        if (config == null) return;

        // ── 药水效果 ──
        for (ItemConfig.EffectEntry ef : config.effects) {
            try {
                MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(
                        ResourceLocation.tryParse(ef.effect));
                if (effect != null) {
                    player.addEffect(new MobEffectInstance(
                            effect, ef.duration, ef.level, false, ef.showParticles));
                }
            } catch (Exception ignored) {}
        }

        // ── 执行指令 ──
        if (!config.onClickCommands.isEmpty()) {
            net.minecraft.server.MinecraftServer server = player.getServer();
            if (server != null) {
                net.minecraft.commands.CommandSourceStack source =
                        player.createCommandSourceStack().withPermission(4).withSuppressedOutput();
                for (String cmd : config.onClickCommands) {
                    String c = cmd.startsWith("/") ? cmd.substring(1) : cmd;
                    server.getCommands().performPrefixedCommand(source, c);
                }
            }
        }

        // ── 营养值变化 ──
        if (!config.nutritionChanges.isEmpty()) {
            NutritionCapability.get(player).ifPresent(tracker -> {
                for (BlockConfig.NutritionChange nc : config.nutritionChanges) {
                    if (nc.value >= 0) {
                        tracker.addValue(nc.group, nc.value);
                    } else {
                        tracker.subtractValue(nc.group, -nc.value);
                    }
                }
            });
        }
    }
}
