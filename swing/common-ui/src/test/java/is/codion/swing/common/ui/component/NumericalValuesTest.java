/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.event.Event;
import is.codion.common.value.Value;
import is.codion.swing.common.ui.textfield.BigDecimalField;
import is.codion.swing.common.ui.textfield.DoubleField;
import is.codion.swing.common.ui.textfield.IntegerField;
import is.codion.swing.common.ui.textfield.LongField;

import org.junit.jupiter.api.Test;

import javax.swing.BoundedRangeModel;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.JProgressBar;
import javax.swing.JSpinner;
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
    LongField longField = new LongField();
    Value<Long> longPropertyValue = Value.propertyValue(this, "longValue",
            Long.class, longValueChangedEvent);
    ComponentValues.longField(longField).link(longPropertyValue);
    assertNull(longField.getLong());
    setLongValue(2L);
    assertEquals(2, longField.getLong().longValue());
    longField.setText("42");
    assertEquals(42, this.longValue.longValue());
    longField.setText("");
    assertNull(this.longValue);
  }

  @Test
  void testLongPrimitive() throws Exception {
    LongField longField = new LongField();
    Value<Long> longPrimitivePropertyValue = Value.propertyValue(this, "longPrimitiveValue",
            long.class, longPrimitiveValueChangedEvent);
    ComponentValue<Long, LongField> componentValue = ComponentValues.longField(longField, false);
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
  void testInteger() throws Exception {
    IntegerField integerField = new IntegerField();
    Value<Integer> integerPropertyValue = Value.propertyValue(this, "integerValue",
            Integer.class, integerValueChangedEvent);
    ComponentValues.integerField(integerField).link(integerPropertyValue);
    assertNull(integerField.getInteger());
    setIntegerValue(2);
    assertEquals(2, integerField.getInteger().intValue());
    integerField.setText("42");
    assertEquals(42, this.integerValue.intValue());
    integerField.setText("");
    assertNull(this.integerValue);
  }

  @Test
  void testInt() throws Exception {
    IntegerField integerField = new IntegerField();
    Value<Integer> integerPropertyValue = Value.propertyValue(this, "intValue", int.class, intValueChangedEvent);
    ComponentValue<Integer, IntegerField> componentValue = ComponentValues.integerField(integerField, false);
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
  void testBigDecimal() {
    DecimalFormat format = (DecimalFormat) NumberFormat.getNumberInstance();
    format.setMaximumFractionDigits(4);

    BigDecimalField bigDecimalField = new BigDecimalField(format);
    bigDecimalField.setSeparators('.', ',');

    bigDecimalField.setBigDecimal(BigDecimal.valueOf(3.14));
    assertEquals("3.14", bigDecimalField.getText());

    bigDecimalField.setText("42.4242");
    assertEquals(BigDecimal.valueOf(42.4242), bigDecimalField.getBigDecimal());
  }

  @Test
  void testDouble() throws Exception {
    DoubleField doubleField = new DoubleField();
    doubleField.setSeparators('.', ',');
    Value<Double> doublePropertyValue = Value.propertyValue(this, "doubleValue",
            Double.class, doubleValueChangedEvent);
    ComponentValues.doubleField(doubleField).link(doublePropertyValue);
    assertNull(doubleField.getDouble());
    setDoubleValue(2.2);
    assertEquals(Double.valueOf(2.2), doubleField.getDouble());
    doubleField.setText("42.2");
    assertEquals(Double.valueOf(42.2), this.doubleValue);
    doubleField.setText("");
    assertNull(this.doubleValue);
  }

  @Test
  void testDoublePrimitive() throws Exception {
    DoubleField doubleField = new DoubleField();
    doubleField.setSeparators('.', ',');
    Value<Double> doublePrimitivePropertyValue = Value.propertyValue(this, "doublePrimitiveValue",
            double.class, doublePrimitiveValueValueChangedEvent);
    ComponentValue<Double, DoubleField> componentValue = ComponentValues.doubleField(doubleField, false);
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
  void doubleComponentValue() {
    final Double value = 10.4;
    DoubleField doubleField = new DoubleField();
    doubleField.setDouble(value);
    ComponentValue<Double, DoubleField> componentValue = ComponentValues.doubleField(doubleField);
    assertEquals(value, componentValue.get());
    componentValue = ComponentValues.doubleField(new DoubleField());
    assertNull(componentValue.get());
  }

  @Test
  void parseDouble() {
    ComponentValue<Double, DoubleField> componentValue = ComponentValues.doubleField(new DoubleField());
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
  void integerValueField() {
    final Integer value = 10;
    IntegerField integerField = new IntegerField();
    integerField.setInteger(value);
    ComponentValue<Integer, IntegerField> componentValue = ComponentValues.integerField(integerField);
    assertEquals(value, componentValue.get());

    componentValue = ComponentValues.integerField(new IntegerField());
    assertNull(componentValue.get());

    componentValue.getComponent().setText("15");
    assertEquals(Integer.valueOf(15), componentValue.get());
  }

  @Test
  void integerSpinnerUiValue() {
    SpinnerNumberModel model = new SpinnerNumberModel();
    JSpinner spinner = new JSpinner(model);
    Value<Integer> value = ComponentValues.integerSpinner(spinner);

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
    JProgressBar progressBar = new JProgressBar(model);
    Value<Integer> value = ComponentValues.progressBar(progressBar);

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
    JSpinner spinner = new JSpinner(model);
    Value<Double> value = ComponentValues.doubleSpinner(spinner);

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
    IntegerField integerField = new IntegerField();
    Value<Integer> value = ComponentValues.integerField(integerField);

    assertNull(value.get());
    integerField.setText("122");
    assertEquals(Integer.valueOf(122), value.get());
    integerField.setText("");
    assertNull(value.get());

    value.set(42);
    assertEquals("42", integerField.getText());
  }

  @Test
  void integerPrimitiveTextUiValue() {
    IntegerField integerField = new IntegerField();
    Value<Integer> value = ComponentValues.integerField(integerField, false);

    assertEquals(Integer.valueOf(0), value.get());
    integerField.setText("122");
    assertEquals(Integer.valueOf(122), value.get());
    integerField.setText("");
    assertEquals(Integer.valueOf(0), value.get());

    value.set(42);
    assertEquals("42", integerField.getText());
  }

  @Test
  void longValue() {
    final Long value = 10L;
    LongField longField = new LongField();
    ComponentValue<Long, LongField> componentValue = ComponentValues.longField(longField);
    componentValue.set(value);
    assertEquals(value, componentValue.get());

    componentValue = ComponentValues.longField(new LongField());
    assertNull(componentValue.get());

    componentValue.getComponent().setText("15");
    assertEquals(Long.valueOf(15), componentValue.get());
  }

  @Test
  void longTextUiValue() {
    LongField longField = new LongField();
    Value<Long> value = ComponentValues.longField(longField);

    assertNull(value.get());
    longField.setText("122");
    assertEquals(Long.valueOf(122), value.get());
    longField.setText("");
    assertNull(value.get());

    value.set(42L);
    assertEquals("42", longField.getText());
  }

  @Test
  void longPrimitiveTextUiValue() {
    LongField longField = new LongField();
    Value<Long> value = ComponentValues.longField(longField, false);

    assertEquals(Long.valueOf(0), value.get());
    longField.setText("122");
    assertEquals(Long.valueOf(122), value.get());
    longField.setText("");
    assertEquals(Long.valueOf(0), value.get());

    value.set(42L);
    assertEquals("42", longField.getText());
  }

  @Test
  void doubleTextUiValue() {
    DoubleField doubleField = new DoubleField();
    doubleField.setSeparators('.', ',');
    Value<Double> value = ComponentValues.doubleField(doubleField);

    assertNull(value.get());
    doubleField.setText("122.2");
    assertEquals(Double.valueOf(122.2), value.get());
    doubleField.setText("");
    assertNull(value.get());

    value.set(42.2);
    assertEquals("42.2", doubleField.getText());
  }

  @Test
  void doublePrimitiveTextUiValue() {
    DoubleField doubleField = new DoubleField();
    doubleField.setSeparators('.', ',');
    Value<Double> value = ComponentValues.doubleField(doubleField, false);

    assertEquals(Double.valueOf(0), value.get());
    doubleField.setText("122.2");
    assertEquals(Double.valueOf(122.2), value.get());
    doubleField.setText("");
    assertEquals(Double.valueOf(0), value.get());

    value.set(42.2);
    assertEquals("42.2", doubleField.getText());
  }

  @Test
  void bigDecimalTextUiValue() {
    BigDecimalField bigDecimalField = new BigDecimalField();
    bigDecimalField.setSeparators('.', ',');
    Value<BigDecimal> value = ComponentValues.bigDecimalField(bigDecimalField);

    assertNull(value.get());
    bigDecimalField.setText("122.2");
    assertEquals(BigDecimal.valueOf(122.2), value.get());
    bigDecimalField.setText("");
    assertNull(value.get());

    value.set(BigDecimal.valueOf(42.2));
    assertEquals("42.2", bigDecimalField.getText());
  }
}
