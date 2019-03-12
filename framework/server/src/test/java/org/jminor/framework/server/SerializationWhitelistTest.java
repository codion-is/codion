/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server;

import org.junit.jupiter.api.Test;
import sun.misc.ObjectInputFilter;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class SerializationWhitelistTest {

  @Test
  public void test() {
    final List<String> whitelistItems = Arrays.asList(
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
