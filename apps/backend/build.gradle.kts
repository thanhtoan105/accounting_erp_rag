plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    id("jacoco")
}

configurations.all {
    resolutionStrategy {
        // Force version 1.33 to avoid Android variant issues in 2.x
        force("org.yaml:snakeyaml:1.33")
    }
}

dependencies {
    // Spring Boot Starters
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")

    // Shared modules
    implementation(project(":packages:shared:supabase-gateway"))

    // Spring Retry (for @EnableRetry)
    implementation("org.springframework.retry:spring-retry")
    implementation("org.springframework:spring-aspects")

    // Database
    runtimeOnly("org.postgresql:postgresql:42.7.3")
    implementation("org.liquibase:liquibase-core:4.27.0")

    // Env
    implementation("me.paulschwarz:spring-dotenv:4.0.0")

    // Lombok
    compileOnly("org.projectlombok:lombok:1.18.32")
    annotationProcessor("org.projectlombok:lombok:1.18.32")

    // Micrometer for metrics
    implementation("io.micrometer:micrometer-registry-prometheus")

    // Testing - Unit & Integration
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.yaml", module = "snakeyaml")
    }
    testImplementation("org.springframework.security:spring-security-test")
    // Explicit snakeyaml dependency to avoid Android variant issues
    testImplementation("org.yaml:snakeyaml:1.33")
    
    // Testcontainers for integration tests
    testImplementation("org.testcontainers:testcontainers:1.19.6")
    testImplementation("org.testcontainers:postgresql:1.19.6")
    testImplementation("org.testcontainers:junit-jupiter:1.19.6")
    
    // REST API Testing
    testImplementation("io.rest-assured:rest-assured:5.4.0")
    testImplementation("io.rest-assured:json-path:5.4.0")
    testImplementation("io.rest-assured:json-schema-validator:5.4.0")
    
    // Contract Testing (Pact for microservices/API boundaries)
    testImplementation("au.com.dius.pact.provider:junit5:4.6.3")
    
    // Performance Testing (Embedded)
    testImplementation("org.awaitility:awaitility:4.2.0")
    
    // Enhanced Assertions
    testImplementation("org.assertj:assertj-core:3.25.3")
    
    // Test Data Builders (optional - for complex domain objects)
    testImplementation("com.github.javafaker:javafaker:1.0.2")
    
    // Archunit for Architecture Testing
    testImplementation("com.tngtech.archunit:archunit-junit5:1.2.1")
}

// ============================================================================
// TEST CONFIGURATION
// ============================================================================

tasks.withType<Test> {
    useJUnitPlatform()
    
    // Separate unit and integration tests
    systemProperty("junit.jupiter.execution.parallel.enabled", "true")
    systemProperty("junit.jupiter.execution.parallel.mode.default", "concurrent")
    systemProperty("junit.jupiter.execution.parallel.config.strategy", "dynamic")
    
    // Memory settings for tests
    minHeapSize = "512m"
    maxHeapSize = "2g"
    
    // Test output
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showStandardStreams = false
    }
    
    // Fail fast on first test failure (optional)
    // failFast = true
    
    // Generate reports
    reports {
        html.required.set(true)
        junitXml.required.set(true)
    }
}

// Unit Tests (fast, no external dependencies)
tasks.register<Test>("unitTest") {
    description = "Run unit tests only (no Testcontainers)"
    group = "verification"
    
    useJUnitPlatform {
        excludeTags("integration", "performance", "contract", "architecture")
    }
    
    shouldRunAfter(tasks.test)
}

// Integration Tests (with Testcontainers)
tasks.register<Test>("integrationTest") {
    description = "Run integration tests with Testcontainers"
    group = "verification"
    
    useJUnitPlatform {
        includeTags("integration")
    }
    
    shouldRunAfter(tasks.named("unitTest"))
}

// Contract Tests (Pact provider verification)
tasks.register<Test>("contractTest") {
    description = "Run contract tests (Pact provider verification)"
    group = "verification"
    
    useJUnitPlatform {
        includeTags("contract")
    }
    
    shouldRunAfter(tasks.named("integrationTest"))
}

// Architecture Tests (ArchUnit)
tasks.register<Test>("architectureTest") {
    description = "Run architecture tests (layering, dependencies, naming conventions)"
    group = "verification"
    
    useJUnitPlatform {
        includeTags("architecture")
    }
    
    shouldRunAfter(tasks.named("unitTest"))
}

// Performance Tests (embedded, not k6)
tasks.register<Test>("performanceTest") {
    description = "Run embedded performance tests"
    group = "verification"
    
    useJUnitPlatform {
        includeTags("performance")
    }
    
    // More resources for performance tests
    maxHeapSize = "4g"
    
    shouldRunAfter(tasks.named("integrationTest"))
}

// Quality Gate Task
tasks.register("qualityGate") {
    description = "Run all tests and quality checks (unit + integration + architecture)"
    group = "verification"
    
    dependsOn(
        tasks.named("unitTest"),
        tasks.named("integrationTest"),
        tasks.named("architectureTest"),
        tasks.named("jacocoTestReport")
    )
}

// ============================================================================
// CODE COVERAGE (JACOCO)
// ============================================================================

jacoco {
    toolVersion = "0.8.11"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test, tasks.named("unitTest"), tasks.named("integrationTest"))
    
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }
    
    classDirectories.setFrom(
        files(classDirectories.files.map {
            fileTree(it) {
                exclude(
                    "**/config/**",
                    "**/dto/**",
                    "**/model/**",
                    "**/entity/**",
                    "**/*Application.class"
                )
            }
        })
    )
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.jacocoTestReport)
    
    violationRules {
        rule {
            limit {
                minimum = "0.80".toBigDecimal() // 80% coverage target
            }
        }
        
        rule {
            element = "CLASS"
            limit {
                minimum = "0.70".toBigDecimal() // 70% per class
            }
            excludes = listOf(
                "*.config.*",
                "*.dto.*",
                "*.model.*",
                "*.entity.*",
                "*Application"
            )
        }
    }
}

tasks.named("check") {
    dependsOn(tasks.jacocoTestCoverageVerification)
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName.set("accounting-erp-rag-backend.jar")
}

tasks.named<Jar>("jar") {
    enabled = false
}
