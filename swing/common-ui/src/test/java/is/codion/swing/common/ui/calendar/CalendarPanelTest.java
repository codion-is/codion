/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.calendar;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

public final class CalendarPanelTest {

  @Test
  void constructor() {
    new CalendarPanel(true).setDate(LocalDate.now());
  }
}
