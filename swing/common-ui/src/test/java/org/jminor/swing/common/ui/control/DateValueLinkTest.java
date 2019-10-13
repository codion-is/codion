/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.control;

import org.jminor.common.DateFormats;
import org.jminor.common.Event;
import org.jminor.common.Events;
import org.jminor.common.Value;
import org.jminor.common.Values;
import org.jminor.swing.common.ui.UiUtil;
import org.jminor.swing.common.ui.UpdateTrigger;
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

  private final Event<LocalTime> timeValueChangedEvent = Events.event();
  private final Event<LocalDate> dateValueChangedEvent = Events.event();
  private final Event<LocalDateTime> timestampValueChangedEvent = Events.event();

  private LocalTime timeValue;
  private LocalDate dateValue;
  private LocalDateTime timestamp;

  @Test
  public void testTime() throws Exception {
    final String format = "HH:mm";
    final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);

    final JFormattedTextField textField = UiUtil.createFormattedField(DateFormats.getDateMask(format));
    final Value<LocalTime> timePropertyValue = Values.propertyValue(this, "time",
            LocalTime.class, timeValueChangedEvent);
    ValueLinks.localTimeValueLink(textField, timePropertyValue, format, UpdateTrigger.KEYSTROKE);
    assertEquals("__:__", textField.getText());

    final LocalTime date = LocalTime.parse("22:42", formatter);

    setTime(date);
    assertEquals("22:42", textField.getText());
    textField.setText("23:50");
    assertEquals(LocalTime.parse("23:50", formatter), timeValue);
    textField.setText("");
    assertNull(timeValue);
  }

  @Test
  public void testDate() throws Exception {
    final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DateFormats.SHORT_DOT);

    final JFormattedTextField textField = UiUtil.createFormattedField(DateFormats.getDateMask(DateFormats.SHORT_DOT));
    final Value<LocalDate> datePropertyValue = Values.propertyValue(this, "date",
            LocalDate.class, dateValueChangedEvent);
    ValueLinks.localDateValueLink(textField, datePropertyValue, DateFormats.SHORT_DOT, UpdateTrigger.KEYSTROKE);
    assertEquals("__.__.____", textField.getText());

    final LocalDate date = LocalDate.parse("03.10.1975", formatter);

    setDate(date);
    assertEquals("03.10.1975", textField.getText());
    textField.setText("03.03.1983");
    assertEquals(LocalDate.parse("03.03.1983", formatter), dateValue);
    textField.setText("");
    assertNull(dateValue);
  }

  @Test
  public void testTimestamp() throws Exception {
    final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DateFormats.SHORT_TIMESTAMP);

    final JFormattedTextField textField = UiUtil.createFormattedField(DateFormats.getDateMask(DateFormats.SHORT_TIMESTAMP));
    final Value<LocalDateTime> timestampPropertyValue = Values.propertyValue(this, "timestamp",
            LocalDateTime.class, timestampValueChangedEvent);
    ValueLinks.localDateTimeValueLink(textField, timestampPropertyValue, DateFormats.SHORT_TIMESTAMP, UpdateTrigger.KEYSTROKE);
    assertEquals("__-__-__ __:__", textField.getText());

    final LocalDateTime date = LocalDateTime.parse("03-10-75 10:34", formatter);

    setTimestamp(date);
    assertEquals("03-10-75 10:34", textField.getText());
    textField.setText("03-03-83 11:42");
    assertEquals(LocalDateTime.parse("03-03-83 11:42", formatter), this.timestamp);
    textField.setText("");
    assertNull(this.timestamp);
  }

  public LocalDateTime getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(final LocalDateTime timestamp) {
    this.timestamp = timestamp;
    timestampValueChangedEvent.fire();
  }

  public LocalDate getDate() {
    return dateValue;
  }

  public void setDate(final LocalDate dateValue) {
    this.dateValue = dateValue;
    dateValueChangedEvent.fire();
  }

  public LocalTime getTime() {
    return timeValue;
  }

  public void setTime(final LocalTime timeValue) {
    this.timeValue = timeValue;
    timeValueChangedEvent.fire();
  }
}
