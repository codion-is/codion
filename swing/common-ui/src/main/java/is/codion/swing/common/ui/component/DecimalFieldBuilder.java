/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.swing.common.ui.component.textfield.NumberField;

/**
 * A builder for a decimal based {@link NumberField}.
 */
public interface DecimalFieldBuilder<T extends Number, B extends DecimalFieldBuilder<T, B>>
        extends NumberFieldBuilder<T, B> {

    /**
   * @param maximumFractionDigits the maximum fraction digits
   * @return this builder instance
   */
  B maximumFractionDigits(int maximumFractionDigits);

  /**
   * Set the decimal separator for this field
   * @param decimalSeparator the decimal separator
   * @return this builder instance
   * @throws IllegalArgumentException in case the decimal separator is the same as the grouping separator
   */
  B decimalSeparator(char decimalSeparator);
}
