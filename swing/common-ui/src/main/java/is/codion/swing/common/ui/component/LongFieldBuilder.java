/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.swing.common.ui.component.textfield.LongField;

/**
 * A builder {@link LongField}.
 */
public interface LongFieldBuilder extends NumberFieldBuilder<Long, LongField, LongFieldBuilder> {

  /**
   * Sets the allowed value range
   * @param from the from value
   * @param to the to value
   * @return this builder instance
   */
  LongFieldBuilder range(long from, long to);
}
