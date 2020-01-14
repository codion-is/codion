/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.value;

import org.jminor.common.value.Value;
import org.jminor.common.value.Values;
import org.jminor.swing.common.ui.textfield.DecimalField;

import javax.swing.SpinnerNumberModel;
import java.text.DecimalFormat;

public final class DoubleValues {

  /**
   * @param spinnerModel the spinner model
   * @return a Value bound to the given component
   */
  public static ComponentValue<Double, SpinnerNumberModel> doubleValue(final SpinnerNumberModel spinnerModel) {
    return new SpinnerNumberValue(spinnerModel);
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
    Values.link(value, doubleValue(decimalField, nullable, updateOnKeystroke));
  }
}
