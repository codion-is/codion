/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.value;

import org.jminor.common.value.Value;
import org.jminor.common.value.Values;
import org.jminor.swing.common.ui.textfield.LongField;

import java.text.NumberFormat;

public final class LongValues {

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
    Values.link(value, longValue(longField, nullable, updateOnKeystroke));
  }
}
