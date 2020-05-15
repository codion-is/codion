/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.common.rmi.server;

import org.junit.jupiter.api.Test;

import java.io.ObjectInputFilter;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;

public final class SerializationWhitelistTest {

  @Test
  public void test() {
    final List<String> whitelistItems = asList(
            "dev.codion.common.value.Value",
            "dev.codion.common.state.State*",
            "dev.codion.common.i18n.*"
    );
    final SerializationWhitelist.SerializationFilter filter = new SerializationWhitelist.SerializationFilter(whitelistItems);
    assertEquals(filter.checkInput("dev.codion.common.value.Value"), ObjectInputFilter.Status.ALLOWED);
    assertEquals(filter.checkInput("dev.codion.common.state.State"), ObjectInputFilter.Status.ALLOWED);
    assertEquals(filter.checkInput("dev.codion.common.state.States"), ObjectInputFilter.Status.ALLOWED);
    assertEquals(filter.checkInput("dev.codion.common.state.StateObserver"), ObjectInputFilter.Status.ALLOWED);
    assertEquals(filter.checkInput("dev.codion.common.event.Event"), ObjectInputFilter.Status.REJECTED);
    assertEquals(filter.checkInput("dev.codion.common.i18n.Messages"), ObjectInputFilter.Status.ALLOWED);
  }

  @Test
  public void testNoWildcards() {
    final List<String> whitelistItems = asList(
            "dev.codion.common.value.Value",
            "dev.codion.common.state.State",
            "dev.codion.common.state.StateObserver"
    );
    final SerializationWhitelist.SerializationFilter filter = new SerializationWhitelist.SerializationFilter(whitelistItems);
    assertEquals(filter.checkInput("dev.codion.common.value.Value"), ObjectInputFilter.Status.ALLOWED);
    assertEquals(filter.checkInput("dev.codion.common.state.State"), ObjectInputFilter.Status.ALLOWED);
    assertEquals(filter.checkInput("dev.codion.common.state.States"), ObjectInputFilter.Status.REJECTED);
    assertEquals(filter.checkInput("dev.codion.common.state.StateObserver"), ObjectInputFilter.Status.ALLOWED);
    assertEquals(filter.checkInput("dev.codion.common.event.Event"), ObjectInputFilter.Status.REJECTED);
    assertEquals(filter.checkInput("dev.codion.common.i18n.Messages"), ObjectInputFilter.Status.REJECTED);
  }
}
