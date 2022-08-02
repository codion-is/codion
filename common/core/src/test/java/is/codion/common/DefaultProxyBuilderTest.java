/*
 * Copyright (c) 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common;

import org.junit.jupiter.api.Test;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.List;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.*;

public final class DefaultProxyBuilderTest {

  @Test
  void test() {
    //not an interface
    assertThrows(IllegalArgumentException.class, () -> ProxyBuilder.builder(String.class));
    try {
      //non-existing method
      ProxyBuilder.builder(List.class)
            .method("nonexistingMethod", arguments -> null);
      fail();
    }
    catch (Exception e) {
      assertTrue(e.getCause() instanceof NoSuchMethodException);
    }

    List<Object> proxyInstance = ProxyBuilder.builder(List.class)
            .delegate(emptyList())
            .method("toString", arguments -> "toStringResult")
            .method("equals", Object.class, arguments ->
                    arguments.arguments().get(0) == emptyList())
            .build();
    assertEquals("toStringResult", proxyInstance.toString());
    assertNotEquals(proxyInstance, proxyInstance);
    assertEquals(proxyInstance, emptyList());
    proxyInstance.hashCode();
    assertTrue(proxyInstance::isEmpty);

    //delegate is an immutable list
    assertThrows(UndeclaredThrowableException.class, () -> proxyInstance.add(new Object()));

    List<Object> proxyInstance2 = ProxyBuilder.builder(List.class)
            .delegate(emptyList())
            .build();
    assertEquals(proxyInstance2, proxyInstance2);
  }
}
