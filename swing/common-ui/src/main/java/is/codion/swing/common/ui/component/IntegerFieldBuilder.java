/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.swing.common.ui.component.textfield.IntegerField;

/**
 * A builder {@link IntegerField}.
 */
public interface IntegerFieldBuilder extends NumberFieldBuilder<Integer, IntegerField, IntegerFieldBuilder> {

  /**
   * Sets the allowed value range
   * @param from the from value
   * @param to the to value
   * @return this builder instance
   */
  IntegerFieldBuilder range(int from, int to);
}
