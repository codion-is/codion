/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.value;

import is.codion.common.Formats;
import is.codion.common.event.EventObserver;
import is.codion.common.value.Nullable;
import is.codion.common.value.Value;
import is.codion.common.value.Values;
import is.codion.swing.common.ui.textfield.BigDecimalField;
import is.codion.swing.common.ui.textfield.DoubleField;
import is.codion.swing.common.ui.textfield.IntegerField;
import is.codion.swing.common.ui.textfield.LongField;

import javax.swing.BoundedRangeModel;
import javax.swing.SpinnerNumberModel;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 * Utility class for numerical {@link ComponentValue} instances.
 */
public final class NumericalValues {

  private NumericalValues() {}

  /**
   * @return a BigDecimal based ComponentValue
   */
  public static ComponentValue<BigDecimal, BigDecimalField> bigDecimalValue() {
    return bigDecimalValue(null, Formats.getBigDecimalNumberFormat());
  }

  /**
   * @param initialValue the initial value
   * @return a BigDecimal based ComponentValue
   */
  public static ComponentValue<BigDecimal, BigDecimalField> bigDecimalValue(final BigDecimal initialValue) {
    return bigDecimalValue(initialValue, Formats.getBigDecimalNumberFormat());
  }

  /**
   * @param initialValue the initial value
   * @param format the number format to use
   * @return a BigDecimal based ComponentValue
   */
  public static ComponentValue<BigDecimal, BigDecimalField> bigDecimalValue(final BigDecimal initialValue,
                                                                            final DecimalFormat format) {
    final BigDecimalField bigDecimalField = new BigDecimalField(format);
    bigDecimalField.setBigDecimal(initialValue);

    return bigDecimalValue(bigDecimalField);
  }

  /**
   * @param bigDecimalField the component
   * @return a Value bound to the given component
   */
  public static ComponentValue<BigDecimal, BigDecimalField> bigDecimalValue(final BigDecimalField bigDecimalField) {
    return bigDecimalValue(bigDecimalField, UpdateOn.KEYSTROKE);
  }

  /**
   * @param bigDecimalField the component
   * @param updateOn specifies when the underlying value should be updated
   * @return a Value bound to the given component
   */
  public static ComponentValue<BigDecimal, BigDecimalField> bigDecimalValue(final BigDecimalField bigDecimalField,
                                                                            final UpdateOn updateOn) {
    return new BigDecimalFieldValue(bigDecimalField, updateOn);
  }

  /**
   * @param spinnerModel the spinner model
   * @return a Value bound to the given component
   */
  public static ComponentValue<Double, SpinnerNumberModel> doubleValue(final SpinnerNumberModel spinnerModel) {
    return new SpinnerNumberValue<>(spinnerModel);
  }

  /**
   * Instantiates a new Double based ComponentValue.
   * @return a Double based ComponentValue
   */
  public static ComponentValue<Double, DoubleField> doubleValue() {
    return doubleValue(null, new DecimalFormat());
  }

  /**
   * Instantiates a new Double based ComponentValue.
   * @param initialValue the initial value
   * @return a Double based ComponentValue
   */
  public static ComponentValue<Double, DoubleField> doubleValue(final Double initialValue) {
    return doubleValue(initialValue, new DecimalFormat());
  }

  /**
   * Instantiates a new Double based ComponentValue.
   * @param initialValue the initial value
   * @param format the number format to use
   * @return a Double based ComponentValue
   */
  public static ComponentValue<Double, DoubleField> doubleValue(final Double initialValue,
                                                                final DecimalFormat format) {
    final DoubleField doubleField = new DoubleField(format);
    doubleField.setDouble(initialValue);

    return doubleValue(doubleField, Nullable.YES);
  }

  /**
   * @param doubleField the component
   * @return a Value bound to the given component
   */
  public static ComponentValue<Double, DoubleField> doubleValue(final DoubleField doubleField) {
    return doubleValue(doubleField, Nullable.YES);
  }

  /**
   * @param doubleField the component
   * @param nullable if {@link Nullable#NO} then the resulting Value translates null to 0
   * @return a Value bound to the given component
   */
  public static ComponentValue<Double, DoubleField> doubleValue(final DoubleField doubleField, final Nullable nullable) {
    return doubleValue(doubleField, nullable, UpdateOn.KEYSTROKE);
  }

  /**
   * @param doubleField the component
   * @param nullable if {@link Nullable#NO} then the resulting Value translates null to 0
   * @param updateOn specifies when the underlying value should be updated
   * @return a Value bound to the given component
   */
  public static ComponentValue<Double, DoubleField> doubleValue(final DoubleField doubleField, final Nullable nullable,
                                                                final UpdateOn updateOn) {
    return new DecimalFieldValue(doubleField, nullable, updateOn);
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
    return integerValue(integerField, Nullable.YES);
  }

  /**
   * @param integerField the component
   * @param nullable if {@link Nullable#NO} then the resulting Value translates null to 0
   * @return a Value bound to the given component
   */
  public static ComponentValue<Integer, IntegerField> integerValue(final IntegerField integerField, final Nullable nullable) {
    return integerValue(integerField, nullable, UpdateOn.KEYSTROKE);
  }

  /**
   * @param integerField the component
   * @param nullable if {@link Nullable#NO} then the resulting Value translates null to 0
   * @param updateOn specifies when the underlying value should be updated
   * @return a Value bound to the given component
   */
  public static ComponentValue<Integer, IntegerField> integerValue(final IntegerField integerField, final Nullable nullable,
                                                                   final UpdateOn updateOn) {
    return new IntegerFieldValue(integerField, nullable, updateOn);
  }

  /**
   * @param spinnerModel the spinner model
   * @return a Value bound to the given model
   */
  public static ComponentValue<Integer, SpinnerNumberModel> integerValue(final SpinnerNumberModel spinnerModel) {
    return new SpinnerNumberValue<>(spinnerModel);
  }

  /**
   * @param boundedRangeModel the bounded range model
   * @return a Value bound to the given model
   */
  public static ComponentValue<Integer, BoundedRangeModel> integerValue(final BoundedRangeModel boundedRangeModel) {
    return new IntegerBoundedRangeModelValue(boundedRangeModel);
  }

  /**
   * Creates a SpinnerNumberModel based on an integer property value
   * @param owner the value owner
   * @param propertyName the property name
   * @param valueChangeEvent an EventObserver notified each time the value changes
   * @return a SpinnerNumberModel based on the value
   */
  public static SpinnerNumberModel integerValueSpinnerModel(final Object owner, final String propertyName,
                                                            final EventObserver<Integer> valueChangeEvent) {
    final SpinnerNumberModel numberModel = new SpinnerNumberModel();
    integerValue(numberModel).link(Values.propertyValue(owner, propertyName, int.class, valueChangeEvent));

    return numberModel;
  }

  /**
   * Creates a SpinnerNumberModel based on an integer value
   * @param integerValue the value
   * @return a SpinnerNumberModel based on the value
   */
  public static SpinnerNumberModel integerValueSpinnerModel(final Value<Integer> integerValue) {
    final SpinnerNumberModel numberModel = new SpinnerNumberModel();
    integerValue(numberModel).link(integerValue);

    return numberModel;
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
    return longValue(longField, Nullable.YES);
  }

  /**
   * @param longField the component
   * @param nullable if {@link Nullable#NO} then the resulting Value translates null to 0
   * @return a Value bound to the given component
   */
  public static ComponentValue<Long, LongField> longValue(final LongField longField, final Nullable nullable) {
    return longValue(longField, nullable, UpdateOn.KEYSTROKE);
  }

  /**
   * @param longField the component
   * @param nullable if {@link Nullable#NO} then the resulting Value translates null to 0
   * @param updateOn specifies when the underlying value should be updated
   * @return a Value bound to the given component
   */
  public static ComponentValue<Long, LongField> longValue(final LongField longField, final Nullable nullable,
                                                          final UpdateOn updateOn) {
    return new LongFieldValue(longField, nullable, updateOn);
  }
}
