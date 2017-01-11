/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.ui.values;

import org.jminor.common.Value;

import javafx.util.StringConverter;

/**
 * A {@link org.jminor.common.Value} based on a String
 * @param <V> the type of the actual value
 */
public interface StringValue<V> extends Value<V> {

  /**
   * @return the {@link StringConverter} used to convert the value to and from String
   */
  StringConverter<V> getConverter();
}
