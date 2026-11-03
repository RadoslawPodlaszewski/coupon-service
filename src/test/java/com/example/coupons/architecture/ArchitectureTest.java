package com.example.coupons.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** Executable version of the layering rules: the build fails if someone breaks the dependency direction. */
@AnalyzeClasses(packages = "com.example.coupons", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule domainDependsOnNothingOutsideItself = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..", "jakarta..", "org.hibernate..", "com.fasterxml..",
                    "..application..", "..infrastructure..", "..interfaces..");

    @ArchTest
    static final ArchRule applicationDoesNotKnowAdaptersOrWeb = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAnyPackage("..infrastructure..", "..interfaces..");

    @ArchTest
    static final ArchRule webDoesNotTouchPersistenceOrDomainRepositories = noClasses()
            .that().resideInAPackage("..interfaces..")
            .should().dependOnClassesThat().resideInAnyPackage("..infrastructure..", "..domain.repository..");

    // ---- CQRS separation ----

    @ArchTest
    static final ArchRule commandSideDoesNotUseTheQuerySide = noClasses()
            .that().resideInAPackage("..application.command..")
            .should().dependOnClassesThat().resideInAPackage("..application.query..");

    @ArchTest
    static final ArchRule querySideBypassesTheWriteModel = noClasses()
            .that().resideInAPackage("..application.query..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..application.command..", "..domain.repository..", "..domain.service..");

    @ArchTest
    static final ArchRule querySideNeverLoadsTheAggregate = noClasses()
            .that().resideInAnyPackage("..application.query..", "..interfaces..")
            .should().dependOnClassesThat().haveFullyQualifiedName("com.example.coupons.domain.model.Coupon");
}
