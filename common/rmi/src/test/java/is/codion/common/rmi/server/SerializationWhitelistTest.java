/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.rmi.server;

import is.codion.common.rmi.server.SerializationWhitelist.SerializationFilter;

import org.junit.jupiter.api.Test;
import sun.misc.ObjectInputFilter;

import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class SerializationWhitelistTest {

  @Test
  void testNoWildcards() {
    final List<String> whitelistItems = asList(
            "#comment",
            "is.codion.common.value.Value",
            "is.codion.common.state.State",
            "is.codion.common.state.StateObserver"
    );
    final SerializationFilter filter = new SerializationFilter(whitelistItems);
    assertEquals(filter.checkInput("is.codion.common.value.Value"), sun.misc.ObjectInputFilter.Status.ALLOWED);
    assertEquals(filter.checkInput("is.codion.common.state.State"), sun.misc.ObjectInputFilter.Status.ALLOWED);
    assertEquals(filter.checkInput("is.codion.common.state.States"), ObjectInputFilter.Status.REJECTED);
    assertEquals(filter.checkInput("is.codion.common.state.StateObserver"), sun.misc.ObjectInputFilter.Status.ALLOWED);
    assertEquals(filter.checkInput("is.codion.common.event.Event"), ObjectInputFilter.Status.REJECTED);
    assertEquals(filter.checkInput("is.codion.common.i18n.Messages"), ObjectInputFilter.Status.REJECTED);
  }

  @Test
  void file() {
    assertThrows(RuntimeException.class, () -> new SerializationFilter("src/test/resources/whitelist_test_non_existing.txt"));
    testFilter(new SerializationFilter("src/test/resources/whitelist_test.txt"));
  }

  @Test
  void classpath() {
    assertThrows(IllegalArgumentException.class, () -> new SerializationFilter("classpath:src/test/resources/whitelist_test.txt"));
    testFilter(new SerializationFilter("classpath:whitelist_test.txt"));
    testFilter(new SerializationFilter("classpath:/whitelist_test.txt"));
  }

  private static void testFilter(final SerializationFilter filter) {
    assertEquals(filter.checkInput("is.codion.common.value.Value"), ObjectInputFilter.Status.ALLOWED);
    assertEquals(filter.checkInput("is.codion.common.state.State"), ObjectInputFilter.Status.ALLOWED);
    assertEquals(filter.checkInput("is.codion.common.state.States"), ObjectInputFilter.Status.ALLOWED);
    assertEquals(filter.checkInput("is.codion.common.state.StateObserver"), ObjectInputFilter.Status.ALLOWED);
    assertEquals(filter.checkInput("is.codion.common.event.Event"), ObjectInputFilter.Status.REJECTED);
    assertEquals(filter.checkInput("is.codion.common.i18n.Messages"), ObjectInputFilter.Status.ALLOWED);
  }
}
