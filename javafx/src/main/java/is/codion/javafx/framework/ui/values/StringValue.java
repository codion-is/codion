/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.javafx.framework.ui.values;

import dev.codion.common.value.Value;

import javafx.util.StringConverter;

/**
 * A {@link Value} based on a String
 * @param <V> the type of the actual value
 */
public interface StringValue<V> extends Value<V> {

  /**
   * @return the {@link StringConverter} used to convert the value to and from String
   */
  StringConverter<V> getConverter();
}
