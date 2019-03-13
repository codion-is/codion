/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui;

import org.jminor.common.DateFormats;
import org.jminor.common.DateUtil;
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
import java.sql.Types;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

public class UiValuesTest {

  @Test
  public void timeUiValue() throws ParseException {
    final SimpleDateFormat format = DateFormats.getDateFormat("HH:mm");
    final JFormattedTextField txt = UiUtil.createFormattedField(DateUtil.getDateMask(format));//HH:mm
    final Value<Date> value = UiValues.dateValue(txt, format, Types.TIME, true);

    assertNull(value.get());
    final String timeString = "22:42";
    txt.setText(timeString);
    final Date date = value.get();
    assertEquals(format.parse(timeString), date);

    final Calendar calendar = Calendar.getInstance();
    calendar.setTime(date);
    assertEquals(1970, calendar.get(Calendar.YEAR));
    assertEquals(Calendar.JANUARY, calendar.get(Calendar.MONTH));
    assertEquals(1, calendar.get(Calendar.DAY_OF_MONTH));

    final String invalidDateString = "23:";
    txt.setText(invalidDateString);
    assertEquals("23:__", txt.getText());
    assertNull(value.get());

    value.set(format.parse(timeString));
    assertEquals(timeString, txt.getText());
  }

  @Test
  public void dateUiValue() throws ParseException {
    final SimpleDateFormat format = DateFormats.getDateFormat(DateFormats.SHORT_DASH);
    final JFormattedTextField txt = UiUtil.createFormattedField(DateUtil.getDateMask(format));//dd-MM-yyyy
    final Value<Date> value = UiValues.dateValue(txt, format, Types.DATE, true);

    assertNull(value.get());
    final String dateString = "03-10-1975";
    txt.setText(dateString);
    assertEquals(format.parse(dateString), value.get());

    final String invalidDateString = "03-10-19";
    txt.setText(invalidDateString);
    assertEquals(invalidDateString + "__", txt.getText());
    assertNull(value.get());

    value.set(format.parse(dateString));
    assertEquals(dateString, txt.getText());
  }

  @Test
  public void timestampUiValue() throws ParseException {
    final SimpleDateFormat format = DateFormats.getDateFormat(DateFormats.TIMESTAMP);
    final JFormattedTextField txt = UiUtil.createFormattedField(DateUtil.getDateMask(format));//dd-MM-yyyy HH:mm
    final Value<Date> value = UiValues.dateValue(txt, format, Types.TIMESTAMP, true);

    assertNull(value.get());
    final String dateString = "03-10-1975 22:45";
    txt.setText(dateString);
    assertEquals(format.parse(dateString), value.get());

    final String invalidDateString = "03-10-1975 22";
    txt.setText(invalidDateString);
    assertEquals(invalidDateString + ":__", txt.getText());
    assertNull(value.get());

    value.set(format.parse(dateString));
    assertEquals(dateString, txt.getText());
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
    final IntegerField txt = new IntegerField();
    final Value<Integer> value = UiValues.integerValue(txt, false, true);

    assertNull(value.get());
    txt.setText("122");
    assertEquals(Integer.valueOf(122), value.get());
    txt.setText("");
    assertNull(value.get());

    value.set(42);
    assertEquals("42", txt.getText());
  }

  @Test
  public void integerPrimitiveTextUiValue() {
    final IntegerField txt = new IntegerField();
    final Value<Integer> value = UiValues.integerValue(txt, true, true);

    assertEquals(Integer.valueOf(0), value.get());
    txt.setText("122");
    assertEquals(Integer.valueOf(122), value.get());
    txt.setText("");
    assertEquals(Integer.valueOf(0), value.get());

    value.set(42);
    assertEquals("42", txt.getText());
  }

  @Test
  public void longTextUiValue() {
    final LongField txt = new LongField();
    final Value<Long> value = UiValues.longValue(txt, false, true);

    assertNull(value.get());
    txt.setText("122");
    assertEquals(Long.valueOf(122), value.get());
    txt.setText("");
    assertNull(value.get());

    value.set(42L);
    assertEquals("42", txt.getText());
  }

  @Test
  public void longPrimitiveTextUiValue() {
    final LongField txt = new LongField();
    final Value<Long> value = UiValues.longValue(txt, true, true);

    assertEquals(Long.valueOf(0), value.get());
    txt.setText("122");
    assertEquals(Long.valueOf(122), value.get());
    txt.setText("");
    assertEquals(Long.valueOf(0), value.get());

    value.set(42L);
    assertEquals("42", txt.getText());
  }

  @Test
  public void doubleTextUiValue() {
    final DoubleField txt = new DoubleField();
    txt.setSeparators('.', ',');
    final Value<Double> value = UiValues.doubleValue(txt, false, true);

    assertNull(value.get());
    txt.setText("122.2");
    assertEquals(Double.valueOf(122.2), value.get());
    txt.setText("");
    assertNull(value.get());

    value.set(42.2);
    assertEquals("42.2", txt.getText());
  }

  @Test
  public void doublePrimitiveTextUiValue() {
    final DoubleField txt = new DoubleField();
    txt.setSeparators('.', ',');
    final Value<Double> value = UiValues.doubleValue(txt, true, true);

    assertEquals(Double.valueOf(0), value.get());
    txt.setText("122.2");
    assertEquals(Double.valueOf(122.2), value.get());
    txt.setText("");
    assertEquals(Double.valueOf(0), value.get());

    value.set(42.2);
    assertEquals("42.2", txt.getText());
  }

  @Test
  public void textUiValueImmediate() {
    final JTextField txt = new JTextField();
    final Value<String> value = UiValues.textValue(txt, null, true);

    assertNull(value.get());
    txt.setText("hello there");
    assertEquals("hello there", value.get());
    txt.setText("");
    assertNull(value.get());

    value.set("hi");
    assertEquals("hi", txt.getText());
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
