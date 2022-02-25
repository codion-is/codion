/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.event.Event;
import is.codion.common.value.Value;
import is.codion.swing.common.ui.textfield.TemporalField;
import is.codion.swing.common.ui.textfield.TemporalInputPanel;

import org.junit.jupiter.api.Test;

import javax.swing.JComponent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.Optional;

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

  public void setTimestamp(final LocalDateTime timestamp) {
    this.timestamp = timestamp;
    timestampValueChangedEvent.onEvent();
  }

  public LocalDate getDate() {
    return dateValue;
  }

  public void setDate(final LocalDate dateValue) {
    this.dateValue = dateValue;
    dateValueChangedEvent.onEvent();
  }

  public LocalTime getTime() {
    return timeValue;
  }

  public void setTime(final LocalTime timeValue) {
    this.timeValue = timeValue;
    timeValueChangedEvent.onEvent();
  }

  @Test
  void testTime() throws Exception {
    final String format = "HH:mm";
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);

    TemporalField<LocalTime> textField = TemporalField.localTimeField(format);
    Value<LocalTime> timePropertyValue = Value.propertyValue(this, "time",
            LocalTime.class, timeValueChangedEvent);
    textField.componentValue().link(timePropertyValue);
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

    TemporalField<LocalDate> textField = TemporalField.localDateField("dd.MM.yyyy");
    Value<LocalDate> datePropertyValue = Value.propertyValue(this, "date",
            LocalDate.class, dateValueChangedEvent);
    textField.componentValue().link(datePropertyValue);
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

    TemporalField<LocalDateTime> textField = TemporalField.localDateTimeField("dd-MM-yy HH:mm");
    Value<LocalDateTime> timestampPropertyValue = Value.propertyValue(this, "timestamp",
            LocalDateTime.class, timestampValueChangedEvent);
    textField.componentValue().link(timestampPropertyValue);
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

    TemporalField<LocalTime> textField = TemporalField.localTimeField(format);
    Value<LocalTime> value = textField.componentValue();

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
    TemporalField<LocalDate> textField = TemporalField.localDateField("dd-MM-yyyy");
    Value<LocalDate> value = textField.componentValue();

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
    TemporalField<LocalDateTime> textField = TemporalField.localDateTimeField("dd-MM-yyyy HH:mm");
    Value<LocalDateTime> value = textField.componentValue();

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

  @Test
  void temporalValue() {
    LocalDate date = LocalDate.now();
    TemporalField<LocalDate> localDateField = TemporalField.localDateField("dd-MM-yyyy");
    localDateField.setTemporal(date);
    ComponentValue<LocalDate, TemporalInputPanel<LocalDate>> componentValue =
            new TemporalInputPanel<>(localDateField, new DefaultCalendarProvider()).componentValue();
    assertEquals(date, componentValue.get());

    localDateField.setTemporal(null);

    componentValue = new TemporalInputPanel<>(localDateField, new DefaultCalendarProvider()).componentValue();
    assertNull(componentValue.get());

    componentValue.getComponent().getInputField().setText(DateTimeFormatter.ofPattern("dd-MM-yyyy").format(date));
    assertEquals(date, componentValue.get());
  }

  private static final class DefaultCalendarProvider implements TemporalInputPanel.CalendarProvider {

    @Override
    public <T extends Temporal> Optional<T> getTemporal(final Class<T> temporalClass, final JComponent dialogOwner,
                                                        final T initialValue) {
      return Optional.empty();
    }

    @Override
    public <T extends Temporal> boolean supports(final Class<T> temporalClass) {
      return false;
    }
  }
}
