/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui;

import org.jminor.common.DateFormats;
import org.jminor.common.Value;
import org.jminor.swing.common.model.checkbox.TristateButtonModel;
import org.jminor.swing.common.ui.textfield.DoubleField;
import org.jminor.swing.common.ui.textfield.IntegerField;
import org.jminor.swing.common.ui.textfield.LongField;

import org.junit.jupiter.api.Test;

import javax.swing.BoundedRangeModel;
import javax.swing.ButtonModel;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.DefaultButtonModel;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

public class UiValuesTest {

  @Test
  public void localTimeUiValue() {
    final String format = "HH:mm";
    final JFormattedTextField textField = UiUtil.createFormattedField(DateFormats.getDateMask(format));//HH:mm
    final Value<LocalTime> value = UiValues.localTimeValue(textField, format, true);

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
    final JFormattedTextField textField = UiUtil.createFormattedField(DateFormats.getDateMask(DateFormats.SHORT_DASH));//dd-MM-yyyy
    final Value<LocalDate> value = UiValues.localDateValue(textField, DateFormats.SHORT_DASH, true);

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
    final JFormattedTextField textField = UiUtil.createFormattedField(DateFormats.getDateMask(DateFormats.TIMESTAMP));//dd-MM-yyyy HH:mm
    final Value<LocalDateTime> value = UiValues.localDateTimeValue(textField, DateFormats.TIMESTAMP, true);

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
  public void integerSpinnerUiValue() {
    final SpinnerNumberModel model = new SpinnerNumberModel();
    final Value<Integer> value = UiValues.integerValue(model);

    assertEquals(Integer.valueOf(0), value.get());
    model.setValue(122);
    assertEquals(Integer.valueOf(122), value.get());
    model.setValue(0);
    assertEquals(Integer.valueOf(0), value.get());

    value.set(42);
    assertEquals(42, model.getValue());
  }

  @Test
  public void integerBoundedRangeModelUiValue() {
    final BoundedRangeModel model = new DefaultBoundedRangeModel(0, 0, 0, 150);
    final Value<Integer> value = UiValues.integerValue(model);

    assertEquals(Integer.valueOf(0), value.get());
    model.setValue(122);
    assertEquals(Integer.valueOf(122), value.get());
    model.setValue(0);
    assertEquals(Integer.valueOf(0), value.get());

    value.set(42);
    assertEquals(42, model.getValue());
  }

  @Test
  public void doubleSpinnerUiValue() {
    final SpinnerNumberModel model = new SpinnerNumberModel(0d, 0d, 130d, 1d);
    final Value<Double> value = UiValues.doubleValue(model);

    assertEquals(Double.valueOf(0d), value.get());
    model.setValue(122.2);
    assertEquals(Double.valueOf(122.2), value.get());
    model.setValue(0d);
    assertEquals(Double.valueOf(0), value.get());

    value.set(42.4);
    assertEquals(42.4, model.getValue());
  }

  @Test
  public void integerTextUiValue() {
    final IntegerField integerField = new IntegerField();
    final Value<Integer> value = UiValues.integerValue(integerField, false, true);

    assertNull(value.get());
    integerField.setText("122");
    assertEquals(Integer.valueOf(122), value.get());
    integerField.setText("");
    assertNull(value.get());

    value.set(42);
    assertEquals("42", integerField.getText());
  }

  @Test
  public void integerPrimitiveTextUiValue() {
    final IntegerField integerField = new IntegerField();
    final Value<Integer> value = UiValues.integerValue(integerField, true, true);

    assertEquals(Integer.valueOf(0), value.get());
    integerField.setText("122");
    assertEquals(Integer.valueOf(122), value.get());
    integerField.setText("");
    assertEquals(Integer.valueOf(0), value.get());

    value.set(42);
    assertEquals("42", integerField.getText());
  }

  @Test
  public void longTextUiValue() {
    final LongField longField = new LongField();
    final Value<Long> value = UiValues.longValue(longField, false, true);

    assertNull(value.get());
    longField.setText("122");
    assertEquals(Long.valueOf(122), value.get());
    longField.setText("");
    assertNull(value.get());

    value.set(42L);
    assertEquals("42", longField.getText());
  }

  @Test
  public void longPrimitiveTextUiValue() {
    final LongField longField = new LongField();
    final Value<Long> value = UiValues.longValue(longField, true, true);

    assertEquals(Long.valueOf(0), value.get());
    longField.setText("122");
    assertEquals(Long.valueOf(122), value.get());
    longField.setText("");
    assertEquals(Long.valueOf(0), value.get());

    value.set(42L);
    assertEquals("42", longField.getText());
  }

  @Test
  public void doubleTextUiValue() {
    final DoubleField doubleField = new DoubleField();
    doubleField.setSeparators('.', ',');
    final Value<Double> value = UiValues.doubleValue(doubleField, false, true);

    assertNull(value.get());
    doubleField.setText("122.2");
    assertEquals(Double.valueOf(122.2), value.get());
    doubleField.setText("");
    assertNull(value.get());

    value.set(42.2);
    assertEquals("42.2", doubleField.getText());
  }

  @Test
  public void doublePrimitiveTextUiValue() {
    final DoubleField doubleField = new DoubleField();
    doubleField.setSeparators('.', ',');
    final Value<Double> value = UiValues.doubleValue(doubleField, true, true);

    assertEquals(Double.valueOf(0), value.get());
    doubleField.setText("122.2");
    assertEquals(Double.valueOf(122.2), value.get());
    doubleField.setText("");
    assertEquals(Double.valueOf(0), value.get());

    value.set(42.2);
    assertEquals("42.2", doubleField.getText());
  }

  @Test
  public void textUiValueImmediate() {
    final JTextField textField = new JTextField();
    final Value<String> value = UiValues.textValue(textField, null, true);

    assertNull(value.get());
    textField.setText("hello there");
    assertEquals("hello there", value.get());
    textField.setText("");
    assertNull(value.get());

    value.set("hi");
    assertEquals("hi", textField.getText());
  }

  @Test
  public void toggleUiValue() {
    final ButtonModel model = new DefaultButtonModel();
    final Value<Boolean> value = UiValues.booleanValue(model);

    assertFalse(value.get());
    model.setSelected(true);
    assertTrue(value.get());
    model.setSelected(false);
    assertFalse(value.get());

    value.set(true);
    assertTrue(model.isSelected());
  }

  @Test
  public void tristateToggleUiValue() {
    final TristateButtonModel model = new TristateButtonModel();
    model.setIndeterminate();
    final Value<Boolean> value = UiValues.booleanValue(model);

    assertNull(value.get());
    model.setSelected(true);
    assertTrue(value.get());
    model.setSelected(false);
    assertFalse(value.get());

    value.set(true);
    assertTrue(model.isSelected());
    value.set(null);
    assertTrue(model.isIndeterminate());

    model.setSelected(false);
    assertFalse(value.get());
    model.setSelected(true);
    assertTrue(value.get());
    model.setIndeterminate();
    assertNull(value.get());
  }

  @Test
  public void selectedItemUiValue() {
    final JComboBox box = new JComboBox(new String[] {null, "one", "two", "three"});
    final Value<Object> value = UiValues.selectedItemValue(box);

    assertNull(value.get());
    box.setSelectedIndex(1);
    assertEquals("one", box.getSelectedItem());
    box.setSelectedIndex(2);
    assertEquals("two", box.getSelectedItem());
    box.setSelectedIndex(3);
    assertEquals("three", box.getSelectedItem());
    box.setSelectedIndex(0);
    assertNull(value.get());

    value.set("two");
    assertEquals("two", box.getSelectedItem());
  }
}
