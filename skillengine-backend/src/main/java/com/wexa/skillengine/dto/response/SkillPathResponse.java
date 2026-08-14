package com.wexa.skillengine.dto.response;

import java.util.List;

/**
 * A single multi-hop path returned by the Cypher traversal, plus a couple of
 * cheap-to-compute stats the frontend can show without recomputing anything.
 */
public class SkillPathResponse {

    private List<SkillNodeDto> path;
    private int hopCount;

    public SkillPathResponse() {
    }

    public SkillPathResponse(List<SkillNodeDto> path) {
        this.path = path;
        this.hopCount = path == null ? 0 : Math.max(0, path.size() - 1);
    }

    public List<SkillNodeDto> getPath() {
        return path;
    }

    public void setPath(List<SkillNodeDto> path) {
        this.path = path;
        this.hopCount = path == null ? 0 : Math.max(0, path.size() - 1);
    }

    public int getHopCount() {
        return hopCount;
    }

    public void setHopCount(int hopCount) {
        this.hopCount = hopCount;
    }
}
