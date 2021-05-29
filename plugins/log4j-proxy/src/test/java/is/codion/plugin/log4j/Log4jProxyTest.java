/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.log4j;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public final class Log4jProxyTest {

  @Test
  public void test() {
    final Log4jProxy proxy = new Log4jProxy();
    proxy.getLogLevel();
    proxy.setLogLevel(proxy.getLogLevels().get(2));
    assertThrows(IllegalArgumentException.class, () -> proxy.setLogLevel("hello"));
  }
}
