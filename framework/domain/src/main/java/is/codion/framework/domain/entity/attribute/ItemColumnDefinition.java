/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson.
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
