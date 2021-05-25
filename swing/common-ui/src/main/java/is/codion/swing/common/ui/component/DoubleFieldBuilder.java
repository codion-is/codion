/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.swing.common.ui.textfield.DoubleField;

/**
 * A builder {@link DoubleField}.
 */
public interface DoubleFieldBuilder extends NumberFieldBuilder<Double, DoubleField, DoubleFieldBuilder> {

  /**
   * @param maximumFractionDigits the maximum fraction digits for floating point numbers, if applicable
   * @return this builder instance
   */
  DoubleFieldBuilder maximumFractionDigits(int maximumFractionDigits);
}
