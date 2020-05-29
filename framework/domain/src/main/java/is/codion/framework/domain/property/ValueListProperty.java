/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

import is.codion.common.item.Item;

import java.util.List;

/**
 * A property based on a list of values, each with a displayable caption.
 * @param <T> the value type
 */
public interface ValueListProperty<T> extends ColumnProperty {

  /**
   * @param value the value to validate
   * @return true if the given value is valid for this property
   */
  boolean isValid(T value);

  /**
   * @return an unmodifiable view of the available values
   */
  List<Item<T>> getValues();

  /**
   * @param value the value
   * @return the caption associated with the given value
   */
  String getCaption(T value);
}
