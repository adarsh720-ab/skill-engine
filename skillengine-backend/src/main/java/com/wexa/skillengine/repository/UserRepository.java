package com.wexa.skillengine.repository;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;
import org.neo4j.driver.types.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * All Cypher for (:User) nodes. Every query is parameterized via Values.parameters(...) —
 * no string concatenation into Cypher, ever, to close off injection.
 */
@Repository
public class UserRepository {

    private final Driver driver;

    @Autowired
    public UserRepository(Driver driver) {
        this.driver = driver;
    }

    public Optional<Node> findByEmail(String email) {
        String cypher = "MATCH (u:User {email: $email}) RETURN u LIMIT 1";
        try (Session session = driver.session()) {
            Result result = session.run(cypher, Values.parameters("email", email));
            if (!result.hasNext()) {
                return Optional.empty();
            }
            Record record = result.next();
            return Optional.of(record.get("u").asNode());
        }
    }

    public boolean existsByEmail(String email) {
        String cypher = "MATCH (u:User {email: $email}) RETURN count(u) AS c";
        try (Session session = driver.session()) {
            Record record = session.run(cypher, Values.parameters("email", email)).single();
            return record.get("c").asLong() > 0;
        }
    }

    /**
     * Creates a new User node. The password passed in MUST already be BCrypt-hashed —
     * this repository has no knowledge of encoding and will store whatever it's given.
     */
    public Node createUser(String email, String bcryptHashedPassword, String role) {
        String id = UUID.randomUUID().toString();
        String cypher = """
                CREATE (u:User {id: $id, email: $email, password: $password, role: $role})
                RETURN u
                """;
        try (Session session = driver.session()) {
            Record record = session.run(cypher, Values.parameters(
                    "id", id,
                    "email", email,
                    "password", bcryptHashedPassword,
                    "role", role
            )).single();
            return record.get("u").asNode();
        }
    }
}