package com.wexa.skillengine.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wexa.skillengine.repository.SkillRepository;
import com.wexa.skillengine.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;

/**
 * Seeds a baseline dataset on startup: a default admin + user account, and the
 * multi-career skill graph loaded from resources/skills-seed.json (the roadmap.sh-
 * style dataset — skills, aliases, entry points, and PREREQUISITE_FOR edges).
 * All operations are idempotent (MERGE), gated behind `seed.enabled` (default true).
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final String SEED_FILE = "skills-seed.json";

    private final UserRepository userRepository;
    private final SkillRepository skillRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${seed.enabled:true}")
    private boolean seedEnabled;

    @Value("${seed.admin.email:admin@skillengine.dev}")
    private String adminEmail;

    @Value("${seed.admin.password:ChangeMe123!}")
    private String adminPassword;

    @Value("${seed.user.email:user@skillengine.dev}")
    private String userEmail;

    @Value("${seed.user.password:ChangeMe123!}")
    private String userPassword;

    @Autowired
    public DataSeeder(UserRepository userRepository, SkillRepository skillRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.skillRepository = skillRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        if (!seedEnabled) {
            log.info("DataSeeder skipped (seed.enabled=false).");
            return;
        }
        seedUsers();
        seedSkillsFromJson();
        log.info("DataSeeder complete.");
    }

    private void seedUsers() {
        if (!userRepository.existsByEmail(adminEmail)) {
            userRepository.createUser(adminEmail, passwordEncoder.encode(adminPassword), "ROLE_ADMIN");
            log.info("Seeded default admin account: {}", adminEmail);
        }
        if (!userRepository.existsByEmail(userEmail)) {
            userRepository.createUser(userEmail, passwordEncoder.encode(userPassword), "ROLE_USER");
            log.info("Seeded default user account: {}", userEmail);
        }
    }

    private void seedSkillsFromJson() throws Exception {
        try (InputStream in = new ClassPathResource(SEED_FILE).getInputStream()) {
            SkillSeedFile seedFile = objectMapper.readValue(in, SkillSeedFile.class);

            // Pass 1: create every node first so pass 2's edges always find both ends.
            for (SkillSeedEntry entry : seedFile.skills) {
                skillRepository.createSkill(
                        entry.name,
                        entry.category,
                        entry.aliases == null ? Collections.emptyList() : entry.aliases,
                        entry.isEntryPoint,
                        entry.usedByPaths == null ? Collections.emptyList() : entry.usedByPaths
                );
            }

            // Pass 2: wire PREREQUISITE_FOR edges.
            int edgeCount = 0;
            for (SkillSeedEntry entry : seedFile.skills) {
                if (entry.prerequisiteFor == null) continue;
                for (String target : entry.prerequisiteFor) {
                    skillRepository.addPrerequisiteEdge(entry.name, target);
                    edgeCount++;
                }
            }

            log.info("Seeded skill graph from {}: {} skills, {} edges.",
                    SEED_FILE, seedFile.skills.size(), edgeCount);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class SkillSeedFile {
        public List<SkillSeedEntry> skills;
        // "combinedPaths" in the JSON is intentionally ignored here — it's
        // presentation metadata for a "Full Stack" quick-start UI, not graph data.
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class SkillSeedEntry {
        public String name;
        public String category;
        public boolean isEntryPoint;
        public List<String> aliases;
        public List<String> prerequisiteFor;
        public List<String> usedByPaths;
    }
}