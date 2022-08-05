/*
 * Copyright (c) 2019 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

import is.codion.common.item.Item;

import java.util.List;

/**
 * A property based on a list of valid items.
 * @param <T> the value type
 */
public interface ItemProperty<T> extends ColumnProperty<T> {

  /**
   * @param value the value to validate
   * @return true if the given value is valid for this property
   */
  boolean isValid(T value);

  /**
   * @return an unmodifiable view of the available items
   */
  List<Item<T>> items();

  /**
   * @param value the value
   * @return the item associated with the given value
   * @throws IllegalArgumentException in case this value is not associated with a valid item
   */
  Item<T> getItem(T value);
}
