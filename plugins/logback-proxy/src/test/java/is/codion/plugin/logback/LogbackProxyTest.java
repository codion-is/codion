/*
 * Copyright (c) 2021 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.logback;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public final class LogbackProxyTest {

  @Test
  void test() {
    LogbackProxy proxy = new LogbackProxy();
    proxy.getLogLevel();
    proxy.setLogLevel(proxy.levels().get(2));
    assertThrows(IllegalArgumentException.class, () -> proxy.setLogLevel("hello"));
    proxy.files();
  }
}
