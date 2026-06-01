package org.berrycrush.samples.graphql;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * GraphQL Sample Application
 * 
 * Demonstrates BerryCrush testing with GraphQL APIs.
 * GraphQL queries and mutations are exposed via HTTP POST to /graphql.
 */
@SpringBootApplication
public class GraphQLApplication {
    public static void main(String[] args) {
        SpringApplication.run(GraphQLApplication.class, args);
    }
}
