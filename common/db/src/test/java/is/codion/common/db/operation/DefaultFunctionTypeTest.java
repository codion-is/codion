/*
 * Copyright (c) 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db.operation;

import is.codion.common.db.exception.DatabaseException;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class DefaultFunctionTypeTest {

  @Test
  void test() throws DatabaseException {
    DefaultFunctionType<String, String, String> functionType = new DefaultFunctionType<>("test");
    assertEquals(functionType, new DefaultFunctionType<>("test"));
    assertEquals(functionType.getName(), functionType.toString());
    functionType.execute("conn", (connection, argument) -> "", "hello");
  }
}
