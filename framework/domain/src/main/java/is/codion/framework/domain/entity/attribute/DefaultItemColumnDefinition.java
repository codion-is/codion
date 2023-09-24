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
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Function;
import java.util.stream.Collectors;

final class DefaultItemColumnDefinition<T> extends DefaultColumnDefinition<T> implements ItemColumnDefinition<T> {

  private static final long serialVersionUID = 1;

  private static final String INVALID_ITEM_SUFFIX_KEY = "invalid_item_suffix";
  private static final String INVALID_ITEM_SUFFIX = ResourceBundle.getBundle(DefaultItemColumnDefinition.class.getName()).getString(INVALID_ITEM_SUFFIX_KEY);

  private final List<Item<T>> items;
  private final Map<T, Item<T>> itemMap;

  DefaultItemColumnDefinition(DefaultColumnDefinitionBuilder<T, ?> builder) {
    super(builder);
    this.items = builder.items;
    this.itemMap = items.stream()
            .collect(Collectors.toMap(Item::get, Function.identity()));
  }

  @Override
  public boolean isValid(T value) {
    return itemMap.containsKey(value);
  }

  @Override
  public List<Item<T>> items() {
    return items;
  }

  @Override
  public Item<T> item(T value) {
    Item<T> item = itemMap.get(value);
    if (item == null) {
      throw new IllegalArgumentException("Invalid item value: " + value);
    }

    return item;
  }

  @Override
  public String toString(T value) {
    Item<T> item = itemMap.get(value);
    if (item == null) {//invalid
      if (value == null && isNullable()) {
        //technically valid
        return "";
      }
      //mark invalid values
      return value + " <" + INVALID_ITEM_SUFFIX + ">";
    }

    return item.caption();
  }
}
