package com.wexa.skillengine.controller;

import com.wexa.skillengine.dto.request.CreateSkillRequest;
import com.wexa.skillengine.dto.response.SkillNodeDto;
import com.wexa.skillengine.service.SkillService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Write-side, ROLE_ADMIN-only endpoints for growing the skill graph
 * (new Skill nodes plus their PREREQUISITE_FOR edges).
 * See SecurityConfig for the /api/v1/admin/** route scoping.
 */
@RestController
@RequestMapping("/api/v1/admin")
@CrossOrigin(origins = "*")
public class AdminSkillController {

    private final SkillService skillService;

    @Autowired
    public AdminSkillController(SkillService skillService) {
        this.skillService = skillService;
    }

    @PostMapping
    public ResponseEntity<SkillNodeDto> createSkill(@Valid @RequestBody CreateSkillRequest request) {
        SkillNodeDto created = skillService.createSkill(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
