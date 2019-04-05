/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common;

import org.junit.jupiter.api.Test;

import java.text.SimpleDateFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class DateFormatsTest {

  @Test
  public void getDateMask() {
    assertEquals("##-##-####", DateFormats.getDateMask(new SimpleDateFormat("dd-MM-yyyy")));
  }
}
