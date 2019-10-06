/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.remote;

import org.junit.jupiter.api.Test;

import java.io.ObjectInputFilter;
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
    assertEquals(filter.checkInput("org.jminor.common.Value"), java.io.ObjectInputFilter.Status.ALLOWED);
    assertEquals(filter.checkInput("org.jminor.common.State"), java.io.ObjectInputFilter.Status.ALLOWED);
    assertEquals(filter.checkInput("org.jminor.common.States"), java.io.ObjectInputFilter.Status.ALLOWED);
    assertEquals(filter.checkInput("org.jminor.common.StateObserver"), java.io.ObjectInputFilter.Status.ALLOWED);
    assertEquals(filter.checkInput("org.jminor.common.Event"), ObjectInputFilter.Status.REJECTED);
    assertEquals(filter.checkInput("org.jminor.common.i18n.Messages"), ObjectInputFilter.Status.ALLOWED);
  }
}
