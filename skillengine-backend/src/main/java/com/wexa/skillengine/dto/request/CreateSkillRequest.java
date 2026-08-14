package com.wexa.skillengine.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public class CreateSkillRequest {

    @NotBlank(message = "name is required")
    private String name;

    @NotBlank(message = "category is required")
    private String category;

    // Names of existing skills that this skill should become PREREQUISITE_FOR.
    // Optional — an empty/omitted list just creates the skill node in isolation.
    private List<String> prerequisiteFor;

    public CreateSkillRequest() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public List<String> getPrerequisiteFor() {
        return prerequisiteFor;
    }

    public void setPrerequisiteFor(List<String> prerequisiteFor) {
        this.prerequisiteFor = prerequisiteFor;
    }
}
