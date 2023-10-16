/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
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
  public boolean valid(T value) {
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
  public String string(T value) {
    Item<T> item = itemMap.get(value);
    if (item == null) {//invalid
      if (value == null && nullable()) {
        //technically valid
        return "";
      }
      //mark invalid values
      return value + " <" + INVALID_ITEM_SUFFIX + ">";
    }

    return item.caption();
  }
}
