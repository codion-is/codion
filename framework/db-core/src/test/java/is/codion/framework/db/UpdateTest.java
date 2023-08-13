/*
 * Copyright (c) 2016 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db;

import is.codion.framework.db.EntityConnection.Update;
import is.codion.framework.db.TestDomain.Employee;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public final class UpdateTest {

  @Test
  void updateConditionDuplicate() {
    assertThrows(IllegalArgumentException.class, () -> Update.all(Employee.TYPE)
            .set(Employee.COMMISSION, 123d)
            .set(Employee.COMMISSION, 123d));
  }
}
