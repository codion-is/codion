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
            "org.jminor.common.value.Value",
            "org.jminor.common.state.State*",
            "org.jminor.common.i18n.*"
    );
    final SerializationWhitelist.SerializationFilter filter = new SerializationWhitelist.SerializationFilter(whitelistItems);
    assertEquals(filter.checkInput("org.jminor.common.value.Value"), ObjectInputFilter.Status.ALLOWED);
    assertEquals(filter.checkInput("org.jminor.common.state.State"), ObjectInputFilter.Status.ALLOWED);
    assertEquals(filter.checkInput("org.jminor.common.state.States"), ObjectInputFilter.Status.ALLOWED);
    assertEquals(filter.checkInput("org.jminor.common.state.StateObserver"), ObjectInputFilter.Status.ALLOWED);
    assertEquals(filter.checkInput("org.jminor.common.event.Event"), ObjectInputFilter.Status.REJECTED);
    assertEquals(filter.checkInput("org.jminor.common.i18n.Messages"), ObjectInputFilter.Status.ALLOWED);
  }

  @Test
  public void testNoWildcards() {
    final List<String> whitelistItems = asList(
            "org.jminor.common.value.Value",
            "org.jminor.common.state.State",
            "org.jminor.common.state.StateObserver"
    );
    final SerializationWhitelist.SerializationFilter filter = new SerializationWhitelist.SerializationFilter(whitelistItems);
    assertEquals(filter.checkInput("org.jminor.common.value.Value"), ObjectInputFilter.Status.ALLOWED);
    assertEquals(filter.checkInput("org.jminor.common.state.State"), ObjectInputFilter.Status.ALLOWED);
    assertEquals(filter.checkInput("org.jminor.common.state.States"), ObjectInputFilter.Status.REJECTED);
    assertEquals(filter.checkInput("org.jminor.common.state.StateObserver"), ObjectInputFilter.Status.ALLOWED);
    assertEquals(filter.checkInput("org.jminor.common.event.Event"), ObjectInputFilter.Status.REJECTED);
    assertEquals(filter.checkInput("org.jminor.common.i18n.Messages"), ObjectInputFilter.Status.REJECTED);
  }
}
