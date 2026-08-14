package com.wexa.skillengine.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class SkillPathRequest {

    @NotBlank(message = "startSkill is required")
    private String startSkill;

    @NotBlank(message = "endSkill is required")
    private String endSkill;

    // Bounds mirror the *1..5 hop limit baked into the Cypher query itself.
    @Min(value = 1, message = "maxHops must be at least 1")
    @Max(value = 5, message = "maxHops cannot exceed 5")
    private int maxHops = 5;

    public SkillPathRequest() {
    }

    public String getStartSkill() {
        return startSkill;
    }

    public void setStartSkill(String startSkill) {
        this.startSkill = startSkill;
    }

    public String getEndSkill() {
        return endSkill;
    }

    public void setEndSkill(String endSkill) {
        this.endSkill = endSkill;
    }

    public int getMaxHops() {
        return maxHops;
    }

    public void setMaxHops(int maxHops) {
        this.maxHops = maxHops;
    }
}
