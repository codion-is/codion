/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.remote;

import org.junit.jupiter.api.Test;
import sun.misc.ObjectInputFilter;

import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;

public final class SerializationWhitelistTest {

  @Test
  public void test() {
    final List<String> whitelistItems = asList(
            "org.jminor.common.Value",
            "org.jminor.common.State*",
            "org.jminor.common.i18n.*"
    );
    final SerializationWhitelist.SerializationFilter filter = new SerializationWhitelist.SerializationFilter(whitelistItems);
    assertEquals(filter.checkInput("org.jminor.common.Value"), sun.misc.ObjectInputFilter.Status.ALLOWED);
    assertEquals(filter.checkInput("org.jminor.common.State"), sun.misc.ObjectInputFilter.Status.ALLOWED);
    assertEquals(filter.checkInput("org.jminor.common.States"), sun.misc.ObjectInputFilter.Status.ALLOWED);
    assertEquals(filter.checkInput("org.jminor.common.StateObserver"), sun.misc.ObjectInputFilter.Status.ALLOWED);
    assertEquals(filter.checkInput("org.jminor.common.Event"), ObjectInputFilter.Status.REJECTED);
    assertEquals(filter.checkInput("org.jminor.common.i18n.Messages"), ObjectInputFilter.Status.ALLOWED);
  }
}
