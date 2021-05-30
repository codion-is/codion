/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.jul;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public final class JulProxyTest {

  @Test
  void test() {
    final JulProxy proxy = new JulProxy();
    proxy.getLogLevel();
    proxy.setLogLevel(proxy.getLogLevels().get(2));
    assertThrows(IllegalArgumentException.class, () -> proxy.setLogLevel("hello"));
  }
}
