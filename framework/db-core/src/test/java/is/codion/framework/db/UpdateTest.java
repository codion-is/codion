/*
 * Copyright (c) 2016 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db;

import is.codion.framework.db.EntityConnection.Update;
import is.codion.framework.db.TestDomain.Employee;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public final class UpdateTest {

  @Test
  void updateDuplicate() {
    assertThrows(IllegalStateException.class, () -> Update.all(Employee.TYPE)
            .set(Employee.COMMISSION, 123d)
            .set(Employee.COMMISSION, 123d));
  }

  @Test
  void updateNoValues() {
    assertThrows(IllegalStateException.class, () -> Update.all(Employee.TYPE).build());
  }
}
