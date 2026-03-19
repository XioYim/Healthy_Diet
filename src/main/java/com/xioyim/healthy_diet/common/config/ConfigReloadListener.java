package com.xioyim.healthy_diet.common.config;

import com.xioyim.healthy_diet.HealthyDietConstants;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class ConfigReloadListener implements net.minecraft.server.packs.resources.PreparableReloadListener {

    @Override
    public CompletableFuture<Void> reload(PreparationBarrier stage, ResourceManager resourceManager,
                                           ProfilerFiller preparationsProfiler, ProfilerFiller reloadProfiler,
                                           Executor backgroundExecutor, Executor gameExecutor) {
        return CompletableFuture.supplyAsync(() -> {
            HealthyDietConstants.LOG.info("[HealthyDiet] Reloading configs...");
            ConfigManager.loadAll();
            return null;
        }, backgroundExecutor).thenCompose(stage::wait).thenAcceptAsync(v -> {
            HealthyDietConstants.LOG.info("[HealthyDiet] Config reload complete.");
        }, gameExecutor);
    }
}
