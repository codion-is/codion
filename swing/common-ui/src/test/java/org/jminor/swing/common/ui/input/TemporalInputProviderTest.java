/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.input;

import org.jminor.common.DateFormats;
import org.jminor.swing.common.ui.LocalDateInputPanel;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class TemporalInputProviderTest {

  @Test
  public void test() {
    final LocalDate date = LocalDate.now();
    TemporalInputProvider<LocalDate> provider = new TemporalInputProvider(new LocalDateInputPanel(date, DateFormats.SHORT_DASH));
    assertEquals(date, provider.getValue());

    provider = new TemporalInputProvider(new LocalDateInputPanel(null, DateFormats.SHORT_DASH));
    assertNull(provider.getValue());

    provider.getInputComponent().getInputField().setText(DateTimeFormatter.ofPattern(DateFormats.SHORT_DASH).format(date));
    assertEquals(date, provider.getValue());
  }
}
