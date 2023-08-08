/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

import is.codion.common.item.Item;
import is.codion.framework.domain.entity.Attribute;

import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

final class DefaultItemProperty<T> extends DefaultColumnProperty<T> implements ItemProperty<T> {

  private static final long serialVersionUID = 1;

  private static final String INVALID_ITEM_SUFFIX_KEY = "invalid_item_suffix";
  private static final String INVALID_ITEM_SUFFIX = ResourceBundle.getBundle(DefaultItemProperty.class.getName()).getString(INVALID_ITEM_SUFFIX_KEY);

  private final List<Item<T>> items;
  private final Map<T, Item<T>> itemMap;

  private DefaultItemProperty(DefaultItemPropertyBuilder<T, ?> builder) {
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

  static final class DefaultItemPropertyBuilder<T, B extends ColumnProperty.Builder<T, B>> extends DefaultColumnPropertyBuilder<T, B> {

    private final List<Item<T>> items;

    DefaultItemPropertyBuilder(Attribute<T> attribute, String caption, List<Item<T>> items) {
      super(attribute, caption);
      validateItems(items);
      this.items = items;
    }

    @Override
    public Property<T> build() {
      return new DefaultItemProperty<>(this);
    }

    private static <T> void validateItems(List<Item<T>> items) {
      if (requireNonNull(items).size() != items.stream()
              .map(new GetItemValue<>())
              .collect(Collectors.toSet())
              .size()) {
        throw new IllegalArgumentException("Item list contains duplicate values: " + items);
      }
    }
  }

  private static final class GetItemValue<T> implements Function<Item<T>, T> {

    @Override
    public T apply(Item<T> item) {
      return item.get();
    }
  }
}
