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
    // Supabase gateway dependency
    api(project(":packages:shared:supabase-gateway"))
    
    // Spring Boot
    api("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework:spring-aspects")
    
    // Lombok
    compileOnly("org.projectlombok:lombok:1.18.32")
    annotationProcessor("org.projectlombok:lombok:1.18.32")
    
    // Logging
    api("org.slf4j:slf4j-api")
    
    // Apache Commons for encoding
    implementation("commons-codec:commons-codec:1.16.1")
    
    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito:mockito-junit-jupiter")
    testImplementation("org.testcontainers:postgresql:1.19.6")
    testImplementation("org.testcontainers:junit-jupiter:1.19.6")
}

tasks.test {
    useJUnitPlatform()
}
