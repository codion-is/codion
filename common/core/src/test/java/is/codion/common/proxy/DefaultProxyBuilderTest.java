/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2022 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.common.proxy;

import org.junit.jupiter.api.Test;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
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
              .method("nonexistingMethod", parameters -> null);
      fail();
    }
    catch (Exception e) {
      assertTrue(e.getCause() instanceof NoSuchMethodException);
    }

    List<Object> proxyInstance = ProxyBuilder.builder(List.class)
            .delegate(emptyList())
            .method("toString", parameters -> "toStringResult")
            .method("equals", Object.class, parameters ->
                    parameters.arguments().get(0) == emptyList())
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

    List<Object> proxyInstance3 = ProxyBuilder.builder(List.class)
            .method("size", parameters -> parameters.delegate().size())
            .build();
    assertThrows(IllegalStateException.class, proxyInstance3::size);//no delegate
    assertThrows(UnsupportedOperationException.class, () -> proxyInstance3.add("testing"));
  }

  @Test
  void example() {
    List<String> list = new ArrayList<>();

    List<String> listProxy = ProxyBuilder.builder(List.class)
            .delegate(list)
            .method("add", Object.class, parameters -> {
              Object item = parameters.arguments().get(0);
              System.out.println("Adding: " + item);

              return parameters.delegate().add(item);
            })
            .method("remove", Object.class, parameters -> {
              Object item = parameters.arguments().get(0);
              System.out.println("Removing: " + item);

              return parameters.delegate().remove(item);
            })
            .method("equals", Object.class, parameters -> {
              Object object = parameters.arguments().get(0);
              System.out.println("Equals: " + object);

              return parameters.delegate().equals(object);
            })
            .method("size", parameters -> {
              System.out.println("Size");

              return parameters.delegate().size();
            })
            .build();

    listProxy.add("hey");
    listProxy.add("whatsup");
    listProxy.remove("hey");
    listProxy.size();
    listProxy.equals(list);
  }
}
