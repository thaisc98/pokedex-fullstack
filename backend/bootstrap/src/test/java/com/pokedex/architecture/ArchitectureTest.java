package com.pokedex.architecture;

import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Surefire test proving ArchUnit runs on Java 25 and enforcing the Clean Architecture layering
 * (ADR 0001): dependencies point inward only and presentation ↮ infrastructure. Adding a cross-layer
 * import (e.g. presentation importing infrastructure) fails this test.
 */
@AnalyzeClasses(packages = "com.pokedex", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule layers_point_inward_only = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("Domain")
            .definedBy("com.pokedex.domain..")
            .layer("Application")
            .definedBy("com.pokedex.application..")
            .layer("Infrastructure")
            .definedBy("com.pokedex.infrastructure..")
            .layer("Presentation")
            .definedBy("com.pokedex.presentation..")
            .layer("Bootstrap")
            .definedBy("com.pokedex", "com.pokedex.config..")
            .whereLayer("Bootstrap")
            .mayNotBeAccessedByAnyLayer()
            .whereLayer("Presentation")
            .mayOnlyBeAccessedByLayers("Bootstrap")
            .whereLayer("Infrastructure")
            .mayOnlyBeAccessedByLayers("Bootstrap")
            .whereLayer("Application")
            .mayOnlyBeAccessedByLayers("Infrastructure", "Presentation", "Bootstrap")
            .whereLayer("Domain")
            .mayOnlyBeAccessedByLayers("Application", "Infrastructure", "Presentation", "Bootstrap");

    @ArchTest
    static final ArchRule domain_and_application_are_framework_free =
            com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses()
                    .that()
                    .resideInAnyPackage("com.pokedex.domain..", "com.pokedex.application..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "org.springframework..",
                            "tools.jackson..",
                            "com.fasterxml.jackson..",
                            "jakarta.persistence..",
                            "lombok..");
}
