pluginManagement {
	repositories {
		gradlePluginPortal()
		mavenCentral()
	}
}

plugins {
	id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
	id("com.gradle.develocity") version "4.0.2"
}

develocity {
	buildScan {
		termsOfUseUrl = "https://gradle.com/help/legal-terms-of-use"
		termsOfUseAgree = "yes"
		publishing.onlyIf { System.getenv("CI") != null }
	}
}

rootProject.name = "berrycrush"

include("berrycrush:bom")
include("berrycrush:api")
include("berrycrush:plugin")
include("berrycrush:report-plugins")
include("berrycrush:core")
include("berrycrush:junit")
include("berrycrush:spring")
include("samples:petstore:app")
include("samples:petstore:scenario")
include("samples:petstore:kotlin-dsl")
include("samples:tictactoe:app")
include("samples:tictactoe:scenario")

// Additional sample projects
include("samples:webflux:app")
include("samples:webflux:scenario")
include("samples:microservices:order-service")
include("samples:microservices:inventory-service")
include("samples:microservices:scenario")
include("samples:grpc-gateway:app")
include("samples:grpc-gateway:scenario")
include("samples:graphql:app")
include("samples:graphql:scenario")
