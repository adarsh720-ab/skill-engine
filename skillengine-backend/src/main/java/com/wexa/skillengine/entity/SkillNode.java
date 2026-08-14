package com.wexa.skillengine.entity;

/**
 * Domain representation of a (:Skill) node in CognoDB.
 * This is a plain POJO — we are NOT using Spring Data Neo4j OGM annotations
 * because the project talks to the driver directly via Cypher (see repository/).
 */
public class SkillNode {

    private String name;
    private String category;

    public SkillNode() {
    }

    public SkillNode(String name, String category) {
        this.name = name;
        this.category = category;
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

    @Override
    public String toString() {
        return "SkillNode{name='" + name + "', category='" + category + "'}";
    }
}
