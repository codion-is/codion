/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db.operation;

import is.codion.common.db.exception.DatabaseException;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class DefaultProcedureTypeTest {

  @Test
  void test() throws DatabaseException {
    DefaultProcedureType<String, String> procedureType = new DefaultProcedureType<>("test");
    assertEquals(procedureType, new DefaultProcedureType<>("test"));
    assertEquals(procedureType.name(), procedureType.toString());
    procedureType.execute("conn", (connection, argument) -> {}, "hello");
  }
}
