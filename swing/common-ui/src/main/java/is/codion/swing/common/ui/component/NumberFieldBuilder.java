/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.swing.common.ui.textfield.NumberField;

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
}
