package com.wexa.skillengine.dto.response;

import java.util.List;

public class SkillStackResponse {

    private String rootSkill;
    private String rootCategory;
    private List<SkillNodeDto> relatedSkills;
    private List<SkillEdgeDto> edges;

    public SkillStackResponse() {
    }

    public SkillStackResponse(String rootSkill, String rootCategory,
                              List<SkillNodeDto> relatedSkills, List<SkillEdgeDto> edges) {
        this.rootSkill = rootSkill;
        this.rootCategory = rootCategory;
        this.relatedSkills = relatedSkills;
        this.edges = edges;
    }

    public String getRootSkill() { return rootSkill; }
    public void setRootSkill(String rootSkill) { this.rootSkill = rootSkill; }
    public String getRootCategory() { return rootCategory; }
    public void setRootCategory(String rootCategory) { this.rootCategory = rootCategory; }
    public List<SkillNodeDto> getRelatedSkills() { return relatedSkills; }
    public void setRelatedSkills(List<SkillNodeDto> relatedSkills) { this.relatedSkills = relatedSkills; }
    public List<SkillEdgeDto> getEdges() { return edges; }
    public void setEdges(List<SkillEdgeDto> edges) { this.edges = edges; }
}