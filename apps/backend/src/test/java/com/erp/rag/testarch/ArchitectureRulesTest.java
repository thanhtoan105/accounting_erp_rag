package com.erp.rag.testarch;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * Architecture Tests using ArchUnit
 * 
 * Enforces:
 * - Layered architecture (Controller → Service → Repository)
 * - Naming conventions (Repository interfaces, Service classes)
 * - Dependency rules (no circular dependencies)
 * - Spring annotations consistency
 * 
 * @author BMAD Test Architect
 */
@Tag("architecture")
class ArchitectureRulesTest {

    private static final JavaClasses importedClasses = 
        new ClassFileImporter()
            .importPackages("com.erp.rag");

    // ========================================================================
    // LAYERED ARCHITECTURE RULES
    // ========================================================================

    @Test
    void layersShouldBeRespected() {
        ArchRule rule = layeredArchitecture()
            .consideringAllDependencies()
            
            .layer("Controllers").definedBy("..controller..")
            .layer("Services").definedBy("..service..")
            .layer("Repositories").definedBy("..repository..")
            .layer("DTOs").definedBy("..dto..")
            .layer("Models").definedBy("..model..")
            
            .whereLayer("Controllers").mayNotBeAccessedByAnyLayer()
            .whereLayer("Services").mayOnlyBeAccessedByLayers("Controllers", "Services")
            .whereLayer("Repositories").mayOnlyBeAccessedByLayers("Services");

        rule.check(importedClasses);
    }

    @Test
    void servicesShouldNotDependOnControllers() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..service..")
            .should().dependOnClassesThat().resideInAPackage("..controller..");

        rule.check(importedClasses);
    }

    @Test
    void repositoriesShouldNotDependOnServices() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..repository..")
            .should().dependOnClassesThat().resideInAPackage("..service..");

        rule.check(importedClasses);
    }

    // ========================================================================
    // NAMING CONVENTIONS
    // ========================================================================

    @Test
    void repositoriesShouldBeInterfaces() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("Repository")
            .should().beInterfaces();

        rule.check(importedClasses);
    }

    @Test
    void servicesShouldBeAnnotatedWithServiceOrHaveSuffix() {
        ArchRule rule = classes()
            .that().resideInAPackage("..service..")
            .and().areNotInterfaces()
            .and().areNotNestedClasses()
            .should().beAnnotatedWith(Service.class)
            .orShould().haveSimpleNameEndingWith("Service");

        rule.check(importedClasses);
    }

    @Test
    void controllersShouldBeAnnotatedWithRestControllerOrController() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("Controller")
            .should().beAnnotatedWith(RestController.class)
            .orShould().beAnnotatedWith(Controller.class);

        rule.check(importedClasses);
    }

    // ========================================================================
    // SPRING ANNOTATIONS
    // ========================================================================

    @Test
    void repositoriesShouldBeAnnotatedWithRepository() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("Repository")
            .and().areInterfaces()
            .should().beAnnotatedWith(Repository.class)
            .orShould().beAssignableTo(org.springframework.data.repository.Repository.class);

        rule.check(importedClasses);
    }

    // ========================================================================
    // PACKAGE DEPENDENCIES
    // ========================================================================

    @Test
    void ragplatformShouldNotDependOnTestArchitecture() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..ragplatform..")
            .should().dependOnClassesThat().resideInAPackage("..testarch..");

        rule.check(importedClasses);
    }

    @Test
    void testClassesShouldNotBeDependedOnByProductionCode() {
        ArchRule rule = noClasses()
            .that().resideOutsideOfPackage("..test..")
            .should().dependOnClassesThat().resideInAPackage("..testarch..");

        rule.check(importedClasses);
    }
}
