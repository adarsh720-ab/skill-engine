package com.wexa.skillengine.dto.response;

public class SkillEdgeDto {
    private String source;
    private String target;

    public SkillEdgeDto() {
    }

    public SkillEdgeDto(String source, String target) {
        this.source = source;
        this.target = target;
    }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }
}