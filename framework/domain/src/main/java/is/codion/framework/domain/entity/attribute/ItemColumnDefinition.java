/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity.attribute;

import is.codion.common.item.Item;

import java.util.List;

/**
 * An attribute based on a list of valid items.
 * @param <T> the value type
 */
public interface ItemColumnDefinition<T> extends ColumnDefinition<T> {

  /**
   * @param value the value to validate
   * @return true if the given value is valid for this attribute
   */
  boolean valid(T value);

  /**
   * @return an unmodifiable view of the available items
   */
  List<Item<T>> items();

  /**
   * @param value the value
   * @return the item associated with the given value
   * @throws IllegalArgumentException in case this value is not associated with a valid item
   */
  Item<T> item(T value);
}
