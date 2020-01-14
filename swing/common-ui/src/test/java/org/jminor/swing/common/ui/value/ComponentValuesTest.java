/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.value;

import org.jminor.common.DateFormats;
import org.jminor.common.Item;
import org.jminor.common.value.Value;
import org.jminor.swing.common.model.checkbox.NullableToggleButtonModel;
import org.jminor.swing.common.model.combobox.ItemComboBoxModel;
import org.jminor.swing.common.ui.textfield.DecimalField;
import org.jminor.swing.common.ui.textfield.IntegerField;
import org.jminor.swing.common.ui.textfield.LongField;
import org.jminor.swing.common.ui.textfield.TextFields;
import org.jminor.swing.common.ui.textfield.TextInputPanel;
import org.jminor.swing.common.ui.time.LocalDateInputPanel;
import org.jminor.swing.common.ui.time.TemporalInputPanel;

import org.junit.jupiter.api.Test;

import javax.swing.BoundedRangeModel;
import javax.swing.ButtonModel;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.DefaultButtonModel;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

public class ComponentValuesTest {

  @Test
  public void selectedItemValue() {
    final List<Item<String>> items = asList(new Item<>(null), new Item<>("one"),
            new Item<>("two"), new Item<>("three"), new Item<>("four"));
    ComponentValue<String, JComboBox<Item<String>>> componentValue = SelectedValues.selectedItemValue("two", items);
    ItemComboBoxModel<String> boxModel = (ItemComboBoxModel<String>) componentValue.getComponent().getModel();
    assertEquals(5, boxModel.getSize());
    assertEquals("two", componentValue.get());

    componentValue = SelectedValues.selectedItemValue(null, items);
    boxModel = (ItemComboBoxModel<String>) componentValue.getComponent().getModel();
    assertEquals(5, boxModel.getSize());
    assertNull(componentValue.get());
  }

  @Test
  public void textValue() {
    final String value = "hello";
    ComponentValue<String, TextInputPanel> componentValue = StringValues.stringValue("none", value, 2);
    assertNull(componentValue.get());

    componentValue = StringValues.stringValue("none", value, 10);
    assertEquals(value, componentValue.get());

    componentValue = StringValues.stringValue("none", null, 10);
    assertNull(componentValue.get());

    componentValue.getComponent().setText("tester");
    assertEquals("tester", componentValue.get());

    componentValue.getComponent().setText("");
    assertNull(componentValue.get());
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

  @Test
  public void longValue() {
    final Long value = 10L;
    ComponentValue<Long, LongField> componentValue = LongValues.longValue(value);
    assertEquals(value, componentValue.get());

    componentValue = LongValues.longValue(null);
    assertNull(componentValue.get());

    componentValue.getComponent().setText("15");
    assertEquals(Long.valueOf(15), componentValue.get());
  }

  @Test
  public void booleanValue() {
    ComponentValue<Boolean, JComboBox> componentValue = BooleanValues.booleanComboBoxValue(false);
    assertEquals(false, componentValue.get());
    componentValue.getComponent().getModel().setSelectedItem(true);
    assertEquals(true, componentValue.get());
    componentValue.getComponent().getModel().setSelectedItem(null);
    assertNull(componentValue.get());
    componentValue = new BooleanComboBoxValue(null);
    assertNull(componentValue.get());
  }

  @Test
  public void localTimeUiValue() {
    final String format = "HH:mm";
    final JFormattedTextField textField = TextFields.createFormattedField(DateFormats.getDateMask(format));//HH:mm
    final Value<LocalTime> value = LocalTimeValues.localTimeValue(textField, format);

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
    final Value<LocalDate> value = LocalDateValues.localDateValue(textField, DateFormats.SHORT_DASH);

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
    final Value<LocalDateTime> value = LocalDateTimeValues.localDateTimeValue(textField, DateFormats.TIMESTAMP);

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
    final Value<Integer> value = IntegerValues.integerValue(model);

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
    final Value<Integer> value = IntegerValues.integerValue(model);

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
    final Value<Double> value = DoubleValues.doubleValue(model);

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
    final Value<Integer> value = IntegerValues.integerValue(integerField, true);

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
    final Value<Integer> value = IntegerValues.integerValue(integerField, false);

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
    final Value<Long> value = LongValues.longValue(longField, true);

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
    final Value<Long> value = LongValues.longValue(longField, false);

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
    final DecimalField decimalField = new DecimalField();
    decimalField.setSeparators('.', ',');
    final Value<Double> value = DoubleValues.doubleValue(decimalField, true);

    assertNull(value.get());
    decimalField.setText("122.2");
    assertEquals(Double.valueOf(122.2), value.get());
    decimalField.setText("");
    assertNull(value.get());

    value.set(42.2);
    assertEquals("42.2", decimalField.getText());
  }

  @Test
  public void doublePrimitiveTextUiValue() {
    final DecimalField decimalField = new DecimalField();
    decimalField.setSeparators('.', ',');
    final Value<Double> value = DoubleValues.doubleValue(decimalField, false);

    assertEquals(Double.valueOf(0), value.get());
    decimalField.setText("122.2");
    assertEquals(Double.valueOf(122.2), value.get());
    decimalField.setText("");
    assertEquals(Double.valueOf(0), value.get());

    value.set(42.2);
    assertEquals("42.2", decimalField.getText());
  }

  @Test
  public void bigDecimalTextUiValue() {
    final DecimalField decimalField = new DecimalField();
    decimalField.setParseBigDecimal(true);
    decimalField.setSeparators('.', ',');
    final Value<BigDecimal> value = BigDecimalValues.bigDecimalValue(decimalField);

    assertNull(value.get());
    decimalField.setText("122.2");
    assertEquals(BigDecimal.valueOf(122.2), value.get());
    decimalField.setText("");
    assertNull(value.get());

    value.set(BigDecimal.valueOf(42.2));
    assertEquals("42.2", decimalField.getText());
  }

  @Test
  public void textUiValueKeystroke() {
    final JTextField textField = new JTextField();
    final Value<String> value = StringValues.stringValue(textField);

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
    final Value<Boolean> value = BooleanValues.booleanButtonModelValue(model);

    assertFalse(value.get());
    model.setSelected(true);
    assertTrue(value.get());
    model.setSelected(false);
    assertFalse(value.get());

    value.set(true);
    assertTrue(model.isSelected());
  }

  @Test
  public void nullableToggleUiValue() {
    final NullableToggleButtonModel model = new NullableToggleButtonModel();
    final Value<Boolean> value = BooleanValues.booleanButtonModelValue(model);

    assertNull(value.get());
    model.setSelected(true);
    assertTrue(value.get());
    model.setSelected(false);
    assertFalse(value.get());

    value.set(true);
    assertTrue(model.isSelected());
    value.set(null);
    assertNull(model.getState());

    model.setSelected(false);
    assertFalse(value.get());
    model.setSelected(true);
    assertTrue(value.get());
    model.setState(null);
    assertNull(value.get());
  }

  @Test
  public void selectedItemUiValue() {
    final JComboBox box = new JComboBox(new String[] {null, "one", "two", "three"});
    final Value<Object> value = SelectedValues.selectedValue(box);

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
