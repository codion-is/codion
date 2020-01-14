/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.value;

import org.jminor.common.Formats;
import org.jminor.common.value.Value;
import org.jminor.common.value.Values;
import org.jminor.swing.common.ui.textfield.DecimalField;

import java.math.BigDecimal;
import java.text.DecimalFormat;

public final class BigDecimalValues {

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
    Values.link(value, bigDecimalValue(decimalField, updateOnKeystroke));
  }
}
