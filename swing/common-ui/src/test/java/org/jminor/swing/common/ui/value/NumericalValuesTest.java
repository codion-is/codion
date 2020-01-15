/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.value;

import org.jminor.common.event.Event;
import org.jminor.common.event.Events;
import org.jminor.common.value.Value;
import org.jminor.common.value.Values;
import org.jminor.swing.common.ui.textfield.DecimalField;
import org.jminor.swing.common.ui.textfield.IntegerField;
import org.jminor.swing.common.ui.textfield.LongField;

import org.junit.jupiter.api.Test;

import javax.swing.BoundedRangeModel;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.SpinnerNumberModel;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class NumericalValuesTest {  private Long longValue;

  private final Event<Long> longValueChangedEvent = Events.event();
  private long longPrimitiveValue;
  private final Event<Long> longPrimitiveValueChangedEvent = Events.event();

  private Integer integerValue;
  private final Event<Integer> integerValueChangedEvent = Events.event();
  private int intValue;
  private final Event<Integer> intValueChangedEvent = Events.event();

  private Double doubleValue;
  private final Event doubleValueChangedEvent = Events.event();
  private double doublePrimitiveValue;
  private final Event doublePrimitiveValueValueChangedEvent = Events.event();

  public Long getLongValue() {
    return longValue;
  }

  public void setLongValue(final Long longValue) {
    this.longValue = longValue;
    longValueChangedEvent.onEvent(this.longValue);
  }

  public long getLongPrimitiveValue() {
    return longPrimitiveValue;
  }

  public void setLongPrimitiveValue(final long longPrimitiveValue) {
    this.longPrimitiveValue = longPrimitiveValue;
    longPrimitiveValueChangedEvent.onEvent(this.longPrimitiveValue);
  }

  public Integer getIntegerValue() {
    return integerValue;
  }

  public void setIntegerValue(final Integer integerValue) {
    this.integerValue = integerValue;
    integerValueChangedEvent.onEvent(this.integerValue);
  }

  public int getIntValue() {
    return intValue;
  }

  public void setIntValue(final int intValue) {
    this.intValue = intValue;
    intValueChangedEvent.onEvent(this.intValue);
  }

  public Double getDoubleValue() {
    return doubleValue;
  }

  public void setDoubleValue(final Double doubleValue) {
    this.doubleValue = doubleValue;
    doubleValueChangedEvent.onEvent();
  }

  public double getDoublePrimitiveValue() {
    return doublePrimitiveValue;
  }

  public void setDoublePrimitiveValue(final double doublePrimitiveValue) {
    this.doublePrimitiveValue = doublePrimitiveValue;
    doublePrimitiveValueValueChangedEvent.onEvent();
  }

  @Test
  public void testLong() throws Exception {
    final LongField longField = new LongField();
    final Value<Long> longPropertyValue = Values.propertyValue(this, "longValue",
            Long.class, longValueChangedEvent);
    NumericalValues.longValueLink(longField, longPropertyValue, true);
    assertNull(longField.getLong());
    setLongValue(2L);
    assertEquals(2, longField.getLong().longValue());
    longField.setText("42");
    assertEquals(42, this.longValue.longValue());
    longField.setText("");
    assertNull(this.longValue);
  }

  @Test
  public void testLongPrimitive() throws Exception {
    final LongField longField = new LongField();
    final Value<Long> longPrimitivePropertyValue = Values.propertyValue(this, "longPrimitiveValue",
            long.class, longPrimitiveValueChangedEvent);
    NumericalValues.longValueLink(longField, longPrimitivePropertyValue, false);
    assertEquals(Long.valueOf(0), longField.getLong());
    setLongPrimitiveValue(2);
    assertEquals(2, longField.getLong().longValue());
    longField.setText("42");
    assertEquals(42, longPrimitiveValue);
    longField.setText("");
    assertEquals(0, longPrimitiveValue);
  }

  @Test
  public void testInteger() throws Exception {
    final IntegerField integerField = new IntegerField();
    final Value<Integer> integerPropertyValue = Values.propertyValue(this, "integerValue",
            Integer.class, integerValueChangedEvent);
    NumericalValues.integerValueLink(integerField, integerPropertyValue, true);
    assertNull(integerField.getInteger());
    setIntegerValue(2);
    assertEquals(2, integerField.getInteger().intValue());
    integerField.setText("42");
    assertEquals(42, this.integerValue.intValue());
    integerField.setText("");
    assertNull(this.integerValue);
  }

  @Test
  public void testInt() throws Exception {
    final IntegerField integerField = new IntegerField();
        final Value<Integer> integerPropertyValue = Values.propertyValue(this, "intValue",
            int.class, intValueChangedEvent);
    NumericalValues.integerValueLink(integerField, integerPropertyValue, false);
    assertEquals((Integer) 0, integerField.getInteger());
    setIntValue(2);
    assertEquals(2, integerField.getInteger().intValue());
    integerField.setText("42");
    assertEquals(42, intValue);
    integerField.setText("");
    assertEquals(0, intValue);
  }

  @Test
  public void testBigDecimal() {
    final DecimalFormat format = (DecimalFormat) NumberFormat.getNumberInstance();
    format.setParseBigDecimal(true);
    format.setMaximumFractionDigits(4);

    final DecimalField decimalField = new DecimalField(format);
    decimalField.setSeparators('.', ',');

    decimalField.setBigDecimal(BigDecimal.valueOf(3.14));
    assertEquals("3.14", decimalField.getText());

    decimalField.setText("42.4242");
    assertEquals(BigDecimal.valueOf(42.4242), decimalField.getBigDecimal());
  }

  @Test
  public void testDouble() throws Exception {
    final DecimalField decimalField = new DecimalField();
    decimalField.setSeparators('.', ',');
    final Value doublePropertyValue = Values.propertyValue(this, "doubleValue",
            Double.class, doubleValueChangedEvent);
    NumericalValues.doubleValueLink(decimalField, doublePropertyValue, true);
    assertNull(decimalField.getDouble());
    setDoubleValue(2.2);
    assertEquals(Double.valueOf(2.2), decimalField.getDouble());
    decimalField.setText("42.2");
    assertEquals(Double.valueOf(42.2), this.doubleValue);
    decimalField.setText("");
    assertNull(this.doubleValue);
  }

  @Test
  public void testDoublePrimitive() throws Exception {
    final DecimalField decimalField = new DecimalField();
    decimalField.setSeparators('.', ',');
    final Value doublePrimitivePropertyValue = Values.propertyValue(this, "doublePrimitiveValue",
            double.class, doublePrimitiveValueValueChangedEvent);
    NumericalValues.doubleValueLink(decimalField, doublePrimitivePropertyValue, false);
    assertEquals((Double) 0.0, decimalField.getDouble());
    setDoublePrimitiveValue(2.2);
    assertEquals(Double.valueOf(2.2), decimalField.getDouble());
    decimalField.setText("42.2");
    assertEquals(42.2, this.doublePrimitiveValue);
    decimalField.setText("");
    assertEquals(0.0, this.doublePrimitiveValue);
  }

  @Test
  public void doubleComponentValue() {
    final Double value = 10.4;
    ComponentValue<Double, DecimalField> componentValue = NumericalValues.doubleValue(value);
    assertEquals(value, componentValue.get());
    componentValue = NumericalValues.doubleValue((Double) null);
    assertNull(componentValue.get());
  }

  @Test
  public void parseDouble() {
    final ComponentValue<Double, DecimalField> componentValue = NumericalValues.doubleValue((Double) null);
    assertNull(componentValue.get());

    componentValue.getComponent().setGroupingUsed(false);

    componentValue.getComponent().setSeparators('.', ',');
    componentValue.getComponent().setText("15.5");
    assertEquals(Double.valueOf(15.5), componentValue.get());
    componentValue.getComponent().setText("15,6");
    assertEquals(Double.valueOf(15.5), componentValue.get());

    componentValue.getComponent().setSeparators(',', '.');
    componentValue.getComponent().setText("15.7");
    assertEquals(Double.valueOf(15.5), componentValue.get());
    componentValue.getComponent().setText("15,7");
    assertEquals(Double.valueOf(15.7), componentValue.get());

    componentValue.getComponent().setGroupingUsed(true);

    componentValue.getComponent().setSeparators('.', ',');
    componentValue.getComponent().setText("15.5");
    assertEquals(Double.valueOf(15.5), componentValue.get());
    componentValue.getComponent().setText("15,6");
    assertEquals(Double.valueOf(156), componentValue.get());

    componentValue.getComponent().setSeparators(',', '.');
    componentValue.getComponent().setText("15.7");
    assertEquals(Double.valueOf(157), componentValue.get());
    componentValue.getComponent().setText("15,7");
    assertEquals(Double.valueOf(15.7), componentValue.get());
  }

  @Test
  public void integerValueField() {
    final Integer value = 10;
    ComponentValue<Integer, IntegerField> componentValue = NumericalValues.integerValue(value);
    assertEquals(value, componentValue.get());

    componentValue = NumericalValues.integerValue((Integer) null);
    assertNull(componentValue.get());

    componentValue.getComponent().setText("15");
    assertEquals(Integer.valueOf(15), componentValue.get());
  }

  @Test
  public void integerSpinnerUiValue() {
    final SpinnerNumberModel model = new SpinnerNumberModel();
    final Value<Integer> value = NumericalValues.integerValue(model);

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
    final Value<Integer> value = NumericalValues.integerValue(model);

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
    final Value<Double> value = NumericalValues.doubleValue(model);

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
    final Value<Integer> value = NumericalValues.integerValue(integerField, true);

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
    final Value<Integer> value = NumericalValues.integerValue(integerField, false);

    assertEquals(Integer.valueOf(0), value.get());
    integerField.setText("122");
    assertEquals(Integer.valueOf(122), value.get());
    integerField.setText("");
    assertEquals(Integer.valueOf(0), value.get());

    value.set(42);
    assertEquals("42", integerField.getText());
  }

  @Test
  public void longValue() {
    final Long value = 10L;
    ComponentValue<Long, LongField> componentValue = NumericalValues.longValue(value);
    assertEquals(value, componentValue.get());

    componentValue = NumericalValues.longValue(null);
    assertNull(componentValue.get());

    componentValue.getComponent().setText("15");
    assertEquals(Long.valueOf(15), componentValue.get());
  }

  @Test
  public void longTextUiValue() {
    final LongField longField = new LongField();
    final Value<Long> value = NumericalValues.longValue(longField, true);

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
    final Value<Long> value = NumericalValues.longValue(longField, false);

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
    final Value<Double> value = NumericalValues.doubleValue(decimalField, true);

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
    final Value<Double> value = NumericalValues.doubleValue(decimalField, false);

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
    final Value<BigDecimal> value = NumericalValues.bigDecimalValue(decimalField);

    assertNull(value.get());
    decimalField.setText("122.2");
    assertEquals(BigDecimal.valueOf(122.2), value.get());
    decimalField.setText("");
    assertNull(value.get());

    value.set(BigDecimal.valueOf(42.2));
    assertEquals("42.2", decimalField.getText());
  }
}
