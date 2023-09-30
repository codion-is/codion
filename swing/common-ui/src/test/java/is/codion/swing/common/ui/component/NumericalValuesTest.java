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
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component;

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

  @Test
  void testLong() {
    Value<Long> longValue = Value.value();
    NumberField<Long> longField = Components.longField(longValue)
            .build();
    assertNull(longField.getNumber());
    longValue.set(2L);
    assertEquals(2, longField.getNumber().longValue());
    longField.setText("42");
    assertEquals(42, longValue.get());
    longField.setText("");
    assertNull(longValue.get());
  }

  @Test
  void testLongPrimitive() {
    Value<Long> longPrimitivePropertyValue = Value.value(null, 0L);
    assertFalse(longPrimitivePropertyValue.nullable());
    ComponentValue<Long, NumberField<Long>> componentValue = Components.longField(longPrimitivePropertyValue)
            .buildValue();
    NumberField<Long> longField = componentValue.component();
    assertEquals(0L, longField.getNumber());
    assertEquals(0, componentValue.get());
    longPrimitivePropertyValue.set(2L);
    assertEquals(2, longField.getNumber().longValue());
    longField.setText("42");
    assertEquals(42, longPrimitivePropertyValue.get());
    longField.setText("");
    assertEquals(0, longPrimitivePropertyValue.get());
  }

  @Test
  void testInteger() {
    Value<Integer> integerPropertyValue = Value.value();
    NumberField<Integer> integerField = Components.integerField(integerPropertyValue)
            .build();
    assertNull(integerField.getNumber());
    integerPropertyValue.set(2);
    assertEquals(2, integerField.getNumber().intValue());
    integerField.setText("42");
    assertEquals(42, integerPropertyValue.get());
    integerField.setText("");
    assertNull(integerPropertyValue.get());
  }

  @Test
  void testInt() {
    Value<Integer> integerPropertyValue = Value.value(null, 0);
    ComponentValue<Integer, NumberField<Integer>> componentValue = Components.integerField(integerPropertyValue)
            .buildValue();
    NumberField<Integer> integerField = componentValue.component();
    assertEquals(0, integerField.getNumber());
    assertEquals(0, componentValue.get());
    integerPropertyValue.set(2);
    assertEquals(2, integerField.getNumber().intValue());
    integerField.setText("42");
    assertEquals(42, integerPropertyValue.get());
    integerField.setText("");
    assertEquals(0, integerPropertyValue.get());
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
  void testDouble() {
    Value<Double> doublePropertyValue = Value.value();
    NumberField<Double> doubleField = Components.doubleField(doublePropertyValue)
            .decimalSeparator('.')
            .groupingSeparator(',')
            .build();
    assertNull(doubleField.getNumber());
    doublePropertyValue.set(2.2);
    assertEquals(Double.valueOf(2.2), doubleField.getNumber());
    doubleField.setText("42.2");
    assertEquals(Double.valueOf(42.2), doublePropertyValue.get());
    doubleField.setText("");
    assertNull(doublePropertyValue.get());
  }

  @Test
  void testDoublePrimitive() {
    Value<Double> doublePrimitivePropertyValue = Value.value(null, 0d);
    ComponentValue<Double, NumberField<Double>> componentValue = Components.doubleField()
            .decimalSeparator('.')
            .groupingSeparator(',')
            .buildValue();
    NumberField<Double> doubleField = componentValue.component();
    componentValue.link(doublePrimitivePropertyValue);
    assertEquals(0d, doubleField.getNumber());
    assertEquals(0d, componentValue.get());
    doublePrimitivePropertyValue.set(2.2);
    assertEquals(Double.valueOf(2.2), doubleField.getNumber());
    doubleField.setText("42.2");
    assertEquals(42.2, doublePrimitivePropertyValue.get());
    doubleField.setText("");
    assertEquals(0.0, doublePrimitivePropertyValue.get());
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
