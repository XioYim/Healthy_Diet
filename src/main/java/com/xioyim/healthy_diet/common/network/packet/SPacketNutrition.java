package com.xioyim.healthy_diet.common.network.packet;

import com.xioyim.healthy_diet.client.ClientNutritionData;
import net.minecraft.network.FriendlyByteBuf;

import java.util.HashMap;
import java.util.Map;

public class SPacketNutrition {

    /** 每种食物的实时厌倦进度数据，同步到客户端用于 Tooltip 显示 */
    public record CooldownState(int eatenCount, boolean onCooldown, int otherFoodsEaten, long cooldownStartTick) {}

    private final Map<String, Float> values;
    private final Map<String, CooldownState> cooldownStates;

    public SPacketNutrition(Map<String, Float> values, Map<String, CooldownState> cooldownStates) {
        this.values = values;
        this.cooldownStates = cooldownStates;
    }

    public static void encode(SPacketNutrition msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.values.size());
        for (Map.Entry<String, Float> e : msg.values.entrySet()) {
            buf.writeUtf(e.getKey());
            buf.writeFloat(e.getValue());
        }
        buf.writeInt(msg.cooldownStates.size());
        for (Map.Entry<String, CooldownState> e : msg.cooldownStates.entrySet()) {
            buf.writeUtf(e.getKey());
            buf.writeInt(e.getValue().eatenCount());
            buf.writeBoolean(e.getValue().onCooldown());
            buf.writeInt(e.getValue().otherFoodsEaten());
            buf.writeLong(e.getValue().cooldownStartTick());
        }
    }

    public static SPacketNutrition decode(FriendlyByteBuf buf) {
        int size = buf.readInt();
        Map<String, Float> values = new HashMap<>();
        for (int i = 0; i < size; i++) values.put(buf.readUtf(), buf.readFloat());

        int cdSize = buf.readInt();
        Map<String, CooldownState> states = new HashMap<>();
        for (int i = 0; i < cdSize; i++) {
            String key = buf.readUtf();
            int eatenCount = buf.readInt();
            boolean onCooldown = buf.readBoolean();
            int otherFoodsEaten = buf.readInt();
            long cooldownStartTick = buf.readLong();
            states.put(key, new CooldownState(eatenCount, onCooldown, otherFoodsEaten, cooldownStartTick));
        }
        return new SPacketNutrition(values, states);
    }

    public static void handle(SPacketNutrition msg) {
        ClientNutritionData.update(msg.values, msg.cooldownStates);
    }
}
