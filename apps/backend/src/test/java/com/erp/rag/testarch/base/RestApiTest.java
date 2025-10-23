package com.erp.rag.testarch.base;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * Base class for REST API tests using REST Assured
 * 
 * Provides pre-configured REST Assured request specification
 * 
 * Usage:
 * <pre>
 * {@code
 * class MyControllerRestApiTest extends RestApiTest {
 *     @Test
 *     void testEndpoint() {
 *         given()
 *             .pathParam("id", "123")
 *         .when()
 *             .get("/api/v1/resource/{id}")
 *         .then()
 *             .statusCode(200)
 *             .body("id", equalTo("123"));
 *     }
 * }
 * }
 * </pre>
 * 
 * @author BMAD Test Architect
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class RestApiTest extends IntegrationTest {

    @LocalServerPort
    protected int port;

    protected RequestSpecification requestSpec;

    @BeforeEach
    void setUpRestAssured() {
        RestAssured.port = port;
        RestAssured.basePath = "/api/v1";
        
        requestSpec = new RequestSpecBuilder()
            .setContentType(ContentType.JSON)
            .setAccept(ContentType.JSON)
            .log(LogDetail.URI)
            .log(LogDetail.BODY)
            .build();
    }
    
    /**
     * Add Bearer token for authenticated requests
     */
    protected RequestSpecification withAuth(String token) {
        return RestAssured.given(requestSpec)
            .header("Authorization", "Bearer " + token);
    }
    
    /**
     * Add company context header
     */
    protected RequestSpecification withCompany(String companyId) {
        return RestAssured.given(requestSpec)
            .header("X-Company-ID", companyId);
    }
}
