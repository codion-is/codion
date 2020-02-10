/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.value;

import org.jminor.common.Formats;
import org.jminor.common.event.EventObserver;
import org.jminor.common.value.Value;
import org.jminor.swing.common.ui.textfield.DecimalField;
import org.jminor.swing.common.ui.textfield.IntegerField;
import org.jminor.swing.common.ui.textfield.LongField;

import javax.swing.BoundedRangeModel;
import javax.swing.SpinnerNumberModel;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import static org.jminor.common.value.Values.propertyValue;

/**
 * Utility class for numerical {@link ComponentValue} instances.
 */
public final class NumericalValues {

  private NumericalValues() {}

  /**
   * @return a BigDecimal based ComponentValue
   */
  public static ComponentValue<BigDecimal, DecimalField> bigDecimalValue() {
    return bigDecimalValue(null, Formats.getBigDecimalNumberFormat());
  }

  /**
   * @param initialValue the initial value
   * @return a BigDecimal based ComponentValue
   */
  public static ComponentValue<BigDecimal, DecimalField> bigDecimalValue(final BigDecimal initialValue) {
    return bigDecimalValue(initialValue, Formats.getBigDecimalNumberFormat());
  }

  /**
   * @param initialValue the initial value
   * @param format the number format to use
   * @return a BigDecimal based ComponentValue
   */
  public static ComponentValue<BigDecimal, DecimalField> bigDecimalValue(final BigDecimal initialValue,
                                                                         final DecimalFormat format) {
    final DecimalField decimalField = new DecimalField(format);
    decimalField.setBigDecimal(initialValue);

    return bigDecimalValue(decimalField);
  }

  /**
   * @param decimalField the component
   * @return a Value bound to the given component
   */
  public static ComponentValue<BigDecimal, DecimalField> bigDecimalValue(final DecimalField decimalField) {
    return bigDecimalValue(decimalField, true);
  }

  /**
   * @param decimalField the component
   * @param updateOnKeystroke if true then the value is updated on each keystroke, otherwise on focus lost
   * @return a Value bound to the given component
   */
  public static ComponentValue<BigDecimal, DecimalField> bigDecimalValue(final DecimalField decimalField,
                                                                         final boolean updateOnKeystroke) {
    return new BigDecimalFieldValue(decimalField, updateOnKeystroke);
  }

  /**
   * @param decimalField the decimal field to link with the value
   * @param value the model value
   */
  public static void bigDecimalValueLink(final DecimalField decimalField, final Value<BigDecimal> value) {
    bigDecimalValueLink(decimalField, value, true);
  }

  /**
   * @param decimalField the decimal field to link with the value
   * @param value the model value
   * @param updateOnKeystroke if true then the value is updated on each keystroke, otherwise on focus lost
   */
  public static void bigDecimalValueLink(final DecimalField decimalField, final Value<BigDecimal> value,
                                         final boolean updateOnKeystroke) {
    value.link(bigDecimalValue(decimalField, updateOnKeystroke));
  }

  /**
   * @param spinnerModel the spinner model
   * @return a Value bound to the given component
   */
  public static ComponentValue<Double, SpinnerNumberModel> doubleValue(final SpinnerNumberModel spinnerModel) {
    return new SpinnerNumberValue(spinnerModel);
  }

  /**
   * Instantiates a new Double based ComponentValue.
   * @return a Double based ComponentValue
   */
  public static ComponentValue<Double, DecimalField> doubleValue() {
    return doubleValue(null, new DecimalFormat());
  }

  /**
   * Instantiates a new Double based ComponentValue.
   * @param initialValue the initial value
   * @return a Double based ComponentValue
   */
  public static ComponentValue<Double, DecimalField> doubleValue(final Double initialValue) {
    return doubleValue(initialValue, new DecimalFormat());
  }

  /**
   * Instantiates a new Double based ComponentValue.
   * @param initialValue the initial value
   * @param format the number format to use
   * @return a Double based ComponentValue
   */
  public static ComponentValue<Double, DecimalField> doubleValue(final Double initialValue,
                                                                 final DecimalFormat format) {
    final DecimalField decimalField = new DecimalField(format);
    decimalField.setDouble(initialValue);

    return doubleValue(decimalField, true);
  }

  /**
   * @param decimalField the component
   * @return a Value bound to the given component
   */
  public static ComponentValue<Double, DecimalField> doubleValue(final DecimalField decimalField) {
    return doubleValue(decimalField, true);
  }

  /**
   * @param decimalField the component
   * @param nullable if false then the resulting Value returns 0 instead of null
   * @return a Value bound to the given component
   */
  public static ComponentValue<Double, DecimalField> doubleValue(final DecimalField decimalField, final boolean nullable) {
    return doubleValue(decimalField, nullable, true);
  }

  /**
   * @param decimalField the component
   * @param nullable if false then the resulting Value returns 0 instead of null
   * @param updateOnKeystroke if true then the value is updated on each keystroke, otherwise on focus lost
   * @return a Value bound to the given component
   */
  public static ComponentValue<Double, DecimalField> doubleValue(final DecimalField decimalField, final boolean nullable,
                                                                 final boolean updateOnKeystroke) {
    return new DecimalFieldValue(decimalField, nullable, updateOnKeystroke);
  }

  /**
   * @param decimalField the decimal field to link with the value
   * @param value the model value
   */
  public static void doubleValueLink(final DecimalField decimalField, final Value<Double> value) {
    doubleValueLink(decimalField, value, true);
  }

  /**
   * @param decimalField the decimal field to link with the value
   * @param value the model value
   * @param nullable if false then 0 is used instead of null
   */
  public static void doubleValueLink(final DecimalField decimalField, final Value<Double> value, final boolean nullable) {
    doubleValueLink(decimalField, value, nullable, true);
  }

  /**
   * @param decimalField the decimal field to link with the value
   * @param value the model value
   * @param nullable if false then 0 is used instead of null
   * @param updateOnKeystroke if true then the value is updated on each keystroke, otherwise on focus lost
   */
  public static void doubleValueLink(final DecimalField decimalField, final Value<Double> value, final boolean nullable,
                                     final boolean updateOnKeystroke) {
    value.link(doubleValue(decimalField, nullable, updateOnKeystroke));
  }

  /**
   * Instantiates a new Integer based ComponentValue.
   * @return a Integer based ComponentValue
   */
  public static ComponentValue<Integer, IntegerField> integerValue() {
    return integerValue(null, NumberFormat.getIntegerInstance());
  }

  /**
   * Instantiates a new Integer based ComponentValue.
   * @param initialValue the initial value
   * @return a Integer based ComponentValue
   */
  public static ComponentValue<Integer, IntegerField> integerValue(final Integer initialValue) {
    return integerValue(initialValue, NumberFormat.getIntegerInstance());
  }

  /**
   * Instantiates a new Integer based ComponentValue.
   * @param initialValue the initial value
   * @param format the number format to use
   * @return a Integer based ComponentValue
   */
  public static ComponentValue<Integer, IntegerField> integerValue(final Integer initialValue, final NumberFormat format) {
    final IntegerField integerField = new IntegerField(format);
    integerField.setInteger(initialValue);

    return integerValue(integerField);
  }

  /**
   * @param integerField the component
   * @return a Value bound to the given component
   */
  public static ComponentValue<Integer, IntegerField> integerValue(final IntegerField integerField) {
    return integerValue(integerField, true);
  }

  /**
   * @param integerField the component
   * @param nullable if false then the resulting Value returns 0 instead of null
   * @return a Value bound to the given component
   */
  public static ComponentValue<Integer, IntegerField> integerValue(final IntegerField integerField, final boolean nullable) {
    return integerValue(integerField, nullable, true);
  }

  /**
   * @param integerField the component
   * @param nullable if false then the resulting Value returns 0 instead of null
   * @param updateOnKeystroke if true then the value is updated on each keystroke, otherwise on focus lost
   * @return a Value bound to the given component
   */
  public static ComponentValue<Integer, IntegerField> integerValue(final IntegerField integerField, final boolean nullable,
                                                                   final boolean updateOnKeystroke) {
    return new IntegerFieldValue(integerField, nullable, updateOnKeystroke);
  }

  /**
   * @param spinnerModel the spinner model
   * @return a Value bound to the given model
   */
  public static ComponentValue<Integer, SpinnerNumberModel> integerValue(final SpinnerNumberModel spinnerModel) {
    return new SpinnerNumberValue(spinnerModel);
  }

  /**
   * @param boundedRangeModel the bounded range model
   * @return a Value bound to the given model
   */
  public static ComponentValue<Integer, BoundedRangeModel> integerValue(final BoundedRangeModel boundedRangeModel) {
    return new IntegerBoundedRangeModelValue(boundedRangeModel);
  }

  /**
   * @param integerField the int field to link with the value
   * @param value the model value
   */
  public static void integerValueLink(final IntegerField integerField, final Value<Integer> value) {
    integerValueLink(integerField, value, true);
  }

  /**
   * @param integerField the int field to link with the value
   * @param value the model value
   * @param nullable if false then 0 is used instead of null
   */
  public static void integerValueLink(final IntegerField integerField, final Value<Integer> value, final boolean nullable) {
    integerValueLink(integerField, value, nullable, true);
  }

  /**
   * @param integerField the int field to link with the value
   * @param value the model value
   * @param nullable if false then 0 is used instead of null
   * @param updateOnKeystroke if true then the value is updated on each keystroke, otherwise on focus lost
   */
  public static void integerValueLink(final IntegerField integerField, final Value<Integer> value, final boolean nullable,
                                      final boolean updateOnKeystroke) {
    value.link(integerValue(integerField, nullable, updateOnKeystroke));
  }

  /**
   * @param owner the value owner
   * @param propertyName the property name
   * @param valueChangeEvent an EventObserver notified each time the value changes
   * @return a SpinnerNumberModel based on the value
   */
  public static SpinnerNumberModel integerSpinnerValueLink(final Object owner, final String propertyName,
                                                           final EventObserver<Integer> valueChangeEvent) {
    return integerSpinnerValueLink(owner, propertyName, valueChangeEvent, false);
  }

  /**
   * @param owner the value owner
   * @param propertyName the property name
   * @param valueChangeEvent an EventObserver notified each time the value changes
   * @param readOnly if true the value link will be read only
   * @return a SpinnerNumberModel based on the value
   */
  public static SpinnerNumberModel integerSpinnerValueLink(final Object owner, final String propertyName,
                                                           final EventObserver<Integer> valueChangeEvent, final boolean readOnly) {
    final SpinnerNumberModel numberModel = new SpinnerNumberModel();
    integerSpinnerValueLink(owner, propertyName, valueChangeEvent, numberModel, readOnly);

    return numberModel;
  }

  /**
   * @param integerValue the value
   * @return a SpinnerNumberModel based on the value
   */
  public static SpinnerNumberModel integerSpinnerValueLink(final Value<Integer> integerValue) {
    return integerSpinnerValueLink(integerValue, false);
  }

  /**
   * @param integerValue the value
   * @param readOnly if true the value link will be read only
   * @return a SpinnerNumberModel based on the value
   */
  public static SpinnerNumberModel integerSpinnerValueLink(final Value<Integer> integerValue, final boolean readOnly) {
    final SpinnerNumberModel numberModel = new SpinnerNumberModel();
    integerSpinnerValueLink(numberModel, integerValue, readOnly);

    return numberModel;
  }

  /**
   * @param owner the value owner
   * @param propertyName the property name
   * @param valueChangeEvent an EventObserver notified each time the value changes
   * @param spinnerModel the spinner model to use
   * @param readOnly if true the value link will be read only
   */
  public static void integerSpinnerValueLink(final Object owner, final String propertyName, final EventObserver<Integer> valueChangeEvent,
                                             final SpinnerNumberModel spinnerModel, final boolean readOnly) {
    integerSpinnerValueLink(spinnerModel, propertyValue(owner, propertyName, int.class, valueChangeEvent), readOnly);
  }

  /**
   * @param spinnerModel the spinner model
   * @param integerValue the value
   */
  public static void integerSpinnerValueLink(final SpinnerNumberModel spinnerModel, final Value<Integer> integerValue) {
    integerSpinnerValueLink(spinnerModel, integerValue, false);
  }

  /**
   * @param spinnerModel the spinner model
   * @param integerValue the value
   * @param readOnly if true the value link will be read only
   */
  public static void integerSpinnerValueLink(final SpinnerNumberModel spinnerModel, final Value<Integer> integerValue, final boolean readOnly) {
    integerValue.link(integerValue(spinnerModel), readOnly);
  }

  /**
   * Instantiates a new Long based ComponentValue.
   * @return a Long based ComponentValue
   */
  public static ComponentValue<Long, LongField> longValue() {
    return longValue((Long) null);
  }

  /**
   * Instantiates a new Long based ComponentValue.
   * @param initialValue the initial value
   * @return a Long based ComponentValue
   */
  public static ComponentValue<Long, LongField> longValue(final Long initialValue) {
    return longValue(initialValue, NumberFormat.getIntegerInstance());
  }

  /**
   * Instantiates a new Long based ComponentValue.
   * @param initialValue the initial value
   * @param format the number format to use
   * @return a Long based ComponentValue
   */
  public static ComponentValue<Long, LongField> longValue(final Long initialValue, final NumberFormat format) {
    final LongField longField = new LongField(format);
    longField.setLong(initialValue);

    return longValue(longField);
  }

  /**
   * @param longField the component
   * @return a Value bound to the given component
   */
  public static ComponentValue<Long, LongField> longValue(final LongField longField) {
    return longValue(longField, true);
  }

  /**
   * @param longField the component
   * @param nullable if false then the resulting Value returns 0 instead of null
   * @return a Value bound to the given component
   */
  public static ComponentValue<Long, LongField> longValue(final LongField longField, final boolean nullable) {
    return longValue(longField, nullable, true);
  }

  /**
   * @param longField the component
   * @param nullable if false then the resulting Value returns 0 instead of null
   * @param updateOnKeystroke if true then the value is updated on each keystroke, otherwise on focus lost
   * @return a Value bound to the given component
   */
  public static ComponentValue<Long, LongField> longValue(final LongField longField, final boolean nullable,
                                                          final boolean updateOnKeystroke) {
    return new LongFieldValue(longField, nullable, updateOnKeystroke);
  }

  /**
   * @param longField the long field to link with the value
   * @param value the model value
   */
  public static void longValueLink(final LongField longField, final Value<Long> value) {
    longValueLink(longField, value, true);
  }

  /**
   * @param longField the long field to link with the value
   * @param value the model value
   * @param nullable if false then 0 is used instead of null
   */
  public static void longValueLink(final LongField longField, final Value<Long> value, final boolean nullable) {
    longValueLink(longField, value, nullable, true);
  }

  /**
   * @param longField the long field to link with the value
   * @param value the model value
   * @param nullable if false then 0 is used instead of null
   * @param updateOnKeystroke if true then the value is updated on each keystroke, otherwise on focus lost
   */
  public static void longValueLink(final LongField longField, final Value<Long> value, final boolean nullable,
                                   final boolean updateOnKeystroke) {
    value.link(longValue(longField, nullable, updateOnKeystroke));
  }
}
