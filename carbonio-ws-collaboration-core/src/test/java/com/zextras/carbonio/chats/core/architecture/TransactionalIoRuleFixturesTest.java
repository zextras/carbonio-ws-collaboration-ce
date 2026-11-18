// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.architecture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.EvaluationResult;
import com.zextras.carbonio.chats.core.architecture.fixture.CleanFixture;
import com.zextras.carbonio.chats.core.architecture.fixture.DirectCallFixture;
import com.zextras.carbonio.chats.core.architecture.fixture.LambdaFixture;
import com.zextras.carbonio.chats.core.architecture.fixture.MethodReferenceFixture;
import com.zextras.carbonio.chats.core.architecture.fixture.PrivateHelperFixture;
import com.zextras.carbonio.chats.core.architecture.fixture.TransactionalCalleeFixture;
import org.junit.jupiter.api.Test;

class TransactionalIoRuleFixturesTest {

  private static EvaluationResult evaluate(Class<?> fixture) {
    return ArchitectureRulesTest.transactionalMethodsDoNotPerformIo.evaluate(
        new ClassFileImporter().importClasses(fixture));
  }

  @Test
  void directCallIsReported() {
    assertTrue(evaluate(DirectCallFixture.class).hasViolation());
  }

  @Test
  void methodReferenceIsReported() {
    assertTrue(evaluate(MethodReferenceFixture.class).hasViolation());
  }

  @Test
  void callThroughPrivateHelpersIsReported() {
    assertTrue(evaluate(PrivateHelperFixture.class).hasViolation());
  }

  @Test
  void callThroughTransactionalMethodOfTheSameClassIsReported() {
    EvaluationResult result = evaluate(TransactionalCalleeFixture.class);
    assertTrue(result.hasViolation());
    assertTrue(
        result.getFailureReport().getDetails().stream()
            .anyMatch(detail -> detail.contains("outer")));
  }

  @Test
  void lambdaBodyIsReported() {
    assertTrue(evaluate(LambdaFixture.class).hasViolation());
  }

  @Test
  void databaseOnlyTransactionIsNotReported() {
    assertFalse(evaluate(CleanFixture.class).hasViolation());
  }
}
