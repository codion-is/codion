/*
 * Copyright (c) 2023 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity.condition;

import is.codion.common.Operator;
import is.codion.framework.domain.TestDomain.Employee;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public final class SingleValueColumnConditionTest {

  @Test
  void test() {
    assertThrows(IllegalArgumentException.class, () -> new SingleValueColumnCondition<>(Employee.ID, 0, Operator.BETWEEN));
    assertThrows(IllegalArgumentException.class, () -> new SingleValueColumnCondition<>(Employee.ID, 0, Operator.BETWEEN_EXCLUSIVE));
    assertThrows(IllegalArgumentException.class, () -> new SingleValueColumnCondition<>(Employee.ID, 0, Operator.NOT_BETWEEN));
    assertThrows(IllegalArgumentException.class, () -> new SingleValueColumnCondition<>(Employee.ID, 0, Operator.NOT_BETWEEN_EXCLUSIVE));
  }
}
