package com.xioyim.healthy_diet.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public class HealthyDietKeys {

    public static final KeyMapping OPEN_SCREEN = new KeyMapping(
            "key.healthy_diet.open_screen",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            "key.categories.healthy_diet"
    );
}
