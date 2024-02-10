/*
 * Copyright (c) 2022 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db.operation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class DefaultFunctionTypeTest {

  @Test
  void test() {
    DefaultFunctionType<String, String, String> functionType = new DefaultFunctionType<>("test");
    assertEquals(functionType, new DefaultFunctionType<>("test"));
    assertEquals(functionType.name(), functionType.toString());
  }
}
