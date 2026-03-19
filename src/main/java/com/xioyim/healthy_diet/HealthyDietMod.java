package com.xioyim.healthy_diet;

import com.xioyim.healthy_diet.client.HealthyDietKeys;
import com.xioyim.healthy_diet.common.capability.INutritionTracker;
import com.xioyim.healthy_diet.common.command.*;
import com.xioyim.healthy_diet.common.config.ConfigManager;
import com.xioyim.healthy_diet.common.config.ConfigReloadListener;
import com.xioyim.healthy_diet.common.network.HealthyDietNetwork;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(HealthyDietConstants.MOD_ID)
public class HealthyDietMod {

    @SuppressWarnings("removal")
    public HealthyDietMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        // Mod 生命周期事件（网络初始化、能力注册）注册到 Mod 总线
        modBus.addListener(this::setup);
        modBus.addListener(this::registerCaps);

        // 游戏事件（命令注册、重载监听器）注册到 Forge 总线
        MinecraftForge.EVENT_BUS.addListener(this::setupCommands);
        MinecraftForge.EVENT_BUS.addListener(this::addReloaders);
    }

    /** FMLCommonSetupEvent：初始化网络频道 + 加载所有配置文件 */
    private void setup(final FMLCommonSetupEvent evt) {
        evt.enqueueWork(() -> {
            HealthyDietNetwork.setup();
            ConfigManager.loadAll();
        });
    }

    private void registerCaps(final RegisterCapabilitiesEvent evt) {
        evt.register(INutritionTracker.class);
    }

    private void setupCommands(final RegisterCommandsEvent evt) {
        HeaDietCommand.register(evt.getDispatcher());
        HeaDietSetCommand.register(evt.getDispatcher());
        HeaDietSetEffectCommand.register(evt.getDispatcher());
        HeaDietSetFoodCommand.register(evt.getDispatcher());
        HeaDietSetCmdCommand.register(evt.getDispatcher());
        HDEffectCommand.register(evt.getDispatcher());
        HDAdditionCommand.register(evt.getDispatcher());
        HeaDietInfoCommand.register(evt.getDispatcher());
        HeaDietRemoveCommand.register(evt.getDispatcher());
        HeaDietSetStageCommand.register(evt.getDispatcher());
        HeaDietStageCommand.register(evt.getDispatcher());
        HeaDietStageDescCommand.register(evt.getDispatcher());
        HeaDietStageCmdCommand.register(evt.getDispatcher());
        HeaDietSetCondCmdCommand.register(evt.getDispatcher());
        HDInfoCommand.register(evt.getDispatcher());
        HDRemoveCommand.register(evt.getDispatcher());
        HDBlockEffectCommand.register(evt.getDispatcher());
        HDBlockCmdCommand.register(evt.getDispatcher());
        HDBlockGroupCommand.register(evt.getDispatcher());
        HDBlockListCommand.register(evt.getDispatcher());
        HDBlockRemoveCommand.register(evt.getDispatcher());
    }

    private void addReloaders(final AddReloadListenerEvent evt) {
        evt.addListener(new ConfigReloadListener());
    }

    @Mod.EventBusSubscriber(modid = HealthyDietConstants.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void registerKeys(final RegisterKeyMappingsEvent evt) {
            evt.register(HealthyDietKeys.OPEN_SCREEN);
        }
    }
}
