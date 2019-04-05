/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.control;

import org.jminor.common.DateFormats;
import org.jminor.common.Event;
import org.jminor.common.Events;
import org.jminor.swing.common.ui.UiUtil;
import org.jminor.swing.common.ui.ValueLinks;

import org.junit.jupiter.api.Test;

import javax.swing.JFormattedTextField;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class DateValueLinkTest {

  private final Event<LocalTime> evtTimeValueChanged = Events.event();
  private final Event<LocalDate> evtDateValueChanged = Events.event();
  private final Event<LocalDateTime> evtTimestampValueChanged = Events.event();

  private LocalTime timeValue;
  private LocalDate dateValue;
  private LocalDateTime timestamp;

  @Test
  public void testTime() throws Exception {
    final String format = "HH:mm";
    final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);

    final JFormattedTextField txtString = UiUtil.createFormattedField(DateFormats.getDateMask(format));
    ValueLinks.localTimeValueLink(txtString, this, "time", evtTimeValueChanged, false, format, true);
    assertEquals("__:__", txtString.getText());

    final LocalTime date = LocalTime.parse("22:42", formatter);

    setTime(date);
    assertEquals("22:42", txtString.getText());
    txtString.setText("23:50");
    assertEquals(LocalTime.parse("23:50", formatter), timeValue);
    txtString.setText("");
    assertNull(timeValue);
  }

  @Test
  public void testDate() throws Exception {
    final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DateFormats.SHORT_DOT);

    final JFormattedTextField txtString = UiUtil.createFormattedField(DateFormats.getDateMask(DateFormats.SHORT_DOT));
    ValueLinks.localDateValueLink(txtString, this, "date", evtDateValueChanged, false, DateFormats.SHORT_DOT, true);
    assertEquals("__.__.____", txtString.getText());

    final LocalDate date = LocalDate.parse("03.10.1975", formatter);

    setDate(date);
    assertEquals("03.10.1975", txtString.getText());
    txtString.setText("03.03.1983");
    assertEquals(LocalDate.parse("03.03.1983", formatter), dateValue);
    txtString.setText("");
    assertNull(dateValue);
  }

  @Test
  public void testTimestamp() throws Exception {
    final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DateFormats.SHORT_TIMESTAMP);

    final JFormattedTextField txtString = UiUtil.createFormattedField(DateFormats.getDateMask(DateFormats.SHORT_TIMESTAMP));
    ValueLinks.localDateTimeValueLink(txtString, this, "timestamp", evtTimestampValueChanged, false, DateFormats.SHORT_TIMESTAMP, true);
    assertEquals("__-__-__ __:__", txtString.getText());

    final LocalDateTime date = LocalDateTime.parse("03-10-75 10:34", formatter);

    setTimestamp(date);
    assertEquals("03-10-75 10:34", txtString.getText());
    txtString.setText("03-03-83 11:42");
    assertEquals(LocalDateTime.parse("03-03-83 11:42", formatter), timestamp);
    txtString.setText("");
    assertNull(timestamp);
  }

  public LocalDateTime getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(final LocalDateTime timestamp) {
    this.timestamp = timestamp;
    evtTimestampValueChanged.fire();
  }

  public LocalDate getDate() {
    return dateValue;
  }

  public void setDate(final LocalDate dateValue) {
    this.dateValue = dateValue;
    evtDateValueChanged.fire();
  }

  public LocalTime getTime() {
    return timeValue;
  }

  public void setTime(final LocalTime timeValue) {
    this.timeValue = timeValue;
    evtTimeValueChanged.fire();
  }
}
