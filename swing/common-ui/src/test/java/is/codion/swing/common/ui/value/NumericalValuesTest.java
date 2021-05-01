/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.value;

import is.codion.common.event.Event;
import is.codion.common.value.Value;
import is.codion.swing.common.ui.textfield.BigDecimalField;
import is.codion.swing.common.ui.textfield.DoubleField;
import is.codion.swing.common.ui.textfield.IntegerField;
import is.codion.swing.common.ui.textfield.LongField;

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
    final Value<Long> longPropertyValue = Value.propertyValue(this, "longValue",
            Long.class, longValueChangedEvent);
    NumericalValues.longValue(longField).link(longPropertyValue);
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
    final Value<Long> longPrimitivePropertyValue = Value.propertyValue(this, "longPrimitiveValue",
            long.class, longPrimitiveValueChangedEvent);
    final ComponentValue<Long, LongField> componentValue = NumericalValues.longValueBuilder()
            .component(longField)
            .nullable(false)
            .build();
    componentValue.link(longPrimitivePropertyValue);
    assertEquals(0L, longField.getLong());
    assertEquals(0, componentValue.get());
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
    final Value<Integer> integerPropertyValue = Value.propertyValue(this, "integerValue",
            Integer.class, integerValueChangedEvent);
    NumericalValues.integerValue(integerField).link(integerPropertyValue);
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
    final Value<Integer> integerPropertyValue = Value.propertyValue(this, "intValue", int.class, intValueChangedEvent);
    final ComponentValue<Integer, IntegerField> componentValue = NumericalValues.integerValueBuilder()
            .component(integerField)
            .nullable(false)
            .build();
    componentValue.link(integerPropertyValue);
    assertEquals(0, integerField.getInteger());
    assertEquals(0, componentValue.get());
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
    format.setMaximumFractionDigits(4);

    final BigDecimalField bigDecimalField = new BigDecimalField(format);
    bigDecimalField.setSeparators('.', ',');

    bigDecimalField.setBigDecimal(BigDecimal.valueOf(3.14));
    assertEquals("3.14", bigDecimalField.getText());

    bigDecimalField.setText("42.4242");
    assertEquals(BigDecimal.valueOf(42.4242), bigDecimalField.getBigDecimal());
  }

  @Test
  public void testDouble() throws Exception {
    final DoubleField doubleField = new DoubleField();
    doubleField.setSeparators('.', ',');
    final Value<Double> doublePropertyValue = Value.propertyValue(this, "doubleValue",
            Double.class, doubleValueChangedEvent);
    NumericalValues.doubleValue(doubleField).link(doublePropertyValue);
    assertNull(doubleField.getDouble());
    setDoubleValue(2.2);
    assertEquals(Double.valueOf(2.2), doubleField.getDouble());
    doubleField.setText("42.2");
    assertEquals(Double.valueOf(42.2), this.doubleValue);
    doubleField.setText("");
    assertNull(this.doubleValue);
  }

  @Test
  public void testDoublePrimitive() throws Exception {
    final DoubleField doubleField = new DoubleField();
    doubleField.setSeparators('.', ',');
    final Value<Double> doublePrimitivePropertyValue = Value.propertyValue(this, "doublePrimitiveValue",
            double.class, doublePrimitiveValueValueChangedEvent);
    final ComponentValue<Double, DoubleField> componentValue = NumericalValues.doubleValueBuilder()
            .component(doubleField)
            .nullable(false)
            .build();
    componentValue.link(doublePrimitivePropertyValue);
    assertEquals(0d, doubleField.getDouble());
    assertEquals(0d, componentValue.get());
    setDoublePrimitiveValue(2.2);
    assertEquals(Double.valueOf(2.2), doubleField.getDouble());
    doubleField.setText("42.2");
    assertEquals(42.2, this.doublePrimitiveValue);
    doubleField.setText("");
    assertEquals(0.0, this.doublePrimitiveValue);
  }

  @Test
  public void doubleComponentValue() {
    final Double value = 10.4;
    ComponentValue<Double, DoubleField> componentValue = NumericalValues.doubleValue(value);
    assertEquals(value, componentValue.get());
    componentValue = NumericalValues.doubleValue();
    assertNull(componentValue.get());
  }

  @Test
  public void parseDouble() {
    final ComponentValue<Double, DoubleField> componentValue = NumericalValues.doubleValue();
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

    componentValue = NumericalValues.integerValue();
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
    final Value<Integer> value = NumericalValues.integerValue(integerField);

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
    final Value<Integer> value = NumericalValues.integerValueBuilder()
            .component(integerField)
            .nullable(false)
            .build();

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
    ComponentValue<Long, LongField> componentValue = NumericalValues.longValueBuilder()
            .initalValue(value)
            .build();
    assertEquals(value, componentValue.get());

    componentValue = NumericalValues.longValue();
    assertNull(componentValue.get());

    componentValue.getComponent().setText("15");
    assertEquals(Long.valueOf(15), componentValue.get());
  }

  @Test
  public void longTextUiValue() {
    final LongField longField = new LongField();
    final Value<Long> value = NumericalValues.longValue(longField);

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
    final Value<Long> value = NumericalValues.longValueBuilder()
            .component(longField)
            .nullable(false)
            .build();

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
    final Value<Double> value = NumericalValues.doubleValue(doubleField);

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
    final Value<Double> value = NumericalValues.doubleValueBuilder()
            .component(doubleField)
            .nullable(false)
            .build();

    assertEquals(Double.valueOf(0), value.get());
    doubleField.setText("122.2");
    assertEquals(Double.valueOf(122.2), value.get());
    doubleField.setText("");
    assertEquals(Double.valueOf(0), value.get());

    value.set(42.2);
    assertEquals("42.2", doubleField.getText());
  }

  @Test
  public void bigDecimalTextUiValue() {
    final BigDecimalField bigDecimalField = new BigDecimalField();
    bigDecimalField.setSeparators('.', ',');
    final Value<BigDecimal> value = NumericalValues.bigDecimalValue(bigDecimalField);

    assertNull(value.get());
    bigDecimalField.setText("122.2");
    assertEquals(BigDecimal.valueOf(122.2), value.get());
    bigDecimalField.setText("");
    assertNull(value.get());

    value.set(BigDecimal.valueOf(42.2));
    assertEquals("42.2", bigDecimalField.getText());
  }
}
