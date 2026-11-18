// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.architecture;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableTo;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.belongToAnyOf;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.codeUnits;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.core.importer.Location;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.library.dependencies.SliceAssignment;
import com.tngtech.archunit.library.dependencies.SliceIdentifier;
import com.zextras.carbonio.chats.core.infrastructure.event.EventDispatcher;
import com.zextras.carbonio.chats.core.infrastructure.messaging.MessageReceivedBroadcaster;
import com.zextras.carbonio.chats.core.infrastructure.storage.StoragesService;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.VideoServerClient;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.VideoServerService;
import com.zextras.carbonio.chats.core.service.CapabilityService;
import com.zextras.carbonio.chats.core.service.HealthcheckService;
import com.zextras.carbonio.chats.core.service.UserService;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import java.util.Set;

@AnalyzeClasses(
    packages = "com.zextras.carbonio.chats.core",
    importOptions = ArchitectureRulesTest.ProductionClassesOnly.class)
class ArchitectureRulesTest {

  static final class ProductionClassesOnly implements ImportOption {
    @Override
    public boolean includes(Location location) {
      return !location.contains("/test-classes/")
          && !location.contains("/classes/java/test/")
          && !location.contains("/ebean-enhanced/test/");
    }
  }

  private static final String SERVICE_PACKAGE = "com.zextras.carbonio.chats.core.service..";
  private static final String DTO_PACKAGE = "com.zextras.carbonio.chats.model..";

  private static final Set<Class<?>> DTO_RETURNING_SERVICES =
      Set.of(HealthcheckService.class, CapabilityService.class, UserService.class);

  @ArchTest
  static final ArchRule servicesDoNotDependOnWebExceptForUserPrincipal =
      noClasses()
          .that()
          .resideInAPackage(SERVICE_PACKAGE)
          .should()
          .dependOnClassesThat(
              resideInAPackage("com.zextras.carbonio.chats.core.web..")
                  .and(not(belongToAnyOf(UserPrincipal.class))));

  @ArchTest
  static final ArchRule repositoriesDoNotDependOnServiceOrWeb =
      noClasses()
          .that()
          .resideInAPackage("com.zextras.carbonio.chats.core.repository..")
          .should()
          .dependOnClassesThat(
              resideInAnyPackage(SERVICE_PACKAGE, "com.zextras.carbonio.chats.core.web.."));

  @ArchTest
  static final ArchRule webDoesNotDependOnRepository =
      noClasses()
          .that()
          .resideInAPackage("com.zextras.carbonio.chats.core.web..")
          .should()
          .dependOnClassesThat(resideInAPackage("com.zextras.carbonio.chats.core.repository.."));

  @ArchTest
  static final ArchRule webHasNoTransactionalMethods =
      noMethods()
          .that()
          .areDeclaredInClassesThat(resideInAPackage("com.zextras.carbonio.chats.core.web.."))
          .should()
          .beAnnotatedWith(io.ebean.annotation.Transactional.class);

  @ArchTest
  static final ArchRule serviceInterfacesDoNotReturnGeneratedDtos =
      methods()
          .that()
          .arePublic()
          .and()
          .areDeclaredInClassesThat(
              resideInAPackage(SERVICE_PACKAGE)
                  .and(JavaClass.Predicates.INTERFACES)
                  .and(not(belongToAnyOf(DTO_RETURNING_SERVICES.toArray(Class<?>[]::new)))))
          .should(notReturnGeneratedDtos());

  @ArchTest
  static final ArchRule serviceImplementationsHaveNoCycles =
      slices().assignedFrom(serviceUnit()).should().beFreeOfCycles();

  @ArchTest
  static final ArchRule transactionalMethodsDoNotPerformIo =
      codeUnits()
          .that()
          .areAnnotatedWith(io.ebean.annotation.Transactional.class)
          .should(notCallIo());

  private static DescribedPredicate<JavaClass> ioTypes() {
    return assignableTo(EventDispatcher.class)
        .or(assignableTo(MessageReceivedBroadcaster.class))
        .or(assignableTo(StoragesService.class))
        .or(assignableTo(VideoServerService.class))
        .or(assignableTo(VideoServerClient.class))
        .or(
            resideInAnyPackage(
                "com.zextras.storages..",
                "com.zextras.filestore..",
                "com.zextras.carbonio.user_management..",
                "com.zextras.carbonio.preview..",
                "com.zextras.carbonio.chats.core.infrastructure.preview..",
                "com.zextras.carbonio.chats.core.infrastructure.profiling..",
                "com.zextras.carbonio.chats.core.infrastructure.authentication..",
                "com.zextras.carbonio.chats.core.infrastructure.instance..",
                "com.zextras.carbonio.chats.core.infrastructure.videoserver.impl..",
                "com.zextras.carbonio.chats.core.web.utility",
                "com.orbitz.consul..",
                "com.rabbitmq..",
                "org.apache.http..",
                "java.net.http.."));
  }

  private static ArchCondition<JavaCodeUnit> notCallIo() {
    DescribedPredicate<JavaClass> io = ioTypes();
    return new ArchCondition<>(
        "not call event dispatch, storage, HTTP/SDK, Consul or video-server code") {
      @Override
      public void check(JavaCodeUnit codeUnit, ConditionEvents events) {
        codeUnit.getCallsFromSelf().stream()
            .filter(call -> io.test(call.getTargetOwner()))
            .forEach(
                call -> events.add(SimpleConditionEvent.violated(codeUnit, call.getDescription())));
      }
    };
  }

  private static ArchCondition<JavaMethod> notReturnGeneratedDtos() {
    return new ArchCondition<>("not return types from " + DTO_PACKAGE) {
      @Override
      public void check(JavaMethod method, ConditionEvents events) {
        method.getReturnType().getAllInvolvedRawTypes().stream()
            .filter(type -> type.getPackageName().startsWith("com.zextras.carbonio.chats.model"))
            .forEach(
                type ->
                    events.add(
                        SimpleConditionEvent.violated(
                            method, method.getFullName() + " returns " + type.getName())));
      }
    };
  }

  private static SliceAssignment serviceUnit() {
    return new SliceAssignment() {
      @Override
      public String getDescription() {
        return "service unit (interface and implementation together)";
      }

      @Override
      public SliceIdentifier getIdentifierOf(JavaClass javaClass) {
        if (!javaClass.getPackageName().startsWith("com.zextras.carbonio.chats.core.service")) {
          return SliceIdentifier.ignore();
        }
        String name = javaClass.getName();
        int nested = name.indexOf('$');
        String outer = nested < 0 ? name : name.substring(0, nested);
        String simple = outer.substring(outer.lastIndexOf('.') + 1);
        return SliceIdentifier.of(
            simple.endsWith("Impl") ? simple.substring(0, simple.length() - 4) : simple);
      }
    };
  }
}
