package com.xioyim.healthy_diet.common.config;

import java.util.ArrayList;
import java.util.List;

public class StageConfig {
    public List<Stage> stages = new ArrayList<>();

    public Stage getStageAt(float value) {
        for (Stage s : stages) {
            if (value >= s.min && value <= s.max) return s;
        }
        return null;
    }

    public int getStageIndex(float value) {
        for (int i = 0; i < stages.size(); i++) {
            Stage s = stages.get(i);
            if (value >= s.min && value <= s.max) return i;
        }
        return -1;
    }

    public static class Stage {
        public float min = 0f;
        public float max = 100f;
        public String label = "";
        public List<String> tooltip = new ArrayList<>();
        public List<String> commands = new ArrayList<>();
    }
}
