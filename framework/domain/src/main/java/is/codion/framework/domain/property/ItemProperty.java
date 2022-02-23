/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
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
  List<Item<T>> getItems();
}
