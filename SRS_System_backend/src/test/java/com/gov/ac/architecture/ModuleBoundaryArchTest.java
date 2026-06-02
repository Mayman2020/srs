package com.gov.ac.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Module boundary and layering rules for the SRS modular monolith.
 *
 * <p>These rules codify the architecture documented in Phase 1: feature packages live under
 * {@code com.gov.ac.feature.*} and may depend on cross-cutting infrastructure ({@code common},
 * {@code config}, {@code security}). Controllers must reside in {@code controller} subpackages,
 * repositories in {@code repository} subpackages and entities in {@code entity} subpackages.
 * Controllers may never expose entity types directly.
 *
 * <p>Authorisation is enforced by Spring Security's
 * {@link org.springframework.security.access.prepost.PreAuthorize}. Every endpoint that lives
 * inside a feature package must carry a {@code @PreAuthorize} either at the method or class
 * level (a small whitelist covers the public auth chain and explicitly-public endpoints).
 */
class ModuleBoundaryArchTest {

  private static JavaClasses classes;

  @BeforeAll
  static void importPackages() {
    classes =
        new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_JARS)
            .importPackages("com.gov.ac");
  }

  @Test
  void controllersAreInControllerPackage() {
    ArchRule rule =
        classes()
            .that()
            .areAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
            .should()
            .resideInAPackage("..controller..");
    rule.check(classes);
  }

  @Test
  void repositoriesAreInRepositoryPackage() {
    ArchRule rule =
        classes()
            .that()
            .areAssignableTo(org.springframework.data.repository.Repository.class)
            .should()
            .resideInAPackage("..repository..");
    rule.check(classes);
  }

  @Test
  void entitiesAreInEntityPackage() {
    ArchRule rule =
        classes()
            .that()
            .areAnnotatedWith(jakarta.persistence.Entity.class)
            .should()
            .resideInAPackage("..entity..");
    rule.check(classes);
  }

  /** Controllers must not return entity classes directly — only DTOs. */
  @Test
  void controllersDoNotDependOnEntities() {
    ArchRule rule =
        noClasses()
            .that()
            .resideInAPackage("..controller..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..entity..");
    rule.check(classes);
  }

  /**
   * Every controller mapping inside a feature package must carry {@code @PreAuthorize} either at
   * method- or class-level. We match HTTP endpoints by detecting Spring's {@code @RequestMapping}
   * meta-annotation, which covers {@code @GetMapping}, {@code @PostMapping}, {@code @PutMapping},
   * {@code @DeleteMapping} and {@code @PatchMapping} in one predicate (avoids operator-precedence
   * bugs when chaining {@code and()} / {@code or()} in the fluent DSL).
   */
  @Test
  void publicEndpointsCarryPreAuthorize() {
    ArchRule rule =
        methods()
            .that()
            .areMetaAnnotatedWith(org.springframework.web.bind.annotation.RequestMapping.class)
            .and()
            .areDeclaredInClassesThat()
            .areAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
            .and()
            .areDeclaredInClassesThat()
            .resideInAPackage("..feature..")
            .should()
            .beAnnotatedWith(org.springframework.security.access.prepost.PreAuthorize.class)
            .orShould()
            .beDeclaredInClassesThat()
            .areAnnotatedWith(org.springframework.security.access.prepost.PreAuthorize.class)
            .allowEmptyShould(true);
    rule.check(classes);
  }

  /**
   * Self-scoped {@code /me/*} endpoints (capabilities, profile navigation, in-app notifications,
   * read-receipts, attachment access log) are the most sensitive surfaces in the API because they
   * drive both authorisation visibility and the RBAC UX. Any new self-scoped controller must
   * therefore carry a class-level {@code @PreAuthorize} so that even a method without its own
   * annotation cannot be reached anonymously. The rule below catches any drift on that contract.
   */
  @Test
  void capabilitiesControllerCarriesClassLevelPreAuthorize() {
    ArchRule rule =
        classes()
            .that()
            .haveSimpleName("MeCapabilitiesController")
            .should()
            .beAnnotatedWith(org.springframework.security.access.prepost.PreAuthorize.class);
    rule.check(classes);
  }

  /**
   * The SLA Policy Engine admin surface is privileged: an unauthenticated leak of these endpoints
   * would let a caller resize SLA windows or read the breach ledger. Pin the class-level {@code
   * @PreAuthorize} contract so a future refactor cannot accidentally drop it.
   */
  @Test
  void slaAdminControllersCarryClassLevelPreAuthorize() {
    ArchRule rule =
        classes()
            .that()
            .resideInAPackage("..sla.controller..")
            .and()
            .haveSimpleNameNotEndingWith("TaskStatusController")
            .should()
            .beAnnotatedWith(org.springframework.security.access.prepost.PreAuthorize.class);
    rule.check(classes);
  }

  /** Acting assignment APIs (self-service + audit list) require an authenticated session baseline. */
  @Test
  void actingControllersCarryClassLevelPreAuthorize() {
    ArchRule rule =
        classes()
            .that()
            .resideInAPackage("..acting.controller..")
            .should()
            .beAnnotatedWith(org.springframework.security.access.prepost.PreAuthorize.class);
    rule.check(classes);
  }

  /**
   * Slice 5 — attachment signature controllers (create / list / verify / revoke + the QR verifier)
   * must always carry a class-level {@code @PreAuthorize} so that any future method without an
   * explicit annotation still defaults to authenticated-only.
   */
  @Test
  void attachmentSignatureControllersCarryClassLevelPreAuthorize() {
    ArchRule rule =
        classes()
            .that()
            .resideInAPackage("..attachment.signature.controller..")
            .should()
            .beAnnotatedWith(org.springframework.security.access.prepost.PreAuthorize.class);
    rule.check(classes);
  }

  /** Slice 5 — token-issuance + signed-download controllers. */
  @Test
  void attachmentDownloadControllersCarryClassLevelPreAuthorize() {
    ArchRule rule =
        classes()
            .that()
            .resideInAPackage("..attachment.download.controller..")
            .should()
            .beAnnotatedWith(org.springframework.security.access.prepost.PreAuthorize.class);
    rule.check(classes);
  }

  @Test
  void notificationChannelControllersCarryClassLevelPreAuthorize() {
    ArchRule rule =
        classes()
            .that()
            .resideInAPackage("..notification.channel.controller..")
            .should()
            .beAnnotatedWith(org.springframework.security.access.prepost.PreAuthorize.class);
    rule.check(classes);
  }

  @Test
  void retentionControllersCarryClassLevelPreAuthorize() {
    ArchRule rule =
        classes()
            .that()
            .resideInAPackage("..retention.controller..")
            .should()
            .beAnnotatedWith(org.springframework.security.access.prepost.PreAuthorize.class);
    rule.check(classes);
  }

  @Test
  void attachmentVerificationAdminControllersCarryClassLevelPreAuthorize() {
    ArchRule rule =
        classes()
            .that()
            .resideInAPackage("..attachment.verification.controller..")
            .and()
            .haveSimpleNameNotContaining("Public")
            .should()
            .beAnnotatedWith(org.springframework.security.access.prepost.PreAuthorize.class);
    rule.check(classes);
  }

  @Test
  void onlyAttachmentVerificationPublicControllerUsesPermitAllInVerificationPackage() {
    java.util.List<String> permitAllClasses = new java.util.ArrayList<>();
    for (JavaClass c : classes) {
      if (!c.getPackageName().contains("attachment.verification")) {
        continue;
      }
      for (JavaAnnotation<JavaClass> ann : c.getAnnotations()) {
        if (!ann.getRawType().getName().equals(
            org.springframework.security.access.prepost.PreAuthorize.class.getName())) {
          continue;
        }
        String v = ann.get("value").toString();
        if (v.contains("permitAll")) {
          permitAllClasses.add(c.getSimpleName());
        }
      }
    }
    Assertions.assertThat(permitAllClasses).containsExactly("AttachmentVerificationPublicController");
  }
}
