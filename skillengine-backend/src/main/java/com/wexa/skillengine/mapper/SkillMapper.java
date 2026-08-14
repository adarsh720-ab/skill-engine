package com.wexa.skillengine.mapper;

import com.wexa.skillengine.dto.response.SkillNodeDto;
import com.wexa.skillengine.dto.response.SkillPathResponse;
import com.wexa.skillengine.entity.SkillNode;
import org.neo4j.driver.types.Node;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Converts between raw driver types (org.neo4j.driver.types.Node) coming back
 * from Bolt result records and our internal entity / DTO shapes.
 * Kept as its own class so controllers/services never touch driver types directly.
 */
@Component
public class SkillMapper {

    public SkillNode toEntity(Node node) {
        if (node == null) {
            return null;
        }
        String name = node.get("name").isNull() ? null : node.get("name").asString();
        String category = node.get("category").isNull() ? null : node.get("category").asString();
        return new SkillNode(name, category);
    }

    public SkillNodeDto toDto(SkillNode entity) {
        if (entity == null) {
            return null;
        }
        return new SkillNodeDto(entity.getName(), entity.getCategory());
    }

    public SkillNodeDto nodeToDto(Node node) {
        return toDto(toEntity(node));
    }

    public List<SkillNodeDto> nodesToDtos(List<Node> nodes) {
        return nodes.stream().map(this::nodeToDto).collect(Collectors.toList());
    }

    public SkillPathResponse toPathResponse(List<Node> nodesInPath) {
        return new SkillPathResponse(nodesToDtos(nodesInPath));
    }
}
