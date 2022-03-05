/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

import is.codion.common.item.Item;
import is.codion.framework.domain.entity.Attribute;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

final class DefaultItemProperty<T> extends DefaultColumnProperty<T> implements ItemProperty<T> {

  private static final long serialVersionUID = 1;

  private final List<Item<T>> items;
  private final Map<T, Item<T>> itemMap;

  private DefaultItemProperty(DefaultItemPropertyBuilder<T, ?> builder) {
    super(builder);
    this.items = builder.items;
    this.itemMap = items.stream()
            .collect(Collectors.toMap(Item::getValue, item -> item));
  }

  @Override
  public boolean isValid(T value) {
    return itemMap.containsKey(value);
  }

  @Override
  public List<Item<T>> getItems() {
    return items;
  }

  @Override
  public Item<T> getItem(T value) {
    Item<T> item = itemMap.get(value);
    if (item == null) {
      throw new IllegalArgumentException("Invalid item value: " + value);
    }

    return item;
  }

  static final class DefaultItemPropertyBuilder<T, B extends ColumnProperty.Builder<T, ItemProperty<T>, B>> extends DefaultColumnPropertyBuilder<T, ItemProperty<T>, B> {

    private final List<Item<T>> items;

    DefaultItemPropertyBuilder(Attribute<T> attribute, String caption, List<Item<T>> items) {
      super(attribute, caption);
      validateItems(items);
      this.items = items;
    }

    @Override
    public ItemProperty<T> build() {
      return new DefaultItemProperty<>(this);
    }

    private static <T> void validateItems(List<Item<T>> items) {
      if (requireNonNull(items).size() != items.stream()
              .map(Item::getValue)
              .collect(Collectors.toSet())
              .size()) {
        throw new IllegalArgumentException("Item list contains duplicate values: " + items);
      }
    }
  }
}
