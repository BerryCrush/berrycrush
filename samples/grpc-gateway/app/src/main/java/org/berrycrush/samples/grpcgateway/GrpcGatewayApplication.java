package org.berrycrush.samples.grpcgateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * gRPC-Gateway Style Application
 * 
 * This application demonstrates BerryCrush testing with a REST API that follows
 * the patterns of a gRPC service exposed through a REST gateway.
 * 
 * In a real gRPC-Gateway setup, you would:
 * 1. Define services in .proto files
 * 2. Generate OpenAPI specs using protoc-gen-openapi
 * 3. Use gRPC-Gateway to expose the gRPC service as REST
 * 
 * This sample simulates that pattern with a direct REST implementation.
 */
@SpringBootApplication
public class GrpcGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GrpcGatewayApplication.class, args);
    }
}
