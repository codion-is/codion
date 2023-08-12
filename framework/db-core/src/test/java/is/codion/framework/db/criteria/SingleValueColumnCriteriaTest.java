/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.criteria;

import is.codion.common.Operator;
import is.codion.framework.db.TestDomain.Employee;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public final class SingleValueColumnCriteriaTest {

  @Test
  void test() {
    assertThrows(IllegalArgumentException.class, () -> new SingleValueColumnCriteria<>(Employee.ID, 0, Operator.BETWEEN));
    assertThrows(IllegalArgumentException.class, () -> new SingleValueColumnCriteria<>(Employee.ID, 0, Operator.BETWEEN_EXCLUSIVE));
    assertThrows(IllegalArgumentException.class, () -> new SingleValueColumnCriteria<>(Employee.ID, 0, Operator.NOT_BETWEEN));
    assertThrows(IllegalArgumentException.class, () -> new SingleValueColumnCriteria<>(Employee.ID, 0, Operator.NOT_BETWEEN_EXCLUSIVE));
  }
}
