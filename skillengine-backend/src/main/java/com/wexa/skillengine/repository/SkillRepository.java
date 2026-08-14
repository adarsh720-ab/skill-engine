package com.wexa.skillengine.repository;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Path;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class SkillRepository {

    private final Driver driver;

    @Autowired
    public SkillRepository(Driver driver) {
        this.driver = driver;
    }

    public Node createSkill(String name, String category) {
        return createSkill(name, category, java.util.Collections.emptyList(), false, java.util.Collections.emptyList());
    }

    public Node createSkill(String name, String category, List<String> aliases,
                            boolean isEntryPoint, List<String> usedByPaths) {
        String cypher = """
                MERGE (s:Skill {name: $name})
                SET s.category = $category,
                    s.aliases = $aliases,
                    s.isEntryPoint = $isEntryPoint,
                    s.usedByPaths = $usedByPaths
                RETURN s
                """;
        try (Session session = driver.session()) {
            Record record = session.run(cypher, Values.parameters(
                    "name", name,
                    "category", category,
                    "aliases", aliases,
                    "isEntryPoint", isEntryPoint,
                    "usedByPaths", usedByPaths
            )).single();
            return record.get("s").asNode();
        }
    }

    public void addPrerequisiteEdge(String fromSkillName, String toSkillName) {
        String cypher = """
                MATCH (from:Skill {name: $from}), (to:Skill {name: $to})
                MERGE (from)-[:PREREQUISITE_FOR]->(to)
                """;
        try (Session session = driver.session()) {
            session.run(cypher, Values.parameters("from", fromSkillName, "to", toSkillName));
        }
    }

    public boolean existsByName(String name) {
        String cypher = "MATCH (s:Skill {name: $name}) RETURN count(s) AS c";
        try (Session session = driver.session()) {
            Record record = session.run(cypher, Values.parameters("name", name)).single();
            return record.get("c").asLong() > 0;
        }
    }

    public List<Path> findPaths(String startSkill, String endSkill, int maxHops) {
        int clampedHops = Math.min(Math.max(maxHops, 1), 5);

        String cypher = String.format("""
                MATCH p = (start:Skill {name: $start})-[:PREREQUISITE_FOR*1..%d]->(end:Skill {name: $end})
                RETURN p
                ORDER BY length(p) ASC
                LIMIT 25
                """, clampedHops);

        try (Session session = driver.session()) {
            List<Record> records = session.run(cypher, Values.parameters(
                    "start", startSkill,
                    "end", endSkill
            )).list();

            List<Path> paths = new ArrayList<>();
            for (Record record : records) {
                paths.add(record.get("p").asPath());
            }
            return paths;
        }
    }

    public List<Node> findAll() {
        String cypher = "MATCH (s:Skill) RETURN s ORDER BY s.name ASC";
        try (Session session = driver.session()) {
            List<Record> records = session.run(cypher).list();
            List<Node> nodes = new ArrayList<>();
            for (Record record : records) {
                nodes.add(record.get("s").asNode());
            }
            return nodes;
        }
    }

    public List<Node> searchRootSkills(String query) {
        String cypher = """
                MATCH (s:Skill)
                WHERE any(word IN split(toLower(s.name), ' ') WHERE word = toLower($query))
                   OR toLower(s.category) = toLower($query)
                   OR any(alias IN coalesce(s.aliases, []) WHERE toLower(alias) = toLower($query))
                RETURN DISTINCT s
                ORDER BY s.name
                """;
        try (Session session = driver.session()) {
            List<Record> records = session.run(cypher, Values.parameters("query", query)).list();
            List<Node> nodes = new ArrayList<>();
            for (Record record : records) {
                nodes.add(record.get("s").asNode());
            }
            return nodes;
        }
    }

    /**
     * Returns only the PREREQUISITE_FOR edges that exist *within* the reachable
     * subgraph (root + everything within maxHops) — not a root-fans-out-to-everyone
     * shape. This is what lets the frontend render a real multi-level tree instead
     * of a star with the root at the center.
     */
    public List<String[]> findRelatedEdges(String rootSkillName, int maxHops) {
        int clampedHops = Math.min(Math.max(maxHops, 1), 5);

        String cypher = String.format("""
                MATCH (root:Skill {name: $name})-[:PREREQUISITE_FOR*1..%d]->(related:Skill)
                WITH root, collect(DISTINCT related.name) + [root.name] AS nodeNames
                MATCH (a:Skill)-[:PREREQUISITE_FOR]->(b:Skill)
                WHERE a.name IN nodeNames AND b.name IN nodeNames
                RETURN DISTINCT a.name AS source, b.name AS target
                """, clampedHops);

        try (Session session = driver.session()) {
            List<Record> records = session.run(cypher, Values.parameters("name", rootSkillName)).list();
            List<String[]> edges = new ArrayList<>();
            for (Record record : records) {
                edges.add(new String[]{record.get("source").asString(), record.get("target").asString()});
            }
            return edges;
        }
    }

    public List<Node> findRelatedSkills(String rootSkillName, int maxHops) {
        int clampedHops = Math.min(Math.max(maxHops, 1), 5);

        String cypher = String.format("""
                MATCH (root:Skill {name: $name})-[:PREREQUISITE_FOR*1..%d]->(related:Skill)
                RETURN DISTINCT related
                ORDER BY related.name
                """, clampedHops);

        try (Session session = driver.session()) {
            List<Record> records = session.run(cypher, Values.parameters("name", rootSkillName)).list();
            List<Node> nodes = new ArrayList<>();
            for (Record record : records) {
                nodes.add(record.get("related").asNode());
            }
            return nodes;
        }
    }

    /**
     * Lightweight autocomplete — NOT a graph traversal. Returns matching skills
     * (name + category) for typing into a field. Empty query returns the
     * career-path entry points, so a user who doesn't know the end skill can
     * still browse instead of being stuck typing blind.
     */
    public List<Node> searchSkills(String query, int limit) {
        String trimmed = query == null ? "" : query.trim();
        int clampedLimit = Math.min(Math.max(limit, 1), 50);

        String cypher = trimmed.isEmpty()
                ? """
                  MATCH (s:Skill)
                  WHERE coalesce(s.isEntryPoint, false) = true
                  RETURN DISTINCT s
                  ORDER BY s.name
                  LIMIT $limit
                  """
                : """
                  MATCH (s:Skill)
                  WHERE toLower(s.name) CONTAINS toLower($query)
                     OR any(alias IN coalesce(s.aliases, []) WHERE toLower(alias) CONTAINS toLower($query))
                  RETURN DISTINCT s
                  ORDER BY s.name
                  LIMIT $limit
                  """;

        try (Session session = driver.session()) {
            List<Record> records = session.run(cypher, Values.parameters(
                    "query", trimmed,
                    "limit", clampedLimit
            )).list();
            List<Node> nodes = new ArrayList<>();
            for (Record record : records) {
                nodes.add(record.get("s").asNode());
            }
            return nodes;
        }
    }
}