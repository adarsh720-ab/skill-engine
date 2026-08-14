package com.wexa.skillengine.config;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Configures a single application-scoped Bolt Driver for CognoDB.
 *
 * CognoDB speaks the Bolt protocol, so the standard Neo4j Java Driver
 * can be used to communicate with it.
 *
 * The connection URI and credentials are externalized through
 * application.yml environment variables.
 */
@Configuration
public class DatabaseConfig {

    @Value("${cognodb.uri}")
    private String uri;

    @Value("${cognodb.username}")
    private String username;

    @Value("${cognodb.password}")
    private String password;

    @Bean(destroyMethod = "close")
    public Driver neo4jDriver() {

        Config driverConfig = Config.builder()
                .withMaxConnectionPoolSize(50)
                .withConnectionAcquisitionTimeout(
                        30,
                        TimeUnit.SECONDS
                )
                .withMaxConnectionLifetime(
                        30,
                        TimeUnit.MINUTES
                )
                .build();

        return GraphDatabase.driver(
                uri,
                AuthTokens.basic(username, password),
                driverConfig
        );
    }
}