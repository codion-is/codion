/*
 * Copyright (c) 2010 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.jul;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public final class JulProxyTest {

  @Test
  void test() {
    JulProxy proxy = new JulProxy();
    proxy.getLogLevel();
    proxy.setLogLevel(proxy.levels().get(2));
    assertThrows(IllegalArgumentException.class, () -> proxy.setLogLevel("hello"));
  }
}
