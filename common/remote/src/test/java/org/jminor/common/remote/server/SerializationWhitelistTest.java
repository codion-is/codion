/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.remote.server;

import org.junit.jupiter.api.Test;
import sun.misc.ObjectInputFilter;

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
    assertEquals(filter.checkInput("org.jminor.common.value.Value"), sun.misc.ObjectInputFilter.Status.ALLOWED);
    assertEquals(filter.checkInput("org.jminor.common.state.State"), sun.misc.ObjectInputFilter.Status.ALLOWED);
    assertEquals(filter.checkInput("org.jminor.common.state.States"), sun.misc.ObjectInputFilter.Status.ALLOWED);
    assertEquals(filter.checkInput("org.jminor.common.state.StateObserver"), sun.misc.ObjectInputFilter.Status.ALLOWED);
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
    assertEquals(filter.checkInput("org.jminor.common.value.Value"), sun.misc.ObjectInputFilter.Status.ALLOWED);
    assertEquals(filter.checkInput("org.jminor.common.state.State"), sun.misc.ObjectInputFilter.Status.ALLOWED);
    assertEquals(filter.checkInput("org.jminor.common.state.States"), ObjectInputFilter.Status.REJECTED);
    assertEquals(filter.checkInput("org.jminor.common.state.StateObserver"), sun.misc.ObjectInputFilter.Status.ALLOWED);
    assertEquals(filter.checkInput("org.jminor.common.event.Event"), ObjectInputFilter.Status.REJECTED);
    assertEquals(filter.checkInput("org.jminor.common.i18n.Messages"), ObjectInputFilter.Status.REJECTED);
  }
}
