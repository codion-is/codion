/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.swing.common.ui.component.textfield.BigDecimalField;

import java.math.BigDecimal;

/**
 * A builder {@link BigDecimalField}.
 */
public interface BigDecimalFieldBuilder extends NumberFieldBuilder<BigDecimal, BigDecimalField, BigDecimalFieldBuilder> {

  /**
   * @param maximumFractionDigits the maximum fraction digits
   * @return this builder instance
   */
  BigDecimalFieldBuilder maximumFractionDigits(int maximumFractionDigits);

  /**
   * Set the decimal separator for this field
   * @param decimalSeparator the decimal separator
   * @return this builder instance
   * @throws IllegalArgumentException in case the decimal separator is the same as the grouping separator
   */
  BigDecimalFieldBuilder decimalSeparator(char decimalSeparator);
}
