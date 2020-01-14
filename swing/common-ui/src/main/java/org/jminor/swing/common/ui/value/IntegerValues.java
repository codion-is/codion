/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.value;

import org.jminor.common.event.EventObserver;
import org.jminor.common.value.Value;
import org.jminor.common.value.Values;
import org.jminor.swing.common.ui.textfield.IntegerField;

import javax.swing.BoundedRangeModel;
import javax.swing.SpinnerNumberModel;
import java.text.NumberFormat;

import static org.jminor.common.value.Values.propertyValue;

public final class IntegerValues {

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
    Values.link(value, integerValue(integerField, nullable, updateOnKeystroke));
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
    Values.link(integerValue, integerValue(spinnerModel), readOnly);
  }
}
