/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2010 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.value.Value;
import is.codion.swing.common.ui.component.text.TemporalField;
import is.codion.swing.common.ui.component.value.ComponentValue;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class TemporalValuesTest {

  @Test
  void testTime() {
    final String format = "HH:mm";
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);

    Value<LocalTime> timePropertyValue = Value.value();
    TemporalField<LocalTime> textField = Components.localTimeField(format, timePropertyValue)
            .build();
    assertEquals("__:__", textField.getText());

    LocalTime date = LocalTime.parse("22:42", formatter);

    timePropertyValue.set(date);
    assertEquals("22:42", textField.getText());
    textField.setText("23:50");
    assertEquals(LocalTime.parse("23:50", formatter), timePropertyValue.get());
    textField.setText("");
    assertNull(timePropertyValue.get());
  }

  @Test
  void testDate() {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    Value<LocalDate> datePropertyValue = Value.value();
    TemporalField<LocalDate> textField = Components.localDateField("dd.MM.yyyy", datePropertyValue)
            .build();
    assertEquals("__.__.____", textField.getText());

    LocalDate date = LocalDate.parse("03.10.1975", formatter);

    datePropertyValue.set(date);
    assertEquals("03.10.1975", textField.getText());
    textField.setText("03.03.1983");
    assertEquals(LocalDate.parse("03.03.1983", formatter), datePropertyValue.get());
    textField.setText("");
    assertNull(datePropertyValue.get());
  }

  @Test
  void testTimestamp() {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yy HH:mm");

    Value<LocalDateTime> timestampPropertyValue = Value.value();
    TemporalField<LocalDateTime> textField = Components.localDateTimeField("dd-MM-yy HH:mm", timestampPropertyValue)
            .build();
    assertEquals("__-__-__ __:__", textField.getText());

    LocalDateTime date = LocalDateTime.parse("03-10-75 10:34", formatter);

    timestampPropertyValue.set(date);
    assertEquals("03-10-75 10:34", textField.getText());
    textField.setText("03-03-83 11:42");
    assertEquals(LocalDateTime.parse("03-03-83 11:42", formatter), timestampPropertyValue.get());
    textField.setText("");
    assertNull(timestampPropertyValue.get());
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
