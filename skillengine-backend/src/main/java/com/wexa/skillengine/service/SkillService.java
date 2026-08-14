package com.wexa.skillengine.service;

import com.wexa.skillengine.dto.request.CreateSkillRequest;
import com.wexa.skillengine.dto.request.SkillPathRequest;
import com.wexa.skillengine.dto.response.SkillNodeDto;
import com.wexa.skillengine.dto.response.SkillPathResponse;
import com.wexa.skillengine.dto.response.SkillStackResponse;

import java.util.List;

public interface SkillService {

    List<SkillPathResponse> findPaths(SkillPathRequest request);

    SkillNodeDto createSkill(CreateSkillRequest request);

    List<SkillStackResponse> findRelated(String query, int maxHops);

    List<SkillNodeDto> searchSkills(String query, int limit);
}