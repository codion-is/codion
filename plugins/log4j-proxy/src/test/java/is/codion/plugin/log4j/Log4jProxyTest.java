/*
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.log4j;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public final class Log4jProxyTest {

  @Test
  void test() {
    Log4jProxy proxy = new Log4jProxy();
    proxy.getLogLevel();
    proxy.setLogLevel(proxy.levels().get(2));
    assertThrows(IllegalArgumentException.class, () -> proxy.setLogLevel("hello"));
    proxy.files();
  }
}
