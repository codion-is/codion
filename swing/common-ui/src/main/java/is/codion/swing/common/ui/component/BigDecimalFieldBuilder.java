/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.swing.common.ui.textfield.BigDecimalField;

import java.math.BigDecimal;

/**
 * A builder {@link BigDecimalField}.
 */
public interface BigDecimalFieldBuilder extends NumberFieldBuilder<BigDecimal, BigDecimalField, BigDecimalFieldBuilder> {

  /**
   * @param maximumFractionDigits the maximum fraction digits for floating point numbers, if applicable
   * @return this builder instance
   */
  BigDecimalFieldBuilder maximumFractionDigits(int maximumFractionDigits);
}
