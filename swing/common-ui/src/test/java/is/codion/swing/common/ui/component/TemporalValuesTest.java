/*
 * Copyright (c) 2010 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.event.Event;
import is.codion.common.value.Value;
import is.codion.swing.common.ui.component.text.TemporalField;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class TemporalValuesTest {

  private final Event<LocalTime> timeValueChangedEvent = Event.event();
  private final Event<LocalDate> dateValueChangedEvent = Event.event();
  private final Event<LocalDateTime> timestampValueChangedEvent = Event.event();

  private LocalTime timeValue;
  private LocalDate dateValue;
  private LocalDateTime timestamp;

  public LocalDateTime getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(LocalDateTime timestamp) {
    this.timestamp = timestamp;
    timestampValueChangedEvent.onEvent();
  }

  public LocalDate getDate() {
    return dateValue;
  }

  public void setDate(LocalDate dateValue) {
    this.dateValue = dateValue;
    dateValueChangedEvent.onEvent();
  }

  public LocalTime getTime() {
    return timeValue;
  }

  public void setTime(LocalTime timeValue) {
    this.timeValue = timeValue;
    timeValueChangedEvent.onEvent();
  }

  @Test
  void testTime() throws Exception {
    final String format = "HH:mm";
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);

    Value<LocalTime> timePropertyValue = Value.propertyValue(this, "time",
            LocalTime.class, timeValueChangedEvent);
    TemporalField<LocalTime> textField = Components.localTimeField(format, timePropertyValue)
            .build();
    assertEquals("__:__", textField.getText());

    LocalTime date = LocalTime.parse("22:42", formatter);

    setTime(date);
    assertEquals("22:42", textField.getText());
    textField.setText("23:50");
    assertEquals(LocalTime.parse("23:50", formatter), timeValue);
    textField.setText("");
    assertNull(timeValue);
  }

  @Test
  void testDate() throws Exception {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    Value<LocalDate> datePropertyValue = Value.propertyValue(this, "date",
            LocalDate.class, dateValueChangedEvent);
    TemporalField<LocalDate> textField = Components.localDateField("dd.MM.yyyy", datePropertyValue)
            .build();
    assertEquals("__.__.____", textField.getText());

    LocalDate date = LocalDate.parse("03.10.1975", formatter);

    setDate(date);
    assertEquals("03.10.1975", textField.getText());
    textField.setText("03.03.1983");
    assertEquals(LocalDate.parse("03.03.1983", formatter), dateValue);
    textField.setText("");
    assertNull(dateValue);
  }

  @Test
  void testTimestamp() throws Exception {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yy HH:mm");

    Value<LocalDateTime> timestampPropertyValue = Value.propertyValue(this, "timestamp",
            LocalDateTime.class, timestampValueChangedEvent);
    TemporalField<LocalDateTime> textField = Components.localDateTimeField("dd-MM-yy HH:mm", timestampPropertyValue)
            .build();
    assertEquals("__-__-__ __:__", textField.getText());

    LocalDateTime date = LocalDateTime.parse("03-10-75 10:34", formatter);

    setTimestamp(date);
    assertEquals("03-10-75 10:34", textField.getText());
    textField.setText("03-03-83 11:42");
    assertEquals(LocalDateTime.parse("03-03-83 11:42", formatter), this.timestamp);
    textField.setText("");
    assertNull(this.timestamp);
  }

  @Test
  void localTimeUiValue() {
    final String format = "HH:mm";

    ComponentValue<LocalTime, TemporalField<LocalTime>> value = Components.localTimeField(format)
            .buildValue();
    TemporalField<LocalTime> textField = value.component();

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);

    assertNull(value.get());
    final String timeString = "22:42";
    textField.setText(timeString);
    LocalTime date = value.get();
    assertEquals(LocalTime.parse(timeString, formatter), date);

    final String invalidDateString = "23:";
    textField.setText(invalidDateString);
    assertEquals("23:__", textField.getText());
    assertNull(value.get());

    value.set(LocalTime.parse(timeString, formatter));
    assertEquals(timeString, textField.getText());
  }

  @Test
  void localDateUiValue() {
    ComponentValue<LocalDate, TemporalField<LocalDate>> value = Components.localDateField("dd-MM-yyyy")
            .buildValue();
    TemporalField<LocalDate> textField = value.component();

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    assertNull(value.get());
    final String dateString = "03-10-1975";
    textField.setText(dateString);
    assertEquals(LocalDate.parse(dateString, formatter), value.get());

    final String invalidDateString = "03-10-19";
    textField.setText(invalidDateString);
    assertEquals(invalidDateString + "__", textField.getText());
    assertNull(value.get());

    value.set(LocalDate.parse(dateString, formatter));
    assertEquals(dateString, textField.getText());
  }

  @Test
  void localDateTimeUiValue() {
    ComponentValue<LocalDateTime, TemporalField<LocalDateTime>> value = Components.localDateTimeField("dd-MM-yyyy HH:mm")
            .buildValue();
    TemporalField<LocalDateTime> textField = value.component();

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    assertNull(value.get());
    final String dateString = "03-10-1975 22:45";
    textField.setText(dateString);
    assertEquals(LocalDateTime.parse(dateString, formatter), value.get());

    final String invalidDateString = "03-10-1975 22";
    textField.setText(invalidDateString);
    assertEquals(invalidDateString + ":__", textField.getText());
    assertNull(value.get());

    value.set(LocalDateTime.parse(dateString, formatter));
    assertEquals(dateString, textField.getText());
  }
}
