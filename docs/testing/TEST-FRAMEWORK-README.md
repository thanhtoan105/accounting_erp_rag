# Test Framework Architecture - accounting_erp_rag

**Version:** 1.0.0  
**Last Updated:** 2025-10-22  
**Owner:** BMAD Test Architect (Murat)

---

## 🎯 Overview

Production-grade JVM test framework for Spring Boot 3.3 ERP platform with:
- **JUnit 5** - Unit testing with parallel execution
- **Testcontainers** - Integration tests with PostgreSQL/pgvector
- **REST Assured** - API contract testing
- **Jacoco** - Code coverage (80% target)
- **ArchUnit** - Architecture governance
- **Pact** - Consumer-driven contract testing
- **Vietnamese Test Data** - Circular 200-compliant fixtures

---

## 📁 Structure

```
apps/backend/src/test/
├── java/com/erp/rag/
│   ├── testarch/                    # Shared test infrastructure
│   │   ├── base/
│   │   │   ├── IntegrationTest.java # Base for Testcontainers tests
│   │   │   └── RestApiTest.java     # Base for REST Assured tests
│   │   ├── fixtures/
│   │   │   └── TestDataFactory.java # Vietnamese accounting data
│   │   ├── helpers/                 # Test utilities (TBD)
│   │   └── matchers/                # Custom AssertJ matchers (TBD)
│   ├── ragplatform/                 # RAG module tests
│   ├── supabase/                    # Supabase gateway tests
│   └── integration/                 # Cross-module integration tests
├── resources/
│   ├── contracts/                   # Pact contracts (TBD)
│   ├── fixtures/                    # JSON/SQL test data
│   ├── application-test.properties  # Test config
│   └── test-schema.sql              # Test schema extensions
└── performance/                     # k6 scripts + embedded perf tests
```

---

## 🧪 Test Types & Tags

| Tag | Purpose | Example Command | Speed |
|-----|---------|----------------|-------|
| (none) | Unit tests - no external deps | `./gradlew unitTest` | ⚡ Fast (< 10s) |
| `@Tag("integration")` | Testcontainers + database | `./gradlew integrationTest` | 🐢 Slow (30-60s) |
| `@Tag("contract")` | Pact provider verification | `./gradlew contractTest` | 🏃 Medium (10-20s) |
| `@Tag("architecture")` | ArchUnit layer rules | `./gradlew architectureTest` | ⚡ Fast |
| `@Tag("performance")` | Embedded performance benchmarks | `./gradlew performanceTest` | 🐌 Very Slow (1-5min) |

---

## 🚀 Quick Start

### Run All Quality Gates
```bash
./gradlew qualityGate
```
Executes: unit + integration + architecture tests + Jacoco coverage report

### Run Specific Test Suite
```bash
# Unit tests only (fastest)
./gradlew unitTest

# Integration tests with Testcontainers
./gradlew integrationTest

# Check test coverage
./gradlew jacocoTestReport
# Open: apps/backend/build/reports/jacoco/test/html/index.html
```

### Run Tests for Specific Module
```bash
# All RAG platform tests
./gradlew :apps:backend:test --tests "com.erp.rag.ragplatform.*"

# Specific test class
./gradlew :apps:backend:test --tests "RagQueryControllerTest"

# Specific test method
./gradlew :apps:backend:test --tests "RagQueryControllerTest.shouldValidateQueryInput"
```

---

## ✅ Writing Tests - Best Practices

### 1. Unit Tests (Services, Utilities)
```java
@ExtendWith(MockitoExtension.class)
class MyServiceTest {
    @Mock private MyRepository repository;
    @InjectMocks private MyService service;
    
    @Test
    void shouldCalculateTotalAmount_WithValidInvoices() {
        // Arrange
        UUID companyId = TestDataFactory.randomCompanyId();
        when(repository.findByCompany(companyId))
            .thenReturn(List.of(/* test data */));
        
        // Act
        BigDecimal total = service.calculateTotal(companyId);
        
        // Assert
        assertThat(total).isEqualByComparingTo("15000000.00"); // VND
        verify(repository).findByCompany(companyId);
    }
}
```

### 2. Integration Tests (Database + Spring Context)
```java
@Tag("integration")
class CustomerRepositoryIntegrationTest extends IntegrationTest {
    @Autowired private CustomerRepository repository;
    @Autowired private TestEntityManager entityManager;
    
    @Test
    void shouldFindCustomersByCompany_WithVietnameseNames() {
        // Arrange: Insert test data
        UUID companyId = TestDataFactory.randomCompanyId();
        Customer customer = new Customer();
        customer.setName("Công ty TNHH Thương mại ABC");
        customer.setTaxId(TestDataFactory.vietnameseTaxId());
        customer.setCompanyId(companyId);
        entityManager.persist(customer);
        entityManager.flush();
        
        // Act
        List<Customer> found = repository.findByCompanyId(companyId);
        
        // Assert
        assertThat(found)
            .hasSize(1)
            .first()
            .extracting(Customer::getName, Customer::getTaxId)
            .containsExactly("Công ty TNHH Thương mại ABC", customer.getTaxId());
    }
}
```

### 3. REST API Tests (Controllers + Security)
```java
@Tag("integration")
class InvoiceApiTest extends RestApiTest {
    @Test
    void shouldCreateInvoice_WhenAuthenticated() {
        String invoiceJson = """
            {
              "customerId": "%s",
              "invoiceNumber": "%s",
              "totalAmount": 5000000.00,
              "description": "Bán hàng hóa theo hợp đồng số 12345"
            }
            """.formatted(
                TestDataFactory.randomCustomerId(),
                TestDataFactory.invoiceNumber()
            );
        
        given(requestSpec)
            .body(invoiceJson)
            .header("Authorization", "Bearer " + testJwt())
        .when()
            .post("/invoices")
        .then()
            .statusCode(201)
            .body("invoiceNumber", notNullValue())
            .body("totalAmount", equalTo(5000000.00f));
    }
}
```

### 4. Architecture Tests (ArchUnit)
```java
@Tag("architecture")
class LayerArchitectureTest {
    private static final JavaClasses importedClasses = 
        new ClassFileImporter().importPackages("com.erp.rag");
    
    @Test
    void servicesShouldNotDependOnControllers() {
        noClasses()
            .that().resideInAPackage("..service..")
            .should().dependOnClassesThat().resideInAPackage("..controller..")
            .check(importedClasses);
    }
    
    @Test
    void repositoriesShouldBeInterfaces() {
        classes()
            .that().haveSimpleNameEndingWith("Repository")
            .should().beInterfaces()
            .check(importedClasses);
    }
}
```

---

## 📊 Coverage Requirements

| Layer | Target | Enforcement |
|-------|--------|-------------|
| Services | ≥ 80% | ✅ Enforced by Jacoco |
| Controllers | ≥ 70% | ✅ Enforced |
| Repositories | ≥ 60% | 📊 Measured only |
| DTOs/Models | Excluded | - |
| Config classes | Excluded | - |

**View Coverage Report:**
```bash
./gradlew jacocoTestReport
open apps/backend/build/reports/jacoco/test/html/index.html
```

---

## 🐳 Testcontainers Setup

### Automatic Container Reuse (Speed Optimization)
Add to `~/.testcontainers.properties`:
```properties
testcontainers.reuse.enable=true
```

Containers are reused across test runs, reducing startup time from 30s → 2s.

### Custom Postgres with pgvector
Already configured in `IntegrationTest.java`:
```java
@Container
protected static PostgreSQLContainer<?> postgres = 
    new PostgreSQLContainer<>(
        DockerImageName.parse("pgvector/pgvector:pg15")
            .asCompatibleSubstituteFor("postgres")
    )
    .withReuse(true);
```

---

## 🔧 Test Data Strategies

### Option 1: In-Memory Test Data (Fastest)
```java
UUID companyId = TestDataFactory.randomCompanyId();
String customerName = TestDataFactory.vietnameseCompanyName();
BigDecimal amount = TestDataFactory.vndAmount(); // 100K-100M VND
```

### Option 2: SQL Fixtures (Repeatable)
Place in `src/test/resources/fixtures/invoices.sql`:
```sql
INSERT INTO accounting.invoices (id, company_id, invoice_number, total_amount)
VALUES 
    ('11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222', 'INV-2025-00001', 5000000.00),
    ('33333333-3333-3333-3333-333333333333', '22222222-2222-2222-2222-222222222222', 'INV-2025-00002', 3500000.00);
```

Load in test:
```java
@Sql("/fixtures/invoices.sql")
class InvoiceServiceTest extends IntegrationTest {
    // Test code
}
```

### Option 3: Builder Pattern (Complex Entities)
```java
public class InvoiceTestBuilder {
    private UUID companyId = TestDataFactory.randomCompanyId();
    private String invoiceNumber = TestDataFactory.invoiceNumber();
    private BigDecimal totalAmount = TestDataFactory.vndAmount();
    
    public InvoiceTestBuilder withCompany(UUID id) {
        this.companyId = id;
        return this;
    }
    
    public Invoice build() {
        Invoice invoice = new Invoice();
        invoice.setCompanyId(companyId);
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setTotalAmount(totalAmount);
        return invoice;
    }
}
```

---

## 🎭 Contract Testing (Pact) - Future

For API contracts between frontend/backend or microservices:

1. **Consumer Test** (Frontend writes):
```java
@PactTestFor(providerName = "rag-query-api", port = "8080")
class RagQueryApiConsumerTest {
    @Pact(consumer = "frontend-app")
    public RequestResponsePact queryEndpointReturns202(PactDslWithProvider builder) {
        return builder
            .given("valid company and user")
            .uponReceiving("POST /api/v1/rag/query")
            .path("/api/v1/rag/query")
            .method("POST")
            .body(/* ... */)
            .willRespondWith()
            .status(202)
            .body(/* ... */)
            .toPact();
    }
}
```

2. **Provider Verification** (Backend runs):
```bash
./gradlew contractTest
```

---

## 🚨 Troubleshooting

### Tests Fail with "Testcontainers not started"
**Solution:** Ensure Docker is running:
```bash
docker ps
```

### "Connection pool exhausted" in Integration Tests
**Solution:** Increase max pool size in `application-test.properties`:
```properties
spring.datasource.hikari.maximum-pool-size=20
```

### Jacoco Coverage Below 80% - Build Fails
**Solution:** Add exclusions or write more tests:
```kotlin
// In build.gradle.kts
classDirectories.setFrom(
    files(classDirectories.files.map {
        fileTree(it) {
            exclude("**/YourUncoverableClass.class")
        }
    })
)
```

### Vietnamese Characters Display as ??????
**Solution:** Add to JVM options:
```bash
./gradlew test -Dfile.encoding=UTF-8
```

---

## 📚 Resources

- **JUnit 5 User Guide:** https://junit.org/junit5/docs/current/user-guide/
- **Testcontainers Java:** https://www.testcontainers.org/
- **REST Assured:** https://rest-assured.io/
- **ArchUnit:** https://www.archunit.org/
- **Pact JVM:** https://docs.pact.io/implementation_guides/jvm

---

## 🏆 Quality Gates

Before merging to `main`:
1. ✅ All unit tests pass (`./gradlew unitTest`)
2. ✅ All integration tests pass (`./gradlew integrationTest`)
3. ✅ Architecture rules pass (`./gradlew architectureTest`)
4. ✅ Code coverage ≥ 80% (`./gradlew jacocoTestCoverageVerification`)
5. 📋 Manual review of new test cases

**CI/CD Integration:**
```yaml
# .github/workflows/test.yml (sample)
- name: Run Quality Gate
  run: ./gradlew qualityGate

- name: Upload Coverage
  uses: codecov/codecov-action@v3
  with:
    files: ./apps/backend/build/reports/jacoco/test/jacocoTestReport.xml
```

---

**🦜 Remember:** "Tests are not an afterthought—they ARE the specification." *- Murat, Test Architect*
