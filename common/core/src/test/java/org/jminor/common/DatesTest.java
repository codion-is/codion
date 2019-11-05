/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Month;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DatesTest {

  @Test
  public void numberOfDaysInRange() {
    final LocalDate start = LocalDate.of(2011, Month.JANUARY, 1);
    LocalDate end = start;
    assertEquals(1, Dates.numberOfDaysInRange(start, end));

    end = LocalDate.of(2011, Month.JANUARY, 2);
    assertEquals(2, Dates.numberOfDaysInRange(start, end));
    end = LocalDate.of(2011, Month.JANUARY, 3);
    assertEquals(3, Dates.numberOfDaysInRange(start, end));
    end = LocalDate.of(2011, Month.FEBRUARY, 1);
    assertEquals(32, Dates.numberOfDaysInRange(start, end));
  }

  @Test
  public void numberOfDaysInRangeToAfterFrom() {
    assertThrows(IllegalArgumentException.class, () -> Dates.numberOfDaysInRange(LocalDate.of(2011, Month.FEBRUARY, 1),
            LocalDate.of(2011, Month.JANUARY, 1)));
  }
}
