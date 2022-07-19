/*
 * Copyright (c) 2015 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.javafx.framework.ui.values;

import is.codion.common.value.Value;

import javafx.util.StringConverter;

/**
 * A {@link Value} based on a String
 * @param <T> the type of the actual value
 */
public interface StringValue<T> extends Value<T> {

  /**
   * @return the {@link StringConverter} used to convert the value to and from String
   */
  StringConverter<T> getConverter();
}
