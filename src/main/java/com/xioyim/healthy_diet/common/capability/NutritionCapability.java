package com.xioyim.healthy_diet.common.capability;

import com.xioyim.healthy_diet.HealthyDietConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.util.LazyOptional;

public class NutritionCapability {

    public static final Capability<INutritionTracker> NUTRITION_TRACKER =
            CapabilityManager.get(new CapabilityToken<>() {});

    public static final ResourceLocation NUTRITION_TRACKER_ID =
            new ResourceLocation(HealthyDietConstants.MOD_ID, "nutrition_tracker");

    public static LazyOptional<INutritionTracker> get(final Player player) {
        return player.getCapability(NUTRITION_TRACKER);
    }
}
