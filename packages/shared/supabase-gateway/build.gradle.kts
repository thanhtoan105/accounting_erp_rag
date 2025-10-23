plugins {
    id("java-library")
    id("io.spring.dependency-management")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:3.2.5")
    }
}

dependencies {
    // Spring Boot
    api("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.retry:spring-retry")
    implementation("org.springframework:spring-aspects")

    // Database
    api("org.postgresql:postgresql:42.7.3")
    api("com.zaxxer:HikariCP:5.1.0")

    // Lombok
    compileOnly("org.projectlombok:lombok:1.18.32")
    annotationProcessor("org.projectlombok:lombok:1.18.32")

    // Logging
    api("org.slf4j:slf4j-api")

    // Metrics
    api("io.micrometer:micrometer-core")

    // Jackson for JSON serialization (schema documentation)
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:postgresql:1.19.6")
}
