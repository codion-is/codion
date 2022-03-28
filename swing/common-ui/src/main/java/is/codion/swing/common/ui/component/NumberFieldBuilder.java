/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.swing.common.ui.component.textfield.NumberField;

/**
 * Builds a NumberField descendant
 * @param <T> the value type
 * @param <C> the number field type
 * @param <B> the builder type
 */
public interface NumberFieldBuilder<T extends Number, C extends NumberField<T>, B extends NumberFieldBuilder<T, C, B>> extends TextFieldBuilder<T, C, B> {

  /**
   * @param minimumValue the minimum numerical value, if applicable
   * @return this builder instance
   */
  B minimumValue(Double minimumValue);

  /**
   * @param maximumValue the maximum numerical value, if applicable
   * @return this builder instance
   */
  B maximumValue(Double maximumValue);

  /**
   * @param groupingSeparator the grouping separator
   * @return this builder instance
   */
  B groupingSeparator(char groupingSeparator);

  /**
   * Note that this is overridden by {@link #format(java.text.Format)}.
   * @param groupingUsed true if grouping should be used
   * @return this builder instance
   */
  B groupingUsed(boolean groupingUsed);
}
