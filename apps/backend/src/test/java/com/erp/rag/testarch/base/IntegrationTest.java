package com.erp.rag.testarch.base;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for integration tests with Testcontainers
 * 
 * Usage:
 * <pre>
 * {@code
 * @ExtendWith(MockitoExtension.class)
 * class MyServiceIntegrationTest extends IntegrationTest {
 *     @Autowired
 *     private MyService service;
 *     
 *     @Test
 *     void testWithRealDatabase() {
 *         // Test implementation
 *     }
 * }
 * }
 * </pre>
 * 
 * @author BMAD Test Architect
 */
@SpringBootTest
@Testcontainers
@Tag("integration")
public abstract class IntegrationTest {

    private static final DockerImageName PGVECTOR_IMAGE = 
        DockerImageName.parse("pgvector/pgvector:pg15")
            .asCompatibleSubstituteFor("postgres");

    @Container
    protected static PostgreSQLContainer<?> postgres = 
        new PostgreSQLContainer<>(PGVECTOR_IMAGE)
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass")
            .withReuse(true); // Reuse container across test classes

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        
        // Disable Liquibase auto-migration in tests (optional)
        // registry.add("spring.liquibase.enabled", () -> "false");
    }
}
