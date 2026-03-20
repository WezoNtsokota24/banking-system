package com.banking.architecture;

// The actual, correct imports!
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.banking")
public class HexagonalArchitectureTest {

    @ArchTest
    static final ArchRule domainShouldNotDependOnAdapters =
            noClasses()
                    .that().resideInAnyPackage("com.banking.domain..")
                    .should().dependOnClassesThat().resideInAnyPackage("com.banking.adapter..");

    @ArchTest
    static final ArchRule domainShouldNotDependOnSpring =
            noClasses()
                    .that().resideInAnyPackage("com.banking.domain..")
                    .should().dependOnClassesThat().resideInAnyPackage("org.springframework..");
}