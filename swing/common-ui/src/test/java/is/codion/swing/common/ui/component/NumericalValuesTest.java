/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.event.Event;
import is.codion.common.value.Value;
import is.codion.swing.common.ui.component.text.NumberField;
import is.codion.swing.common.ui.component.value.ComponentValue;

import org.junit.jupiter.api.Test;

import javax.swing.BoundedRangeModel;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.JProgressBar;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import static org.junit.jupiter.api.Assertions.*;

public class NumericalValuesTest {
  private Long longValue;

  private final Event<Long> longValueChangedEvent = Event.event();
  private long longPrimitiveValue;
  private final Event<Long> longPrimitiveValueChangedEvent = Event.event();

  private Integer integerValue;
  private final Event<Integer> integerValueChangedEvent = Event.event();
  private int intValue;
  private final Event<Integer> intValueChangedEvent = Event.event();

  private Double doubleValue;
  private final Event<Double> doubleValueChangedEvent = Event.event();
  private double doublePrimitiveValue;
  private final Event doublePrimitiveValueValueChangedEvent = Event.event();

  public Long getLongValue() {
    return longValue;
  }

  public void setLongValue(Long longValue) {
    this.longValue = longValue;
    longValueChangedEvent.onEvent(this.longValue);
  }

  public long getLongPrimitiveValue() {
    return longPrimitiveValue;
  }

  public void setLongPrimitiveValue(long longPrimitiveValue) {
    this.longPrimitiveValue = longPrimitiveValue;
    longPrimitiveValueChangedEvent.onEvent(this.longPrimitiveValue);
  }

  public Integer getIntegerValue() {
    return integerValue;
  }

  public void setIntegerValue(Integer integerValue) {
    this.integerValue = integerValue;
    integerValueChangedEvent.onEvent(this.integerValue);
  }

  public int getIntValue() {
    return intValue;
  }

  public void setIntValue(int intValue) {
    this.intValue = intValue;
    intValueChangedEvent.onEvent(this.intValue);
  }

  public Double getDoubleValue() {
    return doubleValue;
  }

  public void setDoubleValue(Double doubleValue) {
    this.doubleValue = doubleValue;
    doubleValueChangedEvent.onEvent();
  }

  public double getDoublePrimitiveValue() {
    return doublePrimitiveValue;
  }

  public void setDoublePrimitiveValue(double doublePrimitiveValue) {
    this.doublePrimitiveValue = doublePrimitiveValue;
    doublePrimitiveValueValueChangedEvent.onEvent();
  }

  @Test
  void testLong() throws Exception {
    Value<Long> longPropertyValue = Value.propertyValue(this, "longValue",
            Long.class, longValueChangedEvent);
    NumberField<Long> longField = Components.longField(longPropertyValue)
            .build();
    assertNull(longField.getNumber());
    setLongValue(2L);
    assertEquals(2, longField.getNumber().longValue());
    longField.setText("42");
    assertEquals(42, this.longValue.longValue());
    longField.setText("");
    assertNull(this.longValue);
  }

  @Test
  void testLongPrimitive() throws Exception {
    Value<Long> longPrimitivePropertyValue = Value.propertyValue(this, "longPrimitiveValue",
            long.class, longPrimitiveValueChangedEvent);
    assertFalse(longPrimitivePropertyValue.isNullable());
    ComponentValue<Long, NumberField<Long>> componentValue = Components.longField(longPrimitivePropertyValue)
            .buildValue();
    NumberField<Long> longField = componentValue.component();
    assertEquals(0L, longField.getNumber());
    assertEquals(0, componentValue.get());
    setLongPrimitiveValue(2);
    assertEquals(2, longField.getNumber().longValue());
    longField.setText("42");
    assertEquals(42, longPrimitiveValue);
    longField.setText("");
    assertEquals(0, longPrimitiveValue);
  }

  @Test
  void testInteger() throws Exception {
    Value<Integer> integerPropertyValue = Value.propertyValue(this, "integerValue",
            Integer.class, integerValueChangedEvent);
    NumberField<Integer> integerField = Components.integerField(integerPropertyValue)
            .build();
    assertNull(integerField.getNumber());
    setIntegerValue(2);
    assertEquals(2, integerField.getNumber().intValue());
    integerField.setText("42");
    assertEquals(42, this.integerValue.intValue());
    integerField.setText("");
    assertNull(this.integerValue);
  }

  @Test
  void testInt() throws Exception {
    Value<Integer> integerPropertyValue = Value.propertyValue(this, "intValue", int.class, intValueChangedEvent);
    ComponentValue<Integer, NumberField<Integer>> componentValue = Components.integerField(integerPropertyValue)
            .buildValue();
    NumberField<Integer> integerField = componentValue.component();
    assertEquals(0, integerField.getNumber());
    assertEquals(0, componentValue.get());
    setIntValue(2);
    assertEquals(2, integerField.getNumber().intValue());
    integerField.setText("42");
    assertEquals(42, intValue);
    integerField.setText("");
    assertEquals(0, intValue);
  }

  @Test
  void testBigDecimal() {
    DecimalFormat format = (DecimalFormat) NumberFormat.getNumberInstance();
    format.setMaximumFractionDigits(4);

    NumberField<BigDecimal> bigDecimalField = Components.bigDecimalField()
            .format(format)
            .build();
    bigDecimalField.setSeparators('.', ',');

    bigDecimalField.setNumber(BigDecimal.valueOf(3.14));
    assertEquals("3.14", bigDecimalField.getText());

    bigDecimalField.setText("42.4242");
    assertEquals(BigDecimal.valueOf(42.4242), bigDecimalField.getNumber());
  }

  @Test
  void testDouble() throws Exception {
    Value<Double> doublePropertyValue = Value.propertyValue(this, "doubleValue",
            Double.class, doubleValueChangedEvent);
    NumberField<Double> doubleField = Components.doubleField(doublePropertyValue)
            .decimalSeparator('.')
            .groupingSeparator(',')
            .build();
    assertNull(doubleField.getNumber());
    setDoubleValue(2.2);
    assertEquals(Double.valueOf(2.2), doubleField.getNumber());
    doubleField.setText("42.2");
    assertEquals(Double.valueOf(42.2), this.doubleValue);
    doubleField.setText("");
    assertNull(this.doubleValue);
  }

  @Test
  void testDoublePrimitive() throws Exception {
    Value<Double> doublePrimitivePropertyValue = Value.propertyValue(this, "doublePrimitiveValue",
            double.class, doublePrimitiveValueValueChangedEvent);
    ComponentValue<Double, NumberField<Double>> componentValue = Components.doubleField()
            .decimalSeparator('.')
            .groupingSeparator(',')
            .buildValue();
    NumberField<Double> doubleField = componentValue.component();
    componentValue.link(doublePrimitivePropertyValue);
    assertEquals(0d, doubleField.getNumber());
    assertEquals(0d, componentValue.get());
    setDoublePrimitiveValue(2.2);
    assertEquals(Double.valueOf(2.2), doubleField.getNumber());
    doubleField.setText("42.2");
    assertEquals(42.2, this.doublePrimitiveValue);
    doubleField.setText("");
    assertEquals(0.0, this.doublePrimitiveValue);
  }

  @Test
  void parseDouble() {
    ComponentValue<Double, NumberField<Double>> componentValue = Components.doubleField()
            .buildValue();
    assertNull(componentValue.get());

    componentValue.component().setGroupingUsed(false);

    componentValue.component().setSeparators('.', ',');
    componentValue.component().setText("15.5");
    assertEquals(Double.valueOf(15.5), componentValue.get());
    componentValue.component().setText("15,6");
    assertEquals(Double.valueOf(15.5), componentValue.get());

    componentValue.component().setSeparators(',', '.');
    componentValue.component().setText("15.7");
    assertEquals(Double.valueOf(15.5), componentValue.get());
    componentValue.component().setText("15,7");
    assertEquals(Double.valueOf(15.7), componentValue.get());

    componentValue.component().setGroupingUsed(true);

    componentValue.component().setSeparators('.', ',');
    componentValue.component().setText("15.5");
    assertEquals(Double.valueOf(15.5), componentValue.get());
    componentValue.component().setText("15,6");
    assertEquals(Double.valueOf(156), componentValue.get());

    componentValue.component().setSeparators(',', '.');
    componentValue.component().setText("15.7");
    assertEquals(Double.valueOf(157), componentValue.get());
    componentValue.component().setText("15,7");
    assertEquals(Double.valueOf(15.7), componentValue.get());
  }

  @Test
  void integerValueField() {
    final Integer value = 10;
    ComponentValue<Integer, NumberField<Integer>> componentValue = Components.integerField()
            .initialValue(value)
            .buildValue();
    assertEquals(value, componentValue.get());

    componentValue = Components.integerField()
            .buildValue();
    assertNull(componentValue.get());

    componentValue.component().setText("15");
    assertEquals(Integer.valueOf(15), componentValue.get());
  }

  @Test
  void integerSpinnerUiValue() {
    SpinnerNumberModel model = new SpinnerNumberModel();
    ComponentValue<Integer, JSpinner> value = Components.integerSpinner(model)
            .buildValue();

    assertEquals(Integer.valueOf(0), value.get());
    model.setValue(122);
    assertEquals(Integer.valueOf(122), value.get());
    model.setValue(0);
    assertEquals(Integer.valueOf(0), value.get());

    value.set(42);
    assertEquals(42, model.getValue());
  }

  @Test
  void integerBoundedRangeModelUiValue() {
    BoundedRangeModel model = new DefaultBoundedRangeModel(0, 0, 0, 150);
    ComponentValue<Integer, JProgressBar> value = Components.progressBar(model)
            .buildValue();

    assertEquals(Integer.valueOf(0), value.get());
    model.setValue(122);
    assertEquals(Integer.valueOf(122), value.get());
    model.setValue(0);
    assertEquals(Integer.valueOf(0), value.get());

    value.set(42);
    assertEquals(42, model.getValue());
  }

  @Test
  void doubleSpinnerUiValue() {
    SpinnerNumberModel model = new SpinnerNumberModel(0d, 0d, 130d, 1d);
    Value<Double> value = Components.doubleSpinner(model)
            .buildValue();

    assertEquals(Double.valueOf(0d), value.get());
    model.setValue(122.2);
    assertEquals(Double.valueOf(122.2), value.get());
    model.setValue(0d);
    assertEquals(Double.valueOf(0), value.get());

    value.set(42.4);
    assertEquals(42.4, model.getValue());
  }

  @Test
  void integerTextUiValue() {
    ComponentValue<Integer, NumberField<Integer>> value = Components.integerField()
            .buildValue();
    NumberField<Integer> integerField = value.component();

    assertNull(value.get());
    integerField.setText("122");
    assertEquals(Integer.valueOf(122), value.get());
    integerField.setText("");
    assertNull(value.get());

    value.set(42);
    assertEquals("42", integerField.getText());
  }

  @Test
  void longValue() {
    final Long value = 10L;
    ComponentValue<Long, NumberField<Long>> componentValue = Components.longField()
            .buildValue();
    componentValue.set(value);
    assertEquals(value, componentValue.get());

    componentValue = Components.longField()
            .buildValue();
    assertNull(componentValue.get());

    componentValue.component().setText("15");
    assertEquals(Long.valueOf(15), componentValue.get());
  }

  @Test
  void longTextUiValue() {
    ComponentValue<Long, NumberField<Long>> value = Components.longField()
            .buildValue();
    NumberField<Long> longField = value.component();

    assertNull(value.get());
    longField.setText("122");
    assertEquals(Long.valueOf(122), value.get());
    longField.setText("");
    assertNull(value.get());

    value.set(42L);
    assertEquals("42", longField.getText());
  }

  @Test
  void bigDecimalTextUiValue() {
    ComponentValue<BigDecimal, NumberField<BigDecimal>> value = Components.bigDecimalField()
            .decimalSeparator('.')
            .groupingSeparator(',')
            .buildValue();

    NumberField<BigDecimal> bigDecimalField = value.component();

    assertNull(value.get());
    bigDecimalField.setText("122.2");
    assertEquals(BigDecimal.valueOf(122.2), value.get());
    bigDecimalField.setText("");
    assertNull(value.get());

    value.set(BigDecimal.valueOf(42.2));
    assertEquals("42.2", bigDecimalField.getText());
  }
}
