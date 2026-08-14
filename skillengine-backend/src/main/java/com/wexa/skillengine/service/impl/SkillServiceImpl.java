package com.wexa.skillengine.service.impl;

import com.wexa.skillengine.dto.request.CreateSkillRequest;
import com.wexa.skillengine.dto.request.SkillPathRequest;
import com.wexa.skillengine.dto.response.SkillEdgeDto;
import com.wexa.skillengine.dto.response.SkillNodeDto;
import com.wexa.skillengine.dto.response.SkillPathResponse;
import com.wexa.skillengine.dto.response.SkillStackResponse;
import com.wexa.skillengine.mapper.SkillMapper;
import com.wexa.skillengine.repository.SkillRepository;
import com.wexa.skillengine.service.SkillService;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Path;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SkillServiceImpl implements SkillService {

    private final SkillRepository skillRepository;
    private final SkillMapper skillMapper;

    @Autowired
    public SkillServiceImpl(SkillRepository skillRepository, SkillMapper skillMapper) {
        this.skillRepository = skillRepository;
        this.skillMapper = skillMapper;
    }

    @Override
    public List<SkillPathResponse> findPaths(SkillPathRequest request) {
        if (!skillRepository.existsByName(request.getStartSkill())) {
            throw new IllegalArgumentException("Unknown start skill: " + request.getStartSkill());
        }
        if (!skillRepository.existsByName(request.getEndSkill())) {
            throw new IllegalArgumentException("Unknown end skill: " + request.getEndSkill());
        }

        List<Path> paths = skillRepository.findPaths(
                request.getStartSkill(),
                request.getEndSkill(),
                request.getMaxHops()
        );

        List<SkillPathResponse> responses = new ArrayList<>();
        for (Path path : paths) {
            List<Node> nodesInPath = new ArrayList<>();
            for (Node node : path.nodes()) {
                nodesInPath.add(node);
            }
            responses.add(skillMapper.toPathResponse(nodesInPath));
        }
        return responses;
    }

    @Override
    public SkillNodeDto createSkill(CreateSkillRequest request) {
        Node created = skillRepository.createSkill(request.getName(), request.getCategory());

        if (request.getPrerequisiteFor() != null) {
            for (String targetSkill : request.getPrerequisiteFor()) {
                if (!skillRepository.existsByName(targetSkill)) {
                    throw new IllegalArgumentException(
                            "Cannot link prerequisite — target skill does not exist: " + targetSkill);
                }
                skillRepository.addPrerequisiteEdge(request.getName(), targetSkill);
            }
        }

        return skillMapper.nodeToDto(created);
    }

    @Override
    public List<SkillStackResponse> findRelated(String query, int maxHops) {
        List<Node> roots = skillRepository.searchRootSkills(query);
        if (roots.isEmpty()) {
            throw new IllegalArgumentException("No skills found matching '" + query + "'");
        }

        List<SkillStackResponse> stacks = new ArrayList<>();
        for (Node root : roots) {
            String rootName = root.get("name").asString();
            String rootCategory = root.get("category").isNull() ? null : root.get("category").asString();

            List<Node> related = skillRepository.findRelatedSkills(rootName, maxHops);
            List<String[]> edgePairs = skillRepository.findRelatedEdges(rootName, maxHops);
            List<SkillEdgeDto> edges = edgePairs.stream()
                    .map(e -> new SkillEdgeDto(e[0], e[1]))
                    .collect(Collectors.toList());

            stacks.add(new SkillStackResponse(rootName, rootCategory, skillMapper.nodesToDtos(related), edges));
        }
        return stacks;
    }

    @Override
    public List<SkillNodeDto> searchSkills(String query, int limit) {
        List<Node> nodes = skillRepository.searchSkills(query, limit);
        return skillMapper.nodesToDtos(nodes);
    }
}