/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity.condition;

import is.codion.common.Operator;
import is.codion.framework.domain.TestDomain.Employee;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public final class DualValueColumnConditionTest {

  @Test
  void test() {
    assertThrows(IllegalArgumentException.class, () -> new DualValueColumnCondition<>(Employee.ID, 0, 1, Operator.EQUAL));
    assertThrows(IllegalArgumentException.class, () -> new DualValueColumnCondition<>(Employee.ID, 0, 1, Operator.NOT_EQUAL));
    assertThrows(IllegalArgumentException.class, () -> new DualValueColumnCondition<>(Employee.ID, 0, 1, Operator.GREATER_THAN));
    assertThrows(IllegalArgumentException.class, () -> new DualValueColumnCondition<>(Employee.ID, 0, 1, Operator.GREATER_THAN_OR_EQUAL));
    assertThrows(IllegalArgumentException.class, () -> new DualValueColumnCondition<>(Employee.ID, 0, 1, Operator.LESS_THAN));
    assertThrows(IllegalArgumentException.class, () -> new DualValueColumnCondition<>(Employee.ID, 0, 1, Operator.LESS_THAN_OR_EQUAL));
    assertThrows(NullPointerException.class, () -> new DualValueColumnCondition<>(Employee.ID, null, 1, Operator.BETWEEN));
    assertThrows(NullPointerException.class, () -> new DualValueColumnCondition<>(Employee.ID, 1, null, Operator.BETWEEN));
  }
}
