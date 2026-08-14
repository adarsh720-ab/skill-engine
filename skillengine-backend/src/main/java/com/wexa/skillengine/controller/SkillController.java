package com.wexa.skillengine.controller;

import com.wexa.skillengine.dto.request.SkillPathRequest;
import com.wexa.skillengine.dto.response.SkillNodeDto;
import com.wexa.skillengine.dto.response.SkillPathResponse;
import com.wexa.skillengine.dto.response.SkillStackResponse;
import com.wexa.skillengine.service.SkillService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/skills")
@CrossOrigin(origins = "*")
public class SkillController {

    private final SkillService skillService;

    @Autowired
    public SkillController(SkillService skillService) {
        this.skillService = skillService;
    }

    @PostMapping("/path")
    public ResponseEntity<List<SkillPathResponse>> findPaths(@Valid @RequestBody SkillPathRequest request) {
        List<SkillPathResponse> paths = skillService.findPaths(request);
        return ResponseEntity.ok(paths);
    }

    @GetMapping("/related")
    public ResponseEntity<List<SkillStackResponse>> findRelated(
            @RequestParam String query,
            @RequestParam(defaultValue = "3") int maxHops) {
        List<SkillStackResponse> stacks = skillService.findRelated(query, maxHops);
        return ResponseEntity.ok(stacks);
    }

    @GetMapping("/search")
    public ResponseEntity<List<SkillNodeDto>> search(
            @RequestParam(required = false, defaultValue = "") String query,
            @RequestParam(required = false, defaultValue = "15") int limit) {
        return ResponseEntity.ok(skillService.searchSkills(query, limit));
    }
}