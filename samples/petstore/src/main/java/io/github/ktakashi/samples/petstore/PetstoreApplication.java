package io.github.ktakashi.samples.petstore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for the Petstore sample API.
 * <p> <p />
 * This Spring Boot application demonstrates the integration
 * of lemon-check scenarios with a real REST API backed by H2 database.
 */
@SpringBootApplication
public class PetstoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(PetstoreApplication.class, args);
    }
}
