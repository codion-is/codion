package dev.codion.swing.common.ui.value;

import dev.codion.common.DateFormats;
import dev.codion.common.event.Event;
import dev.codion.common.event.Events;
import dev.codion.common.value.Value;
import dev.codion.common.value.Values;
import dev.codion.swing.common.ui.textfield.TextFields;
import dev.codion.swing.common.ui.time.LocalDateInputPanel;
import dev.codion.swing.common.ui.time.TemporalInputPanel;

import org.junit.jupiter.api.Test;

import javax.swing.JFormattedTextField;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class TemporalValuesTest {

  private final Event<LocalTime> timeValueChangedEvent = Events.event();
  private final Event<LocalDate> dateValueChangedEvent = Events.event();
  private final Event<LocalDateTime> timestampValueChangedEvent = Events.event();

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
  public void testTime() throws Exception {
    final String format = "HH:mm";
    final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);

    final JFormattedTextField textField = TextFields.createFormattedField(DateFormats.getDateMask(format));
    final Value<LocalTime> timePropertyValue = Values.propertyValue(this, "time",
            LocalTime.class, timeValueChangedEvent);
    timePropertyValue.link(TemporalValues.localTimeValue(textField, format));
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

    final JFormattedTextField textField = TextFields.createFormattedField(DateFormats.getDateMask(DateFormats.SHORT_DOT));
    final Value<LocalDate> datePropertyValue = Values.propertyValue(this, "date",
            LocalDate.class, dateValueChangedEvent);
    datePropertyValue.link(TemporalValues.localDateValue(textField, DateFormats.SHORT_DOT));
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

    final JFormattedTextField textField = TextFields.createFormattedField(DateFormats.getDateMask(DateFormats.SHORT_TIMESTAMP));
    final Value<LocalDateTime> timestampPropertyValue = Values.propertyValue(this, "timestamp",
            LocalDateTime.class, timestampValueChangedEvent);
    timestampPropertyValue.link(TemporalValues.localDateTimeValue(textField, DateFormats.SHORT_TIMESTAMP));
    assertEquals("__-__-__ __:__", textField.getText());

    final LocalDateTime date = LocalDateTime.parse("03-10-75 10:34", formatter);

    setTimestamp(date);
    assertEquals("03-10-75 10:34", textField.getText());
    textField.setText("03-03-83 11:42");
    assertEquals(LocalDateTime.parse("03-03-83 11:42", formatter), this.timestamp);
    textField.setText("");
    assertNull(this.timestamp);
  }

  @Test
  public void localTimeUiValue() {
    final String format = "HH:mm";
    final JFormattedTextField textField = TextFields.createFormattedField(DateFormats.getDateMask(format));//HH:mm
    final Value<LocalTime> value = TemporalValues.localTimeValue(textField, format);

    final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);

    assertNull(value.get());
    final String timeString = "22:42";
    textField.setText(timeString);
    final LocalTime date = value.get();
    assertEquals(LocalTime.parse(timeString, formatter), date);

    final String invalidDateString = "23:";
    textField.setText(invalidDateString);
    assertEquals("23:__", textField.getText());
    assertNull(value.get());

    value.set(LocalTime.parse(timeString, formatter));
    assertEquals(timeString, textField.getText());
  }

  @Test
  public void localDateUiValue() {
    final JFormattedTextField textField = TextFields.createFormattedField(DateFormats.getDateMask(DateFormats.SHORT_DASH));//dd-MM-yyyy
    final Value<LocalDate> value = TemporalValues.localDateValue(textField, DateFormats.SHORT_DASH);

    final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DateFormats.SHORT_DASH);

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
  public void localDateTimeUiValue() {
    final JFormattedTextField textField = TextFields.createFormattedField(DateFormats.getDateMask(DateFormats.TIMESTAMP));//dd-MM-yyyy HH:mm
    final Value<LocalDateTime> value = TemporalValues.localDateTimeValue(textField, DateFormats.TIMESTAMP);

    final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DateFormats.TIMESTAMP);

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
  public void temporalValue() {
    final LocalDate date = LocalDate.now();
    ComponentValue<LocalDate, TemporalInputPanel<LocalDate>> componentValue =
            TemporalValues.temporalValue(new LocalDateInputPanel(date, DateFormats.SHORT_DASH));
    assertEquals(date, componentValue.get());

    componentValue = new TemporalInputPanelValue(new LocalDateInputPanel(null, DateFormats.SHORT_DASH));
    assertNull(componentValue.get());

    componentValue.getComponent().getInputField().setText(DateTimeFormatter.ofPattern(DateFormats.SHORT_DASH).format(date));
    assertEquals(date, componentValue.get());
  }
}
